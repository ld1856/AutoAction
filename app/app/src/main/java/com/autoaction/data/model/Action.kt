package com.autoaction.data.model

data class Action(
    val type: ActionType,
    val x: Int = 0,
    val y: Int = 0,
    val startX: Int = 0,
    val startY: Int = 0,
    val endX: Int = 0,
    val endY: Int = 0,
    val duration: Long = 0,
    val baseDelay: Long = 0,
    val desc: String = "",
    val overrideRandomOffset: Int? = null,
    val overrideRandomDelay: Long? = null
)
