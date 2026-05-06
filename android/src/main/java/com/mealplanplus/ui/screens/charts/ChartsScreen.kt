package com.mealplanplus.ui.screens.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.GlucoseSubType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.ui.screens.health.HealthViewModel
import com.mealplanplus.ui.screens.health.HealthUiState
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.ChartCarbs
import com.mealplanplus.ui.theme.ChartFat
import com.mealplanplus.ui.theme.ChartProtein
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.TextDestructive
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors
import com.mealplanplus.util.toChartLabel
import com.mealplanplus.util.toEpochMs
import com.patrykandpatrick.vico.compose.axis.axisGuidelineComponent
import com.patrykandpatrick.vico.compose.axis.axisLabelComponent
import com.patrykandpatrick.vico.compose.axis.axisLineComponent
import com.patrykandpatrick.vico.compose.axis.axisTickComponent
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.toDynamicShader
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.decoration.ThresholdLine
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.mealplanplus.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

enum class ChartTab(val title: String) {
    STREAK("Streak"),
    NUTRITION("Nutrition"),
    HEALTH("Health")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartsViewModel = hiltViewModel(),
    healthViewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val healthUiState by healthViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ChartTab.STREAK) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Streak & Stats",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
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
                .padding(padding)
                .background(BgPage)
        ) {
            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartTab.entries.forEach { tab ->
                    ChartTabChip(
                        label = tab.title,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    ChartTab.STREAK    -> StreakTab(uiState, viewModel)
                    ChartTab.NUTRITION -> NutritionTab(viewModel, uiState)
                    ChartTab.HEALTH    -> HealthVitalsTab(viewModel, uiState) { healthViewModel.showLogSheet() }
                }
            }
        }
    }

    if (healthUiState.showLogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { healthViewModel.hideLogSheet() },
            sheetState = sheetState,
            containerColor = CardBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            LogReadingSheetContent(
                uiState = healthUiState,
                onBgValueChange = healthViewModel::updateLogBgValue,
                onBgSubTypeChange = healthViewModel::updateLogBgSubType,
                onWeightValueChange = healthViewModel::updateLogWeightValue,
                onBpSystolicChange = healthViewModel::updateLogBpSystolic,
                onBpDiastolicChange = healthViewModel::updateLogBpDiastolic,
                onDateChange = healthViewModel::updateLogDate,
                onNotesChange = healthViewModel::updateLogNotes,
                onSave = healthViewModel::saveAllMetrics,
                onDismiss = healthViewModel::hideLogSheet
            )
        }
    }
}

// ── Tab chip ──────────────────────────────────────────────────────────────────

