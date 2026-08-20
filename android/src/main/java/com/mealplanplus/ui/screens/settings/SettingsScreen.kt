package com.mealplanplus.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.notifications.NotificationHelper
import com.mealplanplus.data.notifications.NotificationSettings
import com.mealplanplus.data.notifications.NotificationType
import java.io.File
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderMuted
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.BuildConfig
import androidx.compose.ui.text.style.TextAlign
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import com.mealplanplus.ui.tour.LocalTourController

/** UI-only Settings screen (matches design 13a). Toggles/collapse flip locally; buttons + dropdowns
 *  are placeholders — functionality (backup, Health Connect, export, notifications) comes next. */
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, onOpenAdmin: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val exporting by viewModel.exporting.collectAsState()
    val notifSettings by viewModel.notifications.collectAsState()
    val socialNotifications by viewModel.socialNotifications.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val hcState by viewModel.healthConnectState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val hcLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.refreshHealthConnect()
    }
    LaunchedEffect(Unit) { viewModel.refreshHealthConnect() }
    // Prompt for the POST_NOTIFICATIONS runtime permission when the user enables any reminder.
    val ensureNotifPerm: (Boolean) -> Unit = { on ->
        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canPost(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val onToggleNotif: (NotificationType, Boolean) -> Unit = { type, on ->
        viewModel.setNotification(type, on); ensureNotifPerm(on)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is ExportEvent.Share -> shareCsv(context, ev.fileName, ev.csv)
                is ExportEvent.Error -> Toast.makeText(context, ev.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.feedbackResult.collect { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
                Text("Settings", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                // Backup & restore intentionally omitted — redundant with backend sync (all data lives
                // in Postgres keyed to the Firebase UID; a reinstall re-syncs). See docs/FEATURES.md → Dropped.

                // ── Health Connect ────────────────────────────────────────────────
                SectionLabel("Health Connect")
                Card {
                    val subtitle = when {
                        !hcState.available -> "Not available on this device"
                        hcState.connected -> "Connected"
                        else -> "Not connected"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(SurfaceMuted), Alignment.Center) {
                            Icon(Icons.Outlined.FavoriteBorder, null, tint = MutedDark, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Health Connect", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(subtitle, fontSize = 11.5.sp, color = MutedLight)
                        }
                        AppSwitch(hcState.connected) { on ->
                            when {
                                !hcState.available ->
                                    Toast.makeText(context, "Health Connect isn't available on this device", Toast.LENGTH_LONG).show()
                                on -> hcLauncher.launch(viewModel.healthConnectPermissions)
                                else -> viewModel.disconnectHealthConnect()
                            }
                        }
                    }
                    if (hcState.connected) {
                        Divider()
                        val s = hcState.summary
                        val weight = s.latestWeightKg?.let { " · %.1f kg".format(it) } ?: ""
                        Text(
                            "Today · %,d steps · %d kcal burned".format(s.steps, s.caloriesBurned) + weight,
                            fontSize = 12.sp, color = MutedDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        )
                    }
                }

                // ── Export data ───────────────────────────────────────────────────
                SectionLabel("Export data")
                Card {
                    ValueRow("Format", "CSV")
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text("Includes", fontSize = 13.5.sp, color = Ink)
                        Spacer(Modifier.weight(1f))
                        Text("Meals · workouts · health", fontSize = 12.sp, color = MutedLight)
                    }
                    Box(
                        Modifier.fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceMuted)
                            .clickable(enabled = !exporting) { viewModel.exportCsv() }.padding(vertical = 12.dp),
                        Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, null, tint = MutedDark, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (exporting) "Exporting…" else "Export  CSV", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedDark)
                        }
                    }
                }

                // ── Notifications (collapsible) ────────────────────────────────────
                NotificationsSection(
                    settings = notifSettings,
                    onSetMaster = { on -> viewModel.setMasterNotifications(on); ensureNotifPerm(on) },
                    onSetMealSlot = { slot, on -> viewModel.setMealSlot(slot, on); ensureNotifPerm(on) },
                    onSetMealTime = viewModel::setMealSlotTime,
                    onToggleType = onToggleNotif,
                    onSetWorkoutTime = viewModel::setWorkoutTime,
                    onSetWeighinDay = viewModel::setWeighinDay,
                    onSetWeighinTime = viewModel::setWeighinTime,
                    onToggleQuiet = viewModel::setQuietHours,
                )

                // ── Social ─────────────────────────────────────────────────────────
                SectionLabel("Social")
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Social notifications", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = Ink)
                            Text("New followers and shared items", fontSize = 11.5.sp, color = MutedLight)
                        }
                        AppSwitch(socialNotifications) { on -> viewModel.setSocialNotifications(on) }
                    }
                }

                // ── Feedback ───────────────────────────────────────────────────────
                SectionLabel("Feedback")
                Card {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { showFeedback = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFDFEAF6)), Alignment.Center) {
                            Text("💬", fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Send feedback", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("Report a bug or suggest an improvement", fontSize = 11.5.sp, color = MutedLight)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MutedFaint, modifier = Modifier.size(18.dp))
                    }
                }

                // ── Help ───────────────────────────────────────────────────────────
                val tourController = LocalTourController.current
                if (tourController != null) {
                    SectionLabel("Help")
                    Card {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                onBack()               // leave Settings so the tour is seen over the app
                                tourController.start()
                            }.padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFDFF2E7)), Alignment.Center) {
                                Text("🧭", fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Replay app tour", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text("Walk through the app's sections again", fontSize = 11.5.sp, color = MutedLight)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MutedFaint, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // ── Admin ──────────────────────────────────────────────────────────
                // Visible only to server admins (email allowlist); the endpoints 403 for everyone else.
                if (isAdmin) {
                    SectionLabel("Admin")
                    Card {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onOpenAdmin() }.padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFF0E6F6)), Alignment.Center) {
                                Text("🛠️", fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Feature flags", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text("Toggle server features on/off", fontSize = 11.5.sp, color = MutedLight)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MutedFaint, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    "© EatMyPlan · v${BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp, color = MutedFaint, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                )
            }
        }
    }

    if (showFeedback) {
        val submitting by viewModel.feedbackSubmitting.collectAsState()
        AlertDialog(
            onDismissRequest = { if (!submitting) showFeedback = false },
            containerColor = Surface,
            title = { Text("Send feedback", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink) },
            text = {
                Column {
                    Text(
                        "Tell us what's working or what's not. Your app version is included automatically.",
                        fontSize = 12.5.sp, color = MutedLight,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("Your feedback…", color = MutedFaint) },
                        enabled = !submitting,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.sendFeedback(feedbackText); feedbackText = ""; showFeedback = false },
                    enabled = !submitting && feedbackText.isNotBlank(),
                ) { Text("Send", color = Teal, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showFeedback = false }, enabled = !submitting) {
                    Text("Cancel", color = MutedDark)
                }
            },
        )
    }
}

