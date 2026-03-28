package com.mealplanplus.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.background
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mealplanplus.MainActivity
import com.mealplanplus.data.model.PlanWithDietName
import com.mealplanplus.util.WidgetAppearanceState
import com.mealplanplus.util.WidgetPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ─── Design tokens (mirrors the app's Color.kt palette) ──────────────────────

private val BrandGreen   = Color(0xFF2E7D52)
private val DarkGreen    = Color(0xFF1B5E20)
private val MediumGreen  = Color(0xFF388E3C)
private val LightGreen   = Color(0xFFE8F5E9)
private val AccentTeal   = Color(0xFF00695C)
private val White        = Color(0xFFFFFFFF)
private val OffWhite     = Color(0xFFFAFCFE)
private val SurfaceVar   = Color(0xFFDBE5DE)
private val TextPrimary  = Color(0xFF1A1C1E)
private val TextMuted    = Color(0xFF89938C)
private val GreenText    = Color(0xFF95F890).copy(alpha = 0.9f)

private val dateKey   = ActionParameters.Key<String>("nav_date")
private val dietIdKey = ActionParameters.Key<Long>("nav_diet_id")

/**
 * Mini calendar widget — 7-day week view starting today.
 * No header; tapping any day opens its meal plan (diet detail if assigned, else
 * the calendar screen for that date).
 */
class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = WidgetDataRepository(context)
        val weekPlans = try { repo.getWeekPlans() } catch (e: Exception) { emptyList() }
        val prefs = WidgetPreferences.getAppearance(context)

        provideContent {
            GlanceTheme { CalendarWidgetContent(weekPlans, prefs) }
        }
    }
}

