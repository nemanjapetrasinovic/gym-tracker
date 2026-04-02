package com.gymtracker.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "training_sessions")
data class TrainingSession(
    @PrimaryKey val date: String,
    val isPersonalTraining: Boolean = false
)

@Entity(tableName = "pt_purchases")
data class PtPurchase(
    @PrimaryKey val month: String,
    val count: Int
)

@Entity(tableName = "pt_carry_seeds")
data class PtCarrySeed(
    @PrimaryKey val month: String,
    val count: Int
)

data class MonthCount(val month: String, val count: Int)

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

    @Query("SELECT strftime('%Y-%m', date) as month, COUNT(*) as count FROM training_sessions WHERE isPersonalTraining = 1 GROUP BY strftime('%Y-%m', date)")
    fun getPtUsedByMonth(): Flow<List<MonthCount>>
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

@Dao
interface PtCarrySeedDao {

    @Query("SELECT * FROM pt_carry_seeds ORDER BY month DESC")
    fun getAllCarrySeeds(): Flow<List<PtCarrySeed>>

    @Query("SELECT * FROM pt_carry_seeds ORDER BY month DESC")
    suspend fun getAllCarrySeedsOnce(): List<PtCarrySeed>

    @Query("SELECT * FROM pt_carry_seeds WHERE month = :month LIMIT 1")
    suspend fun getCarrySeedByMonth(month: String): PtCarrySeed?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCarrySeed(seed: PtCarrySeed)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(seeds: List<PtCarrySeed>)

    @Query("DELETE FROM pt_carry_seeds WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM pt_carry_seeds")
    suspend fun deleteAll()
}

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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pt_carry_seeds` (" +
                "`month` TEXT NOT NULL, " +
                "`count` INTEGER NOT NULL, " +
                "PRIMARY KEY(`month`))"
        )
    }
}

@Database(
    entities = [TrainingSession::class, PtPurchase::class, PtCarrySeed::class],
    version = 3,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun ptPurchaseDao(): PtPurchaseDao
    abstract fun ptCarrySeedDao(): PtCarrySeedDao
}
