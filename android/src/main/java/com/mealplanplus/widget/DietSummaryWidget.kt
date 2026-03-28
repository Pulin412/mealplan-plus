package com.mealplanplus.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mealplanplus.MainActivity
import com.mealplanplus.util.WidgetAppearanceState
import com.mealplanplus.util.WidgetBgStyle
import com.mealplanplus.util.WidgetPreferences
import kotlin.math.roundToInt

// ─── Design tokens ────────────────────────────────────────────────────────────

private val White       = Color(0xFFFFFFFF)
private val OffWhite    = Color(0xFFFAFCFE)
private val TextSub     = Color(0xFF5D6062)
private val TextMuted   = Color(0xFF89938C)

// Macro ring colours (matches HomeScreen MacroRingsCard)
private val CaloriesColor = Color(0xFF2E7D52)   // brand green
private val ProteinColor  = Color(0xFF4A90D9)   // blue
private val CarbsColor    = Color(0xFFF5A623)   // orange
private val FatColor      = Color(0xFFE91E8C)   // pink

// ─── Responsive sizes ─────────────────────────────────────────────────────────

private val WIDE_SIZE   = DpSize(260.dp, 90.dp)
private val SQUARE_SIZE = DpSize(150.dp, 150.dp)

/**
 * Diet Summary widget — one large Calories progress ring plus a compact
 * logged/goal breakdown for Protein, Carbs and Fat.
 *
 * Supports two responsive layouts:
 *   • Wide  (≥ 260 dp wide)  → ring on left, macro text rows on right
 *   • Square (≥ 150 dp tall) → ring on top, macro text rows below
 */
class DietSummaryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(WIDE_SIZE, SQUARE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo        = WidgetDataRepository(context)
        val prefs       = WidgetPreferences.getAppearance(context)
        val density     = context.resources.displayMetrics.density
        val summaryFlow = repo.getTodayDietSummaryFlow()

        provideContent {
            val summary by summaryFlow.collectAsState(initial = null)
            // Re-draw bitmap only when summary actually changes
            val caloriesBitmap = remember(summary) {
                summary?.let { s ->
                    macroRingBitmap(s.consumedCalories, s.goalCalories, "kcal", CaloriesColor, density)
                }
            }
            GlanceTheme { DietSummaryContent(summary, caloriesBitmap, prefs) }
        }
    }
}

// ─── Widget UI ────────────────────────────────────────────────────────────────

@Composable
private fun DietSummaryContent(
    summary: WidgetDataRepository.DietSummaryData?,
    caloriesBitmap: Bitmap?,
    prefs: WidgetAppearanceState = WidgetAppearanceState()
) {
    val currentSize = LocalSize.current
    val isWide = currentSize.width >= 200.dp

    val bgModifier = if (prefs.useDynamic) {
        GlanceModifier.background(GlanceTheme.colors.widgetBackground)
    } else {
        GlanceModifier.background(widgetBgColor(prefs))
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(bgModifier)
            .cornerRadius(16.dp)
            .clickable(onClick = actionRunCallback<OpenHomeFromSummaryCallback>()),
        contentAlignment = Alignment.Center
    ) {
        if (summary == null || caloriesBitmap == null) {
            EmptyState(prefs)
        } else if (isWide) {
            WideLayout(summary, caloriesBitmap, prefs)
        } else {
            SquareLayout(summary, caloriesBitmap, prefs)
        }
    }
}

/** Wide (rectangle): calories ring on the left, macro text rows on the right. */
@Composable
private fun WideLayout(
    summary: WidgetDataRepository.DietSummaryData,
    caloriesBitmap: Bitmap,
    prefs: WidgetAppearanceState
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MacroRingItem(caloriesBitmap, "Calories", 68.dp, prefs)
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(
            modifier = GlanceModifier.defaultWeight().wrapContentHeight()
        ) {
            MacroTextRow("Protein", summary.consumedProtein, summary.goalProtein, "g",    ProteinColor, prefs)
            Spacer(modifier = GlanceModifier.height(5.dp))
            MacroTextRow("Carbs",   summary.consumedCarbs,   summary.goalCarbs,   "g",    CarbsColor,   prefs)
            Spacer(modifier = GlanceModifier.height(5.dp))
            MacroTextRow("Fat",     summary.consumedFat,     summary.goalFat,     "g",    FatColor,     prefs)
        }
    }
}