@Composable
private fun ChartTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) TextPrimary else CardBg)
            .border(1.dp, if (selected) TextPrimary else DividerColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

// ── Streak Tab (landing page — includes insights) ─────────────────────────────

@Composable
private fun StreakTab(uiState: ChartsUiState, viewModel: ChartsViewModel) {
    val today = remember { LocalDate.now() }
    val weekDays = remember {
        val mon = today.minusDays((today.dayOfWeek.value - 1).toLong())
        (0..6).map { mon.plusDays(it.toLong()) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgPage),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Streak hero ───────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                "${uiState.currentStreak}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                lineHeight = 52.sp
                            )
                            Text(
                                "day streak",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${uiState.bestStreak}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesignGreen
                            )
                            Text("best ever", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // This-week row
                    Text(
                        "THIS WEEK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { day ->
                            val dayMs = day.toEpochMs()
                            val isLogged = dayMs in uiState.loggedDatesThisMonth ||
                                (day.isBefore(today) && uiState.currentStreak > 0 &&
                                    today.toEpochDay() - day.toEpochDay() < uiState.currentStreak)
                            val isToday  = day == today
                            val isFuture = day.isAfter(today)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isToday  -> DesignGreen
                                                isLogged && !isFuture -> DesignGreen.copy(alpha = 0.15f)
                                                else     -> DividerColor
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLogged && !isToday && !isFuture) {
                                        Box(
                                            Modifier.size(7.dp).clip(CircleShape).background(DesignGreen)
                                        )
                                    } else if (isToday) {
                                        Text(
                                            day.dayOfMonth.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    fontSize = 9.sp,
                                    color = if (isFuture) Color(0xFFCCCCCC) else TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Monthly overview ──────────────────────────────────────────────────
        item {
            SectionLabel("MONTHLY OVERVIEW")
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val daysInMonth = today.lengthOfMonth()
                    val monthStart  = today.withDayOfMonth(1)
                    val rows = (daysInMonth + 6) / 7
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            for (col in 0..6) {
                                val dayNum = row * 7 + col + 1
                                if (dayNum > daysInMonth) {
                                    Spacer(Modifier.weight(1f))
                                } else {
                                    val dayDate = monthStart.plusDays((dayNum - 1).toLong())
                                    val isLogged  = dayDate.toEpochMs() in uiState.loggedDatesThisMonth
                                    val isToday   = dayDate == today
                                    val isFuture  = dayDate.isAfter(today)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                when {
                                                    isToday  -> DesignGreen
                                                    isLogged -> DesignGreen.copy(alpha = 0.12f)
                                                    else     -> DividerColor
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$dayNum",
                                            fontSize = 10.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isToday  -> Color.White
                                                isLogged -> DesignGreen
                                                isFuture -> Color(0xFFCCCCCC)
                                                else     -> Color(0xFFBBBBBB)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (row < rows - 1) Spacer(Modifier.height(3.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CalLegendDot(DesignGreen, "Logged")
                        CalLegendDot(DesignGreen.copy(alpha = 0.12f), "Today")
                        CalLegendDot(DividerColor, "Not yet")
                    }
                }
            }
        }

        // ── All-time stats ────────────────────────────────────────────────────
        item {
            SectionLabel("ALL-TIME STATS")
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatCard("Days logged", "${uiState.totalDaysLogged}", Modifier.weight(1f))
                MiniStatCard("Avg kcal/day", "${uiState.avgCaloriesPerDay}", Modifier.weight(1f))
                MiniStatCard("Avg protein", "${uiState.avgProteinPerDay}g", Modifier.weight(1f))
            }
        }

        // ── Diet adherence (Insights merged in) ───────────────────────────────
        item {
            SectionLabel("DIET ADHERENCE")
            Spacer(Modifier.height(6.dp))
            DietAdherenceCard(uiState, viewModel)
        }
    }
}

// ── Diet adherence card ───────────────────────────────────────────────────────

@Composable
private fun DietAdherenceCard(uiState: ChartsUiState, viewModel: ChartsViewModel) {
    val totalPlans    = uiState.totalPlans
    val completed     = uiState.completedPlans
    val pct           = if (totalPlans > 0) completed * 100 / totalPlans else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Date range pills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DateRange.entries.filter { it != DateRange.ALL }.forEach { range ->
                    val selected = range == uiState.insightsRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) DesignGreen else DividerColor)
                            .clickable { viewModel.selectInsightsRange(range) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            range.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Big % circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DesignGreen.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$pct%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesignGreen
                        )
                        Text("done", fontSize = 9.sp, color = TextSecondary)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = if (totalPlans > 0) completed.toFloat() / totalPlans else 0f,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = DesignGreen,
                        trackColor = DividerColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$completed completed", fontSize = 11.sp, color = DesignGreen, fontWeight = FontWeight.SemiBold)
                        Text("$totalPlans planned", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// ── Nutrition Tab ─────────────────────────────────────────────────────────────

@Composable
private fun NutritionTab(viewModel: ChartsViewModel, uiState: ChartsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgPage),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Range selector
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DateRange.entries.filter { it != DateRange.ALL }.forEach { range ->
                    val selected = range == uiState.nutritionRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) TextPrimary else DividerColor)
                            .clickable { viewModel.selectNutritionRange(range) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            range.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // Calories trend
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Calories", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    if (uiState.macroTotals.size < 2) {
                        EmptyDataBox("Log at least 2 days to see the trend")
                    } else {
                        CaloriesLineChart(macros = uiState.macroTotals, range = uiState.nutritionRange)
                    }
                }
            }
        }

        // Macro distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Macro Split", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    if (uiState.macroTotals.isEmpty()) {
                        EmptyDataBox("No data yet")
                    } else {
                        MacroDistributionChart(macros = uiState.macroTotals)
                    }
                }
            }
        }

        // Daily averages
        if (uiState.macroTotals.isNotEmpty()) {
            item {
                val avgCal  = uiState.macroTotals.map { it.calories }.average().toInt()
                val avgProt = uiState.macroTotals.map { it.protein  }.average().toInt()
                val avgCarb = uiState.macroTotals.map { it.carbs    }.average().toInt()
                val avgFat  = uiState.macroTotals.map { it.fat      }.average().toInt()
                SectionLabel("DAILY AVERAGES · ${uiState.macroTotals.size} days")
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NutritionAvgCard("Kcal", "$avgCal",     Color(0xFFFF6B35), Modifier.weight(1f))
                    NutritionAvgCard("Protein", "${avgProt}g", ChartProtein, Modifier.weight(1f))
                    NutritionAvgCard("Carbs", "${avgCarb}g",   ChartCarbs,   Modifier.weight(1f))
                    NutritionAvgCard("Fat", "${avgFat}g",      ChartFat,     Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NutritionAvgCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// ── Health Vitals Tab ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthVitalsTab(
    viewModel: ChartsViewModel,
    uiState: ChartsUiState,
    onLogReading: () -> Unit
) {
    var showRangeConfig by remember { mutableStateOf(false) }
    val type = uiState.selectedMetricType
    val showRangeOption = type == MetricType.BLOOD_GLUCOSE || type == MetricType.BLOOD_PRESSURE

    // Local editable fields for range config
    var glMin  by remember(uiState.glucoseNormalMin)   { mutableStateOf(uiState.glucoseNormalMin.toInt().toString()) }
    var glMax  by remember(uiState.glucoseNormalMax)   { mutableStateOf(uiState.glucoseNormalMax.toInt().toString()) }
    var glHigh by remember(uiState.glucoseHighThreshold) { mutableStateOf(uiState.glucoseHighThreshold.toInt().toString()) }
    var bpSys  by remember(uiState.bpSystolicNormal)   { mutableStateOf(uiState.bpSystolicNormal.toInt().toString()) }
    var bpDia  by remember(uiState.bpDiastolicNormal)  { mutableStateOf(uiState.bpDiastolicNormal.toInt().toString()) }

    val recentReadings = remember(uiState.healthMetrics) {
        uiState.healthMetrics.sortedByDescending { it.date }.take(15)
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgPage),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Health Vitals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Trends & history", fontSize = 12.sp, color = TextSecondary)
                }
                OutlinedButton(
                    onClick = onLogReading,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignGreen.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Reading", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Metric type selector
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(MetricType.entries.toList()) { t ->
                    val selected = t == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) TextPrimary else DividerColor)
                            .clickable { viewModel.selectMetricType(t) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(t.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary)
                    }
                }
            }
        }

        // Date range selector
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(DateRange.entries.toList()) { range ->
                    val selected = range == uiState.healthRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) DesignGreen else DividerColor)
                            .clickable { viewModel.selectHealthRange(range) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(range.label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary)
                    }
                }
            }
        }

        // Chart card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${type.displayName} (${type.unit})",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    when {
                        uiState.isLoading -> Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = DesignGreen, strokeWidth = 2.dp) }

                        uiState.healthMetrics.size < 2 -> EmptyDataBox(
                            "Log more ${type.displayName} readings to see trends"
                        )

                        else -> MetricLineChart(
                            metrics = uiState.healthMetrics,
                            type = type,
                            range = uiState.healthRange,
                            glucoseNormalMin = uiState.glucoseNormalMin,
                            glucoseNormalMax = uiState.glucoseNormalMax,
                            glucoseHighThreshold = uiState.glucoseHighThreshold,
                            bpSystolicNormal = uiState.bpSystolicNormal,
                            bpDiastolicNormal = uiState.bpDiastolicNormal
                        )
                    }
                }
            }
        }

        // Reference range config (BG + BP only)
        if (showRangeOption) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRangeConfig = !showRangeConfig }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reference Ranges", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Icon(
                                if (showRangeConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)
                            )
                        }
                        if (showRangeConfig) {
                            HorizontalDivider(color = DividerColor)
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (type == MetricType.BLOOD_GLUCOSE) {
                                    Text("BLOOD GLUCOSE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        color = TextSecondary, letterSpacing = 0.6.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = glMin, onValueChange = { glMin = it },
                                            label = { Text("Normal min (mg/dL)") }, singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).widthIn(min = 100.dp)
                                        )
                                        OutlinedTextField(
                                            value = glMax, onValueChange = { glMax = it },
                                            label = { Text("Normal max (mg/dL)") }, singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).widthIn(min = 100.dp)
                                        )
                                        OutlinedTextField(
                                            value = glHigh, onValueChange = { glHigh = it },
                                            label = { Text("High threshold (mg/dL)") }, singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).widthIn(min = 100.dp)
                                        )
                                    }
                                } else {
                                    Text("BLOOD PRESSURE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        color = TextSecondary, letterSpacing = 0.6.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = bpSys, onValueChange = { bpSys = it },
                                            label = { Text("Systolic (mmHg)") }, singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = bpDia, onValueChange = { bpDia = it },
                                            label = { Text("Diastolic (mmHg)") }, singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (type == MetricType.BLOOD_GLUCOSE) {
                                            viewModel.updateGlucoseRange(
                                                glMin.toFloatOrNull() ?: uiState.glucoseNormalMin,
                                                glMax.toFloatOrNull() ?: uiState.glucoseNormalMax,
                                                glHigh.toFloatOrNull() ?: uiState.glucoseHighThreshold
                                            )
                                        } else {
                                            viewModel.updateBpRange(
                                                bpSys.toFloatOrNull() ?: uiState.bpSystolicNormal,
                                                bpDia.toFloatOrNull() ?: uiState.bpDiastolicNormal
                                            )
                                        }
                                        showRangeConfig = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DesignGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Apply", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }

        // Recent readings list
        if (recentReadings.isNotEmpty()) {
            item { SectionLabel("RECENT READINGS") }
            items(recentReadings, key = { it.id }) { metric ->
                HealthReadingRow(
                    metric = metric,
                    type = type,
                    dateFormatter = dateFormatter
                )
            }
        }
    }
}

