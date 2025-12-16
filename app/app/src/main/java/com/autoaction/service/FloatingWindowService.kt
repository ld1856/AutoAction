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
import com.autoaction.data.settings.GlobalSettings
import com.autoaction.data.settings.SettingsRepository
import com.autoaction.ui.floating.ControlBarContent
import com.autoaction.ui.floating.EdgeIndicatorContent
import com.autoaction.ui.floating.EdgePosition
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
    private lateinit var settingsRepository: SettingsRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controlBarView: View? = null
    private var edgeIndicatorView: View? = null
    private val shortcutViews = mutableMapOf<String, View>()
    private var controlBarParams: WindowManager.LayoutParams? = null
    private var isControlBarExpanded = false
    private val _shouldExpand = MutableStateFlow(false)
    val shouldExpandState = _shouldExpand.asStateFlow()
    private val _edgePosition = MutableStateFlow(EdgePosition.LEFT)
    val edgePositionState = _edgePosition.asStateFlow()
    private val _isNearRightEdge = MutableStateFlow(false)
    val isNearRightEdgeState = _isNearRightEdge.asStateFlow()

    private val _shortcutsVisible = MutableStateFlow(true)
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
        settingsRepository = SettingsRepository(this)

        createControlBar()
        createEdgeIndicator()
        observeEnabledScripts()
    }

    private fun createControlBar() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }
        controlBarParams = params

        controlBarView = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val currentShortcutsVisible by shortcutsVisible.collectAsState()
                val settings by settingsRepository.settings.collectAsState(initial = GlobalSettings())
                val shouldExpand by shouldExpandState.collectAsState()
                val isNearRightEdge by isNearRightEdgeState.collectAsState()
                ControlBarContent(
                    onStartRecording = { startRecording() },
                    onToggleShortcuts = { toggleShortcuts() },
                    shortcutsVisible = currentShortcutsVisible,
                    onOpenSettings = { openMainApp() },
                    onExit = { stopSelf() },
                    onDrag = { dx, dy ->
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        windowManager.updateViewLayout(this, params)

                        // Update right edge proximity state
                        val displayMetrics = resources.displayMetrics
                        _isNearRightEdge.value = params.x > displayMetrics.widthPixels - 200

                        // Check if should auto-minimize after drag ends
                        if (!isControlBarExpanded) {
                            checkAndMinimizeControlBar(params.x, params.y)
                        }
                    },
                    onHideControlBar = { hideControlBarCompletely() },
                    onExpandedChanged = { expanded ->
                        isControlBarExpanded = expanded
                        // Check if should auto-minimize when manually collapsing
                        if (!expanded) {
                            checkAndMinimizeControlBar(params.x, params.y)
                        }
                        // Reset shouldExpand after handling
                        if (_shouldExpand.value) {
                            _shouldExpand.value = false
                        }
                    },
                    shouldExpand = shouldExpand,
                    isNearRightEdge = isNearRightEdge,
                    alpha = settings.controlBarAlpha
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
                val settings by settingsRepository.settings.collectAsState(initial = GlobalSettings())
                ScriptShortcutContent(
                    script = script,
                    globalAlpha = settings.shortcutAlpha
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

    fun bringControlBarToFront() {
        controlBarView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            windowManager.removeView(view)
            windowManager.addView(view, params)
        }
    }

    fun hideControlBar() {
        controlBarView?.visibility = View.GONE
    }

    fun showControlBar() {
        controlBarView?.visibility = View.VISIBLE
    }

    fun toggleControlBarFromApp() {
        if (controlBarView?.visibility == View.VISIBLE) {
            hideControlBarCompletely()
        } else {
            showControlBarAndHideIndicator()
        }
    }

    private fun hideControlBarCompletely() {
        controlBarView?.visibility = View.GONE
        edgeIndicatorView?.visibility = View.GONE
    }

    private fun showControlBarAndHideIndicator() {
        // Reset control bar gravity and position based on where it was minimized
        controlBarView?.let { view ->
            controlBarParams?.let { params ->
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels

                // Always use START gravity for control bar to allow free dragging
                params.gravity = Gravity.START or Gravity.CENTER_VERTICAL

                // Adjust x position if it was minimized on the right edge
                // Convert from END gravity (x=0 at right edge) to START gravity (x=0 at left edge)
                if (_edgePosition.value == EdgePosition.RIGHT) {
                    // Calculate position from left edge
                    params.x = screenWidth - 100  // Position near right edge but with START gravity
                }
                // For LEFT and TOP positions, x and y are already correct

                windowManager.updateViewLayout(view, params)
            }
        }

        controlBarView?.visibility = View.VISIBLE
        edgeIndicatorView?.visibility = View.GONE
        // Don't auto-expand - show collapsed hamburger icon instead
    }

    private fun checkAndMinimizeControlBar(x: Int, y: Int = controlBarParams?.y ?: 0) {
        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Convert relative y (from CENTER_VERTICAL gravity) to absolute screen position
        // When using CENTER_VERTICAL gravity, y=0 means center, negative is above, positive is below
        val absoluteY = screenHeight / 2 + y

        // Auto-minimize if control bar is near any edge
        val isNearLeftEdge = x < 20
        val isNearRightEdge = x > screenWidth - 100  // Account for control bar width
        val isNearTopEdge = absoluteY < 20
        val isNearBottomEdge = absoluteY > screenHeight - 100

        if (isNearLeftEdge || isNearRightEdge || isNearTopEdge || isNearBottomEdge) {
            controlBarView?.visibility = View.GONE
            edgeIndicatorView?.visibility = View.VISIBLE

            // Update edge position state and indicator position based on which edge
            // Priority order: left > right > top > bottom
            edgeIndicatorView?.let { indicator ->
                val indicatorParams = indicator.layoutParams as WindowManager.LayoutParams
                when {
                    isNearLeftEdge -> {
                        _edgePosition.value = EdgePosition.LEFT
                        indicatorParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        indicatorParams.x = 0
                        indicatorParams.y = y
                    }
                    isNearRightEdge -> {
                        _edgePosition.value = EdgePosition.RIGHT
                        indicatorParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        indicatorParams.x = 0
                        indicatorParams.y = y
                    }
                    isNearTopEdge -> {
                        _edgePosition.value = EdgePosition.TOP
                        indicatorParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        // Convert absolute x to offset from horizontal center
                        // CENTER_HORIZONTAL gravity: x=0 is center, negative is left, positive is right
                        indicatorParams.x = x - screenWidth / 2
                        indicatorParams.y = 0
                    }
                    isNearBottomEdge -> {
                        _edgePosition.value = EdgePosition.TOP  // Reuse TOP for now
                        indicatorParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        // Convert absolute x to offset from horizontal center
                        indicatorParams.x = x - screenWidth / 2
                        indicatorParams.y = 0
                    }
                }
                windowManager.updateViewLayout(indicator, indicatorParams)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createEdgeIndicator() {
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

        edgeIndicatorView = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val position by edgePositionState.collectAsState()
                EdgeIndicatorContent(
                    onClick = { showControlBarAndHideIndicator() },
                    position = position
                )
            }
        }

        edgeIndicatorView?.visibility = View.GONE
        windowManager.addView(edgeIndicatorView, params)
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
        edgeIndicatorView?.let { windowManager.removeView(it) }
        shortcutViews.values.forEach { windowManager.removeView(it) }
        shortcutViews.clear()

        scope.cancel()
    }
}
