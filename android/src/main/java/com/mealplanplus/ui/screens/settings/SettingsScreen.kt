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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    onNavigateToBackup: () -> Unit = {},
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
    var appearanceExpanded by remember { mutableStateOf(false) }
    var notificationsExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    if (nowExact && !canScheduleExact) viewModel.onExactAlarmPermissionResult()
                    canScheduleExact = nowExact
                }
                viewModel.checkHealthConnectStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onHealthConnectPermissionsResult()
        if (granted.containsAll(viewModel.healthConnectPermissions)) {
            scope.launch { snackbarHostState.showSnackbar("Health Connect connected!") }
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Permissions not granted. Open Health Connect to allow manually.",
                    actionLabel = "Open HC",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.openHealthConnectSettings(context)
            }
        }
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
            // ── Profile ───────────────────────────────────────────────────
            SettingsSection(title = "Profile") {
                SettingsButtonItem(
                    title = "Your Profile",
                    icon = Icons.Default.Person,
                    onClick = onNavigateToProfile
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Appearance (collapsible) ──────────────────────────────────
            CollapsibleSettingsSection(
                title = "Appearance",
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                SettingsSwitchItem(
                    title = "Follow system theme",
                    subtitle = "Use device dark/light setting",
                    icon = Icons.Default.Phone,
                    checked = themeState.followSystem,
                    onCheckedChange = { viewModel.setFollowSystem(it) }
                )
                if (!themeState.followSystem) {
                    SettingsSwitchItem(
                        title = "Dark mode",
                        subtitle = "Use dark theme",
                        icon = Icons.Default.DarkMode,
                        checked = themeState.darkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitchItem(
                        title = "Dynamic colors",
                        subtitle = "Use colors from your wallpaper",
                        icon = Icons.Default.Palette,
                        checked = themeState.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
                SettingsButtonItem(
                    title = "Widget Appearance",
                    icon = Icons.Outlined.Widgets,
                    onClick = onNavigateToWidgetSettings
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Notifications (collapsible) ───────────────────────────────
            CollapsibleSettingsSection(
                title = "Notifications",
                expanded = notificationsExpanded,
                onToggle = { notificationsExpanded = !notificationsExpanded }
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Exact alarms not allowed",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Without this permission, notifications may fire up to 1 hour late.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) { Text("Grant permission") }
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
                        if (enabled && !permissionGranted) notificationPermissionState?.launchPermissionRequest()
                        else viewModel.setMasterEnabled(enabled)
                    }
                )

                if (notificationState.masterEnabled && permissionGranted) {
                    SettingsSwitchItem(
                        title = "Meal Reminders",
                        subtitle = "Remind me to log breakfast, lunch, and dinner",
                        icon = Icons.Default.Restaurant,
                        checked = notificationState.mealRemindersEnabled,
                        onCheckedChange = { viewModel.setMealRemindersEnabled(it) }
                    )
                    if (notificationState.mealRemindersEnabled) {
                        SettingsTimeItem(
                            label = "Breakfast",
                            hour = notificationState.breakfastHour,
                            minute = notificationState.breakfastMinute,
                            onClick = { timePickerTarget = HourPickerTarget.BREAKFAST }
                        )
                        SettingsTimeItem(
                            label = "Lunch",
                            hour = notificationState.lunchHour,
                            minute = notificationState.lunchMinute,
                            onClick = { timePickerTarget = HourPickerTarget.LUNCH }
                        )
                        SettingsTimeItem(
                            label = "Dinner",
                            hour = notificationState.dinnerHour,
                            minute = notificationState.dinnerMinute,
                            onClick = { timePickerTarget = HourPickerTarget.DINNER }
                        )
                    }
                    SettingsSwitchItem(
                        title = "Streak Protection",
                        subtitle = "Alert me if I haven't logged today and have a streak",
                        icon = Icons.Default.LocalFireDepartment,
                        checked = notificationState.streakProtectionEnabled,
                        onCheckedChange = { viewModel.setStreakProtectionEnabled(it) }
                    )
                    if (notificationState.streakProtectionEnabled) {
                        SettingsTimeItem(
                            label = "Streak alert",
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

            // ── Fitness & Wearables ───────────────────────────────────────
            SettingsSection(title = "Fitness & Wearables") {
                Text(
                    text = "Connect Health Connect to sync steps, calories burned, and weight from fitness watches.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (!uiState.isHealthConnectAvailable) {
                    SettingsActionItem(
                        title = "Health Connect not installed",
                        subtitle = "Install the Health Connect app to sync fitness data",
                        icon = Icons.Default.Watch,
                        actionLabel = "Install",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                }
                            )
                        }
                    )
                } else if (uiState.isHealthConnectConnected) {
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
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        }
                    )
                } else {
                    SettingsActionItem(
                        title = "Health Connect",
                        subtitle = "Grant read access to steps, calories burned, and weight",
                        icon = Icons.Default.Watch,
                        actionLabel = "Connect",
                        onClick = { healthConnectPermissionLauncher.launch(viewModel.healthConnectPermissions) }
                    )
                    HealthConnectSetupGuide(onOpenHealthConnect = { viewModel.openHealthConnectApp(context) })
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Cloud Sync ────────────────────────────────────────────────
            SettingsSection(title = "Cloud Sync") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Sync with server",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            val lastSyncLabel = remember(uiState.lastSyncTimestamp) {
                                if (uiState.lastSyncTimestamp == 0L) "Never synced"
                                else {
                                    val diffMs = System.currentTimeMillis() - uiState.lastSyncTimestamp
                                    val minutes = diffMs / 60_000
                                    when {
                                        minutes < 1  -> "Just now"
                                        minutes < 60 -> "$minutes min ago"
                                        else         -> "${minutes / 60}h ago"
                                    }
                                }
                            }
                            Text(lastSyncLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Button(
                            onClick = { viewModel.triggerSync() },
                            enabled = !uiState.isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = DesignGreen),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Sync now", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    uiState.syncError?.let { error ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Backup & Restore ──────────────────────────────────────────
            SettingsSection(title = "Backup & Restore") {
                SettingsButtonItem(
                    title = "Backup & Restore",
                    icon = Icons.Default.Backup,
                    onClick = onNavigateToBackup
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Data Export ───────────────────────────────────────────────
            SettingsSection(title = "Data Export") {
                Text(
                    text = "Export your data to CSV for sharing with doctors or for backup",
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
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Exporting…")
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

            // ── About ─────────────────────────────────────────────────────
            SettingsSection(title = "About") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MealPlan+",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Version 1.0",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Your all-in-one health companion — track meals, plan diets, log workouts, and monitor your glycemic load and nutrition goals every day.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "© 2026 Pulin™",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    Text(
                        text = "All rights reserved.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── Time picker dialog ────────────────────────────────────────────────────
    timePickerTarget?.let { target ->
        val currentHour = when (target) {
            HourPickerTarget.BREAKFAST   -> notificationState.breakfastHour
            HourPickerTarget.LUNCH       -> notificationState.lunchHour
            HourPickerTarget.DINNER      -> notificationState.dinnerHour
            HourPickerTarget.STREAK_ALERT -> notificationState.streakAlertHour
        }
        val currentMinute = when (target) {
            HourPickerTarget.BREAKFAST   -> notificationState.breakfastMinute
            HourPickerTarget.LUNCH       -> notificationState.lunchMinute
            HourPickerTarget.DINNER      -> notificationState.dinnerMinute
            HourPickerTarget.STREAK_ALERT -> notificationState.streakAlertMinute
        }
        var sliderHour   by remember(currentHour)   { mutableStateOf(currentHour.toFloat()) }
        var sliderMinute by remember(currentMinute) { mutableStateOf(currentMinute.toFloat()) }
        AlertDialog(
            onDismissRequest = { timePickerTarget = null },
            title = { Text("Set time for ${target.label}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatTime(sliderHour.toInt(), sliderMinute.toInt()), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Hour", style = MaterialTheme.typography.labelMedium)
                    Slider(value = sliderHour, onValueChange = { sliderHour = it }, valueRange = 0f..23f, steps = 22)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("12 AM", style = MaterialTheme.typography.labelSmall)
                        Text("11 PM", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Minute", style = MaterialTheme.typography.labelMedium)
                    Slider(value = sliderMinute, onValueChange = { sliderMinute = it }, valueRange = 0f..59f, steps = 58)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(":00", style = MaterialTheme.typography.labelSmall)
                        Text(":59", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = sliderHour.toInt(); val m = sliderMinute.toInt()
                    when (target) {
                        HourPickerTarget.BREAKFAST    -> { viewModel.setBreakfastHour(h); viewModel.setBreakfastMinute(m) }
                        HourPickerTarget.LUNCH        -> { viewModel.setLunchHour(h); viewModel.setLunchMinute(m) }
                        HourPickerTarget.DINNER       -> { viewModel.setDinnerHour(h); viewModel.setDinnerMinute(m) }
                        HourPickerTarget.STREAK_ALERT -> { viewModel.setStreakAlertHour(h); viewModel.setStreakAlertMinute(m) }
                    }
                    timePickerTarget = null
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { timePickerTarget = null }) { Text("Cancel") } }
        )
    }

    // ── Export success dialog ─────────────────────────────────────────────────
    if (uiState.exportSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportState() },
            title = { Text("Export Complete") },
            text = { Text("Your data has been exported. Would you like to share it?") },
            confirmButton = {
                Button(onClick = { viewModel.shareExport(); viewModel.clearExportState() }) { Text("Share") }
            },
            dismissButton = { TextButton(onClick = { viewModel.clearExportState() }) { Text("Close") } }
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
    val m = minute.toString().padStart(2, '0')
    return when {
        hour == 0  -> "12:$m AM"
        hour < 12  -> "$hour:$m AM"
        hour == 12 -> "12:$m PM"
        else       -> "${hour - 12}:$m PM"
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
private fun CollapsibleSettingsSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.8.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        if (expanded) {
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

@Composable
private fun HealthConnectSetupGuide(onOpenHealthConnect: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text("Connect button not working?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
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
                Spacer(Modifier.height(8.dp))
                Text(
                    "Health Connect blocks non-Play-Store apps from the permission dialog. Use one of the methods below — try them in order:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(12.dp))
                HcMethodHeader("Method 1 — Reinstall the app (try first)")
                HealthConnectStep("1", "Uninstall MealPlan+ from your phone")
                HealthConnectStep("2", "Reinstall the APK")
                HealthConnectStep("3", "Open Settings → Fitness & Wearables → tap Connect")
                Spacer(Modifier.height(12.dp))
                HcMethodHeader("Method 2 — Health Connect developer mode")
                HealthConnectStep("1", "Open Health Connect (button below)")
                HealthConnectStep("2", "Find \"About Health Connect\" and tap the version number 7 times")
                HealthConnectStep("3", "Enable \"Allow apps from unknown sources\" in Developer options")
                HealthConnectStep("4", "Come back and tap Connect")
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onOpenHealthConnect, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Health Connect")
                }
                Spacer(Modifier.height(12.dp))
                HcMethodHeader("Method 3 — ADB (Android 14+, requires a PC)")
                HcCodeBlock(
                    "adb shell pm grant com.mealplanplus \\\n" +
                    "  android.permission.health.READ_STEPS\n" +
                    "adb shell pm grant com.mealplanplus \\\n" +
                    "  android.permission.health.READ_TOTAL_CALORIES_BURNED\n" +
                    "adb shell pm grant com.mealplanplus \\\n" +
                    "  android.permission.health.READ_WEIGHT"
                )
            }
        }
    }
}

@Composable
private fun HcMethodHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun HcCodeBlock(code: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthConnectStep(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
    }
}

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
