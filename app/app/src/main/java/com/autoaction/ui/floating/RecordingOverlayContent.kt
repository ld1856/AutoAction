package com.autoaction.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autoaction.ui.theme.AutoActionTheme

@Composable
fun RecordingOverlayContent(
    actionCount: Int,
    onStop: () -> Unit,
    onSave: () -> Unit
) {
    AutoActionTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(4.dp, Color.Red)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "RECORDING",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Red
                )
                Text(
                    text = "$actionCount actions recorded",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, "Stop")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        enabled = actionCount > 0
                    ) {
                        Icon(Icons.Default.Check, "Save")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}
