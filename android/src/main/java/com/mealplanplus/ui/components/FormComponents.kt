package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.*

// ─── Form label (uppercase, small, muted) ────────────────────────────────────

@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) {
    val color = TextMuted
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        color = color,
        modifier = modifier
    )
}

// ─── Section header (uppercase, small, muted — for card sections) ─────────────

@Composable
fun FormSectionLabel(text: String, modifier: Modifier = Modifier) {
    val color = TextMuted
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.6.sp,
        color = color,
        modifier = modifier
    )
}

// ─── FormGroup: label + content slot ─────────────────────────────────────────

@Composable
fun FormGroup(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        FormLabel(label, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

// ─── Styled text field matching design-future.html ───────────────────────────

@Composable
fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val textPrimary = TextPrimary
    val cardBg = CardBg
    val iconBgGray = IconBgGray
    val dividerColor = DividerColor
    val textPlaceholder = TextPlaceholder

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = TextStyle(
            color = textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        ),
        cursorBrush = SolidColor(textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .background(
                        if (isFocused) cardBg else iconBgGray,
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isFocused) textPrimary else dividerColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = textPlaceholder, fontSize = 14.sp)
                }
                innerTextField()
            }
        }
    )
}

// ─── Primary button (dark, full width) ───────────────────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textPrimary = TextPrimary
    val cardBg = CardBg
    val mutedBg = IconBgGray

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled && !isLoading) textPrimary else mutedBg)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = cardBg,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = if (enabled) cardBg else TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Outline button (ghost / secondary) ──────────────────────────────────────

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textPrimary = TextPrimary
    val cardBg = CardBg
    val dividerColor = DividerColor

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.5.dp, dividerColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Screen close / back header row ──────────────────────────────────────────

@Composable
fun ScreenCloseHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = CardBg
    val textPrimary = TextPrimary
    val textMuted = TextMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text("✕", fontSize = 18.sp, color = textMuted)
        }
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
    }
}

// ─── Subtitle row (gray bg, small muted text) ─────────────────────────────────

@Composable
fun ScreenSubtitle(text: String, modifier: Modifier = Modifier) {
    val bg = BgPage
    val color = TextMuted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 14.dp)
    ) {
        Text(text = text, fontSize = 12.sp, color = color)
    }
}
