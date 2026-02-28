package com.mealplanplus.ui.screens.settings

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.local.ImportStrategy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState by viewModel.themeState.collectAsState()

    // File picker for JSON import
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importDietsFromJson(it, ImportStrategy.SKIP_DUPLICATES) }
    }

    // File picker for CSV import
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importDietsFromCsv(it, ImportStrategy.SKIP_DUPLICATES) }
    }

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
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance") {
                // Follow system theme
                SettingsSwitchItem(
                    title = "Follow system theme",
                    subtitle = "Use device's dark/light setting",
                    icon = Icons.Default.Phone,
                    checked = themeState.followSystem,
                    onCheckedChange = { viewModel.setFollowSystem(it) }
                )

                // Dark mode (only if not following system)
                if (!themeState.followSystem) {
                    SettingsSwitchItem(
                        title = "Dark mode",
                        subtitle = "Use dark theme",
                        icon = Icons.Default.Refresh,
                        checked = themeState.darkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }

                // Dynamic colors (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitchItem(
                        title = "Dynamic colors",
                        subtitle = "Use colors from your wallpaper",
                        icon = Icons.Default.Star,
                        checked = themeState.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Data Export Section
            SettingsSection(title = "Data Export") {
                Text(
                    text = "Export your data to CSV for sharing with doctors or backup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SettingsButtonItem(
                    title = "Export Food Log",
                    icon = Icons.Default.List,
                    enabled = !uiState.isExporting,
                    onClick = { viewModel.exportFoodLog() }
                )

                SettingsButtonItem(
                    title = "Export Health Metrics",
                    icon = Icons.Default.Favorite,
                    enabled = !uiState.isExporting,
                    onClick = { viewModel.exportHealthMetrics() }
                )

                if (uiState.isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Exporting...")
                    }
                }

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Data Import Section
            SettingsSection(title = "Data Import") {
                Text(
                    text = "Import diets and meals from JSON or CSV files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SettingsButtonItem(
                    title = "Import from JSON",
                    icon = Icons.Default.Add,
                    enabled = !uiState.isImporting,
                    onClick = { jsonPickerLauncher.launch(arrayOf("application/json", "*/*")) }
                )

                SettingsButtonItem(
                    title = "Import from CSV",
                    icon = Icons.Default.Add,
                    enabled = !uiState.isImporting,
                    onClick = { csvPickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) }
                )

                // Help text for CSV format
                Text(
                    text = "CSV format: diet_name, diet_description, tag, slot, meal_name, food, quantity, unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (uiState.isImporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Importing...")
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            SettingsSection(title = "About") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MealPlan+",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Version 1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Your diabetes-friendly diet tracker",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Export success dialog
    if (uiState.exportSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportState() },
            title = { Text("Export Complete") },
            text = { Text("Your data has been exported. Would you like to share it?") },
            confirmButton = {
                Button(onClick = {
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

    // Import result dialog
    uiState.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportState() },
            title = {
                Text(if (result.success) "Import Complete" else "Import Failed")
            },
            text = {
                Column {
                    if (result.success) {
                        Text("Successfully imported:")
                        Spacer(Modifier.height(8.dp))
                        Text("• ${result.dietsImported} diets")
                        Text("• ${result.mealsImported} meals")
                        if (result.tagsCreated > 0) {
                            Text("• ${result.tagsCreated} tags created")
                        }
                        if (result.skippedDiets.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Skipped ${result.skippedDiets.size} duplicate(s)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            result.errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearImportState() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsButtonItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title)
    }
}
