package com.mealplanplus.ui.screens.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import com.mealplanplus.ui.components.Stepper
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.generated.model.HealthMetricDto
import com.mealplanplus.ui.components.AppCard
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.BorderSoft
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Success
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import com.mealplanplus.ui.theme.StreakFlame
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Diastolic line colour (design spec: violet on the BP chart). */
private val Diastolic = Color(0xFFC7A4DD)
private val DateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
/** Recent-readings list shows the full date incl. year (chart axis keeps the short DateFmt). */
private val RowDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/** How many recent readings to reveal per "Load more" tap. */
private const val READINGS_PAGE = 10

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val tab = state.tab
    val today = remember { LocalDate.now() }

    // Derived view for the active tab.
    val all = state.current
    val window = remember(all, state.range) { all.filter { !dateOf(it).isBefore(today.minusDays(state.range.days)) } }
    val latest = all.lastOrNull()
    val start = window.firstOrNull()

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            // App bar
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
                Text("Health", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(34.dp).clip(CircleShape).background(Teal), Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = OnAccent, modifier = Modifier.size(18.dp))
                }
            }

            MetricTabs(tab, viewModel::setTab)

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(14.dp))
                LatestBlock(tab, latest, start, window.size, state.range)
                Spacer(Modifier.height(12.dp))
                RangeToggle(state.range, viewModel::setRange)
                Spacer(Modifier.height(12.dp))
                TrendChartCard(tab, window, state.range)
                Spacer(Modifier.height(16.dp))
                StatsRow(all, today)
                Spacer(Modifier.height(18.dp))
                Text("Recent readings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(bottom = 10.dp))
                if (all.isEmpty()) {
                    Text("—", fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(bottom = 20.dp))
                } else {
                    // Paginated "Load more" — reveal READINGS_PAGE at a time, newest first.
                    // Reset the page count whenever the tab changes.
                    var shown by remember(tab) { mutableStateOf(READINGS_PAGE) }
                    val reversed = all.asReversed()
                    reversed.take(shown).forEach { RecentRow(tab, it) }
                    if (shown < reversed.size) {
                        val remaining = reversed.size - shown
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)
                                .clip(RoundedCornerShape(11.dp)).border(1.dp, CardBorder, RoundedCornerShape(11.dp))
                                .clickable { shown += READINGS_PAGE }.padding(vertical = 11.dp)) {
                            Text("Load more · $remaining left", fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, color = Teal)
                        }
                    }
                }
                Spacer(Modifier.height(90.dp))
            }
        }

        // FAB → log sheet
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                .size(56.dp).clip(CircleShape).background(Teal).clickable(onClick = viewModel::openLog)) {
            Icon(Icons.Default.Add, contentDescription = "Log reading", tint = OnAccent, modifier = Modifier.size(28.dp))
        }
    }

    if (state.log != null) LogSheet(state, viewModel)
}

// ── Metric tabs (Glucose · Weight · BP) ─────────────────────────────────────────
@Composable
private fun MetricTabs(tab: HealthTab, onSelect: (HealthTab) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(11.dp)).background(BorderSoft).padding(4.dp)) {
        HealthTab.entries.forEach { t ->
            val selected = t == tab
            val interaction = remember { MutableInteractionSource() }
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Surface else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(t) }) {
                Text(t.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (selected) Ink else MutedLight)
            }
        }
    }
}

// ── Latest reading + delta vs range start ───────────────────────────────────────
@Composable
private fun LatestBlock(tab: HealthTab, latest: HealthMetricDto?, start: HealthMetricDto?, count: Int, range: RangeWindow) {
    Column {
        Text(tab.metricLabel, fontSize = 11.sp, color = MutedLight)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
            Text(if (latest != null) valueText(tab, latest) else "—", fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = DmMono, color = Ink)
            Spacer(Modifier.width(5.dp))
            Text(tab.unit, fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(bottom = 3.dp))
        }
        val (label, improving) = deltaLabel(tab, latest, start, range, count)
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (improving == true) Success else MutedLight, modifier = Modifier.padding(top = 4.dp))
    }
}

