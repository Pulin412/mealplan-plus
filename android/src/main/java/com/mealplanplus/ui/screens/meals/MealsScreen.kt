package com.mealplanplus.ui.screens.meals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.ui.components.DietBrowserSection
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPlaceholder
import com.mealplanplus.ui.theme.TextDestructive
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(
    onNavigateToAddMeal: () -> Unit,
    onNavigateToMealDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MealsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPage,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(CardBg).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Meals", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${uiState.filteredMeals.size} meals", fontSize = 12.sp, color = TextSecondary)
                    }
                    IconButton(onClick = onNavigateToAddMeal, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Meal", tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }
                }
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("Search meals…", fontSize = 14.sp, color = TextPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextPlaceholder, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = TagGrayBg,
                        unfocusedContainerColor = TagGrayBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = DesignGreen
                    )
                )
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgPage)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { MealFilterChip("All", uiState.filterSlot == null && !uiState.showDietBrowser) { viewModel.setFilterSlot(null) } }
                DefaultMealSlot.entries
                    .filter { it in listOf(DefaultMealSlot.BREAKFAST, DefaultMealSlot.LUNCH, DefaultMealSlot.DINNER) }
                    .forEach { slot ->
                        item { MealFilterChip(slot.displayName, uiState.filterSlot == slot.name && !uiState.showDietBrowser) { viewModel.setFilterSlot(slot.name) } }
                    }
                item { MealFilterChip("From Diets", uiState.showDietBrowser) { viewModel.setShowDietBrowser(true) } }
            }
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            // Content based on mode
            if (uiState.showDietBrowser) {
                // Diet browser mode
                val filteredDiets = viewModel.getFilteredDiets()

                Text(
                    text = "${filteredDiets.size} diets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                DietBrowserSection(
                    diets = filteredDiets,
                    expandedDietId = uiState.expandedDietId,
                    onDietClick = viewModel::toggleExpandDiet,
                    onMealNavigate = onNavigateToMealDetail,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Meal list mode
                Text(
                    text = "${uiState.filteredMeals.size} meals",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredMeals.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty())
                                    "No meals match your search"
                                else
                                    "No meals yet.\nTap + to create one!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(
                            uiState.filteredMeals.distinctBy { it.meal.id },
                            key = { it.meal.id }
                        ) { mealWithFoods ->
                            EnhancedMealCard(
                                mealWithFoods = mealWithFoods,
                                onClick = { onNavigateToMealDetail(mealWithFoods.meal.id) },
                                onDelete = { viewModel.deleteMeal(mealWithFoods.meal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TextPrimary else CardBg)
            .border(1.dp, if (selected) TextPrimary else TagGrayBg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary)
    }
}

@Composable
fun EnhancedMealCard(
    mealWithFoods: MealWithFoods,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon well
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF0F8FF)),
                contentAlignment = Alignment.Center
            ) { Text("🍽️", fontSize = 18.sp) }
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(mealWithFoods.meal.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (mealWithFoods.items.isNotEmpty()) {
                    Text(
                        text = mealWithFoods.items.take(2).joinToString(", ") { it.food.name } +
                            if (mealWithFoods.items.size > 2) " +${mealWithFoods.items.size - 2}" else "",
                        fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Right: kcal + delete
            Column(horizontalAlignment = Alignment.End) {
                Text("${mealWithFoods.totalCalories.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("kcal", fontSize = 10.sp, color = TextMuted)
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Meal") },
            text = { Text("Delete ${mealWithFoods.meal.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = TextDestructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Keep old MealCard for backward compatibility if needed elsewhere
@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium
                )
                meal.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Meal") },
            text = { Text("Delete ${meal.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
