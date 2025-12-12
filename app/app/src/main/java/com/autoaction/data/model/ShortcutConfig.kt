package com.autoaction.data.model

data class ShortcutConfig(
    val iconType: IconType = IconType.DEFAULT_ICON,
    val iconData: String = "",
    val screenX: Float = 100f,
    val screenY: Float = 100f,
    val alpha: Float = 0.6f,
    val scale: Float = 0.8f,
    val textLabel: String = "",
    val textColor: String = "#FFFFFF"
)
