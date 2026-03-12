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
    val currentStreak: Int = 0,
    val selectedDate: String = LocalDate.now().toString(),
    val selectedDateFormatted: String = "",
    val selectedDateSession: TrainingSession? = null,
    val isSelectedDateToday: Boolean = true
)

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        GymDatabase::class.java,
        "gym_database"
    ).build()

    private val prefs = GymPreferences(application)
    private val repository = GymRepository(db.trainingSessionDao(), prefs)

    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val selectedDateDisplayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<GymUiState> = combine(
        repository.membershipStartDate,
        repository.personalTrainingsPurchased,
        repository.personalTrainingCount,
        repository.allSessions,
        _selectedDate
    ) { args ->
        val membershipStart = args[0] as String?
        val ptPurchased = args[1] as Int
        val ptUsed = args[2] as Int
        @Suppress("UNCHECKED_CAST")
        val sessions = args[3] as List<TrainingSession>
        val selectedDate = args[4] as LocalDate

        val today = LocalDate.now().toString()
        val todaySession = sessions.find { it.date == today }
        val selectedDateStr = selectedDate.toString()
        val selectedDateSession = sessions.find { it.date == selectedDateStr }

        // Membership calculations
        var daysRemaining: Int? = null
        var expiryDate: String? = null
        var progressFraction = 0f
        if (membershipStart != null) {
            val startDate = LocalDate.parse(membershipStart)
            val expiry = startDate.plusDays(30)
            val now = LocalDate.now()
            daysRemaining = maxOf(0, java.time.temporal.ChronoUnit.DAYS.between(now, expiry).toInt())
            expiryDate = expiry.format(displayFormatter)
            val elapsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, now).toInt().coerceIn(0, 30)
            progressFraction = elapsed / 30f
        }

        // Streak calculation
        val streak = calculateStreak(sessions)

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
            currentStreak = streak,
            selectedDate = selectedDateStr,
            selectedDateFormatted = selectedDate.format(selectedDateDisplayFormatter),
            selectedDateSession = selectedDateSession,
            isSelectedDateToday = selectedDate == LocalDate.now()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GymUiState())

    private fun calculateStreak(sessions: List<TrainingSession>): Int {
        if (sessions.isEmpty()) return 0
        val dates = sessions.map { LocalDate.parse(it.date) }.sortedDescending()
        var streak = 0
        var checkDate = LocalDate.now()
        // Allow today or yesterday as streak start
        if (dates.first() != checkDate && dates.first() != checkDate.minusDays(1)) return 0
        if (dates.first() == checkDate.minusDays(1)) checkDate = checkDate.minusDays(1)
        for (date in dates) {
            if (date == checkDate) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else break
        }
        return streak
    }

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

    fun setMembershipStartDate(date: String) {
        viewModelScope.launch {
            repository.setMembershipStartDate(date)
        }
    }

    fun setPersonalTrainingsPurchased(count: Int) {
        viewModelScope.launch {
            repository.setPersonalTrainingsPurchased(count)
        }
    }
}
