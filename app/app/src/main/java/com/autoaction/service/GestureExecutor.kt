package com.autoaction.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.autoaction.data.model.Action
import com.autoaction.data.model.ActionType
import kotlinx.coroutines.delay
import kotlin.random.Random

class GestureExecutor(
    private val service: AccessibilityService
) {

    suspend fun executeAction(
        action: Action,
        globalRandomOffset: Int,
        globalRandomDelay: Long
    ): Boolean {
        val randomOffset = action.overrideRandomOffset ?: globalRandomOffset
        val randomDelay = action.overrideRandomDelay ?: globalRandomDelay

        val success = when (action.type) {
            ActionType.CLICK -> executeClick(action, randomOffset)
            ActionType.LONG_PRESS -> executeLongPress(action, randomOffset)
            ActionType.SWIPE -> executeSwipe(action, randomOffset)
            ActionType.MULTI_TOUCH -> executeMultiTouch(action, randomOffset)
            ActionType.DELAY -> {
                delay(action.duration + getRandomDelay(randomDelay))
                true
            }
        }

        delay(action.baseDelay + getRandomDelay(randomDelay))
        return success
    }

    private fun executeClick(action: Action, randomOffset: Int): Boolean {
        val (x, y) = applyRandomOffset(action.x, action.y, randomOffset)
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }

    private fun executeLongPress(action: Action, randomOffset: Int): Boolean {
        val (x, y) = applyRandomOffset(action.x, action.y, randomOffset)
        val path = Path().apply {
            moveTo(x, y)
        }
        val duration = action.duration.coerceAtLeast(100)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }

    private fun executeSwipe(action: Action, randomOffset: Int): Boolean {
        val (startX, startY) = applyRandomOffset(action.startX, action.startY, randomOffset)
        val (endX, endY) = applyRandomOffset(action.endX, action.endY, randomOffset)
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val duration = action.duration.coerceAtLeast(100)
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

    private fun getRandomDelay(maxDelay: Long): Long {
        if (maxDelay <= 0) return 0
        return Random.nextLong(-maxDelay / 2, maxDelay / 2 + 1)
    }
}
