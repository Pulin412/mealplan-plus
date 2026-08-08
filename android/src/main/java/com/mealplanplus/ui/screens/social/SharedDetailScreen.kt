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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.CopyRequest
import com.mealplanplus.data.generated.model.SharedDietDetailDto
import com.mealplanplus.data.generated.model.SharedMealDetailDto
import com.mealplanplus.data.generated.model.SharedWorkoutDetailDto
import com.mealplanplus.data.repository.SocialRepository
import com.mealplanplus.data.sync.SyncManager
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

data class SharedDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val lines: List<String> = emptyList(),   // read-only rendered content rows
    val copying: Boolean = false,
    val copiedName: String? = null,           // non-null once saved to the user's library
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
        val foodsBySid = b.foods.orEmpty().associateBy { it.serverId }
        val mealsBySid = b.meals.orEmpty().associateBy { it.serverId }
        val lines = buildList {
            b.diet.targetCalories?.let { add("Target: ${it.toInt()} kcal") }
            b.diet.meals.orEmpty().forEach { dm ->
                val name = mealsBySid[dm.mealServerId]?.name ?: "Meal"
                add("${dm.slot}: $name")
            }
            b.diet.foodItems.orEmpty().forEach { fi ->
                val name = foodsBySid[fi.foodServerId]?.name ?: "Food"
                add("${fi.slot}: $name (${fi.quantity.toInt()} ${fi.unit.value.lowercase()})")
            }
        }
        _state.update { it.copy(loading = false, title = b.diet.name, lines = lines) }
    }

    private fun renderMeal(b: SharedMealDetailDto) {
        val foodsBySid = b.foods.orEmpty().associateBy { it.serverId }
        val lines = b.meal.items.orEmpty().map { item ->
            val name = foodsBySid[item.foodServerId]?.name ?: "Food"
            "$name — ${item.quantity.toInt()} ${item.unit.value.lowercase()}"
        }
        _state.update { it.copy(loading = false, title = b.meal.name, lines = lines) }
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
}

@Composable
fun SharedDetailScreen(
    onBack: () -> Unit,
    viewModel: SharedDetailViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("@${viewModel.handle}", fontSize = 15.sp, color = MutedLight)
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (s.lines.isEmpty()) Text("No items", color = MutedLight, fontSize = 13.sp)
                        s.lines.forEach { Text(it, color = Ink, fontSize = 14.sp) }
                    }
                    Spacer(Modifier.height(16.dp))
                    s.copiedName?.let {
                        Text("Saved “$it” to your library ✓", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
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
                            else -> Text("Use this — save to my library", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
