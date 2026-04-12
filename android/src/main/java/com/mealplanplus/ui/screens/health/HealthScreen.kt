package com.mealplanplus.ui.screens.health

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.mealplanplus.ui.theme.BrandGreen
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.healthconnect.ActivityDaySummary
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.GlucoseSubType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.mealplanplus.util.toChartLabel
import com.mealplanplus.util.toLocalDate
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
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111111),
                    navigationIconContentColor = Color(0xFF555555),
                    actionIconContentColor = Color(0xFF555555)
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
                    // Activity tab — Health Connect data
                    item {
                        FilterChip(
                            selected = uiState.isActivityTabSelected,
                            onClick = { viewModel.selectActivityTab() },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏃")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Activity")
                                }
                            }
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

            // ── Period navigator — shared by all tabs ─────────────────────────
            item {
                if (uiState.isActivityTabSelected) {
                    PeriodNavigator(
                        viewType = uiState.activityViewType,
                        rangeLabel = uiState.activityRangeLabel,
                        canGoForward = uiState.activityPeriodOffset < 0,
                        onViewTypeChange = { viewModel.selectActivityViewType(it) },
                        onBack = { viewModel.shiftActivityPeriod(-1) },
                        onForward = { viewModel.shiftActivityPeriod(1) }
                    )
                } else {
                    PeriodNavigator(
                        viewType = uiState.metricViewType,
                        rangeLabel = uiState.metricRangeLabel,
                        canGoForward = uiState.metricPeriodOffset < 0,
                        onViewTypeChange = { viewModel.selectMetricViewType(it) },
                        onBack = { viewModel.shiftMetricPeriod(-1) },
                        onForward = { viewModel.shiftMetricPeriod(1) }
                    )
                }
            }

            if (uiState.isActivityTabSelected) {
                // ── Activity (Health Connect) tab ────────────────────────────
                item { ActivityTabContent(uiState = uiState, viewModel = viewModel) }
            } else {
                // ── Regular metric tabs ──────────────────────────────────────

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

                // Trend chart
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
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
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

                // Recent Readings header + list
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
    val dateLabels = remember(chartMetrics) {
        val fmt = DateTimeFormatter.ofPattern("dd/MM")
        chartMetrics.map { m -> m.date.toChartLabel("dd/MM") }
    }
    val xSpacing = remember(chartMetrics.size) { maxOf(1, chartMetrics.size / 5) }
    val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { v, _ ->
        dateLabels.getOrElse(v.toInt()) { "" }
    }
    ProvideChartStyle(m3ChartStyle()) {
        Chart(
            chart = lineChart(),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(
                itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 4)
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = formatter,
                itemPlacer = remember(xSpacing) { AxisItemPlacer.Horizontal.default(spacing = xSpacing) }
            ),
            modifier = modifier.height(180.dp)
        )
    }
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
                Text(metric.date.toChartLabel("dd/MM/yyyy"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// ── Activity Tab ──────────────────────────────────────────────────────────────

private val StepsColor  = Color(0xFF2E7D52)
private val CaloriesActivityColor = Color(0xFFF57C00)

@Composable
private fun PeriodNavigator(
    viewType: PeriodViewType,
    rangeLabel: String,
    canGoForward: Boolean,
    onViewTypeChange: (PeriodViewType) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Days / Week / Month toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodViewType.entries.forEach { type ->
                val selected = viewType == type
                OutlinedButton(
                    onClick = { onViewTypeChange(type) },
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                         else Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        type.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Navigation row: < [label] >
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous period",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                rangeLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onForward,
                enabled = canGoForward,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next period",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun ActivityTabContent(uiState: HealthUiState, viewModel: HealthViewModel) {
    val history = uiState.activityHistory

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        !uiState.isHcAvailable -> {
            ActivityEmptyCard(
                icon = "📱",
                title = "Health Connect not available",
                body = "Health Connect requires Android 9+ and the companion app on Android 9–13."
            )
        }
        !uiState.isHcConnected -> {
            ActivityEmptyCard(
                icon = "🔗",
                title = "Not connected",
                body = "Go to Settings → Fitness & Wearables and tap Connect to link Health Connect."
            )
        }
        history.isEmpty() -> {
            ActivityEmptyCard(
                icon = "🏃",
                title = "No activity data",
                body = "No steps or calories recorded in the selected period. Make sure your fitness app syncs to Health Connect."
            )
        }
        else -> {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stats row
                val avgSteps = history.map { it.steps }.average().toLong()
                val bestSteps = history.maxOf { it.steps }
                val totalCals = history.sumOf { it.caloriesBurned }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityStatCard("Avg Steps", "$avgSteps", "steps/day", StepsColor, Modifier.weight(1f))
                    ActivityStatCard("Best Day", "$bestSteps", "steps", StepsColor, Modifier.weight(1f))
                    ActivityStatCard("Total Burned", "$totalCals", "kcal", CaloriesActivityColor, Modifier.weight(1f))
                }

                // Steps chart
                ActivityChartCard(
                    title = "Steps Trend",
                    color = StepsColor,
                    history = history,
                    valueSelector = { it.steps.toFloat() },
                    minPoints = 2
                )

                // Calories chart
                ActivityChartCard(
                    title = "Calories Burned Trend",
                    color = CaloriesActivityColor,
                    history = history,
                    valueSelector = { it.caloriesBurned.toFloat() },
                    minPoints = 2
                )

                // Daily history list
                Text(
                    "Daily History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                history.forEach { day ->
                    ActivityDayRow(day)
                    HorizontalDivider(modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ActivityEmptyCard(icon: String, title: String, body: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icon, fontSize = 36.sp)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ActivityStatCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityChartCard(
    title: String,
    color: Color,
    history: List<ActivityDaySummary>,
    valueSelector: (ActivityDaySummary) -> Float,
    minPoints: Int = 2
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val chronological = remember(history) { history.reversed() }
            if (chronological.size >= minPoints) {
                val entries = remember(chronological) {
                    chronological.mapIndexed { i, d -> entryOf(i.toFloat(), valueSelector(d)) }
                }
                val modelProducer = remember(entries) { ChartEntryModelProducer(entries) }
                val dateLabels = remember(chronological) {
                    val fmt = DateTimeFormatter.ofPattern("dd/MM")
                    chronological.map { d -> d.date.format(fmt) }
                }
                val xSpacing = remember(chronological.size) { maxOf(1, chronological.size / 5) }
                val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { v, _ ->
                    dateLabels.getOrElse(v.toInt()) { "" }
                }
                ProvideChartStyle(m3ChartStyle(entityColors = listOf(color))) {
                    Chart(
                        chart = lineChart(),
                        chartModelProducer = modelProducer,
                        startAxis = rememberStartAxis(
                            itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 4)
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = formatter,
                            itemPlacer = remember(xSpacing) { AxisItemPlacer.Horizontal.default(spacing = xSpacing) }
                        ),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            } else {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Need at least 2 days of data to show trend",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun ActivityDayRow(day: ActivityDaySummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 16.sp
                )
                Text(
                    day.date.month.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏃 ", fontSize = 14.sp)
                Text("${day.steps} steps", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = StepsColor)
            }
            Text("🔥 ${day.caloriesBurned} kcal burned", style = MaterialTheme.typography.bodySmall, color = CaloriesActivityColor)
        }
        // Step goal progress ring hint
        val stepGoal = 10_000f
        val fraction = (day.steps / stepGoal).coerceIn(0f, 1f)
        Column(horizontalAlignment = Alignment.End) {
            Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                color = if (fraction >= 1f) StepsColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (fraction >= 1f) FontWeight.Bold else FontWeight.Normal)
            Text("of 10k goal", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

private fun formatHealthValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format("%.1f", value)
