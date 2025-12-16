package com.autoaction.data.settings

enum class RecordingBarPosition(val displayName: String) {
    TOP_LEFT("Top Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right"),
    CENTER_LEFT("Center Left"),
    CENTER("Center"),
    CENTER_RIGHT("Center Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right"),
    CUSTOM("Custom");

    companion object {
        fun fromOrdinal(ordinal: Int): RecordingBarPosition {
            return entries.getOrElse(ordinal) { TOP_LEFT }
        }
    }
}

data class GlobalSettings(
    val randomizationEnabled: Boolean = false,
    val clickOffsetRadius: Int = 10,
    val clickDurationVariance: Long = 50,
    val delayVariance: Long = 100,
    val hapticFeedbackEnabled: Boolean = true,
    val controlBarAlpha: Float = 0.9f,
    val shortcutAlpha: Float = 0.8f,
    val recordingBarPosition: RecordingBarPosition = RecordingBarPosition.TOP_LEFT,
    val recordingBarCustomX: Int = 8,
    val recordingBarCustomY: Int = 48
)
