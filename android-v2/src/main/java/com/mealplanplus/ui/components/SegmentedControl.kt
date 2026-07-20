package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Surface

/**
 * Segmented toggle (design: Foods list/compact view switch). A bordered white track with
 * the active segment filled `selectedColor` (Ink/black by default). Each option renders its
 * own content and picks its own colour via the `selected` flag.
 */
@Composable
fun SegmentedControl(
    optionCount: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    segmentWidth: androidx.compose.ui.unit.Dp = 36.dp,
    segmentHeight: androidx.compose.ui.unit.Dp = 30.dp,
    selectedColor: Color = Ink,
    option: @Composable (index: Int, selected: Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, BorderCool, RoundedCornerShape(9.dp)),
    ) {
        repeat(optionCount) { i ->
            val selected = i == selectedIndex
            val interaction = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(segmentWidth)
                    .height(segmentHeight)
                    .background(if (selected) selectedColor else Surface)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(i) },
            ) {
                option(i, selected)
            }
        }
    }
}
