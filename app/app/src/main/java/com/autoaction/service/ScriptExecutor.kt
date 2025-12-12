package com.autoaction.service

import android.accessibilityservice.AccessibilityService
import com.autoaction.data.model.Script
import kotlinx.coroutines.*

class ScriptExecutor(
    private val service: AccessibilityService
) {
    private val gestureExecutor = GestureExecutor(service)
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun executeScript(script: Script, onComplete: () -> Unit = {}) {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                val loopCount = script.loopCount
                if (loopCount == 0) {
                    while (isActive) {
                        executeScriptOnce(script)
                    }
                } else {
                    repeat(loopCount) {
                        if (isActive) {
                            executeScriptOnce(script)
                        }
                    }
                }
            } finally {
                onComplete()
            }
        }
    }

    private suspend fun executeScriptOnce(script: Script) {
        for (action in script.actions) {
            if (!currentJob?.isActive!!) break
            gestureExecutor.executeAction(
                action,
                script.globalRandomOffset,
                script.globalRandomDelay
            )
        }
    }

    fun stopExecution() {
        currentJob?.cancel()
        currentJob = null
    }

    fun isRunning(): Boolean {
        return currentJob?.isActive == true
    }

    fun cleanup() {
        stopExecution()
        scope.cancel()
    }
}
