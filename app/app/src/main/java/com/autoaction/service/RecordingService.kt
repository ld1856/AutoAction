package com.autoaction.service

import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.autoaction.data.local.AppDatabase
import com.autoaction.data.model.Action
import com.autoaction.data.model.ActionType
import com.autoaction.data.model.Script
import com.autoaction.data.repository.ScriptRepository
import com.autoaction.ui.floating.RecordingOverlayContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

class RecordingService : OverlayService() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: ScriptRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var overlayView: View? = null
    private val recordedActions = mutableListOf<Action>()
    private var lastActionEndTime = 0L
    private var recordingStartTime = 0L

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var isGestureInProgress = false

    private val actionCountState = mutableStateOf(0)

    companion object {
        private const val LONG_PRESS_THRESHOLD_MS = 500L
        private const val MOVEMENT_THRESHOLD_PX = 40f
        private const val MIN_DELAY_MS = 50L
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val database = AppDatabase.getDatabase(this)
        repository = ScriptRepository(database.scriptDao())

        createRecordingOverlay()
        recordingStartTime = System.currentTimeMillis()
        lastActionEndTime = recordingStartTime
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createRecordingOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                RecordingOverlayContent(
                    actionCount = actionCountState.value,
                    onStop = { stopRecording() },
                    onSave = { saveRecording() }
                )
            }
            setOnTouchListener { _, event ->
                handleTouchEvent(event)
                true
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun handleTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                downTime = System.currentTimeMillis()
                isGestureInProgress = true
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
                if (!isGestureInProgress) return

                val upX = event.rawX
                val upY = event.rawY
                val upTime = System.currentTimeMillis()
                val duration = upTime - downTime
                val distance = calculateDistance(downX, downY, upX, upY)

                addDelayIfNeeded(downTime)

                val action = recognizeGesture(
                    downX, downY, upX, upY,
                    duration, distance
                )

                recordedActions.add(action)
                lastActionEndTime = upTime
                isGestureInProgress = false

                updateActionCount()

                dispatchPassthroughGesture(action, downX, downY, upX, upY, duration)
            }
        }
    }

    private fun addDelayIfNeeded(currentActionStartTime: Long) {
        val delay = currentActionStartTime - lastActionEndTime
        if (recordedActions.isNotEmpty() && delay > MIN_DELAY_MS) {
            recordedActions.add(
                Action(
                    type = ActionType.DELAY,
                    duration = delay,
                    desc = "Auto delay ${delay}ms"
                )
            )
        }
    }

    private fun recognizeGesture(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        duration: Long,
        distance: Float
    ): Action {
        return when {
            distance < MOVEMENT_THRESHOLD_PX && duration >= LONG_PRESS_THRESHOLD_MS -> {
                Action(
                    type = ActionType.LONG_PRESS,
                    x = startX.toInt(),
                    y = startY.toInt(),
                    duration = duration,
                    desc = "Long Press (${duration}ms)"
                )
            }
            distance < MOVEMENT_THRESHOLD_PX -> {
                Action(
                    type = ActionType.CLICK,
                    x = startX.toInt(),
                    y = startY.toInt(),
                    duration = 50,
                    desc = "Click"
                )
            }
            else -> {
                Action(
                    type = ActionType.SWIPE,
                    startX = startX.toInt(),
                    startY = startY.toInt(),
                    endX = endX.toInt(),
                    endY = endY.toInt(),
                    duration = duration.coerceAtLeast(100),
                    desc = "Swipe (${distance.toInt()}px, ${duration}ms)"
                )
            }
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    private fun dispatchPassthroughGesture(
        action: Action,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val service = AutoActionService.getInstance() ?: return@launch

                val gesture = when (action.type) {
                    ActionType.CLICK -> {
                        val path = Path().apply {
                            moveTo(startX, startY)
                        }
                        GestureDescription.Builder()
                            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                            .build()
                    }
                    ActionType.LONG_PRESS -> {
                        val path = Path().apply {
                            moveTo(startX, startY)
                        }
                        GestureDescription.Builder()
                            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                            .build()
                    }
                    ActionType.SWIPE -> {
                        val path = Path().apply {
                            moveTo(startX, startY)
                            lineTo(endX, endY)
                        }
                        val swipeDuration = duration.coerceIn(100, 2000)
                        GestureDescription.Builder()
                            .addStroke(GestureDescription.StrokeDescription(path, 0, swipeDuration))
                            .build()
                    }
                    else -> null
                }

                gesture?.let {
                    service.dispatchGesture(it, null, null)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun updateActionCount() {
        actionCountState.value = recordedActions.count { it.type != ActionType.DELAY }
    }

    private fun stopRecording() {
        stopSelf()
    }

    private fun saveRecording() {
        if (recordedActions.isEmpty()) {
            stopSelf()
            return
        }

        val script = Script(
            id = UUID.randomUUID().toString(),
            name = "Recorded Script ${System.currentTimeMillis()}",
            isEnabled = false,
            loopCount = 1,
            actions = recordedActions.toList()
        )

        scope.launch {
            repository.insertScript(script)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        scope.cancel()
    }
}
