package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Surface

/** Standard card: white surface, 1px border, 12dp radius (design §3 component vocabulary). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(padding),
        content = content,
    )
}
