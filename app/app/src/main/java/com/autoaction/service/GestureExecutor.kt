package com.autoaction.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.autoaction.data.model.Action
import com.autoaction.data.model.ActionType
import com.autoaction.data.settings.GlobalSettings
import kotlinx.coroutines.delay
import kotlin.random.Random

class GestureExecutor(
    private val service: AccessibilityService
) {

    suspend fun executeAction(
        action: Action,
        globalSettings: GlobalSettings,
        scriptGlobalRandomOffset: Int = 0,
        scriptGlobalRandomDelay: Long = 0
    ): Boolean {
        val randomOffset = if (globalSettings.randomizationEnabled) {
            action.overrideRandomOffset ?: scriptGlobalRandomOffset.coerceAtLeast(globalSettings.clickOffsetRadius)
        } else {
            0
        }

        val randomDelayVariance = if (globalSettings.randomizationEnabled) {
            action.overrideRandomDelay ?: scriptGlobalRandomDelay.coerceAtLeast(globalSettings.delayVariance)
        } else {
            0
        }

        val randomDurationVariance = if (globalSettings.randomizationEnabled) {
            globalSettings.clickDurationVariance
        } else {
            0
        }

        return when (action.type) {
            ActionType.CLICK -> executeClick(action, randomOffset, randomDurationVariance)
            ActionType.LONG_PRESS -> executeLongPress(action, randomOffset, randomDurationVariance)
            ActionType.SWIPE -> executeSwipe(action, randomOffset, randomDurationVariance)
            ActionType.MULTI_TOUCH -> executeMultiTouch(action, randomOffset)
            ActionType.DELAY -> {
                val finalDelay = action.duration + getRandomVariance(randomDelayVariance)
                delay(finalDelay.coerceAtLeast(0))
                true
            }
        }
    }

    private fun executeClick(action: Action, randomOffset: Int, durationVariance: Long): Boolean {
        val (x, y) = applyRandomOffset(action.x, action.y, randomOffset)
        val duration = (action.duration + getRandomVariance(durationVariance)).coerceAtLeast(10)
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }

    private fun executeLongPress(action: Action, randomOffset: Int, durationVariance: Long): Boolean {
        val (x, y) = applyRandomOffset(action.x, action.y, randomOffset)
        val duration = (action.duration + getRandomVariance(durationVariance)).coerceAtLeast(100)
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }

    private fun executeSwipe(action: Action, randomOffset: Int, durationVariance: Long): Boolean {
        val (startX, startY) = applyRandomOffset(action.startX, action.startY, randomOffset)
        val (endX, endY) = applyRandomOffset(action.endX, action.endY, randomOffset)
        val duration = (action.duration + getRandomVariance(durationVariance)).coerceAtLeast(100)
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }

    private fun executeMultiTouch(action: Action, randomOffset: Int): Boolean {
        return true
    }

    private fun applyRandomOffset(x: Int, y: Int, offset: Int): Pair<Float, Float> {
        val dx = Random.nextInt(-offset, offset + 1)
        val dy = Random.nextInt(-offset, offset + 1)
        return Pair((x + dx).toFloat(), (y + dy).toFloat())
    }

    private fun getRandomVariance(variance: Long): Long {
        if (variance <= 0) return 0
        return Random.nextLong(-variance, variance + 1)
    }
}
