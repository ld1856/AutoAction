package com.autoaction.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autoaction.data.model.Script
import com.autoaction.ui.theme.AutoActionTheme

@Composable
fun ScriptShortcutContent(
    script: Script
) {
    val config = script.shortcutConfig
    
    // We removed internal click handling because the parent WindowManager handles touch for drag & click.
    // Visual feedback for "pressed" state would ideally require passing state down, 
    // but for now we keep it simple to fix the drag issue.

    AutoActionTheme {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(config.scale)
                .alpha(config.alpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = config.textLabel.ifEmpty { script.name.firstOrNull()?.uppercase() ?: "S" },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
