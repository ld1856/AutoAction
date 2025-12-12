package com.autoaction.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.autoaction.data.local.AppDatabase
import com.autoaction.data.repository.ScriptRepository
import com.autoaction.data.settings.GlobalSettings
import com.autoaction.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutoActionService : AccessibilityService() {

    private lateinit var repository: ScriptRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var scriptExecutor: ScriptExecutor
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private var instance: AutoActionService? = null

        fun getInstance(): AutoActionService? = instance

        fun executeScript(scriptId: String) {
            instance?.executeScriptById(scriptId)
        }

        fun stopExecution() {
            instance?.scriptExecutor?.stopExecution()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isRunning.value = true

        val database = AppDatabase.getDatabase(this)
        repository = ScriptRepository(database.scriptDao())
        settingsRepository = SettingsRepository(this)
        scriptExecutor = ScriptExecutor(this) {
            settingsRepository.settings.first()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        instance = null
        scriptExecutor.cleanup()
    }

    private fun executeScriptById(scriptId: String) {
        scope.launch {
            val script = repository.getScriptById(scriptId)
            script?.let {
                scriptExecutor.executeScript(it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
