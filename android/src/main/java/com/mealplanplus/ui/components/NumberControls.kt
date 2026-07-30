package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Teal
import kotlin.math.abs
import kotlin.math.roundToInt

// Reusable numeric input controls, to replace raw number text-boxes app-wide.
//   • Stepper       — − value + for quantities / reps / macro & calorie targets.
//   • RulerPicker   — a horizontal, draggable number strip for age / height / weight.

/**
 * A − value + stepper. Works for whole numbers (decimals = 0) or decimals (e.g. step = 0.5).
 * [value] is a Double so it covers quantities; pass step/decimals to control granularity.
 */
@Composable
fun Stepper(
    value: Double,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    min: Double = 0.0,
    max: Double = 1_000_000.0,
    step: Double = 1.0,
    decimals: Int = 0,
    suffix: String = "",
) {
    fun fmt(v: Double): String = if (decimals <= 0) v.roundToInt().toString() else "%.${decimals}f".format(v)
    // Editable value in the middle; +/- adjust it. Type for big jumps, tap for small ones.
    var text by remember { mutableStateOf(fmt(value)) }
    LaunchedEffect(value) {
        val f = fmt(value)
        if (f != text && text.toDoubleOrNull() != value) text = f
    }
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, BorderCool, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", enabled = value - step >= min - 1e-9) { onChange((value - step).coerceAtLeast(min)) }
        BasicTextField(
            value = text,
            onValueChange = { s ->
                val allowDot = decimals > 0
                val f = s.filter { it.isDigit() || (allowDot && it == '.') }.let { v -> if (v.count { it == '.' } > 1) text else v }
                text = f
                f.toDoubleOrNull()?.let { onChange(it.coerceIn(min, max)) }
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimals > 0) KeyboardType.Decimal else KeyboardType.Number),
            cursorBrush = SolidColor(Teal),
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        )
        if (suffix.isNotEmpty()) Text(suffix, color = MutedFaint, fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp))
        StepButton("+", enabled = value + step <= max + 1e-9) { onChange((value + step).coerceAtMost(max)) }
    }
}

/** Integer convenience over [Stepper]. */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 1_000_000,
    step: Int = 1,
    suffix: String = "",
) = Stepper(value.toDouble(), { onChange(it.roundToInt()) }, modifier, min.toDouble(), max.toDouble(), step.toDouble(), 0, suffix)

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(label, color = if (enabled) Teal else BorderCool, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * A horizontal, draggable ruler of numbers. The value under the centre line is selected; drag or
 * tap a number to change it. On settle it snaps to the nearest number and reports [onChange].
 */
@Composable
fun RulerPicker(
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    val values = remember(range) { range.toList() }
    val listState = rememberLazyListState()
    val itemWidth = 46.dp

    // Index whose centre is closest to the viewport centre (live, for highlighting while dragging).
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf -1
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2f) - mid) }?.index ?: -1
        }
    }

    // Centre the current value on first layout.
    LaunchedEffect(Unit) {
        val idx = values.indexOf(value).coerceIn(0, values.lastIndex)
        listState.scrollToItem(idx)
    }

    // On settle, snap the centred item to the exact centre (once) and report it. Using a pixel-delta
    // guard means once it's centred the delta is ~0 and we stop — no animate⇄settle feedback loop.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) return@collect
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@collect
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val nearest = info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2f) - mid) } ?: return@collect
            val delta = (nearest.offset + nearest.size / 2f) - mid
            if (abs(delta) > 1f) listState.animateScrollBy(delta)
            val v = values[nearest.index]
            if (v != value) onChange(v)
        }
    }

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
            val sidePad = (maxWidth - itemWidth) / 2
            // Centre highlight pill behind the selected number.
            Box(Modifier.width(itemWidth).height(44.dp).clip(RoundedCornerShape(10.dp)).background(Teal.copy(alpha = 0.10f)))
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(horizontal = sidePad),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(values) { i, v ->
                    val active = i == centerIndex
                    Box(Modifier.width(itemWidth).height(44.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "$v",
                            color = if (active) Teal else MutedLight,
                            fontSize = if (active) 22.sp else 15.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.clip(CircleShape).clickable {
                                if (v != value) onChange(v)
                            },
                        )
                    }
                }
            }
            // Fixed centre tick.
            Box(Modifier.width(2.dp).height(52.dp).clip(RoundedCornerShape(1.dp)).background(Teal))
        }
        if (suffix.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(suffix, color = MutedFaint, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
