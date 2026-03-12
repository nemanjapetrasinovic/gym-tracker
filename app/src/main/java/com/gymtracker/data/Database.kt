package com.gymtracker.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ─── Entities ─────────────────────────────────────────────────────────────────

@Entity(tableName = "training_sessions")
data class TrainingSession(
    @PrimaryKey val date: String,          // ISO date string "2024-01-15"
    val isPersonalTraining: Boolean = false
)

@Entity(tableName = "pt_purchases")
data class PtPurchase(
    @PrimaryKey val month: String,  // "2026-03" ISO year-month
    val count: Int                  // total purchased that month
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface TrainingSessionDao {

    @Query("SELECT * FROM training_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions ORDER BY date DESC")
    suspend fun getAllSessionsOnce(): List<TrainingSession>

    @Query("SELECT * FROM training_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): TrainingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrainingSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<TrainingSession>)

    @Delete
    suspend fun deleteSession(session: TrainingSession)

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM training_sessions WHERE isPersonalTraining = 1")
    fun getPersonalTrainingCount(): Flow<Int>
}

@Dao
interface PtPurchaseDao {

    @Query("SELECT * FROM pt_purchases ORDER BY month DESC")
    fun getAllPurchases(): Flow<List<PtPurchase>>

    @Query("SELECT * FROM pt_purchases ORDER BY month DESC")
    suspend fun getAllPurchasesOnce(): List<PtPurchase>

    @Query("SELECT SUM(count) FROM pt_purchases")
    fun getTotalPurchased(): Flow<Int?>

    @Query("SELECT * FROM pt_purchases WHERE month = :month LIMIT 1")
    suspend fun getPurchaseByMonth(month: String): PtPurchase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchase(purchase: PtPurchase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(purchases: List<PtPurchase>)

    @Query("DELETE FROM pt_purchases WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM pt_purchases")
    suspend fun deleteAll()
}

// ─── Migration ────────────────────────────────────────────────────────────────

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pt_purchases` (" +
                "`month` TEXT NOT NULL, " +
                "`count` INTEGER NOT NULL, " +
                "PRIMARY KEY(`month`))"
        )
    }
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [TrainingSession::class, PtPurchase::class], version = 2, exportSchema = false)
abstract class GymDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun ptPurchaseDao(): PtPurchaseDao
}
