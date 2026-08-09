package com.mealplanplus.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mealplanplus.data.generated.model.CopyRequest
import com.mealplanplus.data.generated.model.ReportRequest
import com.mealplanplus.data.generated.model.SharedDietDetailDto
import com.mealplanplus.data.generated.model.SharedMealDetailDto
import com.mealplanplus.data.generated.model.SharedWorkoutDetailDto
import com.mealplanplus.data.repository.DietUi
import com.mealplanplus.data.repository.MealUi
import com.mealplanplus.data.repository.SocialRepository
import com.mealplanplus.data.repository.resolve
import com.mealplanplus.data.repository.toEntity
import com.mealplanplus.data.sync.SyncManager
import com.mealplanplus.ui.screens.diets.SlotGroup
import com.mealplanplus.ui.screens.meals.ExpandedItemRow
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

data class SharedDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    // The shared item is rendered with the SAME view models the owned Diets/Meals screens use, so
    // the layout matches. Exactly one of dietUi/mealUi/lines is populated per type.
    val dietUi: DietUi? = null,
    val mealUi: MealUi? = null,
    val lines: List<String> = emptyList(),   // workout rows (name — N sets)
    val copying: Boolean = false,
    val copiedName: String? = null,           // non-null once saved to the user's library
    val reportBusy: Boolean = false,
    val reportSent: Boolean = false,
)

@HiltViewModel
class SharedDetailViewModel @Inject constructor(
    private val social: SocialRepository,
    private val syncManager: SyncManager,
    savedState: SavedStateHandle,
) : ViewModel() {
    val handle: String = savedState.get<String>("handle").orEmpty()
    val type: String = savedState.get<String>("type").orEmpty()
    private val serverId: UUID = UUID.fromString(savedState.get<String>("serverId").orEmpty())
    /** True when viewing one of your own shared items — copying your own into your library is
     *  meaningless, so the copy action is hidden. */
    val own: Boolean = savedState.get<Boolean>("own") ?: false

    private val _state = MutableStateFlow(SharedDetailUiState())
    val state: StateFlow<SharedDetailUiState> = _state

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (type) {
                "DIET" -> social.sharedDiet(handle, serverId).fold(::renderDiet, ::fail)
                "MEAL" -> social.sharedMeal(handle, serverId).fold(::renderMeal, ::fail)
                "WORKOUT_TEMPLATE" -> social.sharedWorkout(handle, serverId).fold(::renderWorkout, ::fail)
                else -> fail(IllegalStateException("Unknown type"))
            }
        }
    }

    private fun fail(e: Throwable) = _state.update { it.copy(loading = false, error = e.message ?: "Unavailable") }

    private fun renderDiet(b: SharedDietDetailDto) {
        // Convert the shared bundle into the same domain models the owned Diets screen uses, then
        // resolve() to the identical DietUi (slots → meals with expandable ingredients + food rows).
        val foodsById = b.foods.orEmpty().map { it.toEntity() }.associateBy { it.id }
        val mealsById = b.meals.orEmpty().map { it.toEntity() }.associateBy { it.id }
            .mapValues { (_, meal) -> meal.resolve(foodsById) }
        val dietUi = b.diet.toEntity().resolve(mealsById, foodsById)
        _state.update { it.copy(loading = false, title = b.diet.name, dietUi = dietUi) }
    }

    private fun renderMeal(b: SharedMealDetailDto) {
        val foodsById = b.foods.orEmpty().map { it.toEntity() }.associateBy { it.id }
        val mealUi = b.meal.toEntity().resolve(foodsById)
        _state.update { it.copy(loading = false, title = b.meal.name, mealUi = mealUi) }
    }

    private fun renderWorkout(b: SharedWorkoutDetailDto) {
        val lines = b.workout.exercises.orEmpty().map { te ->
            "${te.exerciseName ?: "Exercise"} — ${te.sets.orEmpty().size} sets"
        }
        _state.update { it.copy(loading = false, title = b.workout.name, lines = lines) }
    }

    fun useThis() {
        viewModelScope.launch {
            _state.update { it.copy(copying = true, error = null) }
            social.copy(CopyRequest(CopyRequest.EntityType.valueOf(type), handle, serverId))
                .onSuccess { res ->
                    // Pull the newly-created owned rows into the local store.
                    runCatching { syncManager.sync() }
                    _state.update { it.copy(copying = false, copiedName = res.name) }
                }
                .onFailure { e -> _state.update { it.copy(copying = false, error = e.message ?: "Copy failed") } }
        }
    }

    fun report(reason: String, detail: String?) {
        viewModelScope.launch {
            _state.update { it.copy(reportBusy = true) }
            social.report(
                ReportRequest(
                    entityType = ReportRequest.EntityType.valueOf(type),
                    entityServerId = serverId,
                    reportedHandle = handle,
                    reason = reason,
                    detail = detail,
                ),
            )
            _state.update { it.copy(reportBusy = false, reportSent = true) }
        }
    }

    fun clearReportSent() = _state.update { it.copy(reportSent = false) }
}

