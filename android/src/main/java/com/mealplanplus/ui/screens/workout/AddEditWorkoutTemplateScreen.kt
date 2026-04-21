package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategoryEntity
import com.mealplanplus.data.model.WorkoutTemplateExercise
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import com.mealplanplus.data.model.displayName
import com.mealplanplus.ui.theme.*

@Suppress("UNUSED_VARIABLE")
@Composable
fun AddEditWorkoutTemplateScreen(
    existingTemplateId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onPickExercise: (excludeIds: List<Long>) -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Load existing template if editing
    var existing by remember { mutableStateOf<WorkoutTemplateWithExercises?>(null) }
    LaunchedEffect(existingTemplateId) {
        if (existingTemplateId != null) {
            existing = viewModel.getTemplateWithExercises(existingTemplateId)
        }
    }

    var name        by remember(existing) { mutableStateOf(existing?.template?.name ?: "") }
    var category    by remember(existing) { mutableStateOf(existing?.template?.category ?: "STRENGTH") }
    var notes       by remember(existing) { mutableStateOf(existing?.template?.notes ?: "") }

    val templateExercises = remember(existing) {
        mutableStateListOf<TemplateDraftExercise>().also { list ->
            existing?.exercises?.forEach { ex ->
                list.add(
                    TemplateDraftExercise(
                        exercise = ex.exercise,
                        targetSets = ex.templateExercise.targetSets?.toString() ?: "",
                        targetReps = ex.templateExercise.targetReps?.toString() ?: "",
                        targetWeightKg = ex.templateExercise.targetWeightKg?.let {
                            if (it % 1 == 0.0) it.toInt().toString() else "%.1f".format(it)
                        } ?: "",
                        notes = ex.templateExercise.notes ?: ""
                    )
                )
            }
        }
    }

    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val canSave = name.isNotBlank() && templateExercises.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxSize().background(BgPage)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPage)
                .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                if (existingTemplateId == null) "New Workout" else "Edit Workout",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // ── Details card ─────────────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Name
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormLabel("TEMPLATE NAME", required = true)
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("e.g. Chest Day, Morning Cardio", color = TextSecondary, fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = BgPage,
                                    focusedContainerColor = CardBg,
                                    unfocusedBorderColor = Color(0xFFEBEBEB),
                                    focusedBorderColor = TextPrimary
                                )
                            )
                        }

                        // Category
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormLabel("CATEGORY")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    items(state.categories) { cat ->
                                        val selected = cat.name == category
                                        CategoryChipItem(
                                            cat = cat,
                                            selected = selected,
                                            onSelect = { category = cat.name },
                                            onDelete = if (cat.isSystem) null else ({
                                                viewModel.deleteCategory(cat)
                                                if (category == cat.name) category = "STRENGTH"
                                            })
                                        )
                                    }
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(BgPage)
                                                .clickable { showAddCategory = true }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = "Add category", tint = DesignGreen, modifier = Modifier.size(14.dp))
                                                Text("New", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DesignGreen)
                                            }
                                        }
                                    }
                                }
                                if (showAddCategory) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newCategoryName,
                                            onValueChange = { newCategoryName = it },
                                            placeholder = { Text("Category name", color = TextSecondary, fontSize = 13.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Words,
                                                imeAction = ImeAction.Done
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = BgPage,
                                                focusedContainerColor = CardBg,
                                                unfocusedBorderColor = Color(0xFFEBEBEB),
                                                focusedBorderColor = TextPrimary
                                            )
                                        )
                                        TextButton(onClick = {
                                            if (newCategoryName.isNotBlank()) {
                                                viewModel.addCategory(newCategoryName)
                                                category = newCategoryName.trim().uppercase()
                                            }
                                            newCategoryName = ""
                                            showAddCategory = false
                                        }) {
                                            Text("Add", fontWeight = FontWeight.Bold, color = DesignGreen)
                                        }
                                        TextButton(onClick = { showAddCategory = false; newCategoryName = "" }) {
                                            Text("Cancel", color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        // Notes
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormLabel("NOTES (OPTIONAL)")
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("e.g. Monday push session", color = TextSecondary, fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = BgPage,
                                    focusedContainerColor = CardBg,
                                    unfocusedBorderColor = Color(0xFFEBEBEB),
                                    focusedBorderColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // ── Exercises section ────────────────────────────────────────────
            item {
                Text(
                    "EXERCISES",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                    letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            itemsIndexed(templateExercises, key = { idx, _ -> idx }) { idx, draft ->
                TemplateExerciseRow(
                    draft = draft,
                    isExpanded = expandedIndex == idx,
                    onToggleExpand = { expandedIndex = if (expandedIndex == idx) null else idx },
                    onUpdate = { templateExercises[idx] = it },
                    onRemove = {
                        templateExercises.removeAt(idx)
                        if (expandedIndex == idx) expandedIndex = null
                    }
                )
            }

            // Add exercise button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .clickable { showExercisePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TagGrayBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Text("Add exercise…", fontSize = 14.sp, color = TextSecondary)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // ── Bottom actions ────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().background(BgPage).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val exerciseEntities = templateExercises.mapIndexed { idx, draft ->
                        WorkoutTemplateExercise(
                            templateId = 0L,
                            exerciseId = draft.exercise.id,
                            orderIndex = idx,
                            targetSets = draft.targetSets.toIntOrNull(),
                            targetReps = draft.targetReps.toIntOrNull(),
                            targetWeightKg = draft.targetWeightKg.toDoubleOrNull(),
                            notes = draft.notes.ifBlank { null }
                        )
                    }
                    viewModel.saveTemplate(
                        existingId = existingTemplateId,
                        name = name,
                        category = category,
                        notes = notes,
                        exercises = exerciseEntities,
                        onDone = { onSaved() }
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
            ) {
                    Text("Save Workout", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CardBg)
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerSheet(
            exercises = state.exercises,
            onSelect = { ex ->
                templateExercises.add(TemplateDraftExercise(exercise = ex))
                expandedIndex = templateExercises.lastIndex
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

// ── Template exercise row (expandable) ───────────────────────────────────────

@Composable
private fun TemplateExerciseRow(
    draft: TemplateDraftExercise,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (TemplateDraftExercise) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Header row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IconBgGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(categoryEmoji(draft.exercise.category), fontSize = 16.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(draft.exercise.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    val detail = buildString {
                        if (draft.targetSets.isNotBlank()) append("${draft.targetSets} sets")
                        if (draft.targetReps.isNotBlank()) append(" · ${draft.targetReps} reps")
                        if (draft.targetWeightKg.isNotBlank()) append(" · ${draft.targetWeightKg} kg")
                        if (isEmpty()) append("Tap to set targets")
                    }
                    Text(detail, fontSize = 11.sp, color = TextSecondary)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }

            // ── Expanded target inputs ─────────────────────────────────────
            if (isExpanded) {
                HorizontalDivider(color = DividerColor)
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TargetField(
                            label = "SETS",
                            value = draft.targetSets,
                            onValueChange = { onUpdate(draft.copy(targetSets = it)) },
                            modifier = Modifier.weight(1f)
                        )
                        TargetField(
                            label = "REPS",
                            value = draft.targetReps,
                            onValueChange = { onUpdate(draft.copy(targetReps = it)) },
                            modifier = Modifier.weight(1f)
                        )
                        TargetField(
                            label = "WEIGHT (kg)",
                            value = draft.targetWeightKg,
                            onValueChange = { onUpdate(draft.copy(targetWeightKg = it)) },
                            modifier = Modifier.weight(1.5f),
                            decimal = true
                        )
                    }
                    // Per-exercise notes
                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = { onUpdate(draft.copy(notes = it)) },
                        placeholder = { Text("Notes for this exercise (optional)", color = TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = BgPage,
                            focusedContainerColor = CardBg,
                            unfocusedBorderColor = Color(0xFFEBEBEB),
                            focusedBorderColor = TextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = BgPage,
                focusedContainerColor = CardBg,
                unfocusedBorderColor = Color(0xFFEBEBEB),
                focusedBorderColor = TextPrimary
            )
        )
    }
}

// ── Exercise picker (reusable) ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExercisePickerSheet(
    exercises: List<Exercise>,
    excludeIds: Set<Long> = emptySet(),
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter {
        it.id !in excludeIds && (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgPage)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search exercises…", fontSize = 14.sp, color = TextSecondary)
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp)) {
            items(filtered, key = { it.id }) { ex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(ex) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(IconBgGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(categoryEmoji(ex.category), fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ex.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            listOfNotNull(categoryDisplayName(ex.category), ex.muscleGroup)
                                .joinToString(" · "),
                            fontSize = 11.sp, color = TextSecondary
                        )
                    }
                    if (!ex.isSystem) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DesignGreenLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Custom", fontSize = 10.sp, color = DesignGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

data class TemplateDraftExercise(
    val exercise: Exercise,
    val targetSets: String = "",
    val targetReps: String = "",
    val targetWeightKg: String = "",
    val notes: String = ""
)

@Composable
private fun FormLabel(text: String, required: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.6.sp)
        if (required) Text("*", fontSize = 10.sp, color = DesignGreen, fontWeight = FontWeight.Bold)
    }
}

