package com.mealplanplus.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Data Export",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Export your data to CSV for sharing with doctors or backup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Export Food Log
            OutlinedButton(
                onClick = { viewModel.exportFoodLog() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isExporting
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Food Log")
            }

            // Export Health Metrics
            OutlinedButton(
                onClick = { viewModel.exportHealthMetrics() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isExporting
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Health Metrics")
            }

            if (uiState.isExporting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Exporting...")
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "MealPlan+ v1.0\nDiabetes-friendly diet tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Export success dialog
    if (uiState.exportSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportState() },
            title = { Text("Export Complete") },
            text = { Text("Your data has been exported. Would you like to share it?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.shareExport()
                    viewModel.clearExportState()
                }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportState() }) {
                    Text("Close")
                }
            }
        )
    }
}
