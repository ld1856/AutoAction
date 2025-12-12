package com.autoaction.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoaction.data.local.AppDatabase
import com.autoaction.data.model.Script
import com.autoaction.data.repository.ScriptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScriptViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScriptRepository

    val allScripts: StateFlow<List<Script>>
    val enabledScripts: StateFlow<List<Script>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ScriptRepository(database.scriptDao())

        allScripts = repository.allScripts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        enabledScripts = repository.enabledScripts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun insertScript(script: Script) = viewModelScope.launch {
        repository.insertScript(script)
    }

    fun updateScript(script: Script) = viewModelScope.launch {
        repository.updateScript(script)
    }

    fun deleteScript(script: Script) = viewModelScope.launch {
        repository.deleteScript(script)
    }

    fun toggleScriptEnabled(scriptId: String, isEnabled: Boolean) = viewModelScope.launch {
        repository.updateScriptEnabled(scriptId, isEnabled)
    }

    suspend fun getScriptById(id: String): Script? {
        return repository.getScriptById(id)
    }
}
