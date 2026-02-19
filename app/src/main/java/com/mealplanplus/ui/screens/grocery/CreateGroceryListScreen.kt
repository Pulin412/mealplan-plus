package com.mealplanplus.ui.screens.grocery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroceryListScreen(
    onNavigateBack: () -> Unit,
    onListCreated: (Long) -> Unit,
    viewModel: CreateGroceryListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Navigate when list is generated
    LaunchedEffect(uiState.generatedListId) {
        uiState.generatedListId?.let { listId ->
            viewModel.clearGeneratedId()
            onListCreated(listId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Grocery List") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name input
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("List Name") },
                    placeholder = { Text(uiState.defaultName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Selection mode toggle
            item {
                Text(
                    "Select Dates",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectionMode == SelectionMode.RANGE,
                        onClick = { viewModel.setSelectionMode(SelectionMode.RANGE) },
                        label = { Text("Date Range") },
                        leadingIcon = if (uiState.selectionMode == SelectionMode.RANGE) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = uiState.selectionMode == SelectionMode.SPECIFIC,
                        onClick = { viewModel.setSelectionMode(SelectionMode.SPECIFIC) },
                        label = { Text("Specific Days") },
                        leadingIcon = if (uiState.selectionMode == SelectionMode.SPECIFIC) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Quick select buttons
            item {
                Text(
                    "Quick Select",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.selectNext7Days() },
                        label = { Text("Next 7 days") }
                    )
                    AssistChip(
                        onClick = { viewModel.selectThisWeek() },
                        label = { Text("This week") }
                    )
                    AssistChip(
                        onClick = { viewModel.selectNextWeek() },
                        label = { Text("Next week") }
                    )
                }
            }

            // Date selection based on mode
            if (uiState.selectionMode == SelectionMode.RANGE) {
                item {
                    DateRangeSelector(
                        startDate = uiState.rangeStart,
                        endDate = uiState.rangeEnd,
                        onStartClick = { showStartDatePicker = true },
                        onEndClick = { showEndDatePicker = true }
                    )
                }
            } else {
                item {
                    SpecificDatesSelector(
                        selectedDates = uiState.selectedDates,
                        onDateToggle = viewModel::toggleDate
                    )
                }
            }

            // Summary
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Summary",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${uiState.dateCount} days selected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.plansInRange.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${uiState.plansInRange.size} planned meals found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "No planned meals found for selected dates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Planned meals preview
            if (uiState.plansInRange.isNotEmpty()) {
                item {
                    Text(
                        "Planned Meals",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.plansInRange) { plan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            plan.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            plan.dietName ?: "No diet",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Generate button
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.generateList() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isGenerating && uiState.hasPlans
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isGenerating) "Generating..." else "Generate Grocery List")
                }

                if (!uiState.hasPlans && uiState.dateCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Plan meals for the selected dates first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = uiState.rangeStart,
            onDateSelected = {
                viewModel.setRangeStart(it)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = uiState.rangeEnd,
            onDateSelected = {
                viewModel.setRangeEnd(it)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(
            onClick = onStartClick,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Start",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        OutlinedCard(
            onClick = onEndClick,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "End",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificDatesSelector(
    selectedDates: Set<LocalDate>,
    onDateToggle: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dates = (0..13).map { today.plusDays(it.toLong()) }

    Column {
        Text(
            "Select days (next 2 weeks)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dates) { date ->
                val isSelected = date in selectedDates
                FilterChip(
                    selected = isSelected,
                    onClick = { onDateToggle(date) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                )
            }
        }
        if (selectedDates.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${selectedDates.size} days selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * 24 * 60 * 60 * 1000
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
