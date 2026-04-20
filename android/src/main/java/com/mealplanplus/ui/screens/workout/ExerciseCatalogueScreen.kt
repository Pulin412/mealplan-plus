package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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

@Composable
fun ExerciseCatalogueScreen(
    onBack: () -> Unit,
    onAddExercise: () -> Unit = {},
    onEditExercise: (Long) -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val exercises = remember(state.exercises, state.selectedCategory, state.searchQuery) {
        state.exercises.filter { ex ->
            (state.selectedCategory == null || ex.category == state.selectedCategory) &&
            (state.searchQuery.isBlank() || ex.name.contains(state.searchQuery, ignoreCase = true))
        }
    }
    val grouped = remember(exercises) { exercises.groupBy { it.category } }

    Scaffold(
        containerColor = BgPage,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExercise,
                containerColor = TextPrimary,
                contentColor = CardBg,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise", modifier = Modifier.size(22.dp))
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
                .padding(scaffoldPadding)
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

            // ── Category filter chips ─────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    ExCategoryChip(
                        label = "All",
                        selected = state.selectedCategory == null,
                        count = state.exercises.size,
                        onClick = { viewModel.filterByCategory(null) }
                    )
                }
                items(state.categories) { cat ->
                    ExCategoryChip(
                        label = cat.displayName(),
                        selected = state.selectedCategory == cat.name,
                        count = state.exercises.count { it.category == cat.name },
                        onClick = { viewModel.filterByCategory(cat.name) }
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
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp)) {
                    items(exercises, key = { it.id }) { ex ->
                        ExerciseListItem(ex, onClick = { onEditExercise(ex.id) })
                        HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 64.dp))
                    }
                }
            } else {
                // Grouped by category when showing all
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    state.categories.forEach { cat ->
                        val catExercises = grouped[cat.name] ?: return@forEach
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                catExercises.forEachIndexed { idx, ex ->
                                    ExerciseListItem(ex, onClick = { onEditExercise(ex.id) })
                                    if (idx < catExercises.lastIndex) {
                                        HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 64.dp))
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

// ── Exercise list item ─────────────────────────────────────────────────────────

@Composable
internal fun ExerciseListItem(exercise: Exercise, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                listOfNotNull(exercise.muscleGroup, exercise.equipment)
                    .joinToString(" · ")
                    .ifEmpty { categoryDisplayName(exercise.category) },
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        val isCustom = !exercise.isSystem
        if (isCustom) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DesignGreenLight)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("Custom", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DesignGreen)
            }
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Filter chip ────────────────────────────────────────────────────────────────

@Composable
private fun ExCategoryChip(label: String, selected: Boolean, count: Int, onClick: () -> Unit) {
    val bg     = if (selected) TextPrimary else CardBg
    val border = if (selected) Modifier else Modifier.border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    val text   = if (selected || count == 0) label else "$label  $count"
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

// ── Helpers ────────────────────────────────────────────────────────────────────

internal fun categoryEmoji(category: String?) = when (category?.uppercase()) {
    "STRENGTH"    -> "💪"
    "CARDIO"      -> "🏃"
    "FLEXIBILITY" -> "🧘"
    else          -> "🏋️"
}

@Composable
internal fun categoryBg(category: String?) = when (category?.uppercase()) {
    "STRENGTH"    -> IconBgGray
    "CARDIO"      -> TagBlueBg
    "FLEXIBILITY" -> TagGreenBg
    else          -> TagGrayBg
}
