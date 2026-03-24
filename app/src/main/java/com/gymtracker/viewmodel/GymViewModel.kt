package com.gymtracker.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.room.Room
import com.gymtracker.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Screen { Dashboard, Settings }

data class GymUiState(
    val membershipStartDate: String? = null,
    val membershipDaysRemaining: Int? = null,
    val membershipExpiryDate: String? = null,
    val membershipProgressFraction: Float = 0f,
    val personalTrainingsPurchased: Int = 0,
    val personalTrainingsUsed: Int = 0,
    val personalTrainingsRemaining: Int = 0,
    val todaySession: TrainingSession? = null,
    val allSessions: List<TrainingSession> = emptyList(),
    val selectedDate: String = LocalDate.now().toString(),
    val selectedDateFormatted: String = "",
    val selectedDateSession: TrainingSession? = null,
    val isSelectedDateToday: Boolean = true,
    val ptPurchases: List<PtPurchase> = emptyList(),
    val lastBackupTimestamp: Long? = null
)

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        GymDatabase::class.java,
        "gym_database"
    ).addMigrations(MIGRATION_1_2).build()

    private val prefs = GymPreferences(application)
    private val repository = GymRepository(db.trainingSessionDao(), db.ptPurchaseDao(), prefs)

    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val selectedDateDisplayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

    private val _selectedDate = MutableStateFlow(LocalDate.now())

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
        _selectedDate,
        combine(repository.allPtPurchases, repository.lastBackupTimestamp) { pt, ts -> pt to ts }
    ) { args ->
        val membershipStart = args[0] as String?
        val ptPurchased = args[1] as Int
        val ptUsed = args[2] as Int
        @Suppress("UNCHECKED_CAST")
        val sessions = args[3] as List<TrainingSession>
        val selectedDate = args[4] as LocalDate
        @Suppress("UNCHECKED_CAST")
        val extra = args[5] as Pair<List<PtPurchase>, Long?>
        val ptPurchases = extra.first
        val lastBackup = extra.second

        val today = LocalDate.now().toString()
        val todaySession = sessions.find { it.date == today }
        val selectedDateStr = selectedDate.toString()
        val selectedDateSession = sessions.find { it.date == selectedDateStr }

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

        GymUiState(
            membershipStartDate = membershipStart,
            membershipDaysRemaining = daysRemaining,
            membershipExpiryDate = expiryDate,
            membershipProgressFraction = progressFraction,
            personalTrainingsPurchased = ptPurchased,
            personalTrainingsUsed = ptUsed,
            personalTrainingsRemaining = maxOf(0, ptPurchased - ptUsed),
            todaySession = todaySession,
            allSessions = sessions,
            selectedDate = selectedDateStr,
            selectedDateFormatted = selectedDate.format(selectedDateDisplayFormatter),
            selectedDateSession = selectedDateSession,
            isSelectedDateToday = selectedDate == LocalDate.now(),
            ptPurchases = ptPurchases,
            lastBackupTimestamp = lastBackup
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GymUiState())

    fun setSelectedDate(date: LocalDate) {
        if (!date.isAfter(LocalDate.now())) {
            _selectedDate.value = date
        }
    }

    fun checkIn(isPersonalTraining: Boolean) {
        viewModelScope.launch {
            repository.checkInToday(_selectedDate.value.toString(), isPersonalTraining)
        }
    }

    fun removeSession() {
        viewModelScope.launch {
            repository.removeSession(_selectedDate.value.toString())
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

    fun addPtPurchase(count: Int) {
        viewModelScope.launch {
            repository.addPtPurchase(count)
        }
    }

    suspend fun createBackupCsv(fromDate: String, toDate: String): String {
        val allSessions = repository.getAllSessionsOnce()
        val allPurchases = repository.getAllPtPurchasesOnce()
        val membershipStart = uiState.value.membershipStartDate

        val sessions = allSessions.filter { it.date in fromDate..toDate }
        val fromMonth = fromDate.substring(0, 7)
        val toMonth = toDate.substring(0, 7)
        val purchases = allPurchases.filter { it.month in fromMonth..toMonth }

        return buildString {
            appendLine("# GymTracker Backup")
            appendLine("# membershipStartDate=${membershipStart ?: ""}")
            appendLine()
            appendLine("[sessions]")
            appendLine("date,isPersonalTraining")
            sessions.forEach { s ->
                appendLine("${s.date},${s.isPersonalTraining}")
            }
            appendLine()
            appendLine("[ptPurchases]")
            appendLine("month,count")
            purchases.forEach { p ->
                appendLine("${p.month},${p.count}")
            }
        }
    }

    suspend fun createBackupCsv(): String {
        val sessions = repository.getAllSessionsOnce()
        val purchases = repository.getAllPtPurchasesOnce()
        val membershipStart = uiState.value.membershipStartDate

        return buildString {
            appendLine("# GymTracker Backup")
            appendLine("# membershipStartDate=${membershipStart ?: ""}")
            appendLine()
            appendLine("[sessions]")
            appendLine("date,isPersonalTraining")
            sessions.forEach { s ->
                appendLine("${s.date},${s.isPersonalTraining}")
            }
            appendLine()
            appendLine("[ptPurchases]")
            appendLine("month,count")
            purchases.forEach { p ->
                appendLine("${p.month},${p.count}")
            }
        }
    }

    fun restoreFromCsv(csv: String) {
        viewModelScope.launch {
            val lines = csv.lines()
            var membershipStart: String? = null
            val sessions = mutableListOf<TrainingSession>()
            val purchases = mutableListOf<PtPurchase>()
            var section = ""

            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("# membershipStartDate=") -> {
                        val value = trimmed.substringAfter("=").trim()
                        if (value.isNotEmpty()) membershipStart = value
                    }
                    trimmed == "[sessions]" -> { section = "sessions" }
                    trimmed == "[ptPurchases]" -> { section = "ptPurchases" }
                    trimmed.isEmpty() || trimmed.startsWith("#") -> {}
                    trimmed.startsWith("date,") || trimmed.startsWith("month,") -> {}
                    section == "sessions" -> {
                        val parts = trimmed.split(",", limit = 2)
                        if (parts.size == 2) {
                            sessions.add(TrainingSession(
                                date = parts[0],
                                isPersonalTraining = parts[1].toBooleanStrictOrNull() ?: false
                            ))
                        }
                    }
                    section == "ptPurchases" -> {
                        val parts = trimmed.split(",", limit = 2)
                        if (parts.size == 2) {
                            val count = parts[1].toIntOrNull() ?: 0
                            if (count > 0) purchases.add(PtPurchase(month = parts[0], count = count))
                        }
                    }
                }
            }

            repository.restoreData(sessions, purchases, membershipStart)
        }
    }

    fun setLastBackupTimestamp(ts: Long) {
        viewModelScope.launch {
            repository.setLastBackupTimestamp(ts)
        }
    }
}
