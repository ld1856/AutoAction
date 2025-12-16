package com.autoaction.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoaction.data.settings.RecordingBarPosition
import com.autoaction.ui.theme.AutoActionTheme

@Composable
fun RecordingOverlayContent(
    actionCount: Int,
    onStop: () -> Unit,
    onSave: () -> Unit,
    onGesture: ((Float, Float, Float, Float, Long) -> Unit)? = null,
    barPosition: RecordingBarPosition = RecordingBarPosition.TOP_LEFT,
    customX: Int = 8,
    customY: Int = 48
) {
    AutoActionTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onGesture != null) {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downX = down.position.x
                                val downY = down.position.y
                                val downTime = System.currentTimeMillis()

                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    val upX = up.position.x
                                    val upY = up.position.y
                                    val duration = System.currentTimeMillis() - downTime
                                    onGesture(downX, downY, upX, upY, duration)
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            // Calculate alignment and padding based on position
            val (alignment, paddingModifier) = when (barPosition) {
                RecordingBarPosition.TOP_LEFT -> Alignment.TopStart to Modifier.padding(start = 8.dp, top = 48.dp)
                RecordingBarPosition.TOP_CENTER -> Alignment.TopCenter to Modifier.padding(top = 48.dp)
                RecordingBarPosition.TOP_RIGHT -> Alignment.TopEnd to Modifier.padding(end = 8.dp, top = 48.dp)
                RecordingBarPosition.CENTER_LEFT -> Alignment.CenterStart to Modifier.padding(start = 8.dp)
                RecordingBarPosition.CENTER -> Alignment.Center to Modifier
                RecordingBarPosition.CENTER_RIGHT -> Alignment.CenterEnd to Modifier.padding(end = 8.dp)
                RecordingBarPosition.BOTTOM_LEFT -> Alignment.BottomStart to Modifier.padding(start = 8.dp, bottom = 48.dp)
                RecordingBarPosition.BOTTOM_CENTER -> Alignment.BottomCenter to Modifier.padding(bottom = 48.dp)
                RecordingBarPosition.BOTTOM_RIGHT -> Alignment.BottomEnd to Modifier.padding(end = 8.dp, bottom = 48.dp)
                RecordingBarPosition.CUSTOM -> Alignment.TopStart to Modifier.padding(start = customX.dp, top = customY.dp)
            }

            // 精简的控制条 - 类似FloatingWindowService的控制条样式
            Row(
                modifier = Modifier
                    .align(alignment)
                    .then(paddingModifier)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 录制指示器
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Recording",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                // 动作计数
                Text(
                    text = "$actionCount",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 取消按钮
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onStop() }
                )
                // 保存按钮
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = if (actionCount > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(enabled = actionCount > 0) { onSave() }
                )
            }
        }
    }
}
