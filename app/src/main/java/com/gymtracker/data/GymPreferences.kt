package com.gymtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_prefs")

class GymPreferences(private val context: Context) {

    companion object {
        val MEMBERSHIP_START_DATE = stringPreferencesKey("membership_start_date")
        val PERSONAL_TRAININGS_PURCHASED = intPreferencesKey("personal_trainings_purchased")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val PT_MIGRATION_DONE = booleanPreferencesKey("pt_migration_done")
    }

    val membershipStartDate: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[MEMBERSHIP_START_DATE]
    }

    val personalTrainingsPurchased: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PERSONAL_TRAININGS_PURCHASED] ?: 0
    }

    val lastBackupTimestamp: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[LAST_BACKUP_TIMESTAMP]
    }

    suspend fun setMembershipStartDate(date: String) {
        context.dataStore.edit { prefs ->
            prefs[MEMBERSHIP_START_DATE] = date
        }
    }

    suspend fun setPersonalTrainingsPurchased(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[PERSONAL_TRAININGS_PURCHASED] = count
        }
    }

    suspend fun setLastBackupTimestamp(ts: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_BACKUP_TIMESTAMP] = ts
        }
    }

    suspend fun isPtMigrationDone(): Boolean {
        return context.dataStore.data.first()[PT_MIGRATION_DONE] ?: false
    }

    suspend fun markPtMigrationDone() {
        context.dataStore.edit { prefs ->
            prefs[PT_MIGRATION_DONE] = true
        }
    }

    suspend fun getPersonalTrainingsPurchasedOnce(): Int {
        return context.dataStore.data.first()[PERSONAL_TRAININGS_PURCHASED] ?: 0
    }

    suspend fun clearPersonalTrainingsPurchased() {
        context.dataStore.edit { prefs ->
            prefs.remove(PERSONAL_TRAININGS_PURCHASED)
        }
    }
}
