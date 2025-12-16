package com.autoaction.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autoaction.data.settings.RecordingBarPosition
import com.autoaction.data.settings.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settings.collectAsState(initial = com.autoaction.data.settings.GlobalSettings())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Anti-Detection Randomization",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Randomization")
                Switch(
                    checked = settings.randomizationEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            repository.updateRandomizationEnabled(enabled)
                        }
                    }
                )
            }

            if (settings.randomizationEnabled) {
                Divider()

                Text(
                    text = "Coordinate Offset Range: ±${settings.clickOffsetRadius}px",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.clickOffsetRadius.toFloat(),
                    onValueChange = { value ->
                        scope.launch {
                            repository.updateClickOffsetRadius(value.toInt())
                        }
                    },
                    valueRange = 0f..50f,
                    steps = 49
                )

                Text(
                    text = "Click Duration Variance: ±${settings.clickDurationVariance}ms",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.clickDurationVariance.toFloat(),
                    onValueChange = { value ->
                        scope.launch {
                            repository.updateClickDurationVariance(value.toLong())
                        }
                    },
                    valueRange = 0f..200f,
                    steps = 19
                )

                Text(
                    text = "Delay Variance: ±${settings.delayVariance}ms",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.delayVariance.toFloat(),
                    onValueChange = { value ->
                        scope.launch {
                            repository.updateDelayVariance(value.toLong())
                        }
                    },
                    valueRange = 0f..1000f,
                    steps = 99
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Haptic Feedback")
                Switch(
                    checked = settings.hapticFeedbackEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            repository.updateHapticFeedbackEnabled(enabled)
                        }
                    }
                )
            }

            Divider()

            Text(
                text = "Appearance Settings",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Control Bar Opacity: ${(settings.controlBarAlpha * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.controlBarAlpha,
                onValueChange = { value ->
                    scope.launch {
                        repository.updateControlBarAlpha(value)
                    }
                },
                valueRange = 0.1f..1.0f,
                steps = 17
            )

            Text(
                text = "Shortcut Opacity: ${(settings.shortcutAlpha * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.shortcutAlpha,
                onValueChange = { value ->
                    scope.launch {
                        repository.updateShortcutAlpha(value)
                    }
                },
                valueRange = 0.1f..1.0f,
                steps = 17
            )

            HorizontalDivider()

            Text(
                text = "Recording Settings",
                style = MaterialTheme.typography.titleLarge
            )

            var showPositionMenu by remember { mutableStateOf(false) }

            Text(
                text = "Recording Bar Position",
                style = MaterialTheme.typography.bodyMedium
            )

            Box {
                OutlinedButton(
                    onClick = { showPositionMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(settings.recordingBarPosition.displayName)
                }

                DropdownMenu(
                    expanded = showPositionMenu,
                    onDismissRequest = { showPositionMenu = false }
                ) {
                    RecordingBarPosition.entries.forEach { position ->
                        DropdownMenuItem(
                            text = { Text(position.displayName) },
                            onClick = {
                                scope.launch {
                                    repository.updateRecordingBarPosition(position)
                                }
                                showPositionMenu = false
                            }
                        )
                    }
                }
            }

            if (settings.recordingBarPosition == RecordingBarPosition.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var customX by remember { mutableStateOf(settings.recordingBarCustomX.toString()) }
                    var customY by remember { mutableStateOf(settings.recordingBarCustomY.toString()) }

                    OutlinedTextField(
                        value = customX,
                        onValueChange = {
                            customX = it
                            val x = it.toIntOrNull() ?: 8
                            scope.launch {
                                repository.updateRecordingBarCustomPosition(x, settings.recordingBarCustomY)
                            }
                        },
                        label = { Text("X (dp)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customY,
                        onValueChange = {
                            customY = it
                            val y = it.toIntOrNull() ?: 48
                            scope.launch {
                                repository.updateRecordingBarCustomPosition(settings.recordingBarCustomX, y)
                            }
                        },
                        label = { Text("Y (dp)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About Variance",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All variance values represent bidirectional (±) deviation. " +
                                "For example, a delay variance of 78ms means the actual delay " +
                                "will be randomly adjusted by -78ms to +78ms from the base value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
