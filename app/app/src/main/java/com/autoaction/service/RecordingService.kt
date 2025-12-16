package com.autoaction.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.autoaction.data.local.AppDatabase
import com.autoaction.data.model.Action
import com.autoaction.data.model.ActionType
import com.autoaction.data.model.Script
import com.autoaction.data.repository.ScriptRepository
import com.autoaction.data.settings.GlobalSettings
import com.autoaction.data.settings.SettingsRepository
import com.autoaction.ui.floating.RecordingOverlayContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sqrt

class RecordingService : OverlayService() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: ScriptRepository
    private lateinit var settingsRepository: SettingsRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var inputOverlayView: View? = null
    private val recordedActions = mutableListOf<Action>()
    private var lastActionEndTime = 0L
    private var recordingStartTime = 0L

    companion object {
        private const val LONG_PRESS_THRESHOLD_MS = 500L
        private const val MOVEMENT_THRESHOLD_PX = 40f
        private const val MIN_DELAY_MS = 50L

        private var _instance: RecordingService? = null
        private val _actionCount = mutableStateOf(0)
        private val _isRecording = mutableStateOf(false)

        val actionCountState: androidx.compose.runtime.State<Int> = _actionCount
        val isRecordingState: androidx.compose.runtime.State<Boolean> = _isRecording

        fun getInstance(): RecordingService? = _instance
        fun getActionCount(): Int = _actionCount.value
        fun isRecording(): Boolean = _isRecording.value
    }

    private val actionCountState = _actionCount

    override fun onCreate() {
        super.onCreate()
        _instance = this
        _isRecording.value = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val database = AppDatabase.getDatabase(this)
        repository = ScriptRepository(database.scriptDao())
        settingsRepository = SettingsRepository(this)

        createRecordingOverlay()
        recordingStartTime = System.currentTimeMillis()
        lastActionEndTime = recordingStartTime
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createRecordingOverlay() {
        val inputParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        inputOverlayView = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val settings by settingsRepository.settings.collectAsState(initial = GlobalSettings())
                RecordingOverlayContent(
                    actionCount = actionCountState.value,
                    onStop = { stopRecording() },
                    onSave = { saveRecording() },
                    onGesture = { downX, downY, upX, upY, duration ->
                        handleGesture(downX, downY, upX, upY, duration)
                    },
                    barPosition = settings.recordingBarPosition,
                    customX = settings.recordingBarCustomX,
                    customY = settings.recordingBarCustomY
                )
            }
        }

        windowManager.addView(inputOverlayView, inputParams)

        // 录制时隐藏原始控制条
        FloatingWindowService.getInstance()?.hideControlBar()
    }

    private fun handleGesture(downX: Float, downY: Float, upX: Float, upY: Float, duration: Long) {
        val currentTime = System.currentTimeMillis()
        val distance = calculateDistance(downX, downY, upX, upY)

        addDelayIfNeeded(currentTime - duration)

        val action = recognizeGesture(
            downX, downY, upX, upY,
            duration, distance
        )

        recordedActions.add(action)
        lastActionEndTime = currentTime

        updateActionCount()
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

    private fun updateActionCount() {
        actionCountState.value = recordedActions.count { it.type != ActionType.DELAY }
    }

    fun stopRecording() {
        stopSelf()
    }

    fun saveRecording() {
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
        _instance = null
        _isRecording.value = false
        _actionCount.value = 0
        inputOverlayView?.let { windowManager.removeView(it) }

        // 录制结束时恢复显示原始控制条
        FloatingWindowService.getInstance()?.showControlBar()

        scope.cancel()
    }
}
