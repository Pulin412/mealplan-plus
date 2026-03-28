package com.mealplanplus.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mealplanplus.MainActivity
import com.mealplanplus.util.WidgetAppearanceState
import com.mealplanplus.util.WidgetPreferences

// ─── Design tokens ────────────────────────────────────────────────────────────

private val BrandGreen  = Color(0xFF2E7D52)
private val DarkGreen   = Color(0xFF1B5E20)
private val LightGreen  = Color(0xFFE8F5E9)
private val GreenText   = Color(0xFF95F890).copy(alpha = 0.85f)
private val White       = Color(0xFFFFFFFF)
private val OffWhite    = Color(0xFFFAFCFE)
private val SurfaceVar  = Color(0xFFDBE5DE)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSub     = Color(0xFF5D6062)
private val TextMuted   = Color(0xFF89938C)

// Slot icon background colours (matching HomeScreen TodaysPlanCard)
private fun slotBgColor(slotType: String): Color = when {
    slotType == "BREAKFAST"                         -> Color(0xFFFFF3E0)
    slotType == "LUNCH"                             -> Color(0xFFE3F2FD)
    slotType == "DINNER"                            -> Color(0xFFEDE7F6)
    slotType in listOf("PRE_WORKOUT","POST_WORKOUT")-> Color(0xFFE8F5E9)
    slotType in listOf("EARLY_MORNING","NOON","MID_MORNING") -> Color(0xFFFFFDE7)
    slotType in listOf("EVENING","EVENING_SNACK","POST_DINNER") -> Color(0xFFFCE4EC)
    else                                            -> Color(0xFFE8F5E9)
}

private fun slotEmoji(slotType: String): String = when (slotType) {
    "EARLY_MORNING"  -> "🌅"
    "BREAKFAST"      -> "🍳"
    "NOON"           -> "☀️"
    "MID_MORNING"    -> "🥤"
    "LUNCH"          -> "🥗"
    "PRE_WORKOUT"    -> "⚡"
    "EVENING"        -> "🌆"
    "EVENING_SNACK"  -> "🍎"
    "POST_WORKOUT"   -> "💪"
    "DINNER"         -> "🍽"
    "POST_DINNER"    -> "🌙"
    else             -> "⭐"
}

private val slotTypeKey = ActionParameters.Key<String>("slot_type")
private val isLoggedKey = ActionParameters.Key<Boolean>("is_logged")

/**
 * Today's Plan widget — meal slots with emoji icons and tap-to-log checkboxes.
 * Mirrors the TodaysPlanCard on the Home screen.
 */
class TodayPlanWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo      = WidgetDataRepository(context)
        val prefs     = WidgetPreferences.getAppearance(context)
        val slotsFlow = repo.getTodaySlotsFlow()

        provideContent {
            val state by slotsFlow.collectAsState(initial = null to emptyList())
            GlanceTheme { TodayPlanWidgetContent(state.first, state.second, prefs) }
        }
    }
}