/** Writes the CSV to cacheDir/exports and opens the system share sheet via FileProvider. */
private fun shareCsv(context: Context, fileName: String, csv: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName).apply { writeText(csv) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export data"))
}

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") // ISO 1..7

/** Minutes-since-midnight → "8:00 AM". */
private fun formatMin(min: Int): String {
    val h = min / 60; val m = min % 60
    val h12 = ((h + 11) % 12) + 1
    return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
}

@Composable
private fun NotificationsSection(
    settings: NotificationSettings,
    onSetMaster: (Boolean) -> Unit,
    onSetMealSlot: (String, Boolean) -> Unit,
    onSetMealTime: (String, Int) -> Unit,
    onToggleType: (NotificationType, Boolean) -> Unit,
    onSetWorkoutTime: (Int) -> Unit,
    onSetWeighinDay: (Int) -> Unit,
    onSetWeighinTime: (Int) -> Unit,
    onToggleQuiet: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(true) }
    // Opens the native time picker seeded at [initial], reporting the chosen minutes-since-midnight.
    val pickTime: (Int, (Int) -> Unit) -> Unit = { initial, onPicked ->
        android.app.TimePickerDialog(context, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60,
            android.text.format.DateFormat.is24HourFormat(context)).show()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 8.dp),
    ) {
        Text("NOTIFICATIONS", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MutedFaint, letterSpacing = 0.6.sp)
        Spacer(Modifier.width(8.dp))
        Text(if (!settings.masterEnabled) "Off" else "${settings.onCount} of 4 on", fontSize = 10.sp, color = MutedFaint)
        Spacer(Modifier.weight(1f))
        Icon(
            if (open) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null, tint = MutedFaint, modifier = Modifier.size(18.dp),
        )
    }

    if (open) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        ) {
            // Master switch — governs everything below it.
            NotifRow("🔔", 0xFFEAF0F6, "Notifications", "", settings.masterEnabled, onToggle = onSetMaster)
            Divider()
            // Everything below is greyed out + non-interactive when the master switch is off.
            Box {
                Column(Modifier.alpha(if (settings.masterEnabled) 1f else 0.4f)) {
                    MealReminders(settings, onSetMealSlot, onSetMealTime, pickTime)
                    Divider()
                    // Water — fixed schedule, on/off only.
                    NotifRow("💧", 0xFFDFEAF6, "Water", "Every 2 hours, 8am–8pm", settings.waterEnabled,
                        onToggle = { onToggleType(NotificationType.WATER, it) })
                    Divider()
                    // Workout — editable daily time.
                    NotifRow("🏋️", 0xFFDFF2E7, "Workout", "Daily at ${formatMin(settings.workoutMinute)}", settings.workoutEnabled,
                        trailing = { if (settings.workoutEnabled) TimeChip(settings.workoutMinute) { pickTime(settings.workoutMinute, onSetWorkoutTime) } },
                        onToggle = { onToggleType(NotificationType.WORKOUT, it) })
                    Divider()
                    // Weigh-in — editable weekday + time.
                    WeighinRow(settings, onSetWeighinDay, { pickTime(settings.weighinMinute, onSetWeighinTime) },
                        onToggle = { onToggleType(NotificationType.WEIGHIN, it) })
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Quiet hours", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = MutedDark)
                            Text(if (settings.quietHours) "No reminders 10 PM – 7 AM" else "Off", fontSize = 11.5.sp, color = MutedLight)
                        }
                        AppSwitch(settings.quietHours, onToggleQuiet)
                    }
                }
                if (!settings.masterEnabled) {
                    // Transparent overlay swallows taps so the greyed rows can't be edited while paused.
                    Box(Modifier.matchParentSize().clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null) {})
                }
            }
        }
    }
}

