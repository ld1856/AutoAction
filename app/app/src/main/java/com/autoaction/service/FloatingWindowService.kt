package com.autoaction.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.autoaction.data.local.AppDatabase
import com.autoaction.data.repository.ScriptRepository
import com.autoaction.ui.floating.ControlBarContent
import com.autoaction.ui.floating.ScriptShortcutContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloatingWindowService : OverlayService() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: ScriptRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controlBarView: View? = null
    private val shortcutViews = mutableMapOf<String, View>()

    private val _shortcutsVisible = MutableStateFlow(true) // Track global shortcut visibility
    val shortcutsVisible = _shortcutsVisible.asStateFlow()

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private var instance: FloatingWindowService? = null

        fun getInstance(): FloatingWindowService? = instance

        fun updateShortcuts() {
            instance?.refreshShortcuts()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isRunning.value = true

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val database = AppDatabase.getDatabase(this)
        repository = ScriptRepository(database.scriptDao())

        createControlBar()
        observeEnabledScripts()
    }

    private fun createControlBar() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }

        controlBarView = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val currentShortcutsVisible by shortcutsVisible.collectAsState() // Collect state here
                ControlBarContent(
                    onStartRecording = { startRecording() },
                    onToggleShortcuts = { toggleShortcuts() },
                    shortcutsVisible = currentShortcutsVisible, // Pass the state
                    onOpenSettings = { openMainApp() },
                    onExit = { stopSelf() },
                    onDrag = { dx, dy ->
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        windowManager.updateViewLayout(this, params)
                    }
                )
            }
        }

        windowManager.addView(controlBarView, params)
    }

    private fun observeEnabledScripts() {
        scope.launch {
            repository.enabledScripts.collect { scripts ->
                updateShortcutViews(scripts.map { it.id to it }.toMap())
            }
        }
    }

    private fun updateShortcutViews(enabledScripts: Map<String, com.autoaction.data.model.Script>) {
        val currentIds = shortcutViews.keys.toSet()
        val newIds = enabledScripts.keys

        (currentIds - newIds).forEach { id ->
            shortcutViews[id]?.let { view ->
                windowManager.removeView(view)
                shortcutViews.remove(id)
            }
        }

        (newIds - currentIds).forEach { id ->
            enabledScripts[id]?.let { script ->
                createShortcutView(script)
            }
        }
        // Apply current visibility state to newly added views
        shortcutViews.values.forEach { view ->
            view.visibility = if (_shortcutsVisible.value) View.VISIBLE else View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createShortcutView(script: com.autoaction.data.model.Script) {
        val config = script.shortcutConfig
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = config.screenX.toInt()
            y = config.screenY.toInt()
        }

        val view = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                ScriptShortcutContent(
                    script = script
                )
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 40 || Math.abs(dy) > 40) { // Increased threshold to 40px
                        isDragging = true
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Ensure minimal movement to count as click (threshold must be consistent)
                        val dx = Math.abs(event.rawX - initialTouchX)
                        val dy = Math.abs(event.rawY - initialTouchY)
                        if (dx < 40 && dy < 40) { // Threshold for click
                            AutoActionService.executeScript(script.id)
                        }
                    } else {
                        scope.launch {
                            val updatedScript = script.copy(
                                shortcutConfig = script.shortcutConfig.copy(
                                    screenX = params.x.toFloat(),
                                    screenY = params.y.toFloat()
                                )
                            )
                            repository.updateScript(updatedScript)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        shortcutViews[script.id] = view
        // Apply current visibility state to this new view
        view.visibility = if (_shortcutsVisible.value) View.VISIBLE else View.GONE
        windowManager.addView(view, params)
    }

    private fun refreshShortcuts() {
        scope.launch {
            repository.enabledScripts.collect { scripts ->
                updateShortcutViews(scripts.map { it.id to it }.toMap())
            }
        }
    }

    private fun toggleShortcuts() {
        _shortcutsVisible.value = !_shortcutsVisible.value // Update the state flow
        shortcutViews.values.forEach { view ->
            view.visibility = if (_shortcutsVisible.value) View.VISIBLE else View.GONE
        }
    }

    private fun startRecording() {
        val intent = Intent(this, RecordingService::class.java)
        startService(intent)
    }

    private fun openMainApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        instance = null

        controlBarView?.let { windowManager.removeView(it) }
        shortcutViews.values.forEach { windowManager.removeView(it) }
        shortcutViews.clear()

        scope.cancel()
    }
}
