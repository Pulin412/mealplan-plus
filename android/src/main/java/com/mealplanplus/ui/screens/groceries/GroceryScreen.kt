package com.mealplanplus.ui.screens.groceries

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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.local.SavedGroceryList
import com.mealplanplus.data.repository.unitLabel
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderMuted
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Scrim
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val CAT_ORDER = GroceryCat.entries

@Composable
fun GroceryScreen(onMenu: () -> Unit = {}) {
    val vm: GroceryViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GroceryAppBar(state.savedLists.size, onMenu, vm::openSaved)

            Column(Modifier.padding(horizontal = 16.dp)) {
                DateCard(state, vm)
                if (state.calOpen && !state.isSaved) {
                    Spacer(Modifier.height(10.dp))
                    Calendar(state, vm)
                }

                Spacer(Modifier.height(18.dp))
                ListHeader(state, vm)
                Spacer(Modifier.height(12.dp))

                if (state.total > 0) {
                    Tabs(state, vm)
                    Spacer(Modifier.height(14.dp))
                }

                ShoppingList(state, vm)
                Spacer(Modifier.height(24.dp))
            }
        }

        if (state.sheetSaved) SavedSheet(state, vm)
    }
}

// ── App bar ──────────────────────────────────────────────────────────────────
@Composable
private fun GroceryAppBar(savedCount: Int, onMenu: () -> Unit, onSaved: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
        Text("Groceries", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(onClick = onSaved) { Icon(Icons.Default.BookmarkBorder, "Saved lists", tint = MutedDark) }
            if (savedCount > 0) {
                Box(Modifier.padding(top = 8.dp, end = 8.dp).size(7.dp).clip(CircleShape).background(Teal))
            }
        }
    }
}

// ── Date card ──────────────────────────────────────────────────────────────────
@Composable
private fun DateCard(state: GroceryUiState, vm: GroceryViewModel) {
    val nDays = state.dateKeys.size
    val title = if (state.isSaved) state.activeName ?: "Saved list"
    else "${if (nDays == 0) "No" else nDays} day${if (nDays == 1) "" else "s"} of shopping"
    val range = rangeLabelOf(state.dateKeys)
    val sub = if (state.isSaved) "$nDays day${if (nDays == 1) "" else "s"} · $range"
    else if (nDays == 0) "Tap to pick dates" else range
    val actionLabel = if (state.isSaved) "New list" else if (state.calOpen) "Done" else "Edit dates"

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { if (!state.isSaved) vm.toggleCal() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Teal.copy(alpha = 0.13f)), Alignment.Center) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = Teal, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
            Text(sub, fontSize = 11.5.sp, color = MutedLight, maxLines = 1)
        }
        Text(
            actionLabel, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
            modifier = Modifier.clickable { if (state.isSaved) vm.newList() else vm.toggleCal() }.padding(4.dp),
        )
    }
}

// ── Calendar ──────────────────────────────────────────────────────────────────
@Composable
private fun Calendar(state: GroceryUiState, vm: GroceryViewModel) {
    val month = state.month
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            RoundBtn(Icons.Default.KeyboardArrowLeft, "Previous", vm::prevMonth)
            Spacer(Modifier.weight(1f))
            Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            RoundBtn(Icons.Default.KeyboardArrowRight, "Next", vm::nextMonth)
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Box(Modifier.weight(1f), Alignment.Center) { Text(it, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint) }
            }
        }
        Spacer(Modifier.height(4.dp))
        val firstDow = month.atDay(1).dayOfWeek.value
        val cells = List(firstDow - 1) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val date = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f), Alignment.Center) {
                        if (date != null) DayCell(date, state, vm::toggleDay)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Preset("Next 7 days", state, 7, vm)
            Preset("Next 14 days", state, 14, vm)
            Spacer(Modifier.weight(1f))
            Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MutedDark,
                modifier = Modifier.clickable(onClick = vm::clearDates).padding(vertical = 6.dp, horizontal = 4.dp))
        }
    }
}

@Composable
private fun Preset(label: String, state: GroceryUiState, n: Int, vm: GroceryViewModel) {
    val keys = state.selectedDates.map { it.toString() }.sorted()
    val active = keys.size == n && keys.firstOrNull() == state.today.toString()
    Text(
        label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = if (active) OnAccent else MutedDark,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (active) Teal else SurfaceMuted)
            .clickable { vm.preset(n) }.padding(vertical = 7.dp, horizontal = 11.dp),
    )
}