/** Expandable meal-reminders block — a per-slot enable + time for every canonical meal slot. */
@Composable
private fun MealReminders(
    settings: NotificationSettings,
    onSet: (String, Boolean) -> Unit,
    onTime: (String, Int) -> Unit,
    pickTime: (Int, (Int) -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val onCount = settings.mealSlots.count { it.enabled }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 15.dp, vertical = 10.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFF6EBE0)), Alignment.Center) { Text("🍽️", fontSize = 16.sp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("Meal reminders", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(if (onCount == 0) "Off — tap to set per slot" else "$onCount ${if (onCount == 1) "slot" else "slots"} on",
                fontSize = 11.5.sp, color = MutedLight)
        }
        Icon(if (open) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null, tint = MutedFaint, modifier = Modifier.size(18.dp))
    }
    if (open) {
        Column(Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, bottom = 8.dp)) {
            settings.mealSlots.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(r.slot, fontSize = 13.sp, color = Ink, modifier = Modifier.weight(1f))
                    if (r.enabled) { TimeChip(r.minute) { pickTime(r.minute) { m -> onTime(r.slot, m) } }; Spacer(Modifier.width(8.dp)) }
                    AppSwitch(r.enabled) { on -> onSet(r.slot, on) }
                }
            }
        }
    }
}

/** A notification row with an icon, label/hint, optional trailing content (e.g. a time chip), and a switch. */
@Composable
private fun NotifRow(
    icon: String, bg: Long, label: String, hint: String, enabled: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onToggle: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(bg)), Alignment.Center) { Text(icon, fontSize = 16.sp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (hint.isNotBlank()) Text(hint, fontSize = 11.5.sp, color = MutedLight)
        }
        if (trailing != null) { trailing(); Spacer(Modifier.width(8.dp)) }
        AppSwitch(enabled, onToggle)
    }
}

/** Weigh-in row: weekday dropdown + time chip when enabled. */
@Composable
private fun WeighinRow(
    settings: NotificationSettings,
    onSetDay: (Int) -> Unit,
    onPickTime: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    var dayMenu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFEBE7F3)), Alignment.Center) { Text("⚖️", fontSize = 16.sp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("Weigh-in", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("Weekly", fontSize = 11.5.sp, color = MutedLight)
        }
        if (settings.weighinEnabled) {
            Box {
                Text(WEEKDAYS[settings.weighinDayIso - 1], fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceMuted).clickable { dayMenu = true }.padding(horizontal = 10.dp, vertical = 5.dp))
                DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                    WEEKDAYS.forEachIndexed { i, d -> DropdownMenuItem(text = { Text(d) }, onClick = { onSetDay(i + 1); dayMenu = false }) }
                }
            }
            Spacer(Modifier.width(6.dp))
            TimeChip(settings.weighinMinute, onPickTime)
            Spacer(Modifier.width(8.dp))
        }
        AppSwitch(settings.weighinEnabled, onToggle)
    }
}

/** Small tappable pill showing a formatted time. */
@Composable
private fun TimeChip(minute: Int, onClick: () -> Unit) {
    Text(formatMin(minute), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceMuted).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp))
}

// ── Building blocks ────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MutedFaint,
        letterSpacing = 0.6.sp, modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(CardBorder))

@Composable
private fun ValueRow(label: String, value: String, labelColor: Color = Ink) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 14.dp, vertical = 13.dp)) {
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = labelColor)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MutedFaint, modifier = Modifier.size(18.dp).padding(start = 4.dp))
    }
}

@Composable
private fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = Teal, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = BorderMuted, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

