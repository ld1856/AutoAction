package com.autoaction.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY name ASC")
    fun getAllScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptById(id: String): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE isEnabled = 1")
    fun getEnabledScripts(): Flow<List<ScriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity)

    @Update
    suspend fun updateScript(script: ScriptEntity)

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: String)

    @Query("UPDATE scripts SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateScriptEnabled(id: String, isEnabled: Boolean)
}
