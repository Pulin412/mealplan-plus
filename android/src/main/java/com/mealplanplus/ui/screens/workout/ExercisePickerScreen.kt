package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
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
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategoryEntity
import com.mealplanplus.data.model.displayName
import com.mealplanplus.ui.theme.*

/**
 * Full-screen exercise picker used when adding exercises to a workout template.
 * Calls [viewModel.selectExercise] and pops back; the template screen observes
 * [WorkoutUiState.pendingExercise] to consume the selection.
 */
@Composable
fun ExercisePickerScreen(
    excludeIds: Set<Long> = emptySet(),
    onBack: () -> Unit,
    onPicked: (Exercise) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var search   by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }

    val filtered = remember(state.exercises, search, category, excludeIds) {
        state.exercises.filter { ex ->
            ex.id !in excludeIds &&
            (category == null || ex.category == category) &&
            (search.isBlank() || ex.name.contains(search, ignoreCase = true))
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    Scaffold(containerColor = BgPage) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
                .padding(padding)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgPage)
                    .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add Exercise", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
                    Text("${filtered.size} available", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 1.dp))
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (search.isEmpty()) Text("Search exercises…", fontSize = 14.sp, color = Color(0xFFBBBBBB))
                    androidx.compose.foundation.text.BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Category filter chips ─────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    PickerChip(label = "All", selected = category == null, count = state.exercises.size - excludeIds.size) {
                        category = null
                    }
                }
                items(state.categories) { cat ->
                    PickerChip(
                        label = cat.displayName(),
                        selected = category == cat.name,
                        count = state.exercises.count { it.category == cat.name && it.id !in excludeIds }
                    ) { category = cat.name }
                }
            }

            // ── Exercise list ─────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔍", fontSize = 36.sp)
                        Text("No exercises found", fontSize = 15.sp, color = TextSecondary)
                    }
                }
            } else if (category != null || search.isNotBlank()) {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
                    items(filtered, key = { it.id }) { ex ->
                        PickerExerciseRow(ex) {
                            onPicked(ex)
                        }
                        HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 62.dp))
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    state.categories.forEach { cat ->
                        val catExercises = grouped[cat.name] ?: return@forEach
                        item(key = "hdr_${cat.name}") {
                            Text(
                                cat.displayName().uppercase(),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                            )
                        }
                        item(key = "grp_${cat.name}") {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                catExercises.forEachIndexed { idx, ex ->
                                    PickerExerciseRow(ex) { onPicked(ex) }
                                    if (idx < catExercises.lastIndex) {
                                        HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 62.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerExerciseRow(exercise: Exercise, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(categoryBg(exercise.category)),
            contentAlignment = Alignment.Center
        ) {
            Text(categoryEmoji(exercise.category), fontSize = 18.sp)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(exercise.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                listOfNotNull(exercise.muscleGroup, exercise.equipment).joinToString(" · ").ifEmpty { categoryDisplayName(exercise.category) },
                fontSize = 11.sp, color = TextSecondary
            )
        }

        if (!exercise.isSystem) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(DesignGreenLight).padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("Custom", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DesignGreen)
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TagGrayBg)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

@Composable
private fun PickerChip(label: String, selected: Boolean, count: Int, onClick: () -> Unit) {
    val bg     = if (selected) TextPrimary else CardBg
    val border = if (selected) Modifier else Modifier.border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    val text   = if (selected || count <= 0) label else "$label  $count"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(border)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (selected) CardBg else TextSecondary)
    }
}
