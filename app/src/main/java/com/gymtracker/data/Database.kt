package com.gymtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ─── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "training_sessions")
data class TrainingSession(
    @PrimaryKey val date: String,          // ISO date string "2024-01-15"
    val isPersonalTraining: Boolean = false
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface TrainingSessionDao {

    @Query("SELECT * FROM training_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): TrainingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrainingSession)

    @Delete
    suspend fun deleteSession(session: TrainingSession)

    @Query("SELECT COUNT(*) FROM training_sessions WHERE isPersonalTraining = 1")
    fun getPersonalTrainingCount(): Flow<Int>
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [TrainingSession::class], version = 1, exportSchema = false)
abstract class GymDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao
}
