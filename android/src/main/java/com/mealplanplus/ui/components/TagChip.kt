package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mealplanplus.data.model.Tag

/**
 * Displays a tag with its assigned color
 */
@Composable
fun TagChip(
    tag: Tag,
    modifier: Modifier = Modifier
) {
    val color = parseColor(tag.effectiveColor)

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small
        )
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelSmall,
            color = color.darken(0.3f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Displays a tag chip with just name and color string
 */
@Composable
fun TagChip(
    name: String,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val color = parseColor(colorHex)

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small
        )
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = color.darken(0.3f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Parse hex color string to Color
 */
fun parseColor(hexColor: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hexColor)
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFFFFEB3B) // Default yellow
    }
}

/**
 * Darken a color by a factor (0.0 = no change, 1.0 = black)
 */
fun Color.darken(factor: Float): Color {
    return Color(
        red = red * (1 - factor),
        green = green * (1 - factor),
        blue = blue * (1 - factor),
        alpha = alpha
    )
}

/**
 * Lighten a color by a factor (0.0 = no change, 1.0 = white)
 */
fun Color.lighten(factor: Float): Color {
    return Color(
        red = red + (1 - red) * factor,
        green = green + (1 - green) * factor,
        blue = blue + (1 - blue) * factor,
        alpha = alpha
    )
}
