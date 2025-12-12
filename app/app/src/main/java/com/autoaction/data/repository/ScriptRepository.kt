package com.autoaction.data.repository

import com.autoaction.data.local.ScriptDao
import com.autoaction.data.local.toEntity
import com.autoaction.data.local.toScript
import com.autoaction.data.model.Script
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScriptRepository(private val scriptDao: ScriptDao) {

    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts().map { entities ->
        entities.map { it.toScript() }
    }

    val enabledScripts: Flow<List<Script>> = scriptDao.getEnabledScripts().map { entities ->
        entities.map { it.toScript() }
    }

    suspend fun getScriptById(id: String): Script? {
        return scriptDao.getScriptById(id)?.toScript()
    }

    suspend fun insertScript(script: Script) {
        scriptDao.insertScript(script.toEntity())
    }

    suspend fun updateScript(script: Script) {
        scriptDao.updateScript(script.toEntity())
    }

    suspend fun deleteScript(script: Script) {
        scriptDao.deleteScript(script.toEntity())
    }

    suspend fun deleteScriptById(id: String) {
        scriptDao.deleteScriptById(id)
    }

    suspend fun updateScriptEnabled(id: String, isEnabled: Boolean) {
        scriptDao.updateScriptEnabled(id, isEnabled)
    }
}
