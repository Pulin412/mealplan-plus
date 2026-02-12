package com.mealplanplus.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.Diet
import com.mealplanplus.ui.components.CalendarDayCell
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLog: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val diets by viewModel.diets.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Calendar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.goToToday() }) {
                        Text("Today", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month navigator
            MonthNavigator(
                currentMonth = uiState.currentMonth,
                onPrevious = { viewModel.goToPreviousMonth() },
                onNext = { viewModel.goToNextMonth() }
            )

            // Calendar grid
            CalendarGrid(
                month = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                plans = uiState.plans,
                dietNames = uiState.dietNames,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Selected date details
            SelectedDateCard(
                date = uiState.selectedDate,
                diet = uiState.selectedDiet,
                onAssignDiet = { viewModel.showDietPicker() },
                onClearPlan = { viewModel.clearPlan() },
                onViewLog = { onNavigateToLog(uiState.selectedDate.toString()) }
            )
        }
    }

    // Diet picker dialog
    if (uiState.showDietPicker) {
        DietPickerDialog(
            diets = diets,
            selectedDietId = uiState.selectedDiet?.id,
            onSelect = { viewModel.assignDiet(it) },
            onDismiss = { viewModel.hideDietPicker() }
        )
    }
}

@Composable
fun MonthNavigator(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    plans: Map<String, com.mealplanplus.data.model.Plan>,
    dietNames: Map<String, String> = emptyMap(),
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        val firstDay = month.atDay(1)
        val startOffset = (firstDay.dayOfWeek.value % 7)
        val daysInMonth = month.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val dateStr = date.toString()
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val plan = plans[dateStr]
                        // Only consider it a valid plan if it has a dietId
                        val hasPlan = plan != null && plan.dietId != null
                        val isCompleted = plan?.isCompleted ?: false
                        val dietName = dietNames[dateStr]

                        CalendarDayCell(
                            day = dayNumber,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasPlan = hasPlan,
                            isCompleted = isCompleted,
                            dietName = dietName,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedDateCard(
    date: LocalDate,
    diet: Diet?,
    onAssignDiet: () -> Unit,
    onClearPlan: () -> Unit,
    onViewLog: () -> Unit
) {
    val today = LocalDate.now()
    val isPastDate = date.isBefore(today)
    val canPlan = !isPastDate  // Can only plan for today or future

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (diet != null) {
                Text(
                    text = "Planned: ${diet.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                diet.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = if (isPastDate) "No diet was planned" else "No diet planned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Only show Assign/Change button for today or future dates
                if (canPlan) {
                    OutlinedButton(
                        onClick = onAssignDiet,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (diet != null) "Change" else "Assign Diet")
                    }

                    if (diet != null) {
                        OutlinedButton(onClick = onClearPlan) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Button(onClick = onViewLog) {
                    Text("Log")
                }
            }
        }
    }
}

@Composable
fun DietPickerDialog(
    diets: List<Diet>,
    selectedDietId: Long?,
    onSelect: (Diet?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Diet") },
        text = {
            if (diets.isEmpty()) {
                Text("No diets available. Create some diet templates first!")
            } else {
                LazyColumn {
                    items(diets.distinctBy { it.id }, key = { it.id }) { diet ->
                        val isSelected = diet.id == selectedDietId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(diet) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(diet.name, style = MaterialTheme.typography.bodyLarge)
                                diet.description?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
