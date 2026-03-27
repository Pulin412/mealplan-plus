package com.mealplanplus.ui.screens.diets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.MealFoodItemWithDetails
import com.mealplanplus.data.model.Tag
import androidx.compose.ui.text.style.TextAlign

private val FormGreen = Color(0xFF2E7D52)

// ─── Top Bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietFormTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = FormGreen
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.padding(end = 8.dp).height(36.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FormGreen)
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = FormGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

// ─── Diet Info Card ──────────────────────────────────────────────────────────

@Composable
fun DietInfoCard(
    name: String,
    description: String,
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    newTagName: String = "",
    onNewTagNameChange: ((String) -> Unit)? = null,
    onCreateTag: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Diet Info", style = MaterialTheme.typography.titleSmall, color = FormGreen, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name *") },
                placeholder = { Text("e.g. Low Carb Day") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Tag chips
            Column {
                Text("Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                if (allTags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allTags) { tag ->
                            FilterChip(
                                selected = tag.id in selectedTagIds,
                                onClick = { onTagToggle(tag.id) },
                                label = { Text(tag.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FormGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                // Add new tag inline (optional)
                if (onNewTagNameChange != null && onCreateTag != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = onNewTagNameChange,
                            label = { Text("New Tag") },
                            placeholder = { Text("e.g. Keto") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = onCreateTag, enabled = newTagName.isNotBlank()) {
                            Icon(Icons.Default.Add, contentDescription = "Add tag", tint = if (newTagName.isNotBlank()) FormGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ─── Estimated Totals Card ───────────────────────────────────────────────────

@Composable
fun EstimatedTotalsCard(
    calories: Int,
    protein: Int,
    carbs: Int,
    fat: Int
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Estimated Totals", style = MaterialTheme.typography.titleSmall, color = FormGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroPill(icon = "🔥", value = "$calories", unit = "kcal", color = Color(0xFFE65100))
                MacroPill(icon = "🍞", value = "${carbs}g", unit = "carbs", color = Color(0xFF1565C0))
                MacroPill(icon = "💪", value = "${protein}g", unit = "protein", color = Color(0xFF2E7D32))
                MacroPill(icon = "🔥", value = "${fat}g", unit = "fat", color = Color(0xFF6A1B9A))
            }
        }
    }
}

// ─── Slot Section ─────────────────────────────────────────────────────────────

@Composable
fun DietSlotSection(
    slot: DefaultMealSlot,
    foods: List<MealFoodItemWithDetails>,
    onAddFood: () -> Unit,
    onRemoveFood: (Int) -> Unit,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    isEditing: Boolean = false,
    instructions: String = "",
    onInstructionsChange: ((String) -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val totalKcal = foods.sumOf { it.calculatedCalories }.toInt()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Slot header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = slotEmoji(slot),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = slot.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (foods.isEmpty()) "0 foods" else "${foods.size} foods · ${totalKcal} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // View mode: chevron to open detail screen (only if there are foods)
                    if (!isEditing && foods.isNotEmpty() && onViewDetails != null) {
                        IconButton(
                            onClick = onViewDetails,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "View details",
                                tint = FormGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Food rows
            if (foods.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                foods.forEachIndexed { index, item ->
                    DietFoodItemRow(
                        item = item,
                        onRemove = { onRemoveFood(index) },
                        onIncrement = { onIncrement(index) },
                        onDecrement = { onDecrement(index) }
                    )
                    if (index < foods.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFF5F5F5)
                        )
                    }
                }
            }

            // Instructions field (edit mode) or display (view mode)
            if (isEditing && onInstructionsChange != null) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = onInstructionsChange,
                    label = { Text("Preparation instructions (optional)") },
                    placeholder = { Text("How to prepare this meal…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    minLines = 2,
                    maxLines = 5
                )
            } else if (!isEditing && instructions.isNotBlank()) {
                // View mode: show instructions inline (collapsed summary)
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = FormGreen,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Add Food + Scan buttons (edit mode only)
            if (isEditing) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onAddFood,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = FormGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Food", color = FormGreen)
                    }
                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📷 Scan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ─── Food Item Row ────────────────────────────────────────────────────────────

@Composable
fun DietFoodItemRow(
    item: MealFoodItemWithDetails,
    onRemove: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Food icon placeholder
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = FormGreen.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🍽", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Name + brand
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.food.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.food.brand?.let { brand ->
                Text(
                    text = brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Qty + unit
        Text(
            text = "${item.mealFoodItem.quantity.toInt()} ${item.mealFoodItem.unit.shortLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        // Stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SmallStepButton("-") { onDecrement() }
            Text(
                text = "${item.mealFoodItem.quantity.toInt()}×",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            SmallStepButton("+") { onIncrement() }
        }

        // Kcal
        Text(
            text = "${item.calculatedCalories.toInt()}kcal",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE65100),
            fontSize = 11.sp
        )

        // Delete
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SmallStepButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier
            .size(24.dp)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

fun slotEmoji(slot: DefaultMealSlot): String = when (slot) {
    DefaultMealSlot.EARLY_MORNING -> "🌅"
    DefaultMealSlot.BREAKFAST -> "🍳"
    DefaultMealSlot.MID_MORNING -> "🥪"
    DefaultMealSlot.NOON -> "🌞"
    DefaultMealSlot.LUNCH -> "🍱"
    DefaultMealSlot.PRE_WORKOUT -> "⚡"
    DefaultMealSlot.EVENING -> "🌆"
    DefaultMealSlot.EVENING_SNACK -> "🍎"
    DefaultMealSlot.POST_WORKOUT -> "💪"
    DefaultMealSlot.DINNER -> "🍽"
    DefaultMealSlot.POST_DINNER -> "🌙"
}

// ─── Custom Diet Slot Section ─────────────────────────────────────────────────

@Composable
fun CustomDietSlotSection(
    slotType: String,
    displayName: String,
    foods: List<MealFoodItemWithDetails>,
    isEditing: Boolean,
    instructions: String = "",
    onAddFood: () -> Unit,
    onRemoveFood: (Int) -> Unit,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onInstructionsChange: ((String) -> Unit)? = null,
    onRemoveSlot: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val totalKcal = foods.sumOf { it.calculatedCalories }.toInt()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⭐", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FormGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = FormGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (foods.isEmpty()) "0 foods" else "${foods.size} foods · ${totalKcal} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isEditing && foods.isNotEmpty() && onViewDetails != null) {
                        IconButton(onClick = onViewDetails, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "View details", tint = FormGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (isEditing && onRemoveSlot != null) {
                        IconButton(onClick = onRemoveSlot, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove slot", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (foods.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                foods.forEachIndexed { index, item ->
                    DietFoodItemRow(
                        item = item,
                        onRemove = { onRemoveFood(index) },
                        onIncrement = { onIncrement(index) },
                        onDecrement = { onDecrement(index) }
                    )
                    if (index < foods.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF5F5F5))
                    }
                }
            }

            if (isEditing && onInstructionsChange != null) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = onInstructionsChange,
                    label = { Text("Preparation instructions (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    minLines = 2,
                    maxLines = 5
                )
            } else if (!isEditing && instructions.isNotBlank()) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = FormGreen, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Text(
                        text = instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isEditing) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onAddFood, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = FormGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Food", color = FormGreen)
                    }
                    TextButton(
                        onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📷 Scan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Slots to always show + those with content */
fun slotsToShow(slotFoodItems: Map<DefaultMealSlot, List<MealFoodItemWithDetails>>): List<DefaultMealSlot> {
    val alwaysShow = setOf(
        DefaultMealSlot.BREAKFAST,
        DefaultMealSlot.LUNCH,
        DefaultMealSlot.DINNER,
        DefaultMealSlot.EVENING_SNACK
    )
    return DefaultMealSlot.entries
        .filter { it in alwaysShow || (slotFoodItems[it]?.isNotEmpty() == true) }
        .sortedBy { it.order }
}
