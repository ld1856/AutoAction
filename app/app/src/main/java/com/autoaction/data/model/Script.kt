package com.autoaction.data.model

data class Script(
    val id: String,
    val name: String,
    val isEnabled: Boolean = false,
    val loopCount: Int = 0,
    val globalRandomOffset: Int? = null,
    val globalRandomDelay: Long? = null,
    val shortcutConfig: ShortcutConfig = ShortcutConfig(),
    val actions: List<Action> = emptyList()
)