// ── Health reading row ────────────────────────────────────────────────────────

@Composable
private fun HealthReadingRow(
    metric: HealthMetric,
    type: MetricType,
    dateFormatter: DateTimeFormatter
) {
    val valueText = when (type) {
        MetricType.BLOOD_PRESSURE -> {
            val sys = metric.value.toInt()
            val dia = metric.secondaryValue?.toInt()
            if (dia != null) "$sys / $dia mmHg" else "$sys mmHg"
        }
        MetricType.WEIGHT -> "${"%.1f".format(metric.value)} ${type.unit}"
        else -> "${metric.value.toInt()} ${type.unit}"
    }
    val subLabel = if (type == MetricType.BLOOD_GLUCOSE && metric.subType != null) {
        runCatching { GlucoseSubType.valueOf(metric.subType).displayName }.getOrDefault(metric.subType)
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    metric.date.toLocalDate().format(dateFormatter),
                    fontSize = 12.sp, color = TextSecondary
                )
                if (subLabel != null) {
                    Text(subLabel, fontSize = 10.sp, color = TextMuted)
                }
            }
            Text(valueText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 0.6.sp
    )
}

@Composable
private fun MiniStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun CalLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun EmptyDataBox(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

// ── Log Reading Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogReadingSheetContent(
    uiState: HealthUiState,
    onBgValueChange: (String) -> Unit,
    onBgSubTypeChange: (String) -> Unit,
    onWeightValueChange: (String) -> Unit,
    onBpSystolicChange: (String) -> Unit,
    onBpDiastolicChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val logMetrics = remember { listOf(MetricType.BLOOD_GLUCOSE, MetricType.WEIGHT, MetricType.BLOOD_PRESSURE) }
    var activeMetric by remember { mutableStateOf(MetricType.BLOOD_GLUCOSE) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSubTypeMenu by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.logDate
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Log a Reading", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Enter one or more values and save", fontSize = 12.sp, color = TextSecondary)
        }

        // Metric type pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            logMetrics.forEach { type ->
                val selected = type == activeMetric
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) TextPrimary else BgPage)
                        .border(1.dp, if (selected) TextPrimary else DividerColor, RoundedCornerShape(50))
                        .clickable { activeMetric = type }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = when (type) {
                            MetricType.BLOOD_GLUCOSE -> "Glucose"
                            MetricType.WEIGHT -> "Weight"
                            MetricType.BLOOD_PRESSURE -> "B. Pressure"
                            else -> type.displayName
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) Color.White else TextSecondary
                    )
                }
            }
        }

        // Fields for selected metric
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgPage),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (activeMetric) {
                    MetricType.BLOOD_GLUCOSE -> {
                        Text("BLOOD GLUCOSE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = TextSecondary, letterSpacing = 0.6.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = uiState.logBgValue,
                                onValueChange = onBgValueChange,
                                label = { Text("mg/dL") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedCard(
                                    onClick = { showSubTypeMenu = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            runCatching { GlucoseSubType.valueOf(uiState.logBgSubType).displayName }
                                                .getOrDefault(uiState.logBgSubType),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            color = TextPrimary
                                        )
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                    }
                                }
                                DropdownMenu(expanded = showSubTypeMenu, onDismissRequest = { showSubTypeMenu = false }) {
                                    GlucoseSubType.entries.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st.displayName) },
                                            onClick = { onBgSubTypeChange(st.name); showSubTypeMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    MetricType.WEIGHT -> {
                        Text("WEIGHT", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = TextSecondary, letterSpacing = 0.6.sp)
                        OutlinedTextField(
                            value = uiState.logWeightValue,
                            onValueChange = onWeightValueChange,
                            label = { Text("Value (${MetricType.WEIGHT.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MetricType.BLOOD_PRESSURE -> {
                        Text("BLOOD PRESSURE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = TextSecondary, letterSpacing = 0.6.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = uiState.logBpSystolic,
                                onValueChange = onBpSystolicChange,
                                label = { Text("Systolic") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.logBpDiastolic,
                                onValueChange = onBpDiastolicChange,
                                label = { Text("Diastolic") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // Date + Notes row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = uiState.logDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, null, tint = TextSecondary)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).clickable { showDatePicker = true }
            )
            OutlinedTextField(
                value = uiState.logNotes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        uiState.error?.let {
            Text(it, color = TextDestructive, fontSize = 12.sp)
        }

        // Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DesignGreen),
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateChange(
                            java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}

// ── Charts ────────────────────────────────────────────────────────────────────

/** Returns the X-axis label spacing that produces ~5 visible labels for a given data count. */
private fun niceXSpacing(count: Int): Int = when {
    count <= 6  -> 1
    count <= 12 -> 2
    count <= 30 -> 5
    count <= 60 -> 10
    count <= 90 -> 15
    count <= 180 -> 30
    else         -> 60
}

private fun niceAxisBounds(values: List<Float>): Pair<Float, Float> {
    if (values.isEmpty()) return 0f to 100f
    val dataMin = values.min()
    val dataMax = values.max()
    val range = (dataMax - dataMin).coerceAtLeast(1f)
    val step = when {
        range <= 5    -> 1f
        range <= 10   -> 2f
        range <= 20   -> 5f
        range <= 50   -> 10f
        range <= 100  -> 20f
        range <= 200  -> 50f
        range <= 500  -> 100f
        range <= 1000 -> 200f
        range <= 2000 -> 500f
        else          -> 1000f
    }
    val niceMin = (floor(dataMin.toDouble() / step) * step).toFloat().coerceAtLeast(0f)
    val niceMax = (ceil(dataMax.toDouble() / step) * step).toFloat()
    return niceMin to niceMax
}

private fun metricLineColor(type: MetricType): Color = when (type) {
    MetricType.BLOOD_GLUCOSE   -> Color(0xFFF57F17)
    MetricType.WEIGHT          -> Color(0xFF1565C0)
    MetricType.BLOOD_PRESSURE  -> Color(0xFFD32F2F)
    else                       -> Color(0xFF2E7D32)
}

@Composable
fun MetricLineChart(
    metrics: List<HealthMetric>,
    type: MetricType,
    range: DateRange = DateRange.MONTH,
    glucoseNormalMin: Float = 70f,
    glucoseNormalMax: Float = 100f,
    glucoseHighThreshold: Float = 180f,
    bpSystolicNormal: Float = 120f,
    bpDiastolicNormal: Float = 80f
) {
    val lineColor = metricLineColor(type)
    val entries   = remember(metrics) { metrics.mapIndexed { i, m -> entryOf(i.toFloat(), m.value.toFloat()) } }
    val producer  = remember(entries) { ChartEntryModelProducer(entries) }
    val labelFmt  = if (range == DateRange.WEEK) "EEE" else "d MMM"
    val labels    = remember(metrics, labelFmt) { metrics.map { it.date.toChartLabel(labelFmt) } }

    val (niceMin, niceMax) = remember(metrics) {
        niceAxisBounds(metrics.map { it.value.toFloat() })
    }

    val xFmt = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { v, _ -> labels.getOrElse(v.toInt()) { "" } }
    val yFmt = AxisValueFormatter<AxisPosition.Vertical.Start> { v, _ ->
        when (type) {
            MetricType.WEIGHT -> "%.1f".format(v)
            else              -> v.toInt().toString()
        }
    }

    val labelComp = axisLabelComponent(color = TextSecondary, textSize = 10.sp)
    val lineComp  = axisLineComponent(color = DividerColor.copy(alpha = 0.6f), dynamicShader = null)
    val tickComp  = axisTickComponent(color = DividerColor.copy(alpha = 0.6f), dynamicShader = null)
    val gridComp  = axisGuidelineComponent(color = DividerColor.copy(alpha = 0.35f))

    val bgDecorations: List<ThresholdLine> = remember(type, glucoseNormalMin, glucoseNormalMax, glucoseHighThreshold, bpSystolicNormal, bpDiastolicNormal) {
        when (type) {
            MetricType.BLOOD_GLUCOSE -> listOf(
                ThresholdLine(
                    thresholdRange = glucoseNormalMin..glucoseNormalMax,
                    thresholdLabel = "Normal",
                    lineComponent = ShapeComponent(
                        color = android.graphics.Color.argb(28, 46, 125, 50),
                        shape = Shapes.rectShape
                    ),
                    labelComponent = textComponent { color = android.graphics.Color.argb(180, 46, 125, 50) }
                ),
                ThresholdLine(
                    thresholdValue = glucoseHighThreshold,
                    thresholdLabel = "High",
                    lineComponent = ShapeComponent(
                        color = android.graphics.Color.argb(55, 183, 28, 28),
                        shape = Shapes.rectShape
                    ),
                    labelComponent = textComponent { color = android.graphics.Color.argb(180, 183, 28, 28) }
                )
            )
            MetricType.BLOOD_PRESSURE -> listOf(
                ThresholdLine(
                    thresholdValue = bpSystolicNormal,
                    thresholdLabel = "Sys ${"%.0f".format(bpSystolicNormal)}",
                    lineComponent = ShapeComponent(
                        color = android.graphics.Color.argb(55, 183, 28, 28),
                        shape = Shapes.rectShape
                    ),
                    labelComponent = textComponent { color = android.graphics.Color.argb(180, 183, 28, 28) }
                ),
                ThresholdLine(
                    thresholdValue = bpDiastolicNormal,
                    thresholdLabel = "Dia ${"%.0f".format(bpDiastolicNormal)}",
                    lineComponent = ShapeComponent(
                        color = android.graphics.Color.argb(28, 46, 125, 50),
                        shape = Shapes.rectShape
                    ),
                    labelComponent = textComponent { color = android.graphics.Color.argb(180, 46, 125, 50) }
                )
            )
            else -> emptyList()
        }
    }

    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(
                    lineColor = lineColor,
                    lineThickness = 2.dp,
                    lineBackgroundShader = androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f))
                    ).toDynamicShader()
                )
            ),
            decorations = bgDecorations,
            axisValuesOverrider = AxisValuesOverrider.fixed(minY = niceMin, maxY = niceMax)
        ),
        chartModelProducer = producer,
        isZoomEnabled = false,
        chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = false),
        startAxis = rememberStartAxis(
            label = labelComp, axis = lineComp, tick = tickComp, guideline = gridComp,
            valueFormatter = yFmt,
            itemPlacer = remember { AxisItemPlacer.Vertical.default(maxItemCount = 5) }
        ),
        bottomAxis = rememberBottomAxis(
            label = labelComp, axis = lineComp, tick = tickComp, guideline = null,
            valueFormatter = xFmt,
            itemPlacer = remember(labels.size) { AxisItemPlacer.Horizontal.default(spacing = niceXSpacing(labels.size), addExtremeLabelPadding = true) }
        ),
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}

@Composable
fun CaloriesLineChart(macros: List<DailyMacroSummary>, range: DateRange = DateRange.WEEK) {
    val lineColor = DesignGreen
    val entries   = remember(macros) { macros.mapIndexed { i, m -> entryOf(i.toFloat(), m.calories.toFloat()) } }
    val producer  = remember(entries) { ChartEntryModelProducer(entries) }
    val labelFmt  = if (range == DateRange.WEEK) "EEE" else "d MMM"
    val labels    = remember(macros, labelFmt) { macros.map { it.date.toChartLabel(labelFmt) } }
    val (niceMin, niceMax) = remember(macros) { niceAxisBounds(macros.map { it.calories.toFloat() }) }

    val xFmt = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { v, _ ->
        labels.getOrElse(v.toInt()) { "" }
    }
    val yFmt = AxisValueFormatter<AxisPosition.Vertical.Start> { v, _ -> v.toInt().toString() }

    val labelComp = axisLabelComponent(color = TextSecondary, textSize = 10.sp)
    val lineComp  = axisLineComponent(color = DividerColor.copy(alpha = 0.6f), dynamicShader = null)
    val tickComp  = axisTickComponent(color = DividerColor.copy(alpha = 0.6f), dynamicShader = null)
    val gridComp  = axisGuidelineComponent(color = DividerColor.copy(alpha = 0.35f))

    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(
                    lineColor = lineColor,
                    lineThickness = 2.dp,
                    lineBackgroundShader = androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f))
                    ).toDynamicShader()
                )
            ),
            axisValuesOverrider = AxisValuesOverrider.fixed(minY = niceMin, maxY = niceMax)
        ),
        chartModelProducer = producer,
        isZoomEnabled = false,
        chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = false),
        startAxis = rememberStartAxis(
            label = labelComp,
            axis = lineComp,
            tick = tickComp,
            guideline = gridComp,
            valueFormatter = yFmt,
            itemPlacer = remember { AxisItemPlacer.Vertical.default(maxItemCount = 5) }
        ),
        bottomAxis = rememberBottomAxis(
            label = labelComp,
            axis = lineComp,
            tick = tickComp,
            guideline = null,
            valueFormatter = xFmt,
            itemPlacer = remember(labels.size) { AxisItemPlacer.Horizontal.default(spacing = niceXSpacing(labels.size), addExtremeLabelPadding = true) }
        ),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
