package com.mealplanplus.ui.screens.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.ChartCarbs
import com.mealplanplus.ui.theme.ChartFat
import com.mealplanplus.ui.theme.ChartProtein
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors
import com.mealplanplus.util.toChartLabel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

enum class ChartTab(val title: String) {
    HEALTH("Health"),
    NUTRITION("Nutrition"),
    INSIGHTS("Insights")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ChartTab.HEALTH) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics",
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
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChartTab.entries.toList()) { tab ->
                    ChartTabChip(
                        label = tab.title,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    ChartTab.HEALTH -> HealthChartsTab(viewModel, uiState)
                    ChartTab.NUTRITION -> NutritionChartsTab(viewModel, uiState)
                    ChartTab.INSIGHTS -> InsightsTab(viewModel, uiState)
                }
            }
        }
    }
}

@Composable
private fun ChartTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) TextPrimary else CardBg)
            .border(1.dp, if (selected) TextPrimary else Color(0xFFE8E8E8), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthChartsTab(
    viewModel: ChartsViewModel,
    uiState: ChartsUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Metric type selector
            Text("Metric", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MetricType.entries.toList()) { type ->
                    FilterChip(
                        selected = type == uiState.selectedMetricType,
                        onClick = { viewModel.selectMetricType(type) },
                        label = { Text(type.displayName) }
                    )
                }
            }
        }

        item {
            // Date range selector
            Text("Time Range", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DateRange.entries.toList()) { range ->
                    FilterChip(
                        selected = range == uiState.healthRange,
                        onClick = { viewModel.selectHealthRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Chart
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.healthMetrics.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Need at least 2 data points.\nLog more ${uiState.selectedMetricType.displayName} readings!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                MetricLineChart(
                    metrics = uiState.healthMetrics,
                    type = uiState.selectedMetricType
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionChartsTab(
    viewModel: ChartsViewModel,
    uiState: ChartsUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calories trend card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Calories", style = MaterialTheme.typography.titleMedium)
                        // Date range selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DateRange.entries.filter { it != DateRange.ALL }.forEach { range ->
                                FilterChip(
                                    selected = range == uiState.nutritionRange,
                                    onClick = { viewModel.selectNutritionRange(range) },
                                    label = { Text(range.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.macroTotals.size < 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Need at least 2 days of data",
                                color = TextSecondary
                            )
                        }
                    } else {
                        CaloriesLineChart(macros = uiState.macroTotals)
                    }
                }
            }
        }

        // Macro distribution card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Macro Distribution", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.macroTotals.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No nutrition data",
                                color = TextSecondary
                            )
                        }
                    } else {
                        MacroDistributionChart(macros = uiState.macroTotals)
                    }
                }
            }
        }

        // Weekly summary
        item {
            WeeklySummaryCard(macros = uiState.macroTotals)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsTab(
    viewModel: ChartsViewModel,
    uiState: ChartsUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Date range selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateRange.entries.filter { it != DateRange.ALL }.forEach { range ->
                    FilterChip(
                        selected = range == uiState.insightsRange,
                        onClick = { viewModel.selectInsightsRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }
        }

        // Diet Adherence Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diet Adherence", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    val totalPlans = uiState.totalPlans
                    val completedPlans = uiState.completedPlans
                    val adherencePercent = if (totalPlans > 0) (completedPlans * 100 / totalPlans) else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$adherencePercent%",
                                style = MaterialTheme.typography.headlineLarge,
                                color = DesignGreen
                            )
                            Text(
                                text = "Completion Rate",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = if (totalPlans > 0) completedPlans.toFloat() / totalPlans else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = DesignGreen,
                        trackColor = TagGrayBg
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$completedPlans completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignGreen
                        )
                        Text(
                            text = "$totalPlans total plans",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Days Logged",
                    value = "${uiState.macroTotals.size}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Avg Calories",
                    value = if (uiState.macroTotals.isNotEmpty())
                        "${uiState.macroTotals.map { it.calories }.average().toInt()}"
                    else "0",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = DesignGreen
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun MetricLineChart(
    metrics: List<HealthMetric>,
    type: MetricType
) {
    // Create entries synchronously to avoid race condition
    val entries = remember(metrics) {
        metrics.mapIndexed { index, metric ->
            entryOf(index.toFloat(), metric.value.toFloat())
        }
    }

    val chartEntryModelProducer = remember(entries) {
        ChartEntryModelProducer(entries)
    }

    val dateLabels = remember(metrics) {
        metrics.map { it.date.toChartLabel() }
    }

    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        dateLabels.getOrElse(value.toInt()) { "" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${type.displayName} (${type.unit})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Chart(
                chart = lineChart(),
                chartModelProducer = chartEntryModelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CaloriesLineChart(
    macros: List<DailyMacroSummary>
) {
    // Create entries synchronously to avoid race condition
    val entries = remember(macros) {
        macros.mapIndexed { index, macro ->
            entryOf(index.toFloat(), macro.calories.toFloat())
        }
    }

    val chartEntryModelProducer = remember(entries) {
        ChartEntryModelProducer(entries)
    }

    val dateLabels = remember(macros) {
        macros.map { it.date.toChartLabel() }
    }

    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        dateLabels.getOrElse(value.toInt()) { "" }
    }

    Chart(
        chart = lineChart(),
        chartModelProducer = chartEntryModelProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
fun MacroDistributionChart(
    macros: List<DailyMacroSummary>
) {
    val totalProtein = macros.sumOf { it.protein }
    val totalCarbs = macros.sumOf { it.carbs }
    val totalFat = macros.sumOf { it.fat }
    val total = totalProtein + totalCarbs + totalFat

    if (total <= 0) {
        Text("No macro data available", color = TextSecondary)
        return
    }

    val proteinPercent = (totalProtein / total * 100).toInt()
    val carbsPercent = (totalCarbs / total * 100).toInt()
    val fatPercent = (totalFat / total * 100).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Simple bar representation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            if (proteinPercent > 0) {
                Surface(
                    modifier = Modifier
                        .weight(proteinPercent.toFloat())
                        .fillMaxHeight(),
                    color = ChartProtein
                ) {}
            }
            if (carbsPercent > 0) {
                Surface(
                    modifier = Modifier
                        .weight(carbsPercent.toFloat())
                        .fillMaxHeight(),
                    color = ChartCarbs
                ) {}
            }
            if (fatPercent > 0) {
                Surface(
                    modifier = Modifier
                        .weight(fatPercent.toFloat())
                        .fillMaxHeight(),
                    color = ChartFat
                ) {}
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroLegendItem("Protein", proteinPercent, totalProtein.toInt(), ChartProtein)
            MacroLegendItem("Carbs", carbsPercent, totalCarbs.toInt(), ChartCarbs)
            MacroLegendItem("Fat", fatPercent, totalFat.toInt(), ChartFat)
        }
    }
}

@Composable
fun MacroLegendItem(
    name: String,
    percent: Int,
    grams: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(12.dp),
            color = color,
            shape = MaterialTheme.shapes.small
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text("$name $percent%", style = MaterialTheme.typography.labelMedium)
            Text("${grams}g total", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun WeeklySummaryCard(
    macros: List<DailyMacroSummary>
) {
    val avgCalories = if (macros.isNotEmpty()) macros.map { it.calories }.average().toInt() else 0
    val avgProtein = if (macros.isNotEmpty()) macros.map { it.protein }.average().toInt() else 0
    val avgCarbs = if (macros.isNotEmpty()) macros.map { it.carbs }.average().toInt() else 0
    val avgFat = if (macros.isNotEmpty()) macros.map { it.fat }.average().toInt() else 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Averages", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Based on ${macros.size} days",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$avgCalories", style = MaterialTheme.typography.titleLarge)
                    Text("Calories", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${avgProtein}g", style = MaterialTheme.typography.titleLarge, color = ChartProtein)
                    Text("Protein", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${avgCarbs}g", style = MaterialTheme.typography.titleLarge, color = ChartCarbs)
                    Text("Carbs", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${avgFat}g", style = MaterialTheme.typography.titleLarge, color = ChartFat)
                    Text("Fat", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
