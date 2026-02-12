package com.mealplanplus.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.ui.components.GradientBackground
import com.mealplanplus.ui.components.MiniCalendar
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFoods: () -> Unit,
    onNavigateToMeals: () -> Unit,
    onNavigateToDiets: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogWithDate: (String) -> Unit = { _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MealPlan+") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Plan") },
                            onClick = { showMenu = false; onNavigateToCalendar() },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Health") },
                            onClick = { showMenu = false; onNavigateToHealth() },
                            leadingIcon = { Icon(Icons.Default.Favorite, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Diets") },
                            onClick = { showMenu = false; onNavigateToDiets() },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Meals") },
                            onClick = { showMenu = false; onNavigateToMeals() },
                            leadingIcon = { Icon(Icons.Default.Menu, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Foods") },
                            onClick = { showMenu = false; onNavigateToFoods() },
                            leadingIcon = { Icon(Icons.Default.List, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { showMenu = false; onNavigateToSettings() },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's Summary Card
                TodaySummaryCard(
                    summary = uiState.todaySummary,
                    latestWeight = uiState.latestWeight?.value,
                    latestSugar = uiState.latestSugar?.value,
                    onLogClick = onNavigateToLog
                )

                // Weekly Calories Chart (completed days only)
                if (uiState.weeklyCalories.size >= 2) {
                    WeeklyCaloriesChart(calories = uiState.weeklyCalories)
                }

                // Mini Calendar
                MiniCalendar(
                    currentMonth = uiState.currentMonth,
                    plansForMonth = uiState.plansForMonth,
                    dietNames = uiState.dietNamesForMonth,
                    onPreviousMonth = { viewModel.goToPreviousMonth() },
                    onNextMonth = { viewModel.goToNextMonth() },
                    onDateSelected = { date -> onNavigateToLogWithDate(date.toString()) }
                )
            }
        }
    }
}

@Composable
fun TodaySummaryCard(
    summary: TodaySummary,
    latestWeight: Double?,
    latestSugar: Double?,
    onLogClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Intake",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Button(
                    onClick = onLogClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Macro grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroColumn("Cal", "${summary.calories}")
                MacroColumn("P", "${summary.protein}g")
                MacroColumn("C", "${summary.carbs}g")
                MacroColumn("F", "${summary.fat}g")
            }

            if (latestWeight != null || latestSugar != null) {
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    latestWeight?.let {
                        MetricChip("Weight", "${it.toInt()} kg")
                    }
                    latestSugar?.let {
                        MetricChip("Sugar", "${it.toInt()} mg/dL")
                    }
                }
            }
        }
    }
}

@Composable
fun MacroColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun WeeklyCaloriesChart(calories: List<DailyMacroSummary>) {
    val entries = remember(calories) {
        calories.mapIndexed { index, macro ->
            entryOf(index.toFloat(), macro.calories.toFloat())
        }
    }

    val chartEntryModelProducer = remember(entries) {
        ChartEntryModelProducer(entries)
    }

    val dateLabels = remember(calories) {
        calories.map { it.date.takeLast(5) } // "MM-DD" format
    }

    val bottomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        dateLabels.getOrElse(value.toInt()) { "" }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Calories (Completed Days)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Chart(
                chart = lineChart(),
                chartModelProducer = chartEntryModelProducer,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}
