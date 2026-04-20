package com.mealplanplus.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.WorkoutTemplateCategory
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import com.mealplanplus.ui.theme.*
import com.mealplanplus.util.toEpochMs
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Full-screen day detail view.
 *
 * Uses [CalendarViewModel] with the date provided as `initialDate` nav argument,
 * mirroring the existing CalendarWithDate route so the ViewModel's savedStateHandle
 * wiring works out of the box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDietPicker: (String) -> Unit = {},
    onNavigateToLog: (String) -> Unit = {},
    onNavigateToStartWorkout: (Long) -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.workoutTemplates.collectAsState()
    val date = uiState.selectedDate

    // Observe diet selection result coming back from DietPickerScreen
    val pickedDietId by (savedStateHandle
        ?.getStateFlow("selected_diet_id", -1L)
        ?.collectAsState() ?: remember { mutableStateOf(-1L) })
    LaunchedEffect(pickedDietId) {
        if (pickedDietId != -1L) {
            viewModel.assignDietById(pickedDietId)
            savedStateHandle?.set("selected_diet_id", -1L)
        }
    }

    // Grocery snapshot sheet
    if (uiState.grocerySnapshot != null) {
        GrocerySnapshotSheet(
            dietName = uiState.selectedDiet?.name ?: "",
            items = uiState.grocerySnapshot!!,
            onDismiss = { viewModel.clearGrocerySnapshot() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        DayDetailTopBar(date = date, onBack = onNavigateBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                val isPlanCompleted = uiState.plans[date.toEpochMs()]?.isCompleted ?: false
                PlanDayDetail(
                    date = date,
                    diet = uiState.selectedDiet,
                    dietWithMeals = uiState.selectedDietWithMeals,
                    tags = uiState.selectedDietTags,
                    isPlanCompleted = isPlanCompleted,
                    todayLoggedSlots = uiState.todayLoggedSlots,
                    onAssignDiet = { onNavigateToDietPicker(date.toString()) },
                    onChangeDiet = { onNavigateToDietPicker(date.toString()) },
                    onRemoveDiet = { viewModel.clearPlan() },
                    onViewLog = { onNavigateToLog(date.toString()) },
                    onSlotToggle = { slotType -> viewModel.toggleSlotLogged(slotType) },
                    onToggleFavourite = { diet -> viewModel.toggleFavourite(diet) },
                    onShowGroceries = { viewModel.generateGroceriesForDiet() },
                    isGeneratingGroceries = uiState.isGeneratingGroceries
                )
            }

            // ── Planned Workouts Section ─────────────────────────────────────
            item {
                PlannedWorkoutsSection(
                    plannedWorkouts = uiState.plannedWorkouts,
                    onAddWorkout = { viewModel.showWorkoutPicker() },
                    onRemoveWorkout = { templateId -> viewModel.unplanWorkout(templateId) },
                    onStartWorkout = { templateId -> onNavigateToStartWorkout(templateId) }
                )
            }
        }
    }

    // Workout picker dialog
    if (uiState.showWorkoutPicker) {
        WorkoutPickerSheet(
            templates = templates,
            plannedTemplateIds = uiState.plannedWorkouts.map { it.plannedWorkout.templateId }.toSet(),
            onSelect = { templateId -> viewModel.planWorkout(templateId) },
            onDismiss = { viewModel.hideWorkoutPicker() }
        )
    }
}

// ── Planned workouts section ──────────────────────────────────────────────────

@Composable
private fun PlannedWorkoutsSection(
    plannedWorkouts: List<com.mealplanplus.data.model.PlannedWorkoutWithTemplate>,
    onAddWorkout: () -> Unit,
    onRemoveWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("WORKOUTS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, color = TextSecondary)
            Text(
                "+ Plan workout",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DesignGreen,
                modifier = Modifier.clickable(onClick = onAddWorkout)
            )
        }

        if (plannedWorkouts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No workouts planned", fontSize = 13.sp, color = TextMuted)
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                plannedWorkouts.forEachIndexed { idx, pw ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartWorkout(pw.plannedWorkout.templateId) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(IconBgGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pw.template.template.category.workoutEmoji(), fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pw.template.template.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                "${pw.template.exercises.size} exercises · ${pw.template.template.category.displayLabel()}",
                                fontSize = 11.sp, color = TextSecondary
                            )
                        }
                        IconButton(onClick = { onRemoveWorkout(pw.plannedWorkout.templateId) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (idx < plannedWorkouts.lastIndex) HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

// ── Workout picker bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutPickerSheet(
    templates: List<WorkoutTemplateWithExercises>,
    plannedTemplateIds: Set<Long>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Pick a Workout", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Select a template to add to this day", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

            if (templates.isEmpty()) {
                Text("No templates created yet.\nGo to Workouts → Templates to create one.", fontSize = 13.sp, color = TextMuted)
            } else {
                templates.forEach { t ->
                    val alreadyPlanned = t.template.id in plannedTemplateIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !alreadyPlanned) { onSelect(t.template.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(IconBgGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t.template.category.workoutEmoji(), fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.template.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = if (alreadyPlanned) TextMuted else TextPrimary)
                            Text("${t.exercises.size} exercises · ${t.template.category.displayLabel()}",
                                fontSize = 11.sp, color = TextSecondary)
                        }
                        if (alreadyPlanned) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(TagGrayBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Added", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

private fun WorkoutTemplateCategory.workoutEmoji() = when (this) {
    WorkoutTemplateCategory.STRENGTH    -> "💪"
    WorkoutTemplateCategory.CARDIO      -> "🏃"
    WorkoutTemplateCategory.FLEXIBILITY -> "🧘"
    WorkoutTemplateCategory.MIXED       -> "🏋️"
}

private fun WorkoutTemplateCategory.displayLabel() = when (this) {
    WorkoutTemplateCategory.STRENGTH    -> "Strength"
    WorkoutTemplateCategory.CARDIO      -> "Cardio"
    WorkoutTemplateCategory.FLEXIBILITY -> "Flexibility"
    WorkoutTemplateCategory.MIXED       -> "Mixed"
}

@Composable
private fun DayDetailTopBar(date: LocalDate, onBack: () -> Unit) {
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = "Day Detail",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = date.format(dateFmt),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
    HorizontalDivider(color = DividerColor, thickness = 1.dp)
}
