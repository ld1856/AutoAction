package com.autoaction.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        private val CONTROL_BAR_ALPHA = floatPreferencesKey("control_bar_alpha")
        private val SHORTCUT_ALPHA = floatPreferencesKey("shortcut_alpha")
        private val RECORDING_BAR_POSITION = intPreferencesKey("recording_bar_position")
        private val RECORDING_BAR_CUSTOM_X = intPreferencesKey("recording_bar_custom_x")
        private val RECORDING_BAR_CUSTOM_Y = intPreferencesKey("recording_bar_custom_y")
    }

    val settings: Flow<GlobalSettings> = context.dataStore.data.map { preferences ->
        GlobalSettings(
            randomizationEnabled = preferences[RANDOMIZATION_ENABLED] ?: false,
            clickOffsetRadius = preferences[CLICK_OFFSET_RADIUS] ?: 10,
            clickDurationVariance = preferences[CLICK_DURATION_VARIANCE] ?: 50,
            delayVariance = preferences[DELAY_VARIANCE] ?: 100,
            hapticFeedbackEnabled = preferences[HAPTIC_FEEDBACK_ENABLED] ?: true,
            controlBarAlpha = preferences[CONTROL_BAR_ALPHA] ?: 0.9f,
            shortcutAlpha = preferences[SHORTCUT_ALPHA] ?: 0.8f,
            recordingBarPosition = RecordingBarPosition.fromOrdinal(preferences[RECORDING_BAR_POSITION] ?: 0),
            recordingBarCustomX = preferences[RECORDING_BAR_CUSTOM_X] ?: 8,
            recordingBarCustomY = preferences[RECORDING_BAR_CUSTOM_Y] ?: 48
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

    suspend fun updateControlBarAlpha(alpha: Float) {
        context.dataStore.edit { preferences ->
            preferences[CONTROL_BAR_ALPHA] = alpha.coerceIn(0.1f, 1.0f)
        }
    }

    suspend fun updateShortcutAlpha(alpha: Float) {
        context.dataStore.edit { preferences ->
            preferences[SHORTCUT_ALPHA] = alpha.coerceIn(0.1f, 1.0f)
        }
    }

    suspend fun updateRecordingBarPosition(position: RecordingBarPosition) {
        context.dataStore.edit { preferences ->
            preferences[RECORDING_BAR_POSITION] = position.ordinal
        }
    }

    suspend fun updateRecordingBarCustomPosition(x: Int, y: Int) {
        context.dataStore.edit { preferences ->
            preferences[RECORDING_BAR_CUSTOM_X] = x
            preferences[RECORDING_BAR_CUSTOM_Y] = y
        }
    }
}