// ── Range toggle (7D / 30D / 90D) ───────────────────────────────────────────────
@Composable
private fun RangeToggle(range: RangeWindow, onSelect: (RangeWindow) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RangeWindow.entries.forEach { r ->
            val on = r == range
            Text(r.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = if (on) OnAccent else MutedDark,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) Ink else SurfaceMuted)
                    .clickable { onSelect(r) }.padding(horizontal = 14.dp, vertical = 6.dp))
        }
    }
}

// ── Trend chart ─────────────────────────────────────────────────────────────────
@Composable
private fun TrendChartCard(tab: HealthTab, window: List<HealthMetricDto>, range: RangeWindow) {
    val dual = tab == HealthTab.BP
    val lineColor = when (tab) { HealthTab.WEIGHT -> Success; else -> Teal }
    // Resolve composable colours here — they can't be read inside the DrawScope lambda.
    val gridColor = BorderSoft
    val inkColor = Ink
    val bubbleBg = Surface
    val bubbleBorder = CardBorder
    // Range-aware aggregation: 7D raw · 30D daily avg · 90D weekly avg (Apple-style).
    val points = remember(window, range) { aggregate(window, range) }
    val showDots = range != RangeWindow.D90 && points.size <= 20
    val textMeasurer = rememberTextMeasurer()
    // Tap-selected point index (reset when the series changes).
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    AppCard {
        if (points.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(104.dp), Alignment.Center) {
                Text("No readings in this range", fontSize = 12.sp, color = MutedLight)
            }
        } else {
            val primary = points.map { it.value }
            val secondary = if (dual) points.map { it.secondary ?: it.value } else emptyList()
            val allVals = primary + secondary
            val minV = allVals.minOrNull() ?: 0.0
            val maxV = allVals.maxOrNull() ?: 1.0
            Canvas(
                Modifier.fillMaxWidth().height(104.dp)
                    .pointerInput(points.size) {
                        detectTapGestures { off ->
                            val n = points.size
                            if (n == 0) return@detectTapGestures
                            val padL = size.width * 0.05f; val padR = size.width * 0.05f
                            val frac = ((off.x - padL) / (size.width - padL - padR)).coerceIn(0f, 1f)
                            val idx = if (n == 1) 0 else Math.round(frac * (n - 1))
                            selected = if (selected == idx) null else idx
                        }
                    },
            ) {
                val w = size.width; val h = size.height
                val padL = w * 0.05f; val padR = w * 0.05f
                val padT = 14f; val padB = 14f
                val span = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
                fun px(i: Int, n: Int) = if (n == 1) (padL + w - padR) / 2f else padL + (w - padL - padR) * (i.toFloat() / (n - 1))
                fun py(v: Double) = padT + (h - padT - padB) * (1f - ((v - minV) / span).toFloat())
                // 3 gridlines
                listOf(padT, (padT + (h - padB)) / 2f, h - padB).forEach { y ->
                    drawLine(gridColor, Offset(padL, y), Offset(w - padR, y), strokeWidth = 1f)
                }
                fun offsets(values: List<Double>): List<Offset> = values.mapIndexed { i, v -> Offset(px(i, values.size), py(v)) }
                fun polyline(pts: List<Offset>, color: Color, stroke: Float) {
                    if (pts.size < 2) return
                    val path = Path().apply { moveTo(pts.first().x, pts.first().y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                    drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                if (dual) polyline(offsets(secondary), Diastolic, 2f)
                val mainPts = offsets(primary)
                polyline(mainPts, lineColor, 2.5f)
                if (showDots) mainPts.forEach { drawCircle(lineColor, radius = 2.6f, center = it) }

                // Tap marker: vertical guide + highlighted dot + value bubble.
                selected?.let { i ->
                    if (i !in points.indices) return@let
                    val p = points[i]
                    val x = px(i, points.size)
                    drawLine(lineColor.copy(alpha = 0.35f), Offset(x, padT - 6f), Offset(x, h - padB + 6f), strokeWidth = 1.5f)
                    drawCircle(bubbleBg, radius = 5f, center = Offset(x, py(p.value)))
                    drawCircle(lineColor, radius = 5f, center = Offset(x, py(p.value)), style = Stroke(width = 2f))
                    if (dual) {
                        drawCircle(bubbleBg, radius = 4f, center = Offset(x, py(p.secondary ?: p.value)))
                        drawCircle(Diastolic, radius = 4f, center = Offset(x, py(p.secondary ?: p.value)), style = Stroke(width = 2f))
                    }
                    val label = "${p.label} · ${pointValueText(dual, p)} ${tab.unit}"
                    val measured = textMeasurer.measure(label, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = inkColor))
                    val bw = measured.size.width.toFloat(); val bh = measured.size.height.toFloat()
                    val bx = (x - bw / 2f).coerceIn(0f, w - bw - 8f)
                    val by = 0f
                    drawRoundRect(bubbleBg, topLeft = Offset(bx - 5f, by), size = Size(bw + 10f, bh + 6f), cornerRadius = CornerRadius(6f, 6f))
                    drawRoundRect(bubbleBorder, topLeft = Offset(bx - 5f, by), size = Size(bw + 10f, bh + 6f), cornerRadius = CornerRadius(6f, 6f), style = Stroke(width = 1f))
                    drawText(measured, topLeft = Offset(bx, by + 3f))
                }
            }
            // Axis row: start date · [BP legend] · end date
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(points.first().label, fontSize = 9.5.sp, color = MutedFaint)
                Spacer(Modifier.weight(1f))
                if (dual) {
                    Text("● systolic", fontSize = 9.5.sp, color = lineColor)
                    Spacer(Modifier.width(6.dp))
                    Text("● diastolic", fontSize = 9.5.sp, color = Diastolic)
                    Spacer(Modifier.weight(1f))
                }
                Text(points.last().label, fontSize = 9.5.sp, color = MutedFaint)
            }
            if (range == RangeWindow.D90) {
                Text("Weekly average · tap a point for details", fontSize = 9.sp, color = MutedFaint, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

// ── Streak + readings-in-range ──────────────────────────────────────────────────
@Composable
private fun StatsRow(all: List<HealthMetricDto>, today: LocalDate) {
    val days = remember(all) { all.map { dateOf(it) }.toSortedSet() }
    val streak = remember(days) { currentStreak(days, today) }
    val best = remember(days) { bestStreak(days) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        AppCard(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("🔥", fontSize = 15.sp, color = StreakFlame)
                Text("$streak", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DmMono, color = Ink)
            }
            Text("Day streak · best $best", fontSize = 10.5.sp, color = MutedLight, modifier = Modifier.padding(top = 5.dp))
        }
        AppCard(modifier = Modifier.weight(1f)) {
            Text("${all.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DmMono, color = Ink)
            Text("Readings logged", fontSize = 10.5.sp, color = MutedLight, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

// ── Recent reading row ──────────────────────────────────────────────────────────
@Composable
private fun RecentRow(tab: HealthTab, m: HealthMetricDto) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
            .clip(RoundedCornerShape(11.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)) {
        Text(dateOf(m).format(RowDateFmt), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MutedDark)
        Spacer(Modifier.weight(1f))
        Text(valueText(tab, m), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = DmMono, color = Ink)
        Spacer(Modifier.width(4.dp))
        Text(tab.unit, fontSize = 10.sp, color = MutedFaint)
    }
}

// ── Log sheet ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSheet(state: HealthUiState, vm: HealthViewModel) {
    val log = state.log ?: return
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = vm::closeLog, sheetState = sheetState, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Log ${log.tab.metricLabel.lowercase()}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                modifier = Modifier.padding(bottom = 16.dp))
            if (log.isDual) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { StepperField("Systolic", log.value, vm::setLogValue, 120) }
                    Box(Modifier.weight(1f)) { StepperField("Diastolic", log.secondary, vm::setLogSecondary, 80) }
                }
            } else {
                LabeledField("Reading · ${log.tab.unit}", log.value, vm::setLogValue, "Enter value", decimal = true)
            }
            state.error?.let { Text(it, color = com.mealplanplus.ui.theme.Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(18.dp))
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (log.canSave) Teal else SurfaceMuted)
                    .then(if (log.canSave) Modifier.clickable(onClick = vm::saveLog) else Modifier)) {
                Text("Save reading", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (log.canSave) OnAccent else MutedLight)
            }
        }
    }
}

@Composable
private fun StepperField(label: String, value: String, onChange: (String) -> Unit, default: Int) {
    // Whole-number vitals (BP) — pre-fill a sensible default so a tap-to-save works.
    LaunchedEffect(Unit) { if (value.isBlank()) onChange(default.toString()) }
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(bottom = 5.dp))
        Stepper(value.toIntOrNull() ?: default, { onChange(it.toString()) }, min = 0, max = 400, step = 1, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit, hint: String, decimal: Boolean = false) {
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(bottom = 5.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.5.dp, BorderCool, RoundedCornerShape(12.dp))
                .padding(horizontal = 13.dp, vertical = 12.dp)) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(hint, fontSize = 15.sp, color = MutedLight)
                BasicTextField(value, onChange, singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = Ink, fontFamily = DmMono),
                    keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
                    cursorBrush = SolidColor(Teal), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ── Chart aggregation ───────────────────────────────────────────────────────────
/** One plotted point: a raw reading (7D) or an averaged bin (30D daily / 90D weekly). */
private data class ChartPoint(val label: String, val value: Double, val secondary: Double?)

/**
 * Range-aware aggregation so long ranges don't over-populate the chart (industry standard):
 * 7D = raw readings, 30D = daily average, 90D = weekly average. [label] is the point's date label.
 */
private fun aggregate(window: List<HealthMetricDto>, range: RangeWindow): List<ChartPoint> {
    if (window.isEmpty()) return emptyList()
    fun bin(keyOf: (LocalDate) -> LocalDate): List<ChartPoint> =
        window.groupBy { keyOf(dateOf(it)) }.toSortedMap().map { (key, rs) ->
            ChartPoint(key.format(DateFmt), rs.map { it.value }.average(),
                rs.mapNotNull { it.secondaryValue }.let { if (it.isEmpty()) null else it.average() })
        }
    return when (range) {
        RangeWindow.D7 -> window.map { ChartPoint(dateOf(it).format(DateFmt), it.value, it.secondaryValue) }
        RangeWindow.D30 -> bin { it }                                    // daily
        RangeWindow.D90 -> bin { it.minusDays((it.dayOfWeek.value - 1).toLong()) } // week start (Mon)
    }
}

/** Marker value text: "128/82" for BP, formatted number otherwise. */
private fun pointValueText(dual: Boolean, p: ChartPoint): String =
    if (dual) "${fmtNum(p.value)}/${fmtNum(p.secondary ?: 0.0)}" else fmtNum(p.value)

// ── Pure helpers ────────────────────────────────────────────────────────────────
private fun dateOf(m: HealthMetricDto): LocalDate = m.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()

private fun fmtNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)

