package com.gymtracker.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.room.Room
import com.gymtracker.data.GymDatabase
import com.gymtracker.data.GymPreferences
import com.gymtracker.data.GymRepository
import com.gymtracker.data.MIGRATION_1_2
import com.gymtracker.data.MIGRATION_2_3
import com.gymtracker.data.MonthCount
import com.gymtracker.data.PtCarrySeed
import com.gymtracker.data.PtPurchase
import com.gymtracker.data.TrainingSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Screen { Dashboard, Settings }

enum class PtCarrySource { None, Derived, Manual }

data class PtMonthBreakdown(
    val month: String,      // "YYYY-MM"
    val purchased: Int,
    val carriedIn: Int,
    val available: Int,
    val used: Int,
    val overused: Int,
    val carriedOut: Int,     // max(0, purchased + carriedIn - used)
    val manualCarrySeed: Int? = null,
    val hasPreviousHistory: Boolean = false,
    val carrySource: PtCarrySource = PtCarrySource.None,
    val manualCarryIgnored: Boolean = false
)

data class GymUiState(
    val membershipStartDate: String? = null,
    val membershipDaysRemaining: Int? = null,
    val membershipExpiryDate: String? = null,
    val membershipProgressFraction: Float = 0f,
    val personalTrainingsPurchased: Int = 0,
    val personalTrainingsUsed: Int = 0,
    val ptAvailableThisMonth: Int = 0,
    val ptUsedThisMonth: Int = 0,
    val ptRemainingThisMonth: Int = 0,
    val ptPurchasedThisMonth: Int = 0,
    val ptCarriedOver: Int = 0,
    val ptOverusedThisMonth: Int = 0,
    val ptTotalOverused: Int = 0,
    val ptMonthlyBreakdown: List<PtMonthBreakdown> = emptyList(),
    val ptEditorMonth: String = YearMonth.now().toString(),
    val ptSelectedMonthBreakdown: PtMonthBreakdown? = null,
    val todaySession: TrainingSession? = null,
    val allSessions: List<TrainingSession> = emptyList(),
    val ptCarrySeeds: List<PtCarrySeed> = emptyList(),
    val ptPurchases: List<PtPurchase> = emptyList(),
    val lastBackupTimestamp: Long? = null
)

data class BackupPayload(
    val sessions: List<TrainingSession>,
    val purchases: List<PtPurchase>,
    val carrySeeds: List<PtCarrySeed>,
    val membershipStart: String?
)

fun buildBackupCsv(payload: BackupPayload): String {
    return buildString {
        appendLine("# GymTracker Backup")
        appendLine("# membershipStartDate=${payload.membershipStart ?: ""}")
        appendLine()
        appendLine("[sessions]")
        appendLine("date,isPersonalTraining")
        payload.sessions.forEach { s ->
            appendLine("${s.date},${s.isPersonalTraining}")
        }
        appendLine()
        appendLine("[ptPurchases]")
        appendLine("month,count")
        payload.purchases.forEach { p ->
            appendLine("${p.month},${p.count}")
        }
        appendLine()
        appendLine("[ptCarrySeeds]")
        appendLine("month,count")
        payload.carrySeeds.forEach { seed ->
            appendLine("${seed.month},${seed.count}")
        }
    }
}

fun parseBackupCsv(csv: String): BackupPayload {
    val lines = csv.lines()
    var membershipStart: String? = null
    val sessions = mutableListOf<TrainingSession>()
    val purchases = mutableListOf<PtPurchase>()
    val carrySeeds = mutableListOf<PtCarrySeed>()
    var section = ""

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("# membershipStartDate=") -> {
                val value = trimmed.substringAfter("=").trim()
                membershipStart = value.ifEmpty { null }
            }
            trimmed == "[sessions]" -> { section = "sessions" }
            trimmed == "[ptPurchases]" -> { section = "ptPurchases" }
            trimmed == "[ptCarrySeeds]" -> { section = "ptCarrySeeds" }
            trimmed.isEmpty() || trimmed.startsWith("#") -> {}
            trimmed.startsWith("date,") || trimmed.startsWith("month,") -> {}
            section == "sessions" -> {
                val parts = trimmed.split(",", limit = 2)
                if (parts.size == 2) {
                    sessions.add(
                        TrainingSession(
                            date = parts[0],
                            isPersonalTraining = parts[1].toBooleanStrictOrNull() ?: false
                        )
                    )
                }
            }
            section == "ptPurchases" -> {
                val parts = trimmed.split(",", limit = 2)
                if (parts.size == 2) {
                    val count = parts[1].toIntOrNull() ?: 0
                    if (count > 0) purchases.add(PtPurchase(month = parts[0], count = count))
                }
            }
            section == "ptCarrySeeds" -> {
                val parts = trimmed.split(",", limit = 2)
                if (parts.size == 2) {
                    val count = parts[1].toIntOrNull() ?: 0
                    if (count > 0) carrySeeds.add(PtCarrySeed(month = parts[0], count = count))
                }
            }
        }
    }

    return BackupPayload(
        sessions = sessions,
        purchases = purchases,
        carrySeeds = carrySeeds,
        membershipStart = membershipStart
    )
}