@Composable
private fun TotalsRow(kcal: Int, protein: Double, carbs: Double, fat: Double) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "P ${protein.roundToInt()}g · C ${carbs.roundToInt()}g · F ${fat.roundToInt()}g",
                color = MutedLight, fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Text("$kcal kcal", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SharedDetailScreen(
    onBack: () -> Unit,
    viewModel: SharedDetailViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()
    var reportOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("@${viewModel.handle}", fontSize = 15.sp, color = MutedLight)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { reportOpen = true }) { Text("Report", color = MutedLight, fontSize = 13.sp) }
        }
        if (reportOpen) {
            ReportDialog(
                subject = "this item",
                busy = s.reportBusy,
                onDismiss = { reportOpen = false },
                onSubmit = { reason, detail -> viewModel.report(reason, detail) },
            )
        }
        if (s.reportSent) {
            AlertDialog(
                onDismissRequest = { viewModel.clearReportSent(); reportOpen = false },
                title = { Text("Report received") },
                text = { Text("Thanks — our team will review this item.") },
                confirmButton = { TextButton(onClick = { viewModel.clearReportSent(); reportOpen = false }) { Text("Done") } },
            )
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Teal) }
            s.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.error!!, color = MutedLight) }
            else -> Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    Text(s.title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val d = s.dietUi
                        val m = s.mealUi
                        when {
                            d != null -> {
                                if (d.slots.isEmpty()) Text("No items", color = MutedLight, fontSize = 13.sp)
                                d.slots.forEach { SlotGroup(it) }
                                TotalsRow(d.totalKcal, d.totalProtein, d.totalCarbs, d.totalFat)
                            }
                            m != null -> {
                                if (m.items.isEmpty()) Text("No items", color = MutedLight, fontSize = 13.sp)
                                m.items.forEach { ExpandedItemRow(it) }
                                TotalsRow(m.totalKcal, m.totalProtein, m.totalCarbs, m.totalFat)
                            }
                            else -> {
                                if (s.lines.isEmpty()) Text("No items", color = MutedLight, fontSize = 13.sp)
                                s.lines.forEach { Text(it, color = Ink, fontSize = 14.sp) }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    s.copiedName?.let {
                        Text("Saved “$it” to your library ✓", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                // Copy is only for other people's shared items — hidden when viewing your own.
                if (!viewModel.own) {
                    Box(Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = viewModel::useThis,
                            enabled = !s.copying && s.copiedName == null,
                            colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = OnAccent),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) {
                            when {
                                s.copying -> CircularProgressIndicator(color = OnAccent, modifier = Modifier.height(22.dp))
                                s.copiedName != null -> Text("Saved ✓", fontWeight = FontWeight.Bold)
                                else -> Text("Copy to personal library", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
