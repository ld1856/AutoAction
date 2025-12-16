package com.autoaction.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoaction.service.RecordingService
import com.autoaction.ui.theme.AutoActionTheme

@Composable
fun ControlBarContent(
    onStartRecording: () -> Unit,
    onToggleShortcuts: () -> Unit,
    shortcutsVisible: Boolean,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onHideControlBar: () -> Unit,
    onExpandedChanged: (Boolean) -> Unit = {},
    shouldExpand: Boolean = false,
    isNearRightEdge: Boolean = false,
    alpha: Float = 0.9f
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        onExpandedChanged(expanded)
    }

    LaunchedEffect(shouldExpand) {
        if (shouldExpand) {
            expanded = true
        }
    }
    val isRecording by RecordingService.isRecordingState
    val actionCount by RecordingService.actionCountState

    AutoActionTheme {
        Box(
            modifier = Modifier
                .clip(if (isRecording) RoundedCornerShape(16.dp) else CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha))
                .then(
                    if (!isRecording) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(12.dp)
        ) {
            if (isRecording) {
                // 录制模式：显示录制状态和控制按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Recording",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "$actionCount",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable {
                            RecordingService.getInstance()?.stopRecording()
                        }
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            RecordingService.getInstance()?.saveRecording()
                        }
                    )
                }
            } else if (!expanded) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expanded = true }
                )
            } else {
                Row(
                    horizontalArrangement = if (isNearRightEdge) Arrangement.spacedBy(16.dp, Alignment.End) else Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isNearRightEdge) {
                        // When near right edge, show icons in reverse order with collapse on right
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onExit() }
                        )
                        Icon(
                            imageVector = Icons.Default.RemoveCircle,
                            contentDescription = "Hide Control Bar",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onHideControlBar() }
                        )
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onOpenSettings() }
                        )
                        Icon(
                            imageVector = if (shortcutsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Shortcuts",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onToggleShortcuts() }
                        )
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = "Record",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onStartRecording() }
                        )
                        // Collapse button on right
                        Icon(
                            imageVector = Icons.Default.MenuOpen,
                            contentDescription = "Collapse",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { expanded = false }
                        )
                    } else {
                        // Normal order with collapse on left
                        Icon(
                            imageVector = Icons.Default.MenuOpen,
                            contentDescription = "Collapse",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { expanded = false }
                        )
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
                            imageVector = Icons.Default.RemoveCircle,
                            contentDescription = "Hide Control Bar",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onHideControlBar() }
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
}