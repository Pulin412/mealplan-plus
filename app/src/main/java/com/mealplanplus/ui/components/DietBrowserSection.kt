package com.mealplanplus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.MealWithFoods

/**
 * Reusable component for browsing diets and their meals.
 * Works in two modes:
 * - Browse mode (onMealSelected = null): Shows meals, tapping navigates to edit
 * - Picker mode (onMealSelected != null): Shows meals, tapping selects the meal
 */
@Composable
fun DietBrowserSection(
    diets: List<DietWithMeals>,
    expandedDietId: Long?,
    onDietClick: (Long) -> Unit,
    onMealSelected: ((Long) -> Unit)? = null,
    onMealNavigate: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (diets.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
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
                    text = "No diets available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(diets.distinctBy { it.diet.id }, key = { it.diet.id }) { dietWithMeals ->
                DietBrowserCard(
                    dietWithMeals = dietWithMeals,
                    isExpanded = expandedDietId == dietWithMeals.diet.id,
                    onClick = { onDietClick(dietWithMeals.diet.id) },
                    onMealSelected = onMealSelected,
                    onMealNavigate = onMealNavigate
                )
            }
        }
    }
}

@Composable
fun DietBrowserCard(
    dietWithMeals: DietWithMeals,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onMealSelected: ((Long) -> Unit)?,
    onMealNavigate: ((Long) -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dietWithMeals.diet.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        // Tags
                        dietWithMeals.diet.getTagList().filter { it != DietTag.CUSTOM }.take(2).forEach { tag ->
                            Surface(
                                color = when (tag) {
                                    DietTag.REMISSION -> MaterialTheme.colorScheme.primaryContainer
                                    DietTag.MAINTENANCE -> MaterialTheme.colorScheme.secondaryContainer
                                    DietTag.SOS -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = tag.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = when (tag) {
                                        DietTag.REMISSION -> MaterialTheme.colorScheme.onPrimaryContainer
                                        DietTag.MAINTENANCE -> MaterialTheme.colorScheme.onSecondaryContainer
                                        DietTag.SOS -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    if (!isExpanded) {
                        Text(
                            text = "${dietWithMeals.totalCalories.toInt()} cal • ${dietWithMeals.meals.count { it.value != null }} meals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded content - meal slots
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Show meals by slot order
                    DefaultMealSlot.entries.sortedBy { it.order }.forEach { slot ->
                        val mealWithFoods = dietWithMeals.meals[slot.name]
                        if (mealWithFoods != null) {
                            DietMealRow(
                                slotName = slot.displayName,
                                mealWithFoods = mealWithFoods,
                                onClick = {
                                    if (onMealSelected != null) {
                                        onMealSelected(mealWithFoods.meal.id)
                                    } else if (onMealNavigate != null) {
                                        onMealNavigate(mealWithFoods.meal.id)
                                    }
                                },
                                isClickable = onMealSelected != null || onMealNavigate != null
                            )
                        }
                    }

                    // Macro summary
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MacroChip("Cal", "${dietWithMeals.totalCalories.toInt()}")
                        MacroChip("P", "${dietWithMeals.totalProtein.toInt()}g")
                        MacroChip("C", "${dietWithMeals.totalCarbs.toInt()}g")
                        MacroChip("F", "${dietWithMeals.totalFat.toInt()}g")
                    }
                }
            }
        }
    }
}

@Composable
fun DietMealRow(
    slotName: String,
    mealWithFoods: MealWithFoods,
    onClick: () -> Unit,
    isClickable: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slotName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = mealWithFoods.meal.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${mealWithFoods.totalCalories.toInt()} cal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isClickable) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MacroChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