/** Display value for a reading — "120/80" for BP, formatted number otherwise. */
private fun valueText(tab: HealthTab, m: HealthMetricDto): String =
    if (tab == HealthTab.BP) "${fmtNum(m.value)}/${fmtNum(m.secondaryValue ?: 0.0)}" else fmtNum(m.value)

/**
 * Delta of the latest reading vs the first reading in the window. Returns the label and an
 * `improving` flag (true = lower than start → shown green; null = no delta to show).
 */
private fun deltaLabel(tab: HealthTab, latest: HealthMetricDto?, start: HealthMetricDto?, range: RangeWindow, count: Int): Pair<String, Boolean?> {
    if (latest == null) return "—" to null
    if (start == null || count < 2 || start === latest) return "First reading in ${range.label}" to null
    val d = latest.value - start.value
    val arrow = if (d < 0) "▼" else if (d > 0) "▲" else "•"
    return "$arrow ${fmtNum(kotlin.math.abs(d))} ${tab.unit} vs ${range.label} start" to (d < 0)
}

/** Consecutive days up to & including today that have ≥1 reading. */
private fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
    var d = today; var c = 0
    while (d in days) { c++; d = d.minusDays(1) }
    return c
}

/** Longest run of consecutive logged days in history. */
private fun bestStreak(days: Set<LocalDate>): Int {
    if (days.isEmpty()) return 0
    val sorted = days.toList().sorted()
    var best = 1; var run = 1
    for (i in 1 until sorted.size) {
        run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
        if (run > best) best = run
    }
    return best
}
