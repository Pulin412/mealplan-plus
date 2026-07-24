package com.mealplanplus.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderMuted
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Success
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal

/** UI-only Settings screen (matches design 13a). Toggles/collapse flip locally; buttons + dropdowns
 *  are placeholders — functionality (backup, Health Connect, export, notifications) comes next. */
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
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
                // ── Backup & restore ──────────────────────────────────────────────
                SectionLabel("Backup & restore")
                Card {
                    var autoBackup by remember { mutableStateOf(true) }
                    ToggleRow("Auto-backup", "Encrypted, to your account", autoBackup) { autoBackup = it }
                    Divider()
                    ValueRow("Frequency", "Daily")
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Icon(Icons.Default.Check, null, tint = Success, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Last backup: Today, 8:04 AM", fontSize = 12.sp, color = MutedDark)
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledBtn("Back up now", Modifier.weight(1f))
                        OutlineBtn("Restore", Modifier.weight(1f))
                    }
                }

                // ── Health Connect ────────────────────────────────────────────────
                SectionLabel("Health Connect")
                Card {
                    var connected by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(SurfaceMuted), Alignment.Center) {
                            Icon(Icons.Outlined.FavoriteBorder, null, tint = MutedDark, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Health Connect", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(if (connected) "Connected" else "Not connected", fontSize = 11.5.sp, color = MutedLight)
                        }
                        AppSwitch(connected) { connected = it }
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
                            .clickable { }.padding(vertical = 12.dp),
                        Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, null, tint = MutedDark, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export  CSV", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedDark)
                        }
                    }
                }

                // ── Notifications (collapsible) ────────────────────────────────────
                NotificationsSection()
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

private data class NotifDef(val key: String, val label: String, val hint: String, val icon: String, val bg: Long)

private val NOTIF_DEFS = listOf(
    NotifDef("meals", "Meal reminders", "At each planned slot", "🍽️", 0xFFF6EBE0),
    NotifDef("water", "Water", "Every 2 hours, 8am–8pm", "💧", 0xFFDFEAF6),
    NotifDef("workout", "Workout", "On scheduled days", "🏋️", 0xFFDFF2E7),
    NotifDef("weighin", "Weigh-in", "Weekly, Sunday 8am", "⚖️", 0xFFEBE7F3),
    NotifDef("glucose", "Glucose check", "Before & after meals", "🩸", 0xFFF6E4E2),
)

@Composable
private fun NotificationsSection() {
    var open by remember { mutableStateOf(true) }
    val on = remember { mutableStateMapOf("meals" to true, "water" to true, "workout" to true, "weighin" to false, "glucose" to true) }
    val onCount = on.count { it.value }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 8.dp),
    ) {
        Text("NOTIFICATIONS", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MutedFaint, letterSpacing = 0.6.sp)
        Spacer(Modifier.width(8.dp))
        Text("$onCount of ${NOTIF_DEFS.size} on", fontSize = 10.sp, color = MutedFaint)
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
            NOTIF_DEFS.forEachIndexed { i, n ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp)) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(n.bg)), Alignment.Center) {
                        Text(n.icon, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(n.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(n.hint, fontSize = 11.5.sp, color = MutedLight)
                    }
                    AppSwitch(on[n.key] == true) { on[n.key] = it }
                }
                if (i < NOTIF_DEFS.size - 1) Divider()
            }
            Divider()
            ValueRow("Quiet hours", "10 PM – 7 AM", labelColor = MutedDark)
        }
    }
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
private fun ToggleRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(hint, fontSize = 11.5.sp, color = MutedLight)
        }
        AppSwitch(checked, onChange)
    }
}

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
private fun FilledBtn(text: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(11.dp)).background(Teal).clickable { }.padding(vertical = 12.dp), Alignment.Center) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnAccent)
    }
}

@Composable
private fun OutlineBtn(text: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(11.dp)).border(1.5.dp, BorderMuted, RoundedCornerShape(11.dp)).clickable { }.padding(vertical = 12.dp), Alignment.Center) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Teal)
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

