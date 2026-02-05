package com.mealplanplus.ui.screens.charts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Trends") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
        ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Date range selector
            Text("Time Range", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DateRange.entries.toList()) { range ->
                    FilterChip(
                        selected = range == uiState.selectedRange,
                        onClick = { viewModel.selectRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chart
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.metrics.size < 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Need at least 2 data points to show a chart.\nLog more ${uiState.selectedMetricType.displayName} readings!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                MetricLineChart(
                    metrics = uiState.metrics,
                    type = uiState.selectedMetricType
                )
            }
        }
    }
}

@Composable
fun MetricLineChart(
    metrics: List<HealthMetric>,
    type: MetricType
) {
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(metrics) {
        val entries = metrics.mapIndexed { index, metric ->
            entryOf(index.toFloat(), metric.value.toFloat())
        }
        chartEntryModelProducer.setEntries(entries)
    }

    val dateLabels = remember(metrics) {
        metrics.map { it.date.takeLast(5) }  // Show MM-DD
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
