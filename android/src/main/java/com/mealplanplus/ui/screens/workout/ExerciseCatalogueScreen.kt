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
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.ui.theme.*

@Composable
fun ExerciseCatalogueScreen(
    onBack: () -> Unit,
    onAddExercise: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val exercises = viewModel.filteredExercises()
    val grouped = exercises.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPage)
                .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Exercises",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    "${state.exercises.size} exercises",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        // ── Search bar (design-future .search-bar pattern) ────────────────
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
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color(0xFFBBBBBB),
                modifier = Modifier.size(16.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (state.searchQuery.isEmpty()) {
                    Text("Search exercises…", fontSize = 14.sp, color = Color(0xFFBBBBBB))
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Category chips (design-future .chip pattern) ──────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                CategoryFilterChip(
                    label = "All",
                    selected = state.selectedCategory == null,
                    count = state.exercises.size,
                    onClick = { viewModel.filterByCategory(null) }
                )
            }
            items(ExerciseCategory.entries) { cat ->
                CategoryFilterChip(
                    label = cat.displayName(),
                    selected = state.selectedCategory == cat,
                    count = state.exercises.count { it.category == cat },
                    onClick = { viewModel.filterByCategory(cat) }
                )
            }
        }

        // ── Exercise list ─────────────────────────────────────────────────
        if (exercises.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔍", fontSize = 36.sp)
                    Text("No exercises found", fontSize = 15.sp, color = TextSecondary)
                }
            }
        } else if (state.selectedCategory != null || state.searchQuery.isNotBlank()) {
            // Flat list when filtered
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
                items(exercises, key = { it.id }) { ex ->
                    ExerciseListItem(ex)
                    HorizontalDivider(
                        color = Color(0xFFF5F5F5),
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 64.dp)
                    )
                }
                item { AddCustomExerciseButton(onClick = onAddExercise) }
            }
        } else {
            // Grouped by category when showing all
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                ExerciseCategory.entries.forEach { cat ->
                    val catExercises = grouped[cat] ?: return@forEach
                    item(key = "header_${cat.name}") {
                        Text(
                            cat.displayName().uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                        )
                    }
                    item(key = "card_${cat.name}") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            catExercises.forEachIndexed { idx, ex ->
                                ExerciseListItem(ex)
                                if (idx < catExercises.lastIndex) {
                                    HorizontalDivider(
                                        color = Color(0xFFF5F5F5),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(start = 64.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item { AddCustomExerciseButton(onClick = onAddExercise) }
            }
        }
    }
}

// ── "+ Add custom exercise" outline button (.form-btn-outline) ────────────────

@Composable
private fun AddCustomExerciseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "+ Add custom exercise",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

// ── Exercise list item (design-future .ex-item) ───────────────────────────────

@Composable
private fun ExerciseListItem(exercise: Exercise) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon (38×38, 11dp radius)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(categoryBg(exercise.category)),
            contentAlignment = Alignment.Center
        ) {
            Text(categoryEmoji(exercise.category), fontSize = 18.sp)
        }

        // Name + detail
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                exercise.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                listOfNotNull(exercise.muscleGroup, exercise.equipment)
                    .joinToString(" · ")
                    .ifEmpty { exercise.category.displayName() },
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Category badge — Strength: blue (.tag.tb), others: gray (.tag.tgr)
        val isStrength = exercise.category == ExerciseCategory.STRENGTH
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(if (isStrength) TagBlueBg else TagGrayBg)
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                exercise.category.displayName(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isStrength) TagBlue else TagGray
            )
        }
    }
}

// ── Filter chip (design-future .chip) ────────────────────────────────────────

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (!selected) Modifier.border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(16.dp))
                else Modifier
            )
            .background(if (selected) Color(0xFF111111) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            if (count > 0 && !selected) "$label  $count" else label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF555555)
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ExerciseCategory.displayName() = when (this) {
    ExerciseCategory.STRENGTH    -> "Strength"
    ExerciseCategory.CARDIO      -> "Cardio"
    ExerciseCategory.FLEXIBILITY -> "Flexibility"
    ExerciseCategory.OTHER       -> "Other"
}

private fun categoryEmoji(category: ExerciseCategory) = when (category) {
    ExerciseCategory.STRENGTH    -> "💪"
    ExerciseCategory.CARDIO      -> "🏃"
    ExerciseCategory.FLEXIBILITY -> "🧘"
    ExerciseCategory.OTHER       -> "🏋️"
}

@Composable
private fun categoryBg(category: ExerciseCategory) = when (category) {
    ExerciseCategory.STRENGTH    -> IconBgGray
    ExerciseCategory.CARDIO      -> TagBlueBg
    ExerciseCategory.FLEXIBILITY -> TagGreenBg
    ExerciseCategory.OTHER       -> TagGrayBg
}
