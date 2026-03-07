package com.mealplanplus.ui.screens.health

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.mealplanplus.ui.theme.BrandGreen
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.GlucoseSubType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HealthScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCharts: () -> Unit,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customTypes by viewModel.customTypes.collectAsState()
    var customTypeToDelete by remember { mutableStateOf<CustomMetricType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Metrics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.showLogSheet() },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Log Reading", style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        val selectedUnit = when {
            uiState.selectedCustomTypeId != null ->
                customTypes.find { it.id == uiState.selectedCustomTypeId }?.unit ?: ""
            else -> uiState.selectedMetricType?.unit ?: ""
        }
        val selectedDisplayName = when {
            uiState.selectedCustomTypeId != null ->
                customTypes.find { it.id == uiState.selectedCustomTypeId }?.name ?: "Custom"
            else -> uiState.selectedMetricType?.displayName ?: ""
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Metric type tabs
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MetricType.entries.toList()) { type ->
                        FilterChip(
                            selected = uiState.selectedMetricType == type && uiState.selectedCustomTypeId == null,
                            onClick = { viewModel.selectMetricType(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                    items(customTypes) { customType ->
                        FilterChip(
                            selected = uiState.selectedCustomTypeId == customType.id,
                            onClick = { viewModel.selectCustomType(customType.id) },
                            label = { Text(customType.name) },
                            modifier = Modifier.combinedClickable(
                                onClick = { viewModel.selectCustomType(customType.id) },
                                onLongClick = { customTypeToDelete = customType }
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.showAddCustomTypeDialog() },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("Add Metric")
                                }
                            }
                        )
                    }
                }
            }

            // BG summary cards — only when BG selected and has data
            if (uiState.selectedMetricType == MetricType.BLOOD_GLUCOSE &&
                uiState.estimatedA1c != null && uiState.timeInRangePercent != null
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BgSummaryCard("Est. A1C", String.format("%.1f%%", uiState.estimatedA1c), Modifier.weight(1f))
                        BgSummaryCard("Time in Range", "${uiState.timeInRangePercent}%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Period selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Period:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(7 to "7d", 14 to "14d", 30 to "30d").forEach { (days, label) ->
                        val selected = uiState.selectedPeriodDays == days
                        OutlinedButton(
                            onClick = { viewModel.selectPeriod(days) },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Stats row
            uiState.stats?.let { stats ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricStatCard("Average", formatHealthValue(stats.avg), selectedUnit, Modifier.weight(1f))
                        MetricStatCard("Minimum", formatHealthValue(stats.min), selectedUnit, Modifier.weight(1f))
                        MetricStatCard("Maximum", formatHealthValue(stats.max), selectedUnit, Modifier.weight(1f))
                    }
                }
            }

            // BG Range Distribution
            if (uiState.selectedMetricType == MetricType.BLOOD_GLUCOSE) {
                uiState.bgDistribution?.let { dist ->
                    item {
                        BgDistributionCard(
                            distribution = dist,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Trend chart — always visible
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "$selectedDisplayName Trend ($selectedUnit)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        if (uiState.metrics.size >= 2) {
                            HealthTrendChart(metrics = uiState.metrics, modifier = Modifier.fillMaxWidth())
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Log at least 2 readings to see trend",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recent Readings header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Readings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }

            val recentMetrics = uiState.metrics.take(10)
            if (recentMetrics.isNotEmpty()) {
                items(recentMetrics, key = { it.id }) { metric ->
                    RecentReadingRow(
                        metric = metric,
                        metricType = uiState.selectedMetricType,
                        unit = selectedUnit,
                        onDelete = { viewModel.deleteMetric(metric) }
                    )
                }
            } else if (!uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tap + Log Reading to add data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Log Reading bottom sheet
    if (uiState.showLogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideLogSheet() },
            sheetState = sheetState
        ) {
            LogReadingSheet(
                uiState = uiState,
                customTypes = customTypes,
                onBgValueChange = viewModel::updateLogBgValue,
                onBgSubTypeChange = viewModel::updateLogBgSubType,
                onWeightValueChange = viewModel::updateLogWeightValue,
                onBpSystolicChange = viewModel::updateLogBpSystolic,
                onBpDiastolicChange = viewModel::updateLogBpDiastolic,
                onCustomValueChange = viewModel::updateLogCustomValue,
                onDateChange = viewModel::updateLogDate,
                onNotesChange = viewModel::updateLogNotes,
                onSave = { viewModel.saveAllMetrics() },
                onDismiss = { viewModel.hideLogSheet() }
            )
        }
    }

    // Add custom type dialog
    if (uiState.showAddCustomTypeDialog) {
        AddCustomTypeDialog(
            name = uiState.newCustomTypeName,
            unit = uiState.newCustomTypeUnit,
            minValue = uiState.newCustomTypeMin,
            maxValue = uiState.newCustomTypeMax,
            error = uiState.error,
            onNameChange = viewModel::updateNewCustomTypeName,
            onUnitChange = viewModel::updateNewCustomTypeUnit,
            onMinChange = viewModel::updateNewCustomTypeMin,
            onMaxChange = viewModel::updateNewCustomTypeMax,
            onCreate = { viewModel.addCustomType() },
            onDismiss = { viewModel.hideAddCustomTypeDialog() }
        )
    }

    // Delete custom type confirmation
    customTypeToDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { customTypeToDelete = null },
            title = { Text("Remove Metric") },
            text = { Text("Remove \"${type.name}\" metric? Existing readings are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomType(type)
                    customTypeToDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { customTypeToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BgSummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun MetricStatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (unit.isNotBlank()) Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BgDistributionCard(distribution: BgDistribution, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Range Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            BgRangeBar("Low (<70)", distribution.lowPercent, Color(0xFFE53935))
            BgRangeBar("In Range (70–140)", distribution.inRangePercent, Color(0xFF43A047))
            BgRangeBar("Elevated (140–200)", distribution.elevatedPercent, Color(0xFFFFA726))
            BgRangeBar("High (>200)", distribution.highPercent, Color(0xFFEF5350))
        }
    }
}

@Composable
fun BgRangeBar(label: String, percent: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$percent%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = percent / 100f,
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun HealthTrendChart(metrics: List<HealthMetric>, modifier: Modifier = Modifier) {
    val chartMetrics = remember(metrics) { metrics.reversed() }
    val entries = remember(chartMetrics) {
        chartMetrics.mapIndexed { i, m -> entryOf(i.toFloat(), m.value.toFloat()) }
    }
    val modelProducer = remember(entries) { ChartEntryModelProducer(entries) }
    val dateLabels = remember(chartMetrics) { chartMetrics.map { it.date.takeLast(5) } }
    val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { v, _ ->
        dateLabels.getOrElse(v.toInt()) { "" }
    }
    Chart(
        chart = lineChart(),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = formatter),
        modifier = modifier.height(180.dp)
    )
}

@Composable
fun RecentReadingRow(
    metric: HealthMetric,
    metricType: MetricType?,
    unit: String,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val valueText = when {
        metricType == MetricType.BLOOD_PRESSURE && metric.secondaryValue != null ->
            "${metric.value.toInt()}/${metric.secondaryValue.toInt()} $unit"
        else -> "${formatHealthValue(metric.value)} $unit"
    }
    val subTypeLabel = if (metricType == MetricType.BLOOD_GLUCOSE && metric.subType != null) {
        runCatching { GlucoseSubType.valueOf(metric.subType).displayName }.getOrNull()
    } else null

    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(valueText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    subTypeLabel?.let {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(metric.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Reading") },
            text = { Text("Delete this reading?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogReadingSheet(
    uiState: HealthUiState,
    customTypes: List<CustomMetricType>,
    onBgValueChange: (String) -> Unit,
    onBgSubTypeChange: (String) -> Unit,
    onWeightValueChange: (String) -> Unit,
    onBpSystolicChange: (String) -> Unit,
    onBpDiastolicChange: (String) -> Unit,
    onCustomValueChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showSubTypeMenu by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.logDate
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )
    val selectedCustomType = customTypes.find { it.id == uiState.selectedCustomTypeId }
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Log Reading", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        // Blood Glucose
        Text("Blood Glucose", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = uiState.logBgValue,
                onValueChange = onBgValueChange,
                label = { Text("Value (mg/dL)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1f)) {
                OutlinedCard(onClick = { showSubTypeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            runCatching { GlucoseSubType.valueOf(uiState.logBgSubType).displayName }
                                .getOrDefault(uiState.logBgSubType),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
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

        HorizontalDivider()

        // Weight
        Text("Weight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = uiState.logWeightValue,
            onValueChange = onWeightValueChange,
            label = { Text("Value (${MetricType.WEIGHT.unit})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // Blood Pressure
        Text("Blood Pressure", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = uiState.logBpSystolic,
                onValueChange = onBpSystolicChange,
                label = { Text("Systolic") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.logBpDiastolic,
                onValueChange = onBpDiastolicChange,
                label = { Text("Diastolic") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Custom metric
        if (selectedCustomType != null) {
            HorizontalDivider()
            Text(selectedCustomType.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = uiState.logCustomValue,
                onValueChange = onCustomValueChange,
                label = { Text("Value (${selectedCustomType.unit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()

        // Date
        OutlinedTextField(
            value = uiState.logDate.format(dateFormatter),
            onValueChange = {},
            label = { Text("Date") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, null)
                }
            },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
        )

        // Notes
        OutlinedTextField(
            value = uiState.logNotes,
            onValueChange = onNotesChange,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Save")
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
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AddCustomTypeDialog(
    name: String,
    unit: String,
    minValue: String,
    maxValue: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Metric Type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = onNameChange,
                    label = { Text("Metric name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit, onValueChange = onUnitChange,
                    label = { Text("Unit (e.g. bpm, steps)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minValue, onValueChange = onMinChange,
                        label = { Text("Min (opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxValue, onValueChange = onMaxChange,
                        label = { Text("Max (opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = name.isNotBlank() && unit.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatHealthValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format("%.1f", value)
