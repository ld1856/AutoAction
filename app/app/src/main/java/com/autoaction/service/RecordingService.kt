package com.autoaction.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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

class RecordingService : OverlayService() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: ScriptRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var overlayView: View? = null
    private val recordedActions = mutableListOf<Action>()
    private var lastEventTime = 0L
    private var recordingStartTime = 0L

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val database = AppDatabase.getDatabase(this)
        repository = ScriptRepository(database.scriptDao())

        createRecordingOverlay()
        recordingStartTime = System.currentTimeMillis()
        lastEventTime = recordingStartTime
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
                    actionCount = recordedActions.size,
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
                val currentTime = System.currentTimeMillis()
                val delay = currentTime - lastEventTime

                if (recordedActions.isNotEmpty() && delay > 50) {
                    recordedActions.add(
                        Action(
                            type = ActionType.DELAY,
                            duration = delay,
                            desc = "Auto delay"
                        )
                    )
                }

                recordedActions.add(
                    Action(
                        type = ActionType.CLICK,
                        x = event.rawX.toInt(),
                        y = event.rawY.toInt(),
                        duration = 50,
                        desc = "Click ${recordedActions.size + 1}"
                    )
                )

                lastEventTime = currentTime
                (overlayView as? ComposeView)?.setContent {
                    RecordingOverlayContent(
                        actionCount = recordedActions.size,
                        onStop = { stopRecording() },
                        onSave = { saveRecording() }
                    )
                }
            }
        }
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
