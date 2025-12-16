package com.autoaction.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoaction.data.model.Script
import com.autoaction.service.AutoActionService
import com.autoaction.service.FloatingWindowService
import com.autoaction.ui.viewmodel.ScriptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListScreen(
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ScriptViewModel = viewModel()
) {
    val context = LocalContext.current
    val scripts by viewModel.allScripts.collectAsState()
    val serviceRunning by AutoActionService.isRunning.collectAsState()
    val floatingRunning by FloatingWindowService.isRunning.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AutoAction") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (serviceRunning) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = "Service Status",
                            tint = if (serviceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onError, // Changed tint for better contrast
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .then(if (!serviceRunning) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .padding(4.dp) // Add padding inside the background
                                } else Modifier)
                                .clickable {
                                    if (!serviceRunning) {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                }
                        )
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.Settings, "Settings")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("App Settings") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToSettings()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, "Settings")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Open Accessibility Settings") },
                                    onClick = {
                                        showMenu = false
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Overlay Permission") },
                                    onClick = {
                                        showMenu = false
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else if (!floatingRunning) {
                            context.startService(Intent(context, FloatingWindowService::class.java))
                        } else {
                            // Toggle control bar visibility when service is running
                            FloatingWindowService.getInstance()?.toggleControlBarFromApp()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Widgets, "Toggle Floating")
                }

                FloatingActionButton(
                    onClick = { onNavigateToEditor(null) }
                ) {
                    Icon(Icons.Default.Add, "Create Script")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(scripts, key = { it.id }) { script ->
                ScriptCard(
                    script = script,
                    onToggle = { viewModel.toggleScriptEnabled(script.id, !script.isEnabled) },
                    onClick = { onNavigateToEditor(script.id) },
                    onDelete = { viewModel.deleteScript(script) }
                )
            }
        }
    }
}

@Composable
fun ScriptCard(
    script: Script,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = script.name.firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${script.actions.size} actions | ${if (script.loopCount == 0) "∞" else script.loopCount} loops",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }

            Switch(
                checked = script.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Script") },
            text = { Text("Are you sure you want to delete \"${script.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