fun MacroDistributionChart(macros: List<DailyMacroSummary>) {
    val totalProt = macros.sumOf { it.protein }
    val totalCarb = macros.sumOf { it.carbs }
    val totalFat  = macros.sumOf { it.fat }
    val total     = totalProt + totalCarb + totalFat
    if (total <= 0) return

    val pProt = (totalProt / total * 100).toInt()
    val pCarb = (totalCarb / total * 100).toInt()
    val pFat  = (totalFat  / total * 100).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(4.dp))
        ) {
            if (pProt > 0) Box(Modifier.weight(pProt.toFloat()).fillMaxHeight().background(ChartProtein))
            if (pCarb > 0) Box(Modifier.weight(pCarb.toFloat()).fillMaxHeight().background(ChartCarbs))
            if (pFat  > 0) Box(Modifier.weight(pFat.toFloat() ).fillMaxHeight().background(ChartFat))
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroLegendItem("Protein", pProt, totalProt.toInt(), ChartProtein)
            MacroLegendItem("Carbs",   pCarb, totalCarb.toInt(), ChartCarbs)
            MacroLegendItem("Fat",     pFat,  totalFat.toInt(),  ChartFat)
        }
    }
}

@Composable
private fun MacroLegendItem(name: String, pct: Int, grams: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Column {
            Text("$name $pct%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("${grams}g", fontSize = 10.sp, color = TextSecondary)
        }
    }
}