/** Square: calories ring centered on top, macro text rows stacked below. */
@Composable
private fun SquareLayout(
    summary: WidgetDataRepository.DietSummaryData,
    caloriesBitmap: Bitmap,
    prefs: WidgetAppearanceState
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MacroRingItem(caloriesBitmap, "Calories", 72.dp, prefs)
        Spacer(modifier = GlanceModifier.height(8.dp))
        MacroTextRow("Protein", summary.consumedProtein, summary.goalProtein, "g",    ProteinColor, prefs)
        Spacer(modifier = GlanceModifier.height(4.dp))
        MacroTextRow("Carbs",   summary.consumedCarbs,   summary.goalCarbs,   "g",    CarbsColor,   prefs)
        Spacer(modifier = GlanceModifier.height(4.dp))
        MacroTextRow("Fat",     summary.consumedFat,     summary.goalFat,     "g",    FatColor,     prefs)
    }
}

@Composable
private fun MacroRingItem(
    bitmap: Bitmap,
    label: String,
    imageSize: Dp,
    prefs: WidgetAppearanceState,
    modifier: GlanceModifier = GlanceModifier
) {
    val labelColor = widgetSubTextColor(prefs)?.let { ColorProvider(it) }
        ?: GlanceTheme.colors.onSurfaceVariant
    Column(
        modifier = modifier.wrapContentHeight().padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = label,
            modifier = GlanceModifier.size(imageSize)
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(
                color = labelColor,
                fontSize = (9f.scaled(prefs)).sp,
                fontWeight = widgetFontWeight(prefs),
                textAlign = TextAlign.Center
            )
        )
    }
}

