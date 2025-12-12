package com.autoaction.data.model

data class Script(
    val id: String,
    val name: String,
    val isEnabled: Boolean = false,
    val loopCount: Int = 0,
    val globalRandomOffset: Int = 0,
    val globalRandomDelay: Long = 0,
    val shortcutConfig: ShortcutConfig = ShortcutConfig(),
    val actions: List<Action> = emptyList()
)
