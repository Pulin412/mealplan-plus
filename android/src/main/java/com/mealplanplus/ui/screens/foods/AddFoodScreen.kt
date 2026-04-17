package com.mealplanplus.ui.screens.foods

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.components.*
import com.mealplanplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: AddFoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Pre-fill form when returning from barcode scanner
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<String>("scanned_food_name")?.let { name ->
            val food = FoodItem(
                name = name,
                brand = savedStateHandle.get<String>("scanned_food_brand"),
                caloriesPer100 = savedStateHandle.get<Double>("scanned_food_calories") ?: 0.0,
                proteinPer100 = savedStateHandle.get<Double>("scanned_food_protein") ?: 0.0,
                carbsPer100 = savedStateHandle.get<Double>("scanned_food_carbs") ?: 0.0,
                fatPer100 = savedStateHandle.get<Double>("scanned_food_fat") ?: 0.0
            )
            viewModel.prefillFromFood(food)
            savedStateHandle.remove<String>("scanned_food_name")
            savedStateHandle.remove<String>("scanned_food_brand")
            savedStateHandle.remove<Double>("scanned_food_calories")
            savedStateHandle.remove<Double>("scanned_food_protein")
            savedStateHandle.remove<Double>("scanned_food_carbs")
            savedStateHandle.remove<Double>("scanned_food_fat")
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val bgPage = BgPage
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    val iconBgGray = IconBgGray
    val tagGrayBg = TagGrayBg

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgPage)
    ) {
        // ── Header row ──
        ScreenCloseHeader(title = "Add Food", onClose = onNavigateBack)

        // ── Subtitle ──
        ScreenSubtitle(text = "Add to your food catalogue")

        // ── Form content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // Food name
            FormGroup(label = "Food Name *") {
                DesignTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    placeholder = "e.g. Avocado",
                    imeAction = ImeAction.Next
                )
            }

            // Macros — 2×2 grid
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormGroup(label = "Calories (per 100g)", modifier = Modifier.weight(1f)) {
                    DesignTextField(
                        value = uiState.calories,
                        onValueChange = viewModel::updateCalories,
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                }
                FormGroup(label = "Protein (g)", modifier = Modifier.weight(1f)) {
                    DesignTextField(
                        value = uiState.protein,
                        onValueChange = viewModel::updateProtein,
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormGroup(label = "Carbs (g)", modifier = Modifier.weight(1f)) {
                    DesignTextField(
                        value = uiState.carbs,
                        onValueChange = viewModel::updateCarbs,
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                }
                FormGroup(label = "Fat (g)", modifier = Modifier.weight(1f)) {
                    DesignTextField(
                        value = uiState.fat,
                        onValueChange = viewModel::updateFat,
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                }
            }

            // Brand (optional)
            FormGroup(label = "Brand (optional)") {
                DesignTextField(
                    value = uiState.brand,
                    onValueChange = viewModel::updateBrand,
                    placeholder = "e.g. Amul, Organic India…",
                    imeAction = ImeAction.Next
                )
            }

            // Serving Size + Unit
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormGroup(label = "Serving Size", modifier = Modifier.weight(1.6f)) {
                    DesignTextField(
                        value = uiState.servingSize,
                        onValueChange = viewModel::updateServingSize,
                        placeholder = "100",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                }
                FormGroup(label = "Unit", modifier = Modifier.weight(1f)) {
                    DesignTextField(
                        value = uiState.servingUnit,
                        onValueChange = viewModel::updateServingUnit,
                        placeholder = "g",
                        imeAction = ImeAction.Next
                    )
                }
            }

            // Glycemic Index (optional)
            FormGroup(label = "Glycemic Index (optional, 0–100)") {
                DesignTextField(
                    value = uiState.glycemicIndex,
                    onValueChange = viewModel::updateGlycemicIndex,
                    placeholder = "e.g. 55",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            }

            // ── Barcode scan row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgGray)
                    .clickable { onNavigateToScanner() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.CropFree,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Scan barcode to auto-fill",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(tagGrayBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textMuted)
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Save button ──
            PrimaryButton(
                text = if (uiState.isLoading) "" else "Save Food",
                onClick = viewModel::saveFood,
                isLoading = uiState.isLoading
            )

            // ── Search Online button ──
            OutlineButton(
                text = "Search Online (USDA / OpenFoodFacts)",
                onClick = viewModel::showUsdaSearch
            )

            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    fontSize = 12.sp,
                    color = TextDestructive,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    // USDA Search Modal
    if (uiState.showUsdaSearch) {
        UsdaSearchModal(
            searchQuery = uiState.usdaSearchQuery,
            onSearchQueryChange = viewModel::updateUsdaSearchQuery,
            onSearch = viewModel::searchUsda,
            isLoading = uiState.isSearchingUsda,
            results = uiState.usdaSearchResults,
            error = uiState.usdaSearchError,
            onSelectFood = viewModel::copyFromUsda,
            onDismiss = viewModel::hideUsdaSearch
        )
    }
}

// ─── USDA Search Modal ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsdaSearchModal(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
    results: List<UsdaFoodResult>,
    error: String?,
    onSelectFood: (UsdaFoodResult) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = CardBg
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    val designGreen = DesignGreen

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f),
        containerColor = cardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Search USDA Database",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                "Find generic foods — rice, chicken, apple, etc.",
                fontSize = 12.sp,
                color = textMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Search field
            DesignTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search foods…",
                imeAction = ImeAction.Search,
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )

            Spacer(Modifier.height(10.dp))

            PrimaryButton(
                text = "Search",
                onClick = onSearch,
                isLoading = isLoading,
                enabled = searchQuery.length >= 2
            )

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = designGreen) }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(error, color = TextDestructive, fontSize = 13.sp)
                    }
                }
                results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Search for foods above", color = textMuted, fontSize = 13.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results.distinctBy { it.fdcId }, key = { it.fdcId }) { food ->
                            UsdaFoodResultRow(food = food, onSelect = { onSelectFood(food) })
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun UsdaFoodResultRow(food: UsdaFoodResult, onSelect: () -> Unit) {
    val cardBg = CardBg
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    val dividerColor = DividerColor
    val designGreen = DesignGreen
    val iconBgGray = IconBgGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, dividerColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgGray),
            contentAlignment = Alignment.Center
        ) {
            Text("🥗", fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(food.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, maxLines = 2)
            food.brand?.let {
                Text(it, fontSize = 11.sp, color = textMuted)
            }
            Text(
                "Per ${food.servingSize.toInt()}${food.servingUnit}: ${food.calories.toInt()} cal · P ${food.protein.toInt()}g C ${food.carbs.toInt()}g F ${food.fat.toInt()}g",
                fontSize = 11.sp,
                color = designGreen,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(Icons.Default.Add, contentDescription = "Copy", tint = designGreen, modifier = Modifier.size(20.dp))
    }
}
