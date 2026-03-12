package com.gymtracker.data

import kotlinx.coroutines.flow.Flow

class GymRepository(
    private val dao: TrainingSessionDao,
    private val prefs: GymPreferences
) {
    val allSessions: Flow<List<TrainingSession>> = dao.getAllSessions()
    val personalTrainingCount: Flow<Int> = dao.getPersonalTrainingCount()
    val membershipStartDate: Flow<String?> = prefs.membershipStartDate
    val personalTrainingsPurchased: Flow<Int> = prefs.personalTrainingsPurchased

    suspend fun checkInToday(date: String, isPersonalTraining: Boolean) {
        val existing = dao.getSessionByDate(date)
        if (existing != null) {
            // Toggle off if same type, update if different type
            if (existing.isPersonalTraining == isPersonalTraining) {
                dao.deleteSession(existing)
            } else {
                dao.insertSession(existing.copy(isPersonalTraining = isPersonalTraining))
            }
        } else {
            dao.insertSession(TrainingSession(date = date, isPersonalTraining = isPersonalTraining))
        }
    }

    suspend fun removeSession(date: String) {
        val session = dao.getSessionByDate(date)
        session?.let { dao.deleteSession(it) }
    }

    suspend fun getSessionByDate(date: String): TrainingSession? = dao.getSessionByDate(date)

    suspend fun setMembershipStartDate(date: String) = prefs.setMembershipStartDate(date)
    suspend fun setPersonalTrainingsPurchased(count: Int) = prefs.setPersonalTrainingsPurchased(count)
}
