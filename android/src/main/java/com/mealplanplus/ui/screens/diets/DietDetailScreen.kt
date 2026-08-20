package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.DietsApi
import com.mealplanplus.data.generated.model.DietUsageDto
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.DietSlotUi
import com.mealplanplus.data.repository.DietUi
import com.mealplanplus.data.model.DietEntryKind
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DietDetailViewModel @Inject constructor(
    private val repository: DietRepository,
    private val dietsApi: DietsApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val dietId: String = savedStateHandle.get<String>("dietId") ?: ""

    data class State(val diet: DietUi? = null, val usage: DietUsageDto? = null, val loading: Boolean = true)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        // Nutrition + per-slot come from the local resolved diet.
        viewModelScope.launch {
            repository.getDiets().collect { list ->
                _state.update { it.copy(diet = list.firstOrNull { d -> d.diet.id == dietId }, loading = false) }
            }
        }
        // Usage is a server aggregate; resolve the local UUID to the numeric diet id via listDiets.
        viewModelScope.launch {
            runCatching {
                val numericId = dietsApi.listDiets(false).body().orEmpty()
                    .firstOrNull { it.serverId?.toString() == dietId }?.id
                if (numericId != null) {
                    dietsApi.getDietUsage(numericId).body()?.let { u -> _state.update { it.copy(usage = u) } }
                }
            }
        }
    }
}

private fun gram(v: Double?): String = v?.let { "${it.roundToInt()} g" } ?: "—"

@Composable
fun DietDetailScreen(onBack: () -> Unit = {}, viewModel: DietDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val diet = state.diet

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink) }
            Text(diet?.diet?.name ?: "Diet", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
        }

        if (diet == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(if (state.loading) "Loading…" else "Couldn't load this diet", fontSize = 13.sp, color = MutedLight)
            }
            return@Column
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (!diet.diet.description.isNullOrBlank()) {
                Text(diet.diet.description!!, fontSize = 12.5.sp, color = MutedFaint, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            }

            // ── Nutrition ────────────────────────────────────────────────
            Card("Nutrition · whole diet") {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 10.dp)) {
                    Text("${diet.totalKcal}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = DmMono)
                    Spacer(Modifier.width(6.dp))
                    Text("kcal total", fontSize = 12.sp, color = MutedFaint, modifier = Modifier.padding(bottom = 3.dp))
                }
                StatGrid(listOf(
                    "Protein" to gram(diet.totalProtein), "Carbs" to gram(diet.totalCarbs), "Fat" to gram(diet.totalFat),
                    "Fiber" to gram(diet.totalFiber), "Sugars" to gram(diet.totalSugars),
                    "Sat. fat" to gram(diet.totalSatFat), "Sodium" to gram(diet.totalSodium),
                ))
            }

            // ── Usage ────────────────────────────────────────────────────
            Card("Usage") {
                val u = state.usage
                if (u == null) {
                    Text("—", fontSize = 12.sp, color = MutedFaint)
                } else {
                    var win by remember { mutableStateOf(30) }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppBg).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(7, 30, 90).forEach { w ->
                            val on = win == w
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                                .background(if (on) Surface else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { win = w }.padding(vertical = 6.dp), Alignment.Center) {
                                Text("Last $w days", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (on) Teal else MutedFaint)
                            }
                        }
                    }
                    val count = when (win) { 7 -> u.last7Days; 30 -> u.last30Days; else -> u.last90Days }
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                        Text("$count", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = DmMono)
                        Spacer(Modifier.width(6.dp))
                        Text("day(s) assigned in the last $win days", fontSize = 12.sp, color = MutedFaint, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")
                    val range = if (u.timesAssigned > 0)
                        " · First ${u.firstUsedDate?.format(fmt) ?: "—"} · Last ${u.lastUsedDate?.format(fmt) ?: "—"}" else ""
                    Text("${u.timesAssigned} day${if (u.timesAssigned == 1) "" else "s"} all-time$range",
                        fontSize = 11.5.sp, color = MutedFaint, modifier = Modifier.padding(top = 6.dp))
                }
            }

            // ── Per slot (with ingredients) ──────────────────────────────
            Card("Per slot") {
                if (diet.slots.isEmpty()) {
                    Text("This diet has no meals yet.", fontSize = 12.sp, color = MutedFaint)
                } else {
                    diet.slots.forEach { SlotBlock(it) }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SlotBlock(slot: DietSlotUi) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 8.dp)) {
            Text(slot.slot.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Teal)
            Spacer(Modifier.width(4.dp))
            Text(if (open) "▾" else "▸", fontSize = 9.sp, color = MutedFaint)
            Spacer(Modifier.weight(1f))
            Text("${slot.kcal} kcal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = DmMono)
        }
        if (open) {
            slot.entries.forEach { e ->
                Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 2.dp)) {
                    Text(if (e.kind == DietEntryKind.MEAL) "🍲 ${e.name}" else "• ${e.name}",
                        fontSize = 12.sp, fontWeight = if (e.kind == DietEntryKind.MEAL) FontWeight.SemiBold else FontWeight.Normal,
                        color = Ink, modifier = Modifier.weight(1f))
                    Text(e.meta, fontSize = 10.sp, color = MutedFaint, fontFamily = DmMono)
                }
                e.mealFoods.forEach { mf ->
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, bottom = 1.dp)) {
                        Text("• ${mf.name}", fontSize = 11.sp, color = MutedFaint, modifier = Modifier.weight(1f))
                        Text(mf.meta, fontSize = 9.5.sp, color = MutedFaint, fontFamily = DmMono)
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp))
        .background(Surface).padding(16.dp)) {
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedFaint, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

@Composable
private fun StatGrid(stats: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stats.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(AppBg).padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(label.uppercase(), fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, fontFamily = DmMono, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
