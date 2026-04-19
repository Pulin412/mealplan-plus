package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.ui.theme.*

@Composable
fun ExerciseCatalogueScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val exercises = viewModel.filteredExercises()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TopBarGreen)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Exercise Catalogue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Search
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search exercises…") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        // Category filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip(
                    label = "All",
                    selected = state.selectedCategory == null,
                    onClick = { viewModel.filterByCategory(null) }
                )
            }
            items(ExerciseCategory.entries) { cat ->
                CategoryChip(
                    label = cat.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = state.selectedCategory == cat,
                    onClick = { viewModel.filterByCategory(cat) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (exercises.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No exercises found", color = TextSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                items(exercises, key = { it.id }) { ex ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(ex.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                CategoryBadge(ex.category.name.lowercase().replaceFirstChar { it.uppercase() })
                                ex.muscleGroup?.let { CategoryBadge(it) }
                                ex.equipment?.let { CategoryBadge(it) }
                            }
                            ex.description?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrandGreen,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun CategoryBadge(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        color = TextSecondary,
        modifier = Modifier
            .background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
