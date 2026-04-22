package com.mealplanplus.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.WorkoutSessionWithSets
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import com.mealplanplus.ui.screens.workout.categoryBg
import com.mealplanplus.ui.screens.workout.categoryDisplayName
import com.mealplanplus.ui.screens.workout.categoryEmoji
import com.mealplanplus.ui.screens.workout.TemplateCard
import com.mealplanplus.ui.screens.workout.workoutTemplateCategoryDisplayName
import com.mealplanplus.ui.screens.workout.workoutTemplateCategoryEmoji
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
    onNavigateToStartWorkout: (Long, LocalDate) -> Unit = { _, _ -> },
    onNavigateToSession: (Long) -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.workoutTemplates.collectAsState()
    val exercises by viewModel.allExercises.collectAsState()
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
                    loggedWorkouts = uiState.loggedWorkouts,
                    onAddWorkout = { viewModel.showWorkoutPicker() },
                    onRemoveWorkout = { templateId -> viewModel.unplanWorkout(templateId) },
                    onStartWorkout = { templateId -> onNavigateToStartWorkout(templateId, date) },
                    onViewSession = onNavigateToSession
                )
            }
        }
    }

    // Workout picker — full-screen overlay (stays in composition, no nav state loss)
    if (uiState.showWorkoutPicker) {
        PlanWorkoutOverlay(
            templates = templates,
            exercises = exercises,
            plannedTemplateIds = uiState.plannedWorkouts.map { it.plannedWorkout.templateId }.toSet(),
            onSelectTemplate = { templateId ->
                viewModel.planWorkout(templateId)
                viewModel.hideWorkoutPicker()
            },
            onSelectExercises = { selected -> viewModel.planQuickWorkout(selected) },
            onDismiss = { viewModel.hideWorkoutPicker() }
        )
    }
}

// ── Planned workouts section ──────────────────────────────────────────────────

@Composable
private fun PlannedWorkoutsSection(
    plannedWorkouts: List<com.mealplanplus.data.model.PlannedWorkoutWithTemplate>,
    loggedWorkouts: List<WorkoutSessionWithSets> = emptyList(),
    onAddWorkout: () -> Unit,
    onRemoveWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onViewSession: (Long) -> Unit = {}
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
                    // Match a logged session to this planned workout via templateId stored in session.notes
                    val loggedSession = loggedWorkouts.find {
                        it.session.notes == pw.plannedWorkout.templateId.toString()
                    }
                    val isLogged = loggedSession != null

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isLogged) onViewSession(loggedSession!!.session.id)
                                else onStartWorkout(pw.plannedWorkout.templateId)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (isLogged) Color(0xFFE8F5EE) else IconBgGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isLogged) "✅" else workoutTemplateCategoryEmoji(pw.template.template.category),
                                fontSize = 16.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pw.template.template.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            if (isLogged) {
                                val s = loggedSession!!.sets.size
                                val e = loggedSession.sets.map { it.exercise.id }.distinct().size
                                Text(
                                    "$s set${if (s != 1) "s" else ""} · $e exercise${if (e != 1) "s" else ""}",
                                    fontSize = 11.sp, color = DesignGreen
                                )
                            } else {
                                Text(
                                    "${pw.template.exercises.size} exercises · ${workoutTemplateCategoryDisplayName(pw.template.template.category)}",
                                    fontSize = 11.sp, color = TextSecondary
                                )
                            }
                        }
                        if (!isLogged) {
                            IconButton(onClick = { onRemoveWorkout(pw.plannedWorkout.templateId) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Text("›", fontSize = 18.sp, color = TextMuted)
                        }
                    }
                    if (idx < plannedWorkouts.lastIndex) HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

// ── Plan workout full-screen overlay ─────────────────────────────────────────

private enum class PlanTab { WORKOUTS, EXERCISES }

@Composable
private fun PlanWorkoutOverlay(
    templates: List<WorkoutTemplateWithExercises>,
    exercises: List<Exercise>,
    plannedTemplateIds: Set<Long>,
    onSelectTemplate: (Long) -> Unit,
    onSelectExercises: (List<Exercise>) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(PlanTab.WORKOUTS) }
    val selectedExercises = remember { mutableStateListOf<Exercise>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(26.dp))
            }
            Text("Plan Workout", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = DividerColor)

        // ── Tab toggle ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(PlanTab.WORKOUTS to "My Workouts", PlanTab.EXERCISES to "Pick Exercises").forEach { (t, label) ->
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (tab == t) BgPage else Color.Transparent)
                        .clickable { tab = t; selectedExercises.clear() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (tab == t) TextPrimary else TextSecondary)
                }
            }
        }

        when (tab) {
            PlanTab.WORKOUTS -> {
                if (templates.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💪", fontSize = 36.sp)
                            Text("No workouts yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Go to Workouts to create one.", fontSize = 13.sp, color = TextMuted)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates, key = { it.template.id }) { t ->
                            val alreadyPlanned = t.template.id in plannedTemplateIds
                            TemplateCard(
                                item = t,
                                onTap = {
                                    if (!alreadyPlanned) onSelectTemplate(t.template.id)
                                },
                                modifier = Modifier.alpha(if (alreadyPlanned) 0.5f else 1f)
                            )
                            if (alreadyPlanned) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                        .background(TagGrayBg)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Already planned for this day", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            PlanTab.EXERCISES -> {
                if (exercises.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No exercises found.\nImport exercises from Profile first.",
                            fontSize = 13.sp, color = TextMuted,
                            textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp)
                    ) {
                        items(exercises, key = { it.id }) { ex ->
                            val isSelected = ex in selectedExercises
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (isSelected) selectedExercises.remove(ex) else selectedExercises.add(ex) }
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(categoryBg(ex.category)),
                                    contentAlignment = Alignment.Center
                                ) { Text(categoryEmoji(ex.category), fontSize = 16.sp) }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(ex.muscleGroup ?: categoryDisplayName(ex.category), fontSize = 11.sp, color = TextSecondary)
                                }
                                Box(
                                    modifier = Modifier.size(22.dp).clip(CircleShape)
                                        .background(if (isSelected) DesignGreen else BgPage)
                                        .border(1.5.dp, if (isSelected) DesignGreen else DividerColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                }
                            }
                            HorizontalDivider(color = DividerColor)
                        }
                    }

                    if (selectedExercises.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().background(BgPage).padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Button(
                                onClick = { onSelectExercises(selectedExercises.toList()) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                            ) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add ${selectedExercises.size} exercise${if (selectedExercises.size > 1) "s" else ""} as workout",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CardBg)
                            }
                        }
                    }
                }
            }
        }
    }
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
