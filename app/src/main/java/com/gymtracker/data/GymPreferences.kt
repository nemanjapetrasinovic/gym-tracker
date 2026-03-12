package com.gymtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_prefs")

class GymPreferences(private val context: Context) {

    companion object {
        val MEMBERSHIP_START_DATE = stringPreferencesKey("membership_start_date")
        val PERSONAL_TRAININGS_PURCHASED = intPreferencesKey("personal_trainings_purchased")
    }

    val membershipStartDate: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[MEMBERSHIP_START_DATE]
    }

    val personalTrainingsPurchased: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PERSONAL_TRAININGS_PURCHASED] ?: 0
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
}
