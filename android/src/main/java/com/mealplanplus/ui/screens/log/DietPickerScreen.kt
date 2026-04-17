package com.mealplanplus.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.mealplanplus.ui.screens.diets.DietDisplayItem
import com.mealplanplus.ui.screens.diets.DietsViewModel
import com.mealplanplus.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPickerScreen(
    date: String,
    onNavigateBack: () -> Unit,
    onDietSelected: (Long, String) -> Unit,
    onNavigateHome: () -> Unit = {},
    viewModel: DietsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf<DietDisplayItem?>(null) }
    var searchActive by remember { mutableStateOf(false) }

    val parsedDate = try { LocalDate.parse(date) } catch (e: Exception) { LocalDate.now() }
    val isFutureDate = parsedDate.isAfter(LocalDate.now())
    val actionText = if (isFutureDate) "Plan for" else "Log for"
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM")

    Scaffold(
        containerColor = BgPage,
        topBar = {
            Column {
                // Minimal top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg)
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Diet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "$actionText ${parsedDate.format(dateFmt)}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(
                            if (searchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }
                }

                // Collapsible search bar
                AnimatedVisibility(
                    visible = searchActive,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBg)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search diets...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignGreen,
                            unfocusedBorderColor = DividerColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Tag filter row
                if (uiState.allTags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBg)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            DietFilterPill(
                                label = "All",
                                selected = uiState.selectedTagIds.isEmpty() && !uiState.showFavouritesOnly,
                                onClick = viewModel::clearTagFilters
                            )
                        }
                        item {
                            DietFilterPill(
                                label = "★ Faves",
                                selected = uiState.showFavouritesOnly,
                                onClick = viewModel::toggleFavouritesFilter,
                                selectedColor = Color(0xFFFFC107)
                            )
                        }
                        items(uiState.allTags) { tag ->
                            val count = uiState.tagCountMap[tag.id] ?: 0
                            DietFilterPill(
                                label = "${tag.name} $count",
                                selected = tag.id in uiState.selectedTagIds,
                                onClick = { viewModel.toggleTagFilter(tag.id) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DividerColor)

                // Count line
                Text(
                    text = when {
                        uiState.searchQuery.isNotEmpty() -> "${uiState.diets.size} results for \"${uiState.searchQuery}\""
                        uiState.showFavouritesOnly -> "${uiState.diets.size} favourite diets"
                        uiState.selectedTagIds.isNotEmpty() -> "${uiState.diets.size} matching diets"
                        else -> "${uiState.diets.size} of ${uiState.totalDietCount} diets"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier
                        .background(BgPage)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = DesignGreen) }
            }

            uiState.diets.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextMuted
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when {
                                uiState.showFavouritesOnly -> "No favourite diets yet"
                                uiState.searchQuery.isNotBlank() -> "No diets match \"${uiState.searchQuery}\""
                                else -> "No diets available"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(uiState.diets.distinctBy { it.diet.id }, key = { it.diet.id }) { item ->
                        DietPickerRow(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    // Confirmation bottom sheet
    selectedItem?.let { item ->
        DietConfirmSheet(
            item = item,
            actionText = actionText,
            date = parsedDate.format(dateFmt),
            onConfirm = {
                onDietSelected(item.diet.id, date)
                selectedItem = null
            },
            onDismiss = { selectedItem = null }
        )
    }
}

// ── Compact picker row ────────────────────────────────────────────────────────

@Composable
private fun DietPickerRow(
    item: DietDisplayItem,
    onClick: () -> Unit
) {
    val totalMacroG = (item.totalProtein + item.totalCarbs + item.totalFat).coerceAtLeast(1)
    val proteinFrac = item.totalProtein.toFloat() / totalMacroG
    val carbsFrac   = item.totalCarbs.toFloat()   / totalMacroG
    // fat fills the remainder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: slot coverage dots
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SlotDot(filled = DefaultMealSlot.BREAKFAST.name in item.assignedSlots, label = "B")
            SlotDot(filled = DefaultMealSlot.LUNCH.name    in item.assignedSlots, label = "L")
            SlotDot(filled = DefaultMealSlot.DINNER.name   in item.assignedSlots, label = "D")
        }

        // Middle: name + tags + macro bar
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.diet.isFavourite) {
                    Text("★", fontSize = 11.sp, color = Color(0xFFFFC107))
                }
                Text(
                    text = item.diet.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.diet.isSystem) {
                    Text(
                        text = "built-in",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Tags + meal count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.tags.take(2).forEach { tag ->
                    val tagColor = try {
                        Color(android.graphics.Color.parseColor(tag.color))
                    } catch (e: Exception) {
                        TextMuted
                    }
                    Text(
                        text = tag.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = tagColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tagColor.copy(alpha = 0.10f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (item.tags.size > 2) {
                    Text("+${item.tags.size - 2}", fontSize = 10.sp, color = TextMuted)
                }
                Text(
                    text = "· ${item.mealCount} meals",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(Modifier.height(8.dp))

            // Macro ratio bar — protein | carbs | fat
            if (item.totalCalories > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(proteinFrac.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(ChartProtein)
                    )
                    Box(
                        modifier = Modifier
                            .weight(carbsFrac.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(ChartCarbs)
                    )
                    Box(
                        modifier = Modifier
                            .weight((1f - proteinFrac - carbsFrac).coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(ChartFat)
                    )
                }
            }
        }

        // Right: calorie + arrow
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.totalCalories}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DesignGreen
            )
            Text(
                text = "kcal",
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = "Select",
            tint = DividerColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Slot coverage dot ─────────────────────────────────────────────────────────

@Composable
private fun SlotDot(filled: Boolean, label: String) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (filled) DesignGreen else DividerColor)
    )
}

// ── Filter pill ───────────────────────────────────────────────────────────────

@Composable
private fun DietFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = DesignGreen
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) selectedColor else TagGrayBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

// ── Confirmation bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietConfirmSheet(
    item: DietDisplayItem,
    actionText: String,
    date: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Diet name
            Text(
                text = item.diet.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$actionText $date",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(Modifier.height(20.dp))

            // Macro summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgPage)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ConfirmMacroCell("${item.totalCalories}", "kcal", DesignGreen)
                ConfirmMacroCell("${item.totalProtein}g", "protein", ChartProtein)
                ConfirmMacroCell("${item.totalCarbs}g", "carbs", ChartCarbs)
                ConfirmMacroCell("${item.totalFat}g", "fat", ChartFat)
            }

            Spacer(Modifier.height(16.dp))

            // Slot coverage
            if (item.mealCount > 0) {
                Text(
                    text = "SLOTS COVERED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultMealSlot.entries.forEach { slot ->
                        val covered = slot.name in item.assignedSlots
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (covered) DesignGreen else DividerColor)
                            )
                            Text(
                                text = slot.displayName,
                                fontSize = 12.sp,
                                color = if (covered) TextPrimary else TextMuted,
                                fontWeight = if (covered) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Confirm button
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Confirm",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = TextMuted)
            }
        }
    }
}

@Composable
private fun ConfirmMacroCell(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted
        )
    }
}
