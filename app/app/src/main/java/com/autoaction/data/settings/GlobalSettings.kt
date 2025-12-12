package com.autoaction.data.settings

data class GlobalSettings(
    val randomizationEnabled: Boolean = false,
    val clickOffsetRadius: Int = 10,
    val clickDurationVariance: Long = 50,
    val delayVariance: Long = 100,
    val hapticFeedbackEnabled: Boolean = true,
    val controlBarAlpha: Float = 0.9f,
    val shortcutAlpha: Float = 0.8f
)
