package com.mealplanplus.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.generated.model.DayPlanDto
import com.mealplanplus.ui.theme.AppBg
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlanScreen() {
    val viewModel: PlanViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)) {
            Text("Plan", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            MonthCalendar(state, onPrev = viewModel::prevMonth, onNext = viewModel::nextMonth, onDay = viewModel::selectDay)
            Spacer(Modifier.height(18.dp))
            NextSeven(state, onDay = viewModel::selectDay)
            Spacer(Modifier.height(24.dp))
        }
    }

    state.selectedDate?.let { date ->
        DayPlanSheet(date, state, onSetDiet = { viewModel.setDiet(date, it) }, onClear = { viewModel.clearDay(date) }, onClose = { viewModel.selectDay(null) })
    }
}

// ── Month calendar ──────────────────────────────────────────────────────────────
@Composable
private fun MonthCalendar(state: PlanUiState, onPrev: () -> Unit, onNext: () -> Unit, onDay: (LocalDate) -> Unit) {
    val month = state.month
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceMuted).clickable(onClick = onPrev), Alignment.Center) {
                Icon(Icons.Default.KeyboardArrowLeft, "Previous", tint = MutedDark, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceMuted).clickable(onClick = onNext), Alignment.Center) {
                Icon(Icons.Default.KeyboardArrowRight, "Next", tint = MutedDark, modifier = Modifier.size(18.dp))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Box(Modifier.weight(1f), Alignment.Center) { Text(it, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint) }
            }
        }
        Spacer(Modifier.height(4.dp))
        val firstDow = month.atDay(1).dayOfWeek.value // Mon=1..Sun=7
        val cells = List(firstDow - 1) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val date = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f), Alignment.Center) {
                        if (date != null) DayCell(date, state, onDay)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, state: PlanUiState, onDay: (LocalDate) -> Unit) {
    val plan = state.plansByDate[date]
    val isToday = date == state.today
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(34.dp).clip(CircleShape)
            .background(if (isToday) Teal else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onDay(date) }, verticalArrangement = Arrangement.Center) {
        Text("${date.dayOfMonth}", fontSize = 12.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) OnAccent else Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 1.dp)) {
            if (plan?.dietId != null) Dot(if (isToday) OnAccent else Teal)
            if (!plan?.plannedWorkouts.isNullOrEmpty()) Dot(if (isToday) OnAccent else Success)
        }
    }
}

@Composable private fun Dot(color: androidx.compose.ui.graphics.Color) = Box(Modifier.size(4.dp).clip(CircleShape).background(color))

// ── Next 7 days ──────────────────────────────────────────────────────────────
@Composable
private fun NextSeven(state: PlanUiState, onDay: (LocalDate) -> Unit) {
    Text("Next 7 days", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))) {
        (0..6).forEach { offset ->
            val date = state.today.plusDays(offset.toLong())
            val plan = state.plansByDate[date]
            val diet = plan?.dietId?.let { id -> state.diets.firstOrNull { it.id == id } }
            val workouts = plan?.plannedWorkouts?.mapNotNull { it.activityName }?.takeIf { it.isNotEmpty() }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onDay(date) }.padding(horizontal = 12.dp, vertical = 11.dp)) {
                Column(Modifier.width(52.dp)) {
                    Text(if (offset == 0) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(date.format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 9.5.sp, color = MutedFaint)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    if (diet != null) Text(diet.name, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                    else Text("No diet", fontSize = 11.sp, color = MutedLight)
                    Text(workouts?.joinToString(", ") ?: "No workout", fontSize = 10.sp, color = if (workouts != null) Success else MutedFaint)
                }
                if (diet != null) Text("${diet.kcal}", fontFamily = DmMono, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        }
    }
}

// ── Day plan sheet ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DayPlanSheet(date: LocalDate, state: PlanUiState, onSetDiet: (Long?) -> Unit, onClear: () -> Unit, onClose: () -> Unit) {
    val plan: DayPlanDto? = state.plansByDate[date]
    val workouts = plan?.plannedWorkouts.orEmpty()
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Set the diet and workout for this day.", fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp))
                }
                if (plan != null) Text("Clear day", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Danger, modifier = Modifier.clickable(onClick = onClear))
            }

            SectionLabel("Diet plan")
            state.diets.forEach { d -> DietRadio(d.name, "${d.kcal} kcal", plan?.dietId == d.id) { onSetDiet(d.id) } }
            val selectedDiet = plan?.dietId?.let { id -> state.diets.firstOrNull { it.id == id } }
            selectedDiet?.let { DietSlots(it) }

            SectionLabel("Exercises")
            if (workouts.isEmpty()) {
                Text("No workouts planned.", fontSize = 11.5.sp, color = MutedLight, modifier = Modifier.padding(bottom = 8.dp))
            } else {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    workouts.forEach { w ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceMuted).padding(start = 11.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
                            Text(w.activityName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("  ×", fontSize = 12.sp, color = MutedFaint)
                        }
                    }
                }
            }
            // Workouts library + Start are mocked for now — the Exercises/Workouts screens land next.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).border(1.5.dp, CardBorder, RoundedCornerShape(11.dp)).clickable(enabled = false) {}.padding(vertical = 11.dp)) {
                Text("＋ Add from library", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MutedLight)
            }
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).clickable(enabled = false) {}.padding(vertical = 13.dp)) {
                Text("▶  Start workout", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedLight)
            }
            Text("Workouts coming soon — the Exercises & Workouts screens are next.", fontSize = 9.5.sp, color = MutedFaint, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// ── Selected diet detail (slots + foods, like the Home diet view) ────────────────
@Composable
private fun DietSlots(diet: DietSummary) {
    if (diet.slots.isEmpty()) {
        Text("This diet has no meals yet.", fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(top = 8.dp))
        return
    }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        diet.slots.forEach { slot ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                SlotBadge(slot.slot)
                Spacer(Modifier.weight(1f))
                Text("${slot.kcal} kcal", fontFamily = DmMono, fontSize = 10.sp, color = MutedFaint)
            }
            slot.lines.forEach { line ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(top = if (line.header) 4.dp else 2.dp, start = if (line.header) 0.dp else 8.dp)) {
                    Text(line.name, fontSize = if (line.header) 12.5.sp else 11.sp,
                        fontWeight = if (line.header) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (line.header) Ink else MutedDark, modifier = Modifier.weight(1f))
                    Text(line.meta, fontSize = 9.5.sp, color = MutedFaint, fontFamily = DmMono)
                }
            }
        }
    }
}

@Composable
private fun SlotBadge(slot: String) =
    Text(slot.uppercase(), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Teal.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp))

@Composable
private fun SectionLabel(text: String) =
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

@Composable
private fun DietRadio(name: String, kcal: String?, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Box(Modifier.size(18.dp).clip(CircleShape).border(1.5.dp, if (selected) Teal else CardBorder, CircleShape), Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(Teal))
        }
        Spacer(Modifier.width(10.dp))
        Text(name, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = Ink, modifier = Modifier.weight(1f))
        kcal?.let { Text(it, fontFamily = DmMono, fontSize = 10.5.sp, color = MutedFaint) }
    }
}
