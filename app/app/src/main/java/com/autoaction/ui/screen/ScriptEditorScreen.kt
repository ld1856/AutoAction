package com.autoaction.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoaction.data.model.*
import com.autoaction.ui.viewmodel.ScriptViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ScriptViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var script by remember { mutableStateOf<Script?>(null) }
    var scriptName by remember { mutableStateOf("") }
    var loopCount by remember { mutableStateOf("1") }
    var actions by remember { mutableStateOf<List<Action>>(emptyList()) }
    var globalRandomOffset by remember { mutableStateOf("10") }
    var globalRandomDelay by remember { mutableStateOf("200") }
    var isEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(scriptId) {
        if (scriptId != null) {
            val loadedScript = viewModel.getScriptById(scriptId)
            loadedScript?.let {
                script = it
                scriptName = it.name
                loopCount = it.loopCount.toString()
                actions = it.actions
                globalRandomOffset = it.globalRandomOffset.toString()
                globalRandomDelay = it.globalRandomDelay.toString()
                isEnabled = it.isEnabled
            }
        } else {
            scriptName = "New Script"
            loopCount = "1"
            actions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Script") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Show Shortcut",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                        IconButton(onClick = {
                            scope.launch {
                                val newScript = Script(
                                    id = scriptId ?: UUID.randomUUID().toString(),
                                    name = scriptName,
                                    loopCount = loopCount.toIntOrNull() ?: 1,
                                    globalRandomOffset = globalRandomOffset.toIntOrNull() ?: 10,
                                    globalRandomDelay = globalRandomDelay.toLongOrNull() ?: 200,
                                    actions = actions,
                                    isEnabled = isEnabled,
                                    shortcutConfig = script?.shortcutConfig ?: ShortcutConfig()
                                )
                                if (scriptId == null) {
                                    viewModel.insertScript(newScript)
                                } else {
                                    viewModel.updateScript(newScript)
                                }
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Default.Check, "Save")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                actions = actions + Action(
                    type = ActionType.CLICK,
                    x = 500,
                    y = 500,
                    baseDelay = 50,
                    desc = "Click ${actions.size + 1}"
                )
            }) {
                Icon(Icons.Default.Add, "Add Action")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = scriptName,
                            onValueChange = { scriptName = it },
                            label = { Text("Script Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = loopCount,
                            onValueChange = { loopCount = it },
                            label = { Text("Loop Count (0 = infinite)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = globalRandomOffset,
                            onValueChange = { globalRandomOffset = it },
                            label = { Text("Random Offset (px)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = globalRandomDelay,
                            onValueChange = { globalRandomDelay = it },
                            label = { Text("Random Delay (ms)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Actions (${actions.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            itemsIndexed(actions) { index, action ->
                ActionCard(
                    action = action,
                    index = index,
                    onUpdate = { updatedAction ->
                        actions = actions.toMutableList().apply {
                            set(index, updatedAction)
                        }
                    },
                    onDelete = {
                        actions = actions.toMutableList().apply {
                            removeAt(index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    action: Action,
    index: Int,
    onUpdate: (Action) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${index + 1}. ${action.type.name}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = when (action.type) {
                            ActionType.CLICK -> "(${action.x}, ${action.y})"
                            ActionType.SWIPE -> "(${action.startX}, ${action.startY}) → (${action.endX}, ${action.endY})"
                            ActionType.DELAY -> "${action.duration}ms"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            "Expand"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                when (action.type) {
                    ActionType.CLICK -> {
                        var x by remember { mutableStateOf(action.x.toString()) }
                        var y by remember { mutableStateOf(action.y.toString()) }
                        var delay by remember { mutableStateOf(action.baseDelay.toString()) }

                        OutlinedTextField(
                            value = x,
                            onValueChange = {
                                x = it
                                onUpdate(action.copy(x = it.toIntOrNull() ?: 0))
                            },
                            label = { Text("X") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = y,
                            onValueChange = {
                                y = it
                                onUpdate(action.copy(y = it.toIntOrNull() ?: 0))
                            },
                            label = { Text("Y") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = delay,
                            onValueChange = {
                                delay = it
                                onUpdate(action.copy(baseDelay = it.toLongOrNull() ?: 0))
                            },
                            label = { Text("Delay (ms)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ActionType.DELAY -> {
                        var duration by remember { mutableStateOf(action.duration.toString()) }
                        OutlinedTextField(
                            value = duration,
                            onValueChange = {
                                duration = it
                                onUpdate(action.copy(duration = it.toLongOrNull() ?: 0))
                            },
                            label = { Text("Duration (ms)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
