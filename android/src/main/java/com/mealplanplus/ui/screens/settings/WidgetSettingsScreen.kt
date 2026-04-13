package com.mealplanplus.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.util.WidgetAccentPresets
import com.mealplanplus.util.WidgetAppearanceState
import com.mealplanplus.util.WidgetBgStyle
import com.mealplanplus.util.WidgetTextColorPresets
import com.mealplanplus.util.WidgetTextWeight
import com.mealplanplus.util.WidgetTextSize
import kotlinx.coroutines.launch
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.minimalTopAppBarColors

// ─── Preset bg options shown in the UI ────────────────────────────────────────

private data class BgOption(val id: String, val label: String, val color: Color?)

private val bgOptions = listOf(
    BgOption(WidgetBgStyle.LIGHT,      "White",        Color(0xFFFFFFFF)),
    BgOption(WidgetBgStyle.LIGHT_GREY, "Light Grey",   Color(0xFFF5F5F5)),
    BgOption(WidgetBgStyle.GREEN_TINT, "Green",        Color(0xFFE8F5E9)),
    BgOption(WidgetBgStyle.TEAL_TINT,  "Teal",         Color(0xFFE0F2F1)),
    BgOption(WidgetBgStyle.DARK,       "Dark",         Color(0xFF1C1C1E)),
    BgOption(WidgetBgStyle.DYNAMIC,    "Wallpaper",    null),  // gradient placeholder
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: WidgetSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope   = rememberCoroutineScope()
    var isApplying by remember { mutableStateOf(false) }
    var applyDone  by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = { Text("Widget Appearance", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = minimalTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Live preview ─────────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            WidgetPreviewCard(state)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Background style ─────────────────────────────────────────────
            SettingsSection(title = "Background") {
                Text(
                    text = "Choose a background style for all widgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                BgStylePicker(
                    selected = state.bgStyle,
                    onSelect = { viewModel.setBgStyle(it) }
                )

                Spacer(Modifier.height(16.dp))
                // Opacity slider (only meaningful when not on a fully opaque dark bg)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val pct = (state.bgAlpha * 100).toInt()
                    Text(
                        text = "Opacity  $pct%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = state.bgAlpha,
                        onValueChange = { viewModel.setBgAlpha(it) },
                        valueRange = 0.3f..1.0f,
                        steps = 13  // 0.05 increments → 14 stops
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Header / accent color ─────────────────────────────────────────
            SettingsSection(title = "Header Color") {
                Text(
                    text = "Pick the color used for widget headers and progress rings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                AccentColorPicker(
                    selected = state.accentColor,
                    onSelect = { viewModel.setAccentColor(it) }
                )
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Text ─────────────────────────────────────────────────────────
            SettingsSection(title = "Text") {
                Text(
                    text = "Color, weight and size for widget labels and values",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Text color swatches
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(6.dp))
                TextColorPicker(
                    selected = state.textColor,
                    onSelect = { viewModel.setTextColor(it) }
                )

                // Weight toggle
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(6.dp))
                TextWeightPicker(
                    selected = state.textWeight,
                    onSelect = { viewModel.setTextWeight(it) }
                )

                // Size toggle
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Size",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(6.dp))
                TextSizePicker(
                    selected = state.textSize,
                    onSelect = { viewModel.setTextSize(it) }
                )
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Apply now button ──────────────────────────────────────────────
            SettingsSection(title = "Apply") {
                Text(
                    text = "Widgets refresh automatically every 15 minutes. Tap below to apply immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Button(
                    onClick = {
                        if (!isApplying) {
                            isApplying = true
                            applyDone  = false
                            scope.launch {
                                viewModel.forceUpdateWidgets()
                                isApplying = false
                                applyDone  = true
                            }
                        }
                    },
                    enabled = !isApplying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when {
                        isApplying -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        applyDone  -> Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        else       -> Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isApplying) "Applying…" else if (applyDone) "Applied!" else "Apply to widgets now")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Live preview ─────────────────────────────────────────────────────────────

@Composable
private fun WidgetPreviewCard(state: WidgetAppearanceState) {
    val bgColor = when (state.bgStyle) {
        WidgetBgStyle.LIGHT      -> Color(0xFFFFFFFF)
        WidgetBgStyle.LIGHT_GREY -> Color(0xFFF5F5F5)
        WidgetBgStyle.GREEN_TINT -> Color(0xFFE8F5E9)
        WidgetBgStyle.TEAL_TINT  -> Color(0xFFE0F2F1)
        WidgetBgStyle.DARK       -> Color(0xFF1C1C1E)
        else                     -> MaterialTheme.colorScheme.secondaryContainer
    }.copy(alpha = state.bgAlpha)

    val accentColor = try {
        Color(android.graphics.Color.parseColor(state.accentColor))
    } catch (_: Exception) { Color(0xFF2E7D52) }

    val onAccent = run {
        val lum = 0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue
        if (lum < 0.45f) Color.White else Color(0xFF1A1C1E)
    }

    // Resolved preview text color and styling
    val textColor: Color = if (state.textColor == WidgetTextColorPresets.AUTO) {
        // auto: contrast against bg
        val bgLum = 0.2126f * bgColor.red + 0.7152f * bgColor.green + 0.0722f * bgColor.blue
        if (bgLum > 0.4f) Color(0xFF1A1C1E) else Color.White
    } else {
        try { Color(android.graphics.Color.parseColor(state.textColor)) }
        catch (_: Exception) { Color(0xFF1A1C1E) }
    }
    val subTextColor = textColor.copy(alpha = 0.6f)
    val previewFontWeight = if (state.textWeight == WidgetTextWeight.BOLD) FontWeight.Bold else FontWeight.Normal
    val previewFontScale  = state.textSize.scale

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Mock widget shell
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
        ) {
            // Mock header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "✅ Today's Plan",
                    color = onAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Mock macro rings row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MockRing("Cal",  Color(0xFF2E7D52), textColor, subTextColor, previewFontWeight, previewFontScale)
                MockRing("Pro",  Color(0xFF4A90D9), textColor, subTextColor, previewFontWeight, previewFontScale)
                MockRing("Carb", Color(0xFFF5A623), textColor, subTextColor, previewFontWeight, previewFontScale)
                MockRing("Fat",  Color(0xFFE91E8C), textColor, subTextColor, previewFontWeight, previewFontScale)
            }

            // Mock day strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("M","T","W","T","F","S","S").forEachIndexed { i, day ->
                    val isToday = i == 0
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) accentColor else bgColor.copy(alpha = 1f))
                            .border(
                                width = if (!isToday) 1.dp else 0.dp,
                                color = accentColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = day,
                            color = if (isToday) onAccent else textColor,
                            fontSize = (10f * previewFontScale).sp,
                            fontWeight = previewFontWeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MockRing(
    label: String,
    ringColor: Color,
    textColor: Color = Color(0xFF1A1C1E),
    subTextColor: Color = Color(0xFF5D6062),
    fontWeight: FontWeight = FontWeight.Normal,
    fontScale: Float = 1f
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.12f))
                .border(width = 3.dp, color = ringColor.copy(alpha = 0.7f), shape = CircleShape)
        ) {
            Text(
                text = "75",
                color = textColor,
                fontSize = (11f * fontScale).sp,
                fontWeight = fontWeight
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = (9f * fontScale).sp, color = subTextColor, fontWeight = fontWeight)
    }
}

// ─── Background style picker ──────────────────────────────────────────────────

@Composable
private fun BgStylePicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bgOptions.forEach { option ->
            BgStyleChip(
                option = option,
                isSelected = selected == option.id,
                onClick = { onSelect(option.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BgStyleChip(
    option: BgOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
    val swatchColor = option.color ?: MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(swatchColor)
                .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                val iconColor = if (option.id == WidgetBgStyle.LIGHT || option.id == WidgetBgStyle.LIGHT_GREY)
                    Color(0xFF2E7D52) else Color.White
                Icon(Icons.Default.Check, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Accent color picker ──────────────────────────────────────────────────────

@Composable
private fun AccentColorPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WidgetAccentPresets.all.forEach { hex ->
            val color = try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) { Color.Gray }

            val isSelected = selected.equals(hex, ignoreCase = true)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(0.3f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            ) {
                if (isSelected) {
                    val lum = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (lum < 0.45f) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─── Text color picker ────────────────────────────────────────────────────────

@Composable
private fun TextColorPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WidgetTextColorPresets.all.forEach { hex ->
            val isAuto = hex == WidgetTextColorPresets.AUTO
            val isSelected = selected.equals(hex, ignoreCase = true)

            if (isAuto) {
                // "Auto" chip — shows "A" label
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Gray.copy(0.3f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                ) {
                    Text(
                        text = if (isSelected) "✓" else "A",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                val color = try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (_: Exception) { Color.Gray }
                val lum = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else Color.Gray.copy(0.3f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (lum < 0.45f) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Text weight picker ───────────────────────────────────────────────────────

@Composable
private fun TextWeightPicker(selected: WidgetTextWeight, onSelect: (WidgetTextWeight) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WidgetTextWeight.values().forEach { weight ->
            val isSelected = selected == weight
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(weight) },
                label = {
                    Text(
                        text = weight.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = if (weight == WidgetTextWeight.BOLD) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// ─── Text size picker ─────────────────────────────────────────────────────────

@Composable
private fun TextSizePicker(selected: WidgetTextSize, onSelect: (WidgetTextSize) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WidgetTextSize.values().forEach { size ->
            val isSelected = selected == size
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(size) },
                label = {
                    Text(
                        text = size.label,
                        fontSize = when (size) {
                            WidgetTextSize.SMALL  -> 11.sp
                            WidgetTextSize.MEDIUM -> 13.sp
                            WidgetTextSize.LARGE  -> 15.sp
                        }
                    )
                }
            )
        }
    }
}
