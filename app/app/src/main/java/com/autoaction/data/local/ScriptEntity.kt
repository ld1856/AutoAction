package com.autoaction.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.autoaction.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isEnabled: Boolean = false,
    val loopCount: Int = 0,
    val globalRandomOffset: Int? = null,
    val globalRandomDelay: Long? = null,
    val shortcutConfigJson: String,
    val actionsJson: String
)

fun ScriptEntity.toScript(): Script {
    val gson = Gson()
    val shortcutConfig = gson.fromJson(shortcutConfigJson, ShortcutConfig::class.java)
    val actionsType = object : TypeToken<List<Action>>() {}.type
    val actions = gson.fromJson<List<Action>>(actionsJson, actionsType)

    return Script(
        id = id,
        name = name,
        isEnabled = isEnabled,
        loopCount = loopCount,
        globalRandomOffset = globalRandomOffset,
        globalRandomDelay = globalRandomDelay,
        shortcutConfig = shortcutConfig,
        actions = actions
    )
}

fun Script.toEntity(): ScriptEntity {
    val gson = Gson()
    return ScriptEntity(
        id = id,
        name = name,
        isEnabled = isEnabled,
        loopCount = loopCount,
        globalRandomOffset = globalRandomOffset,
        globalRandomDelay = globalRandomDelay,
        shortcutConfigJson = gson.toJson(shortcutConfig),
        actionsJson = gson.toJson(actions)
    )
}
