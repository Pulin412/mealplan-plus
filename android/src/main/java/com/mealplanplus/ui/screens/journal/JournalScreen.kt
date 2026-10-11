package com.mealplanplus.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import android.widget.Toast
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Success
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
private fun statusColor(s: JStatus): Color? = when (s) {
    JStatus.DONE -> Success; JStatus.MISSED -> Danger; JStatus.PENDING -> Teal; JStatus.NONE -> null
}
private fun statusMark(s: JStatus): String = when (s) {
    JStatus.DONE -> "✓"; JStatus.MISSED -> "✗"; JStatus.PENDING -> "•"; JStatus.NONE -> ""
}

@Composable
fun JournalScreen(
    onBack: () -> Unit = {},
    onOpenRunner: (Long, String) -> Unit = { _, _ -> },
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<LocalDate?>(null) }
    var calOpen by remember { mutableStateOf(false) }

    // Opt-in note lock (device biometric/credential). Starts locked each visit if enabled.
    val context = LocalContext.current
    var lockEnabled by remember { mutableStateOf(isJournalLockEnabled(context)) }
    var locked by remember { mutableStateOf(isJournalLockEnabled(context)) }
    val toggleLock = {
        when {
            locked -> (context as? FragmentActivity)?.let { act ->
                promptJournalUnlock(act, onSuccess = { locked = false }, onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() })
            }
            lockEnabled -> locked = true                                   // re-lock now
            else -> { setJournalLockEnabled(context, true); lockEnabled = true; locked = true } // turn it on
        }
        Unit
    }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(Surface).padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
                Column(Modifier.weight(1f)) {
                    Text("Journal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Your daily diary — notes, diets & workouts", fontSize = 11.5.sp, color = MutedLight)
                }
                if (!state.atToday) {
                    Text("Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(SurfaceMuted)
                            .clickable { viewModel.resetToToday() }.padding(horizontal = 11.dp, vertical = 7.dp))
                    Spacer(Modifier.width(6.dp))
                }
                val lockLabel = if (locked) "Locked" else if (lockEnabled) "Unlocked" else "Lock"
                val lockAccent = !locked && lockEnabled
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(if (lockAccent) Teal.copy(alpha = 0.12f) else Surface)
                        .border(1.dp, if (lockAccent) Teal else CardBorder, RoundedCornerShape(9.dp))
                        .clickable(onClick = toggleLock).padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Icon(if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, lockLabel,
                        tint = if (lockAccent) Teal else MutedDark, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(lockLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (lockAccent) Teal else MutedDark)
                }
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).border(1.dp, CardBorder, RoundedCornerShape(9.dp))
                    .clickable { calOpen = true }, Alignment.Center) {
                    Icon(Icons.Outlined.CalendarMonth, "Jump to a date", tint = MutedDark, modifier = Modifier.size(18.dp))
                }
            }

            state.error?.let { Text(it, fontSize = 12.sp, color = Danger, modifier = Modifier.fillMaxWidth().padding(16.dp)) }

            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(state.entries, key = { it.date.toString() }) { e ->
                    DayCard(e = e, today = state.today, locked = locked,
                        editing = editing == e.date,
                        onEdit = { editing = e.date }, onClose = { editing = null },
                        onSave = { t -> viewModel.saveNote(e.date, t); editing = null },
                        onDelete = { viewModel.deleteNote(e.date); editing = null },
                        onOpenWorkout = { w -> w.templateId?.let { onOpenRunner(it, w.name) } })
                    Spacer(Modifier.height(10.dp))
                }
                item {
                    Text("Load older days", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MutedDark,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.5.dp, CardBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.loadOlder() }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(80.dp))
                }
            }
        }

        if (calOpen) CalendarDialog(state, onPick = { viewModel.jumpTo(it); calOpen = false }, onDismiss = { calOpen = false })
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DayCard(
    e: JDay, today: LocalDate, locked: Boolean, editing: Boolean,
    onEdit: () -> Unit, onClose: () -> Unit, onSave: (String) -> Unit, onDelete: () -> Unit,
    onOpenWorkout: (JWorkout) -> Unit,
) {
    var showMeals by remember(e.date) { mutableStateOf(false) }
    var expandedWorkout by remember(e.date) { mutableStateOf<String?>(null) }
    val isToday = e.date == today
    val isPast = e.date.isBefore(today)

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
        .border(1.dp, if (isToday) Teal else CardBorder, RoundedCornerShape(14.dp)).padding(14.dp)) {

        Row(verticalAlignment = Alignment.Bottom) {
            Text("${e.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${e.date.dayOfMonth}",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.width(6.dp))
            Text(e.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + if (isToday) " · Today" else "",
                fontSize = 11.sp, color = MutedLight)
        }

        if (e.dietName != null || e.workouts.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp)) {
                if (e.dietName != null) {
                    val c = statusColor(e.dietStatus)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceMuted)
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)).clickable { showMeals = !showMeals }
                            .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(e.dietName, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                        if (c != null) { Spacer(Modifier.width(5.dp)); Text(statusMark(e.dietStatus), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = c) }
                        Spacer(Modifier.width(4.dp)); Text(if (showMeals) "▲" else "▼", fontSize = 8.sp, color = MutedFaint)
                    }
                }
                e.workouts.forEach { w ->
                    val wc = when (w.status) { "done" -> Success; "active" -> Teal; else -> MutedDark }
                    // Done → expand a read-only log. Not-done today/upcoming → open the runner.
                    // Past & not done (missed) → not tappable.
                    val hasLog = w.status == "done" && w.log.isNotEmpty()
                    val canRun = w.status != "done" && w.templateId != null && !isPast
                    val onClick: (() -> Unit)? = when {
                        hasLog -> ({ expandedWorkout = if (expandedWorkout == w.key) null else w.key })
                        canRun -> ({ onOpenWorkout(w) })
                        else -> null
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceMuted)
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                            .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(w.name, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Spacer(Modifier.width(5.dp))
                        Text(if (w.status == "done") "✓" else if (w.status == "active") "•" else "✗", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = wc)
                        when {
                            hasLog -> { Spacer(Modifier.width(4.dp)); Text(if (expandedWorkout == w.key) "▲" else "▼", fontSize = 8.sp, color = MutedFaint) }
                            canRun -> { Spacer(Modifier.width(3.dp)); Text("›", fontSize = 12.sp, color = MutedFaint) }
                        }
                    }
                }
            }
        }

        // Read-only workout log (completed session), expanded under its pill.
        val openW = e.workouts.firstOrNull { it.key == expandedWorkout }
        if (openW != null && openW.log.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
                openW.log.forEach { ex ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Text(ex.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                        Text("${ex.sets.size} set${if (ex.sets.size == 1) "" else "s"}", fontFamily = DmMono, fontSize = 10.5.sp, color = MutedFaint)
                    }
                    ex.sets.forEachIndexed { i, set ->
                        Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 1.dp)) {
                            Text("Set ${i + 1}", fontSize = 11.sp, color = MutedDark, modifier = Modifier.width(54.dp))
                            Text(set.label, fontFamily = DmMono, fontSize = 11.sp, color = Ink)
                        }
                    }
                }
            }
        }

        if (showMeals && e.meals.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
                e.meals.forEach { meal ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Text(meal.slot.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceMuted).padding(horizontal = 6.dp, vertical = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(meal.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                        Text("${meal.kcal} kcal", fontFamily = DmMono, fontSize = 10.5.sp, color = MutedFaint)
                    }
                    meal.items.forEach { f ->
                        Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 1.dp)) {
                            Text("• ${f.name}", fontSize = 11.5.sp, color = MutedDark, modifier = Modifier.weight(1f))
                            Text(f.meta, fontFamily = DmMono, fontSize = 10.sp, color = MutedFaint)
                        }
                    }
                }
            }
        }

        // Note — the focus of the day (hidden while locked)
        Box(Modifier.padding(top = 10.dp)) {
            if (locked) {
                if (e.note.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceMuted).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Icon(Icons.Outlined.Lock, null, tint = MutedDark, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Note hidden — unlock to view", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = MutedDark)
                    }
                }
            } else if (editing) {
                NoteEditor(e.note, onSave, onClose, if (e.note.isNotEmpty()) onDelete else null)
            } else if (e.note.isNotEmpty()) {
                Text(e.note, fontSize = 13.sp, color = Ink,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Teal.copy(alpha = 0.08f))
                        .clickable { onEdit() }.padding(horizontal = 12.dp, vertical = 10.dp))
            } else {
                Text("✎ Add a note…", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = MutedLight,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, CardBorder, RoundedCornerShape(10.dp)).clickable { onEdit() }
                        .padding(horizontal = 12.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun NoteEditor(initial: String, onSave: (String) -> Unit, onCancel: () -> Unit, onDelete: (() -> Unit)?) {
    var draft by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = draft, onValueChange = { draft = it },
            placeholder = { Text("e.g. Cheated a bit — pizza night, back on track Monday.", fontSize = 13.sp, color = MutedLight) },
            modifier = Modifier.fillMaxWidth(), minLines = 3)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            if (onDelete != null) Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Danger, modifier = Modifier.clickable { onDelete() }.padding(4.dp))
            Spacer(Modifier.weight(1f))
            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.clickable { onCancel() }.padding(horizontal = 10.dp, vertical = 4.dp))
            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnAccent,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Teal).clickable { onSave(draft) }.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun CalendarDialog(state: JournalUiState, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    var ym by remember { mutableStateOf(YearMonth.from(state.endDate)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(18.dp)).background(Surface).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceMuted).clickable { ym = ym.minusMonths(1) }, Alignment.Center) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Previous", tint = MutedDark, modifier = Modifier.size(18.dp))
                }
                Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceMuted).clickable { ym = ym.plusMonths(1) }, Alignment.Center) {
                    Icon(Icons.Default.KeyboardArrowRight, "Next", tint = MutedDark, modifier = Modifier.size(18.dp))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Box(Modifier.weight(1f), Alignment.Center) { Text(it, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint) }
                }
            }
            val firstDow = ym.atDay(1).dayOfWeek.value
            val cells = List(firstDow - 1) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { i ->
                        val date = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(1f), Alignment.Center) {
                            if (date != null) {
                                val isSel = date == state.endDate
                                val isToday = date == state.today
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Teal else Color.Transparent)
                                    .border(1.dp, if (isToday && !isSel) Teal else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable { onPick(date) }, Alignment.Center) {
                                    Text("${date.dayOfMonth}", fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) OnAccent else Ink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
