package com.gymtracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

class GymRepository(
    private val dao: TrainingSessionDao,
    private val ptDao: PtPurchaseDao,
    private val prefs: GymPreferences
) {
    val allSessions: Flow<List<TrainingSession>> = dao.getAllSessions()
    val personalTrainingCount: Flow<Int> = dao.getPersonalTrainingCount()
    val membershipStartDate: Flow<String?> = prefs.membershipStartDate
    val totalPtPurchased: Flow<Int> = ptDao.getTotalPurchased().map { it ?: 0 }
    val allPtPurchases: Flow<List<PtPurchase>> = ptDao.getAllPurchases()
    val lastBackupTimestamp: Flow<Long?> = prefs.lastBackupTimestamp

    suspend fun checkInToday(date: String, isPersonalTraining: Boolean) {
        val existing = dao.getSessionByDate(date)
        if (existing != null) {
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

    suspend fun updateSessionType(date: String, isPersonalTraining: Boolean) {
        val existing = dao.getSessionByDate(date)
        if (existing != null) {
            dao.insertSession(existing.copy(isPersonalTraining = isPersonalTraining))
        }
    }

    suspend fun getSessionByDate(date: String): TrainingSession? = dao.getSessionByDate(date)

    suspend fun setMembershipStartDate(date: String) = prefs.setMembershipStartDate(date)

    suspend fun setPtPurchaseForMonth(month: String, count: Int) {
        if (count <= 0) {
            ptDao.deleteByMonth(month)
        } else {
            ptDao.upsertPurchase(PtPurchase(month = month, count = count))
        }
    }

    suspend fun addPtPurchase(count: Int) {
        val month = YearMonth.now().toString()
        val existing = ptDao.getPurchaseByMonth(month)
        val newTotal = (existing?.count ?: 0) + count
        ptDao.upsertPurchase(PtPurchase(month = month, count = newTotal))
    }

    suspend fun getAllSessionsOnce(): List<TrainingSession> = dao.getAllSessionsOnce()
    suspend fun getAllPtPurchasesOnce(): List<PtPurchase> = ptDao.getAllPurchasesOnce()

    suspend fun restoreData(
        sessions: List<TrainingSession>,
        purchases: List<PtPurchase>,
        membershipStart: String?
    ) {
        dao.deleteAll()
        ptDao.deleteAll()
        dao.insertAll(sessions)
        ptDao.insertAll(purchases)
        if (membershipStart != null) {
            prefs.setMembershipStartDate(membershipStart)
        }
    }

    suspend fun setLastBackupTimestamp(ts: Long) = prefs.setLastBackupTimestamp(ts)

    suspend fun runMigrationIfNeeded() {
        if (prefs.isPtMigrationDone()) return
        val oldCount = prefs.getPersonalTrainingsPurchasedOnce()
        if (oldCount > 0) {
            val currentMonth = YearMonth.now().toString()
            ptDao.upsertPurchase(PtPurchase(month = currentMonth, count = oldCount))
        }
        prefs.clearPersonalTrainingsPurchased()
        prefs.markPtMigrationDone()
    }
}
