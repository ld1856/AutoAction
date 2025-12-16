package com.autoaction.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.autoaction.ui.theme.AutoActionTheme

enum class EdgePosition {
    LEFT, RIGHT, TOP
}

@Composable
fun EdgeIndicatorContent(
    onClick: () -> Unit,
    position: EdgePosition = EdgePosition.LEFT
) {
    AutoActionTheme {
        val (size, shape, icon) = when (position) {
            EdgePosition.LEFT -> Triple(
                Modifier.width(16.dp).height(48.dp),
                RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                Icons.Default.ChevronRight
            )
            EdgePosition.RIGHT -> Triple(
                Modifier.width(16.dp).height(48.dp),
                RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                Icons.Default.KeyboardArrowLeft
            )
            EdgePosition.TOP -> Triple(
                Modifier.width(48.dp).height(16.dp),
                RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                Icons.Default.KeyboardArrowDown
            )
        }

        Box(
            modifier = size
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Show Control Bar",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