@Composable
private fun CalendarWidgetContent(
    weekPlans: List<PlanWithDietName>,
    prefs: WidgetAppearanceState = WidgetAppearanceState()
) {
    val today   = LocalDate.now()
    val fmt     = DateTimeFormatter.ISO_LOCAL_DATE
    val planMap = weekPlans.associateBy { it.date }

    val bgModifier = if (prefs.useDynamic) {
        GlanceModifier.background(GlanceTheme.colors.widgetBackground)
    } else {
        GlanceModifier.background(widgetBgColor(prefs))
    }

    // ── 7-day row fills the whole widget (no header) ─────────────────────────
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(bgModifier)
            .cornerRadius(16.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (offset in 0..6) {
            val day     = today.plusDays(offset.toLong())
            val plan    = planMap[day.format(fmt)]
            val isToday = offset == 0

            DayCell(
                date     = day,
                dietName = plan?.dietName,
                isToday  = isToday,
                dietId   = plan?.dietId,
                prefs    = prefs,
                modifier = GlanceModifier.defaultWeight()
            )
            if (offset < 6) Spacer(modifier = GlanceModifier.width(3.dp))
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    dietName: String?,
    isToday: Boolean,
    dietId: Long?,
    prefs: WidgetAppearanceState = WidgetAppearanceState(),
    modifier: GlanceModifier = GlanceModifier
) {
    val hasDiet = dietId != null

    // Resolve cell background and text colors based on prefs vs dynamic theme
    val accentColor = if (prefs.useDynamic) null else widgetAccentColor(prefs)
    val onAccentColor = if (prefs.useDynamic) null else widgetOnAccentColor(prefs)
    val accentContainerColor = if (prefs.useDynamic) null else widgetAccentContainer(prefs)

    // User-set body text colors (null = fall back to theme)
    val bodyTextColor = widgetTextColor(prefs)?.let { ColorProvider(it) }
    val subBodyTextColor = widgetSubTextColor(prefs)?.let { ColorProvider(it) }

    val bgColor: Any = when {  // ColorProvider (dynamic) or Color (manual)
        isToday && accentColor != null -> accentColor
        isToday                        -> GlanceTheme.colors.primary
        hasDiet && accentContainerColor != null -> accentContainerColor
        hasDiet                        -> GlanceTheme.colors.primaryContainer
        else                           -> GlanceTheme.colors.surfaceVariant
    }
    val dayNumColor = when {
        isToday && onAccentColor != null -> ColorProvider(onAccentColor)
        isToday                          -> GlanceTheme.colors.onPrimary
        bodyTextColor != null            -> bodyTextColor
        else                             -> GlanceTheme.colors.onSurface
    }
    val dayLabelColor = when {
        isToday && onAccentColor != null  -> ColorProvider(onAccentColor)
        isToday                           -> GlanceTheme.colors.onPrimary
        hasDiet && onAccentColor != null  -> ColorProvider(onAccentColor.copy(alpha = 0.8f))
        hasDiet                           -> GlanceTheme.colors.onPrimaryContainer
        subBodyTextColor != null          -> subBodyTextColor
        else                              -> GlanceTheme.colors.onSurfaceVariant
    }
    val dietColor = dayLabelColor

    val dayLabel = date.dayOfWeek
        .getDisplayName(JTextStyle.SHORT, Locale.getDefault())
        .uppercase(Locale.getDefault())
        .take(2)

    val clickParams = actionParametersOf(
        dateKey   to date.toString(),
        dietIdKey to (dietId ?: -1L)
    )

    val cellModifier = modifier
        .run {
            when (bgColor) {
                is Color         -> background(bgColor)
                is androidx.glance.unit.ColorProvider -> background(bgColor)
                else             -> background(GlanceTheme.colors.surfaceVariant)
            }
        }
        .cornerRadius(10.dp)
        .padding(horizontal = 2.dp, vertical = 5.dp)
        .clickable(onClick = actionRunCallback<CalendarDayClickCallback>(clickParams))

    Column(
        modifier = cellModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Day letter (Mon → M, Tue → T, etc.)
        Text(
            text = dayLabel,
            style = TextStyle(
                color = dayLabelColor,
                fontSize = (7f.scaled(prefs)).sp,
                fontWeight = widgetFontWeight(prefs)
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        // Day number
        Text(
            text = date.dayOfMonth.toString(),
            style = TextStyle(
                color = dayNumColor,
                fontSize = (13f.scaled(prefs)).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        // Full diet name (2 lines) or a muted dot when no plan
        if (hasDiet) {
            Text(
                text = dietName ?: "·",
                style = TextStyle(
                    color = dietColor,
                    fontSize = (6f.scaled(prefs)).sp,
                    fontWeight = widgetFontWeight(prefs),
                    textAlign = TextAlign.Center
                ),
                maxLines = 2
            )
        } else {
            Text(
                text = "·",
                style = TextStyle(
                    color = subBodyTextColor ?: GlanceTheme.colors.onSurfaceVariant,
                    fontSize = (8f.scaled(prefs)).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

class CalendarDayClickCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val date   = parameters[dateKey]   ?: return
        val dietId = parameters[dietIdKey] ?: -1L
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                // Open the Meal Plan (CalendarScreen) pre-selected to the tapped date.
                putExtra(EXTRA_NAVIGATE_TO, NAV_CALENDAR_FOR_DATE)
                putExtra(EXTRA_DATE, date)
                putExtra(EXTRA_DIET_ID, dietId)   // kept for context; -1 = no diet assigned
            }
        )
    }
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}

// ─── Shared intent extra constants ───────────────────────────────────────────

const val EXTRA_NAVIGATE_TO = "widget_navigate_to"
const val EXTRA_DATE        = "widget_date"
const val EXTRA_DIET_ID     = "widget_diet_id"
const val EXTRA_SLOT_TYPE   = "widget_slot_type"

const val NAV_CALENDAR           = "calendar"
const val NAV_DIET_DETAIL        = "diet_detail"
const val NAV_HOME               = "home"
/** Navigate to the DailyLog screen pre-loaded for a specific date. */
const val NAV_LOG_FOR_DATE       = "log_for_date"
/** Navigate to the Meal Plan (CalendarScreen) pre-selected to a specific date. */
const val NAV_CALENDAR_FOR_DATE  = "calendar_for_date"

/**
 * Carries a widget deep-link between [MainActivity] and [MealPlanNavHost].
 *
 * [id] is a monotonically increasing timestamp so that two taps to the same
 * destination type (e.g. tapping different calendar dates that both have diets)
 * each produce a distinct value and always trigger the LaunchedEffect in NavHost.
 */
data class WidgetDeepLink(
    val target: String,
    val date:   String? = null,
    val dietId: Long?   = null,
    val id:     Long    = System.currentTimeMillis()
)
