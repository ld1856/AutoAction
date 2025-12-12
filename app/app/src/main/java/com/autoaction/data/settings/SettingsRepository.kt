package com.autoaction.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val RANDOMIZATION_ENABLED = booleanPreferencesKey("randomization_enabled")
        private val CLICK_OFFSET_RADIUS = intPreferencesKey("click_offset_radius")
        private val CLICK_DURATION_VARIANCE = longPreferencesKey("click_duration_variance")
        private val DELAY_VARIANCE = longPreferencesKey("delay_variance")
        private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
    }

    val settings: Flow<GlobalSettings> = context.dataStore.data.map { preferences ->
        GlobalSettings(
            randomizationEnabled = preferences[RANDOMIZATION_ENABLED] ?: false,
            clickOffsetRadius = preferences[CLICK_OFFSET_RADIUS] ?: 10,
            clickDurationVariance = preferences[CLICK_DURATION_VARIANCE] ?: 50,
            delayVariance = preferences[DELAY_VARIANCE] ?: 100,
            hapticFeedbackEnabled = preferences[HAPTIC_FEEDBACK_ENABLED] ?: true
        )
    }

    suspend fun updateRandomizationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RANDOMIZATION_ENABLED] = enabled
        }
    }

    suspend fun updateClickOffsetRadius(radius: Int) {
        context.dataStore.edit { preferences ->
            preferences[CLICK_OFFSET_RADIUS] = radius
        }
    }

    suspend fun updateClickDurationVariance(variance: Long) {
        context.dataStore.edit { preferences ->
            preferences[CLICK_DURATION_VARIANCE] = variance
        }
    }

    suspend fun updateDelayVariance(variance: Long) {
        context.dataStore.edit { preferences ->
            preferences[DELAY_VARIANCE] = variance
        }
    }

    suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }
}