@Composable
private fun DayCell(date: LocalDate, state: GroceryUiState, onDay: (LocalDate) -> Unit) {
    val isSel = date in state.selectedDates
    val isToday = date == state.today
    val hasDiet = date in state.plannedDates
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
            .background(if (isSel) Teal else Color.Transparent)
            .border(1.dp, if (isToday && !isSel) Teal else Color.Transparent, RoundedCornerShape(9.dp))
            .clickable { onDay(date) },
    ) {
        Text("${date.dayOfMonth}", fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
            color = if (isSel) OnAccent else Ink)
        Box(Modifier.padding(top = 1.dp).size(4.dp).clip(CircleShape)
            .background(if (hasDiet) (if (isSel) OnAccent.copy(alpha = 0.9f) else Teal) else Color.Transparent))
    }
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit) {
    Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceMuted).clickable(onClick = onClick), Alignment.Center) {
        Icon(icon, cd, tint = MutedDark, modifier = Modifier.size(18.dp))
    }
}

// ── List header (title + Save + progress) ─────────────────────────────────────
@Composable
private fun ListHeader(state: GroceryUiState, vm: GroceryViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(if (state.isSaved) "Saved list" else "Shopping list", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.weight(1f))
        if (!state.isSaved && state.total > 0) {
            Text("Clear list", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Danger,
                modifier = Modifier.clickable(onClick = vm::clearList).padding(4.dp))
            Spacer(Modifier.width(6.dp))
        }
        if (!state.isSaved) {
            IconButton(onClick = vm::refresh, enabled = !state.refreshing, modifier = Modifier.size(34.dp)) {
                if (state.refreshing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = Teal, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.Refresh, "Recalculate from plan", tint = MutedDark, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(2.dp))
        }
        val label = if (state.isSaved) "Done" else "Save"
        val enabled = state.isSaved || (state.total > 0 && state.selectedDates.isNotEmpty())
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) Teal else MutedLight,
            modifier = Modifier.clickable(enabled = enabled) { if (state.isSaved) vm.newList() else vm.saveList() }.padding(4.dp))
    }
    if (state.total > 0) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(SurfaceMuted)) {
                val frac = if (state.total == 0) 0f else state.boughtCount.toFloat() / state.total
                Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(CircleShape).background(Teal))
            }
            Spacer(Modifier.width(10.dp))
            Text("${state.boughtCount} / ${state.total}", fontFamily = DmMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedDark)
        }
    }
}

// ── Tabs ──────────────────────────────────────────────────────────────────────
@Composable
private fun Tabs(state: GroceryUiState, vm: GroceryViewModel) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceMuted).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Tab("All · ${state.total}", state.view == GroceryView.ALL, Modifier.weight(1f)) { vm.setView(GroceryView.ALL) }
        Tab("To buy · ${state.toBuy.size}", state.view == GroceryView.TO_BUY, Modifier.weight(1f)) { vm.setView(GroceryView.TO_BUY) }
        Tab("Bought · ${state.boughtCount}", state.view == GroceryView.BOUGHT, Modifier.weight(1f)) { vm.setView(GroceryView.BOUGHT) }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) Surface else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 8.dp), Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Ink else Muted)
    }
}

// ── Shopping list body ─────────────────────────────────────────────────────────
@Composable
private fun ShoppingList(state: GroceryUiState, vm: GroceryViewModel) {
    // Each row is independent: unchecked rows sit in their aisle, checked rows in Bought.
    val primary = if (state.view == GroceryView.BOUGHT) state.bought else state.toBuy
    val groups = CAT_ORDER.mapNotNull { cat ->
        val rs = primary.filter { it.category == cat }
        if (rs.isEmpty()) null else cat to rs
    }
    val showBought = state.view == GroceryView.ALL && state.bought.isNotEmpty()

    val nDays = state.dateKeys.size
    val empty = state.total == 0 || (groups.isEmpty() && !showBought)
    if (empty) {
        val msg = when {
            nDays == 0 -> "Pick the days you want to shop for and your list builds itself."
            state.total == 0 -> "No meals are planned on the selected days. Add a diet in Plan first."
            state.view == GroceryView.TO_BUY -> "Everything is checked off — you’re all set!"
            state.view == GroceryView.BOUGHT -> "Nothing checked off yet. Tick items as you shop."
            else -> "Nothing here yet."
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), Alignment.Center) {
            Text(msg, fontSize = 13.sp, color = MutedLight, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        return
    }

    groups.forEach { (cat, rows) ->
        CategoryGroup(cat, rows, vm::toggleRow)
        Spacer(Modifier.height(12.dp))
    }

    if (showBought) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
            Text("Bought · ${state.bought.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedDark)
            Spacer(Modifier.weight(1f))
            Text("Uncheck all", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier.clickable(onClick = vm::uncheckAll).padding(4.dp))
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        ) {
            state.bought.forEach { ItemRow(it, vm::toggleRow) }
        }
    }
}

