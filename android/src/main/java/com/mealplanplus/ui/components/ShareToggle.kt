package com.mealplanplus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.Teal

/** Per-item Share toggle — a globe that fills teal when the item is shared with followers. */
@Composable
fun ShareToggle(shared: Boolean, onClick: () -> Unit, size: Dp = 26.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = if (shared) Icons.Filled.Public else Icons.Outlined.Public,
            contentDescription = if (shared) "Shared with followers" else "Not shared",
            tint = if (shared) Teal else MutedFaint,
            modifier = Modifier.size(size * 0.66f),
        )
    }
}