fun buildRangeBackupPayload(
    allSessions: List<TrainingSession>,
    allPurchases: List<PtPurchase>,
    allCarrySeeds: List<PtCarrySeed>,
    membershipStart: String?,
    fromDate: String,
    toDate: String
): BackupPayload {
    val sessions = allSessions.filter { it.date in fromDate..toDate }
    val fromMonth = fromDate.substring(0, 7)
    val toMonth = toDate.substring(0, 7)
    val purchases = allPurchases.filter { it.month in fromMonth..toMonth }
    val ptUsedByMonth = allSessions
        .asSequence()
        .filter { it.isPersonalTraining }
        .groupingBy { it.date.substring(0, 7) }
        .eachCount()
        .map { MonthCount(month = it.key, count = it.value) }
    val breakdown = computePtCarryover(
        purchases = allPurchases,
        usedByMonth = ptUsedByMonth,
        carrySeeds = allCarrySeeds,
        selectedMonth = fromMonth,
        currentMonth = toMonth
    )
    val carrySeedMap = linkedMapOf<String, PtCarrySeed>()
    breakdown.filter { it.month in fromMonth..toMonth }.forEach { entry ->
        if (entry.month == fromMonth) {
            if (entry.carriedIn > 0) {
                carrySeedMap[entry.month] = PtCarrySeed(entry.month, entry.carriedIn)
            }
        } else if (entry.carrySource == PtCarrySource.Manual && entry.carriedIn > 0) {
            carrySeedMap[entry.month] = PtCarrySeed(entry.month, entry.carriedIn)
        }
    }

    return BackupPayload(
        sessions = sessions,
        purchases = purchases,
        carrySeeds = carrySeedMap.values.toList(),
        membershipStart = membershipStart
    )
}