/** A single macro row: coloured dot · label · consumed/goalUnit */
@Composable
private fun MacroTextRow(
    label: String,
    consumed: Int,
    goal: Int,
    unit: String,
    color: Color,
    prefs: WidgetAppearanceState
) {
    val textColor = widgetTextColor(prefs)?.let { ColorProvider(it) }
        ?: GlanceTheme.colors.onSurface
    val subColor  = widgetSubTextColor(prefs)?.let { ColorProvider(it) }
        ?: GlanceTheme.colors.onSurfaceVariant
    val goalMet = goal > 0 && consumed >= goal
    val valueStr = if (goalMet) "✓" else "$consumed/$goal$unit"

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colour dot
        Box(
            modifier = GlanceModifier
                .size(7.dp)
                .background(ColorProvider(color))
                .cornerRadius(4.dp),
            contentAlignment = Alignment.Center
        ) {}
        Spacer(modifier = GlanceModifier.width(6.dp))
        // Macro name
        Text(
            text = label,
            style = TextStyle(
                color = textColor,
                fontSize = (10f.scaled(prefs)).sp,
                fontWeight = widgetFontWeight(prefs)
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        // Value
        Text(
            text = valueStr,
            style = TextStyle(
                color = if (goalMet) ColorProvider(color) else subColor,
                fontSize = (10f.scaled(prefs)).sp
            )
        )
    }
}

@Composable
private fun EmptyState(prefs: WidgetAppearanceState = WidgetAppearanceState()) {
    val textColor = widgetSubTextColor(prefs)?.let { ColorProvider(it) }
        ?: GlanceTheme.colors.onSurfaceVariant
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "🥗",
            style = TextStyle(fontSize = (24f.scaled(prefs)).sp, textAlign = TextAlign.Center)
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = "No diet planned today",
            style = TextStyle(
                color = textColor,
                fontSize = (11f.scaled(prefs)).sp,
                fontWeight = widgetFontWeight(prefs),
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

// ─── Bitmap drawing ───────────────────────────────────────────────────────────

/**
 * Draws a single circular progress ring into a [Bitmap].
 *
 * @param consumed   Amount consumed today
 * @param goal       Daily goal (from diet plan)
 * @param unit       Label shown below the value ("g" / "kcal")
 * @param ringColor  Arc colour (also used for the center text)
 * @param density    Screen density (from DisplayMetrics) for correct pixel sizing
 */
private fun macroRingBitmap(
    consumed: Int,
    goal: Int,
    unit: String,
    ringColor: Color,
    density: Float
): Bitmap {
    val sizePx   = (72 * density).roundToInt()
    val bmp      = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas   = Canvas(bmp)

    val progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val cx       = sizePx / 2f
    val cy       = sizePx / 2f
    val strokeW  = sizePx * 0.115f
    val radius   = (sizePx / 2f) - strokeW * 0.65f

    val colorArgb = ringColor.toAndroidArgb()
    val trackArgb = ringColor.copy(alpha = 0.15f).toAndroidArgb()

    // ── Background track ──────────────────────────────────────────────────────
    canvas.drawCircle(
        cx, cy, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color      = trackArgb
            style      = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap  = Paint.Cap.ROUND
        }
    )

    // ── Progress arc ──────────────────────────────────────────────────────────
    if (progress > 0.005f) {
        canvas.drawArc(
            RectF(cx - radius, cy - radius, cx + radius, cy + radius),
            -90f, 360f * progress, false,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color      = colorArgb
                style      = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap  = Paint.Cap.ROUND
            }
        )
    }

    // ── Center text: remaining (goal - consumed), vertically centered ────────
    val remaining    = (goal - consumed).coerceAtLeast(0)
    val goalMet      = consumed >= goal && goal > 0
    val centerStr    = if (goalMet) "✓" else remaining.toString()
    val subLabel     = if (goalMet) "done" else "$unit left"

    val valueFontPx  = when {
        centerStr.length >= 4 -> sizePx * 0.175f
        centerStr.length == 3 -> sizePx * 0.20f
        else                  -> sizePx * 0.225f
    }
    val unitFontPx   = sizePx * 0.115f
    val lineGap      = sizePx * 0.018f
    val mutedArgb    = android.graphics.Color.argb(200, 93, 96, 98) // TextSub
    // When goal met, use a slightly desaturated tint so ✓ doesn't over-shout
    val centerArgb   = if (goalMet) ringColor.copy(alpha = 0.75f).toAndroidArgb() else colorArgb

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = centerArgb
        textSize  = valueFontPx
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.DEFAULT_BOLD
    }
    val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = mutedArgb
        textSize  = unitFontPx
        textAlign = Paint.Align.CENTER
    }

    // Proper two-line centering using font metrics
    val valueAscent  = -valuePaint.ascent()   // positive magnitude above baseline
    val valueDescent = valuePaint.descent()
    val unitAscent   = -unitPaint.ascent()
    val unitDescent  = unitPaint.descent()

    val totalH     = valueAscent + valueDescent + lineGap + unitAscent + unitDescent
    val topY       = cy - totalH / 2f
    val valueBaseY = topY + valueAscent
    val unitBaseY  = valueBaseY + valueDescent + lineGap + unitAscent

    canvas.drawText(centerStr, cx, valueBaseY, valuePaint)
    canvas.drawText(subLabel,  cx, unitBaseY,  unitPaint)

    return bmp
}

/** Converts a Compose [Color] to an Android ARGB integer for use with [Paint]. */
private fun Color.toAndroidArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red   * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue  * 255).roundToInt()
)

// ─── Action callbacks ─────────────────────────────────────────────────────────

class OpenHomeFromSummaryCallback : ActionCallback {
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

class DietSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DietSummaryWidget()
}