@Composable
private fun CategoryGroup(cat: GroceryCat, rows: List<GroceryRow>, onToggle: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)).padding(bottom = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(cat.colorArgb)))
            Spacer(Modifier.width(7.dp))
            Text(cat.label.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MutedDark, letterSpacing = 0.5.sp)
            Spacer(Modifier.width(6.dp))
            Text("${rows.size}", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
        }
        rows.forEach { ItemRow(it, onToggle) }
    }
}

/** One independently-checkable list row. */
@Composable
private fun ItemRow(row: GroceryRow, onToggle: (String) -> Unit) {
    val bought = row.checked
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(row.id) }.padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                .background(if (bought) Teal else Surface)
                .border(1.5.dp, if (bought) Teal else BorderMuted, RoundedCornerShape(6.dp)),
            Alignment.Center,
        ) {
            if (bought) Icon(Icons.Default.Check, null, tint = OnAccent, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(11.dp))
        Text(
            row.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (bought) MutedFaint else Ink,
            textDecoration = if (bought) TextDecoration.LineThrough else null,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${fmtQty(row.qty)} ${unitLabel(row.unit)}", fontFamily = DmMono, fontSize = 12.sp,
            color = if (bought) MutedFaint else MutedDark,
        )
    }
}

// ── Saved lists sheet ──────────────────────────────────────────────────────────
@Composable
private fun SavedSheet(state: GroceryUiState, vm: GroceryViewModel) {
    Box(Modifier.fillMaxSize().background(Scrim).clickable(onClick = vm::closeSheet), Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(AppBg)
                .clickable(enabled = false) {}.padding(16.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(BorderMuted))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Saved lists", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("${state.savedLists.size}", fontSize = 12.sp, color = MutedLight)
            }
            if (state.savedLists.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                    Text("No saved lists yet. Build one and tap Save.", fontSize = 13.sp, color = MutedLight)
                }
            } else {
                state.savedLists.forEach { SavedRow(it, it.id == state.activeId, vm) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SavedRow(list: SavedGroceryList, active: Boolean, vm: GroceryViewModel) {
    val done = list.checked.count { it.value }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(if (active) Teal.copy(alpha = 0.08f) else Surface)
            .border(1.dp, if (active) Teal else BorderMuted, RoundedCornerShape(12.dp))
            .clickable { vm.loadSaved(list.id) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(if (active) Teal else SurfaceMuted), Alignment.Center) {
            Icon(Icons.Default.BookmarkBorder, null, tint = if (active) OnAccent else Muted, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(list.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
            Text("${list.items.size} items · $done bought · ${list.days} day${if (list.days == 1) "" else "s"}",
                fontSize = 11.sp, color = MutedLight)
        }
        IconButton(onClick = { vm.deleteSaved(list.id) }) {
            Icon(Icons.Outlined.Delete, "Delete", tint = MutedFaint, modifier = Modifier.size(18.dp))
        }
    }
}

// ── helpers ──────────────────────────────────────────────────────────────────
private fun fmtQty(n: Double): String {
    val r = (n * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else "%.1f".format(r)
}

private fun rangeLabelOf(keys: List<String>): String {
    if (keys.isEmpty()) return "Tap to pick dates"
    fun lbl(k: String): String {
        val d = LocalDate.parse(k)
        return "${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}"
    }
    return if (keys.size == 1) lbl(keys.first()) else "${lbl(keys.first())} – ${lbl(keys.last())}"
}