@Composable
private fun TodayPlanWidgetContent(
    dietName: String?,
    slots: List<WidgetDataRepository.TodaySlot>,
    prefs: WidgetAppearanceState = WidgetAppearanceState()
) {
    val loggedCount = slots.count { it.isLogged }
    val total       = slots.size

    val bgModifier = if (prefs.useDynamic) {
        GlanceModifier.background(GlanceTheme.colors.widgetBackground)
    } else {
        GlanceModifier.background(widgetBgColor(prefs))
    }
    val accentColor   = if (prefs.useDynamic) null else widgetAccentColor(prefs)
    val onAccentColor = if (prefs.useDynamic) null else widgetOnAccentColor(prefs)
    val accentContainerColor = if (prefs.useDynamic) null else widgetAccentContainer(prefs)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(bgModifier)
            .cornerRadius(16.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .run {
                    if (accentColor != null) background(accentColor)
                    else background(GlanceTheme.colors.primary)
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✅ Today's Plan",
                    style = TextStyle(
                        color = if (onAccentColor != null) ColorProvider(onAccentColor)
                                else GlanceTheme.colors.onPrimary,
                        fontSize = (13f.scaled(prefs)).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (total > 0) {
                    Box(
                        modifier = GlanceModifier
                            .run {
                                if (accentContainerColor != null) background(accentContainerColor)
                                else background(GlanceTheme.colors.primaryContainer)
                            }
                            .cornerRadius(10.dp)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$loggedCount/$total",
                            style = TextStyle(
                                color = if (onAccentColor != null) ColorProvider(onAccentColor)
                                        else GlanceTheme.colors.onPrimaryContainer,
                                fontSize = (10f.scaled(prefs)).sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            if (dietName != null) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = dietName,
                    style = TextStyle(
                        color = if (onAccentColor != null) ColorProvider(onAccentColor)
                                else GlanceTheme.colors.onPrimary,
                        fontSize = (10f.scaled(prefs)).sp
                    )
                )
            }

            // Progress dots
            if (total > 0) {
                Spacer(modifier = GlanceModifier.height(5.dp))
                val dots = (1..total).joinToString(" ") { i -> if (i <= loggedCount) "●" else "○" }
                Text(
                    text = dots,
                    style = TextStyle(
                        color = if (onAccentColor != null) ColorProvider(onAccentColor)
                                else GlanceTheme.colors.onPrimary,
                        fontSize = (9f.scaled(prefs)).sp
                    )
                )
            }
        }

        // ── Slot rows ────────────────────────────────────────────────────────
        if (slots.isEmpty()) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No plan for today",
                    style = TextStyle(
                        color = widgetSubTextColor(prefs)?.let { ColorProvider(it) }
                            ?: GlanceTheme.colors.onSurfaceVariant,
                        fontSize = (12f.scaled(prefs)).sp,
                        fontWeight = widgetFontWeight(prefs)
                    )
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
        } else {
            Spacer(modifier = GlanceModifier.height(6.dp))
            slots.take(6).forEach { slot ->
                SlotRow(slot = slot, prefs = prefs, accentColor = accentColor, onAccentColor = onAccentColor)
            }
            if (slots.size > 6) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Box(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    Text(
                        text = "+ ${slots.size - 6} more",
                        style = TextStyle(
                            color = widgetSubTextColor(prefs)?.let { ColorProvider(it) }
                                ?: GlanceTheme.colors.onSurfaceVariant,
                            fontSize = (10f.scaled(prefs)).sp
                        )
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
        }
    }
}

@Composable
private fun SlotRow(
    slot: WidgetDataRepository.TodaySlot,
    prefs: WidgetAppearanceState = WidgetAppearanceState(),
    accentColor: Color? = null,
    onAccentColor: Color? = null
) {
    val toggleAction = actionRunCallback<ToggleSlotCallback>(
        actionParametersOf(
            slotTypeKey to slot.slotType,
            isLoggedKey to slot.isLogged
        )
    )

    val textDecoration = if (slot.isLogged) TextDecoration.LineThrough else TextDecoration.None
    val bodyText   = widgetTextColor(prefs)?.let { ColorProvider(it) }
    val subText    = widgetSubTextColor(prefs)?.let { ColorProvider(it) }
    val textColor  = when {
        slot.isLogged && subText != null -> subText
        slot.isLogged                    -> GlanceTheme.colors.onSurfaceVariant
        bodyText != null                 -> bodyText
        else                             -> GlanceTheme.colors.onSurface
    }
    // Slot emoji bg colors stay hardcoded (semantic per meal type)
    val iconBg     = slotBgColor(slot.slotType)
    val checkboxBg = when {
        slot.isLogged && accentColor != null -> accentColor
        slot.isLogged                        -> GlanceTheme.colors.primary
        else                                 -> GlanceTheme.colors.surfaceVariant
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clickable(onClick = toggleAction),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji icon box
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .background(ColorProvider(iconBg))
                .cornerRadius(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = slotEmoji(slot.slotType),
                style = TextStyle(fontSize = 14.sp)
            )
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        // Slot name
        Text(
            text = slot.displayName,
            style = TextStyle(
                color = textColor,
                fontSize = (12f.scaled(prefs)).sp,
                fontWeight = if (slot.isLogged) FontWeight.Normal else widgetFontWeight(prefs),
                textDecoration = textDecoration
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Checkbox circle
        Box(
            modifier = GlanceModifier
                .size(22.dp)
                .run {
                    when (checkboxBg) {
                        is Color -> background(checkboxBg)
                        is androidx.glance.unit.ColorProvider -> background(checkboxBg)
                        else -> background(GlanceTheme.colors.surfaceVariant)
                    }
                }
                .cornerRadius(11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (slot.isLogged) "✓" else "",
                style = TextStyle(
                    color = when {
                        slot.isLogged && onAccentColor != null -> ColorProvider(onAccentColor)
                        slot.isLogged                          -> GlanceTheme.colors.onPrimary
                        else                                   -> GlanceTheme.colors.onSurfaceVariant
                    },
                    fontSize = (11f.scaled(prefs)).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class ToggleSlotCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val slotType = parameters[slotTypeKey] ?: return
        val repo     = WidgetDataRepository(context)

        // Use a DB-direct check instead of the baked-in isLoggedKey parameter.
        // The parameter can be stale if the app updated the DB since the widget last rendered,
        // causing the toggle to go in the wrong direction.
        repo.toggleSlot(slotType)

        TodayPlanWidget().updateAll(context)
        DietSummaryWidget().updateAll(context)
    }
}

class OpenHomeCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAVIGATE_TO, NAV_HOME)
            }
        )
    }
}

class TodayPlanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayPlanWidget()
}