fun computePtCarryover(
    purchases: List<PtPurchase>,
    usedByMonth: List<MonthCount>,
    carrySeeds: List<PtCarrySeed>,
    selectedMonth: String,
    currentMonth: String
): List<PtMonthBreakdown> {
    val purchaseMap = purchases.associateBy { it.month }
    val usedMap = usedByMonth.associateBy({ it.month }, { it.count })
    val carrySeedMap = carrySeeds.associateBy({ it.month }, { it.count })
    val actualMonthKeys = (purchaseMap.keys + usedMap.keys + carrySeedMap.keys).toSortedSet()
    val start = when {
        actualMonthKeys.isEmpty() -> YearMonth.parse(selectedMonth)
        YearMonth.parse(selectedMonth).isBefore(YearMonth.parse(actualMonthKeys.first())) -> YearMonth.parse(selectedMonth)
        else -> YearMonth.parse(actualMonthKeys.first())
    }

    val breakdown = mutableListOf<PtMonthBreakdown>()
    var carryover = 0
    var month = start
    val current = YearMonth.parse(currentMonth)
    var hasPreviousHistory = false

    while (!month.isAfter(current)) {
        val monthStr = month.toString()
        val purchased = purchaseMap[monthStr]?.count ?: 0
        val used = usedMap[monthStr] ?: 0
        val manualCarrySeed = carrySeedMap[monthStr]
        val carriedIn = if (!hasPreviousHistory && manualCarrySeed != null) manualCarrySeed else carryover
        val carrySource = when {
            !hasPreviousHistory && manualCarrySeed != null -> PtCarrySource.Manual
            carriedIn > 0 -> PtCarrySource.Derived
            else -> PtCarrySource.None
        }
        val manualCarryIgnored = hasPreviousHistory && manualCarrySeed != null
        val available = purchased + carriedIn
        val overused = maxOf(0, used - available)
        val carriedOut = maxOf(0, available - used)
        breakdown.add(
            PtMonthBreakdown(
                month = monthStr,
                purchased = purchased,
                carriedIn = carriedIn,
                available = available,
                used = used,
                overused = overused,
                carriedOut = carriedOut,
                manualCarrySeed = manualCarrySeed,
                hasPreviousHistory = hasPreviousHistory,
                carrySource = carrySource,
                manualCarryIgnored = manualCarryIgnored
            )
        )
        if (purchaseMap.containsKey(monthStr) || usedMap.containsKey(monthStr) || carrySeedMap.containsKey(monthStr)) {
            hasPreviousHistory = true
        }
        carryover = carriedOut
        month = month.plusMonths(1)
    }

    return breakdown
}

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        GymDatabase::class.java,
        "gym_database"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

    private val prefs = GymPreferences(application)
    private val repository = GymRepository(
        db.trainingSessionDao(),
        db.ptPurchaseDao(),
        db.ptCarrySeedDao(),
        prefs
    )

    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val _selectedPtEditorMonth = MutableStateFlow(YearMonth.now())

    private val _currentScreen = MutableStateFlow(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) { _currentScreen.value = screen }

    init {
        viewModelScope.launch {
            repository.runMigrationIfNeeded()
        }
    }

    val uiState: StateFlow<GymUiState> = combine(
        repository.membershipStartDate,
        repository.totalPtPurchased,
        repository.personalTrainingCount,
        repository.allSessions,
        _selectedPtEditorMonth,
        combine(
            repository.allPtPurchases,
            repository.allPtCarrySeeds,
            repository.lastBackupTimestamp,
            repository.ptUsedByMonth
        ) { pt, carrySeeds, ts, usedByMonth -> listOf(pt, carrySeeds, ts, usedByMonth) }
    ) { args ->
        val membershipStart = args[0] as String?
        val ptPurchased = args[1] as Int
        val ptUsed = args[2] as Int
        @Suppress("UNCHECKED_CAST")
        val sessions = args[3] as List<TrainingSession>
        val selectedPtEditorMonth = args[4] as YearMonth
        @Suppress("UNCHECKED_CAST")
        val extra = args[5] as List<Any?>
        @Suppress("UNCHECKED_CAST")
        val ptPurchases = extra[0] as List<PtPurchase>
        @Suppress("UNCHECKED_CAST")
        val ptCarrySeeds = extra[1] as List<PtCarrySeed>
        val lastBackup = extra[2] as Long?
        @Suppress("UNCHECKED_CAST")
        val ptUsedByMonth = extra[3] as List<MonthCount>

        val today = LocalDate.now().toString()
        val todaySession = sessions.find { it.date == today }

        var daysRemaining: Int? = null
        var expiryDate: String? = null
        var progressFraction = 0f
        if (membershipStart != null) {
            val startDate = LocalDate.parse(membershipStart)
            val expiry = startDate.plusMonths(1)
            val now = LocalDate.now()
            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, expiry).toInt()
            daysRemaining = maxOf(0, java.time.temporal.ChronoUnit.DAYS.between(now, expiry).toInt())
            expiryDate = expiry.format(displayFormatter)
            val elapsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, now).toInt().coerceIn(0, totalDays)
            progressFraction = elapsed / totalDays.toFloat()
        }

        val currentMonth = YearMonth.now().toString()
        val monthlyBreakdown = computePtCarryover(
            purchases = ptPurchases,
            usedByMonth = ptUsedByMonth,
            carrySeeds = ptCarrySeeds,
            selectedMonth = selectedPtEditorMonth.toString(),
            currentMonth = currentMonth
        )
        val currentMonthBreakdown = monthlyBreakdown.lastOrNull { it.month == currentMonth }
        val selectedMonthBreakdown = monthlyBreakdown.lastOrNull { it.month == selectedPtEditorMonth.toString() }
        val totalOverused = monthlyBreakdown.sumOf { it.overused }

        GymUiState(
            membershipStartDate = membershipStart,
            membershipDaysRemaining = daysRemaining,
            membershipExpiryDate = expiryDate,
            membershipProgressFraction = progressFraction,
            personalTrainingsPurchased = ptPurchased,
            personalTrainingsUsed = ptUsed,
            ptAvailableThisMonth = currentMonthBreakdown?.available ?: 0,
            ptUsedThisMonth = currentMonthBreakdown?.used ?: 0,
            ptRemainingThisMonth = currentMonthBreakdown?.carriedOut ?: 0,
            ptPurchasedThisMonth = currentMonthBreakdown?.purchased ?: 0,
            ptCarriedOver = currentMonthBreakdown?.carriedIn ?: 0,
            ptOverusedThisMonth = currentMonthBreakdown?.overused ?: 0,
            ptTotalOverused = totalOverused,
            ptMonthlyBreakdown = monthlyBreakdown,
            ptEditorMonth = selectedPtEditorMonth.toString(),
            ptSelectedMonthBreakdown = selectedMonthBreakdown,
            todaySession = todaySession,
            allSessions = sessions,
            ptCarrySeeds = ptCarrySeeds,
            ptPurchases = ptPurchases,
            lastBackupTimestamp = lastBackup
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GymUiState())

    fun setSelectedPtEditorMonth(month: YearMonth) {
        if (!month.isAfter(YearMonth.now())) {
            _selectedPtEditorMonth.value = month
        }
    }

    fun checkIn(isPersonalTraining: Boolean) {
        viewModelScope.launch {
            repository.checkInToday(LocalDate.now().toString(), isPersonalTraining)
        }
    }

    fun removeSession() {
        viewModelScope.launch {
            repository.removeSession(LocalDate.now().toString())
        }
    }

    fun removeSessionForDate(date: String) {
        viewModelScope.launch {
            repository.removeSession(date)
        }
    }

    fun updateSessionType(date: String, isPersonalTraining: Boolean) {
        viewModelScope.launch {
            repository.updateSessionType(date, isPersonalTraining)
        }
    }

    fun setMembershipStartDate(date: String) {
        viewModelScope.launch {
            repository.setMembershipStartDate(date)
        }
    }

    fun setPtPurchaseForMonth(month: String, count: Int) {
        viewModelScope.launch {
            repository.setPtPurchaseForMonth(month, count)
        }
    }

    fun setPtCarrySeedForMonth(month: String, count: Int) {
        viewModelScope.launch {
            repository.setPtCarrySeedForMonth(month, count)
        }
    }

    fun checkInForDate(date: String, isPersonalTraining: Boolean) {
        viewModelScope.launch {
            repository.checkInToday(date, isPersonalTraining)
        }
    }

    fun addPtPurchase(count: Int) {
        viewModelScope.launch {
            repository.addPtPurchase(count)
        }
    }

    suspend fun createBackupCsv(fromDate: String, toDate: String): String {
        val allSessions = repository.getAllSessionsOnce()
        val allPurchases = repository.getAllPtPurchasesOnce()
        val allCarrySeeds = repository.getAllPtCarrySeedsOnce()
        val membershipStart = uiState.value.membershipStartDate
        return buildBackupCsv(
            buildRangeBackupPayload(
                allSessions = allSessions,
                allPurchases = allPurchases,
                allCarrySeeds = allCarrySeeds,
                membershipStart = membershipStart,
                fromDate = fromDate,
                toDate = toDate
            )
        )
    }

    suspend fun createBackupCsv(): String {
        val sessions = repository.getAllSessionsOnce()
        val purchases = repository.getAllPtPurchasesOnce()
        val carrySeeds = repository.getAllPtCarrySeedsOnce()
        val membershipStart = uiState.value.membershipStartDate
        return buildBackupCsv(
            BackupPayload(
                sessions = sessions,
                purchases = purchases,
                carrySeeds = carrySeeds,
                membershipStart = membershipStart
            )
        )
    }

    suspend fun restoreFromCsv(csv: String) {
        val backup = parseBackupCsv(csv)
        repository.restoreData(
            sessions = backup.sessions,
            purchases = backup.purchases,
            carrySeeds = backup.carrySeeds,
            membershipStart = backup.membershipStart
        )
    }

    fun setLastBackupTimestamp(ts: Long) {
        viewModelScope.launch {
            repository.setLastBackupTimestamp(ts)
        }
    }
}
