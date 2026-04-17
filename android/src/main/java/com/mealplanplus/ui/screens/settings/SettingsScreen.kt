package com.mealplanplus.ui.screens.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.local.ImportStrategy
import kotlinx.coroutines.launch
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWidgetSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState by viewModel.themeState.collectAsState()
    val notificationState by viewModel.notificationState.collectAsState()

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    var timePickerTarget by remember { mutableStateOf<HourPickerTarget?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Re-check exact-alarm permission on every resume (user may have just granted it).
    var canScheduleExact by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.canScheduleExactAlarms()
            } else true
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val nowExact = am.canScheduleExactAlarms()
                    if (nowExact && !canScheduleExact) {
                        viewModel.onExactAlarmPermissionResult()
                    }
                    canScheduleExact = nowExact
                }
                // Re-check HC permissions each time the user returns (they may have revoked in settings)
                viewModel.checkHealthConnectStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Health Connect permission launcher — result is the Set<String> of actually granted permissions
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onHealthConnectPermissionsResult()
        if (granted.containsAll(viewModel.healthConnectPermissions)) {
            scope.launch { snackbarHostState.showSnackbar("Health Connect connected!") }
        } else {
            // Dialog dismissed or permissions denied — guide the user to HC settings
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Permissions not granted. Open Health Connect to allow manually.",
                    actionLabel = "Open HC",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.openHealthConnectSettings(context)
                }
            }
        }
    }

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
        containerColor = BgPage,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Section
            SettingsSection(title = "Profile") {
                SettingsButtonItem(
                    title = "Your Profile",
                    icon = Icons.Default.Person,
                    onClick = onNavigateToProfile
                )
            }

            Spacer(Modifier.height(4.dp))

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

                // Widget appearance
                SettingsButtonItem(
                    title = "Widget Appearance",
                    icon = Icons.Outlined.Widgets,
                    onClick = onNavigateToWidgetSettings
                )
            }

            Spacer(Modifier.height(4.dp))

            // Notifications Section
            SettingsSection(title = "Notifications") {
                // Warn when SCHEDULE_EXACT_ALARM is not granted (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚠️  Exact alarms not allowed",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Without this permission, notifications may fire up to 1 hour late. Tap below to grant it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Grant permission")
                            }
                        }
                    }
                }

                val permissionGranted = notificationPermissionState?.status?.isGranted ?: true
                SettingsSwitchItem(
                    title = "Enable Notifications",
                    subtitle = if (!permissionGranted) "Tap to grant permission" else "Master on/off for all alerts",
                    icon = Icons.Default.Notifications,
                    checked = notificationState.masterEnabled && permissionGranted,
                    onCheckedChange = { enabled ->
                        if (enabled && !permissionGranted) {
                            notificationPermissionState?.launchPermissionRequest()
                        } else {
                            viewModel.setMasterEnabled(enabled)
                        }
                    }
                )

                if (notificationState.masterEnabled && permissionGranted) {
                    SettingsSwitchItem(
                        title = "Meal Reminders",
                        subtitle = "Remind me to log breakfast, lunch, and dinner",
                        icon = Icons.Default.Notifications,
                        checked = notificationState.mealRemindersEnabled,
                        onCheckedChange = { viewModel.setMealRemindersEnabled(it) }
                    )

                    if (notificationState.mealRemindersEnabled) {
                        SettingsTimeItem(
                            label = "Breakfast reminder",
                            hour = notificationState.breakfastHour,
                            minute = notificationState.breakfastMinute,
                            onClick = { timePickerTarget = HourPickerTarget.BREAKFAST }
                        )
                        SettingsTimeItem(
                            label = "Lunch reminder",
                            hour = notificationState.lunchHour,
                            minute = notificationState.lunchMinute,
                            onClick = { timePickerTarget = HourPickerTarget.LUNCH }
                        )
                        SettingsTimeItem(
                            label = "Dinner reminder",
                            hour = notificationState.dinnerHour,
                            minute = notificationState.dinnerMinute,
                            onClick = { timePickerTarget = HourPickerTarget.DINNER }
                        )
                    }

                    SettingsSwitchItem(
                        title = "Streak Protection",
                        subtitle = "Alert me if I haven't logged today and have a streak",
                        icon = Icons.Default.Star,
                        checked = notificationState.streakProtectionEnabled,
                        onCheckedChange = { viewModel.setStreakProtectionEnabled(it) }
                    )

                    if (notificationState.streakProtectionEnabled) {
                        SettingsTimeItem(
                            label = "Streak alert time",
                            hour = notificationState.streakAlertHour,
                            minute = notificationState.streakAlertMinute,
                            onClick = { timePickerTarget = HourPickerTarget.STREAK_ALERT }
                        )
                    }

                    SettingsSwitchItem(
                        title = "Weekly Plan Reminder",
                        subtitle = "Remind me on Mondays if I haven't planned this week",
                        icon = Icons.Default.DateRange,
                        checked = notificationState.weeklyPlanEnabled,
                        onCheckedChange = { viewModel.setWeeklyPlanEnabled(it) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Fitness & Wearables Section
            SettingsSection(title = "Fitness & Wearables") {
                Text(
                    text = "Connect Android Health Connect to sync steps, calories burned, and weight from fitness watches (Garmin, Fitbit, Samsung Galaxy Watch, etc.).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (!uiState.isHealthConnectAvailable) {
                    // HC not installed — show install prompt
                    SettingsActionItem(
                        title = "Health Connect not installed",
                        subtitle = "Install the Health Connect app to sync fitness data",
                        icon = Icons.Default.Watch,
                        actionLabel = "Install",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                            }
                            context.startActivity(intent)
                        }
                    )
                } else if (uiState.isHealthConnectConnected) {
                    // Connected — show status and disconnect option
                    SettingsActionItem(
                        title = "Health Connect",
                        subtitle = buildString {
                            append("Connected — syncing steps, calories & weight")
                            uiState.healthConnectLastSyncWeight?.let { w ->
                                append("\nLatest weight: ${"%.1f".format(w)} kg")
                            }
                        },
                        icon = Icons.Default.Watch,
                        actionLabel = "Manage",
                        onClick = {
                            val intent = Intent("androidx.health.ACTION_MANAGE_HEALTH_PERMISSIONS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(fallback)
                            }
                        }
                    )
                } else {
                    // Available but not connected
                    SettingsActionItem(
                        title = "Health Connect",
                        subtitle = "Grant read access to steps, calories burned, and weight.",
                        icon = Icons.Default.Watch,
                        actionLabel = "Connect",
                        onClick = {
                            healthConnectPermissionLauncher.launch(
                                viewModel.healthConnectPermissions
                            )
                        }
                    )

                    // Setup guide for sideloaded / debug builds
                    HealthConnectSetupGuide(
                        onOpenHealthConnect = { viewModel.openHealthConnectApp(context) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

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

            Spacer(Modifier.height(4.dp))

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

            Spacer(Modifier.height(4.dp))

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
                            text = "Your personal meal planning tracker",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Time picker dialog (hour + minute)
    timePickerTarget?.let { target ->
        val currentHour = when (target) {
            HourPickerTarget.BREAKFAST -> notificationState.breakfastHour
            HourPickerTarget.LUNCH -> notificationState.lunchHour
            HourPickerTarget.DINNER -> notificationState.dinnerHour
            HourPickerTarget.STREAK_ALERT -> notificationState.streakAlertHour
        }
        val currentMinute = when (target) {
            HourPickerTarget.BREAKFAST -> notificationState.breakfastMinute
            HourPickerTarget.LUNCH -> notificationState.lunchMinute
            HourPickerTarget.DINNER -> notificationState.dinnerMinute
            HourPickerTarget.STREAK_ALERT -> notificationState.streakAlertMinute
        }
        var sliderHour by remember(currentHour) { mutableStateOf(currentHour.toFloat()) }
        var sliderMinute by remember(currentMinute) { mutableStateOf(currentMinute.toFloat()) }
        AlertDialog(
            onDismissRequest = { timePickerTarget = null },
            title = { Text("Set time for ${target.label}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(sliderHour.toInt(), sliderMinute.toInt()),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Hour", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = sliderHour,
                        onValueChange = { sliderHour = it },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("12 AM", style = MaterialTheme.typography.labelSmall)
                        Text("11 PM", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Minute", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = sliderMinute,
                        onValueChange = { sliderMinute = it },
                        valueRange = 0f..59f,
                        steps = 58
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(":00", style = MaterialTheme.typography.labelSmall)
                        Text(":59", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = sliderHour.toInt()
                    val m = sliderMinute.toInt()
                    when (target) {
                        HourPickerTarget.BREAKFAST -> {
                            viewModel.setBreakfastHour(h)
                            viewModel.setBreakfastMinute(m)
                        }
                        HourPickerTarget.LUNCH -> {
                            viewModel.setLunchHour(h)
                            viewModel.setLunchMinute(m)
                        }
                        HourPickerTarget.DINNER -> {
                            viewModel.setDinnerHour(h)
                            viewModel.setDinnerMinute(m)
                        }
                        HourPickerTarget.STREAK_ALERT -> {
                            viewModel.setStreakAlertHour(h)
                            viewModel.setStreakAlertMinute(m)
                        }
                    }
                    timePickerTarget = null
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTarget = null }) { Text("Cancel") }
            }
        )
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

private enum class HourPickerTarget(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    STREAK_ALERT("Streak Alert")
}

private fun formatTime(hour: Int, minute: Int): String {
    val minuteStr = minute.toString().padStart(2, '0')
    return when {
        hour == 0 -> "12:$minuteStr AM"
        hour < 12 -> "$hour:$minuteStr AM"
        hour == 12 -> "12:$minuteStr PM"
        else -> "${hour - 12}:$minuteStr PM"
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column { content() }
        }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(TagGrayBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DesignGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = DividerColor,
                uncheckedBorderColor = DividerColor
            )
        )
    }
}

@Composable
fun SettingsTimeItem(
    label: String,
    hour: Int,
    minute: Int = 0,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(46.dp))
        Text(text = label, fontSize = 14.sp, modifier = Modifier.weight(1f), color = TextSecondary)
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(formatTime(hour, minute), fontSize = 14.sp, color = DesignGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SettingsButtonItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(TagGrayBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 14.sp, color = if (enabled) TextPrimary else TextMuted, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

/**
 * Expandable card shown when Health Connect is available but permissions have not been granted.
 * Covers three methods ordered by ease: reinstall, HC developer mode, ADB.
 */
@Composable
private fun HealthConnectSetupGuide(onOpenHealthConnect: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Connect button not working?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Health Connect blocks non-Play-Store apps from the permission dialog. " +
                           "Use one of the methods below — try them in order:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Method A ─────────────────────────────────────────────
                HcMethodHeader("Method 1 — Reinstall the app (try first)")
                HealthConnectStep("1", "Uninstall MealPlan+ from your phone")
                HealthConnectStep("2", "Reinstall the APK")
                HealthConnectStep("3", "Open Settings → Fitness & Wearables → tap Connect")

                Spacer(modifier = Modifier.height(12.dp))

                // ── Method B ─────────────────────────────────────────────
                HcMethodHeader("Method 2 — Health Connect developer mode")
                Text(
                    text = "Navigation varies by Android version and device:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                HealthConnectStep("1", "Open Health Connect (button below)")
                HealthConnectStep("2",
                    "Find \"About Health Connect\":\n" +
                    "• Android 14+ Pixel: tap ⋮ menu (top-right) → About\n" +
                    "• Android 14+ Samsung: Settings gear → About Health Connect\n" +
                    "• Android 9–13 (HC app): tap profile icon → scroll down to About"
                )
                HealthConnectStep("3", "Tap the Health Connect version number 7 times")
                HealthConnectStep("4",
                    "A \"Developer options\" item appears in the menu — tap it, then enable " +
                    "\"Allow apps from unknown sources\"\n" +
                    "(If you don't see this option, your HC version doesn't support it — use Method 3)"
                )
                HealthConnectStep("5", "Come back and tap Connect — the dialog will now work")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenHealthConnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Health Connect")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Method C ─────────────────────────────────────────────
                HcMethodHeader("Method 3 — ADB (Android 14+ only, requires a PC)")
                Text(
                    text = "With USB debugging on and your phone connected to a computer:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                HcCodeBlock(
                    "adb shell pm grant com.mealplanplus \\\n" +
                    "  android.permission.health.READ_STEPS\n" +
                    "adb shell pm grant com.mealplanplus \\\n" +
                    "  android.permission.health.READ_TOTAL_CALORIES_BURNED\n" +
                    "adb shell pm grant com.mealplanplus \\\n" +
                    "  android.permission.health.READ_WEIGHT"
                )
                Text(
                    text = "After running all three commands, come back here and tap Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HcMethodHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun HcCodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthConnectStep(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * A settings row with an icon, title/subtitle on the left and a text action button on the right.
 * Used for items that launch an external flow (permissions, app settings, install prompts).
 */
@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(TagGrayBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(actionLabel, fontSize = 13.sp, color = DesignGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}
