package com.autoaction.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.autoaction.ui.theme.AutoActionTheme

@Composable
fun ControlBarContent(
    onStartRecording: () -> Unit,
    onToggleShortcuts: () -> Unit,
    shortcutsVisible: Boolean,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    alpha: Float = 0.9f
) {
    var expanded by remember { mutableStateOf(false) }

    AutoActionTheme {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .padding(12.dp)
        ) {
            if (!expanded) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expanded = true }
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Collapse button
                    Icon(
                        imageVector = Icons.Default.MenuOpen,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { expanded = false }
                    )
                    
                    // Divider or Spacer could go here, but spacing is handled by Arrangement
                    
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Record",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { onStartRecording() }
                    )
                    Icon(
                        imageVector = if (shortcutsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Shortcuts",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onToggleShortcuts() }
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenSettings() }
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { onExit() }
                    )
                }
            }
        }
    }
}