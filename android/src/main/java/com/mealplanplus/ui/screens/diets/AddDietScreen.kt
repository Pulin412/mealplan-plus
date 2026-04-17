package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.Tag
import com.mealplanplus.ui.components.*
import com.mealplanplus.ui.theme.*

@Composable
fun AddDietScreen(
    onNavigateBack: () -> Unit,
    onDietSaved: (Long) -> Unit,
    onNavigateToFoodPicker: () -> Unit = {},
    viewModel: AddDietViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedDietId) {
        uiState.savedDietId?.let { onDietSaved(it) }
    }

    val bgPage = BgPage
    val cardBg = CardBg
    val textMuted = TextMuted
    val dividerColor = DividerColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgPage)
    ) {
        // ── Header ──
        ScreenCloseHeader(title = "Create Diet", onClose = onNavigateBack)
        ScreenSubtitle(text = "Assign meals to slots for a full day plan")

        // ── Form ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // Diet name
            FormGroup(label = "Diet Name *") {
                DesignTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    placeholder = "e.g. My Remission Diet",
                    imeAction = ImeAction.Next
                )
            }

            // Type selector (from tags) + add new type inline
            Column {
                FormLabel("Type", modifier = Modifier.padding(bottom = 8.dp))
                if (uiState.allTags.isNotEmpty()) {
                    DietTypeSelector(
                        allTags = uiState.allTags,
                        selectedTagIds = uiState.selectedTagIds,
                        onTagToggle = viewModel::toggleTag
                    )
                    Spacer(Modifier.height(10.dp))
                }
                // Add custom type row
                AddTypeRow(
                    value = uiState.newTagName,
                    onValueChange = viewModel::updateNewTagName,
                    onAdd = viewModel::createAndSelectTag
                )
            }

            // Description
            FormGroup(label = "Description") {
                DesignTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    placeholder = "Brief description of this diet plan…",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    imeAction = ImeAction.Default
                )
            }

            // ── Slot preview section ──
            FormSectionLabel(
                "Assign Meals to Slots",
                modifier = Modifier.padding(top = 6.dp)
            )

            // Slot rows card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
            ) {
                SlotPreviewRow(
                    slot = DefaultMealSlot.BREAKFAST,
                    dotColor = SlotBreakfast,
                    isLast = false,
                    dividerColor = dividerColor
                )
                SlotPreviewRow(
                    slot = DefaultMealSlot.NOON,
                    dotColor = SlotDefault,
                    isLast = false,
                    dividerColor = dividerColor
                )
                SlotPreviewRow(
                    slot = DefaultMealSlot.LUNCH,
                    dotColor = SlotLunch,
                    isLast = false,
                    dividerColor = dividerColor
                )
                SlotPreviewRow(
                    slot = DefaultMealSlot.DINNER,
                    dotColor = SlotDinner,
                    isLast = true,
                    dividerColor = dividerColor
                )
            }

            Text(
                "You can assign meals to each slot after saving the diet.",
                fontSize = 11.sp,
                color = textMuted,
                modifier = Modifier.padding(top = 0.dp)
            )

            // Error
            uiState.error?.let { error ->
                Text(
                    error,
                    fontSize = 12.sp,
                    color = TextDestructive
                )
            }

            Spacer(Modifier.height(4.dp))

            // Save button
            PrimaryButton(
                text = "Save Diet",
                onClick = viewModel::saveDiet,
                isLoading = uiState.isLoading
            )
        }
    }
}

// ─── Add new type row ─────────────────────────────────────────────────────────

@Composable
private fun AddTypeRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    val cardBg = CardBg
    val iconBgGray = IconBgGray

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DesignTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "Add new type…",
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onAdd() }),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (value.isNotBlank()) textPrimary else iconBgGray)
                .clickable(enabled = value.isNotBlank()) { onAdd() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (value.isNotBlank()) cardBg else textMuted
            )
        }
    }
}

// ─── Type selector (tag chips styled as type buttons) ─────────────────────────

@Composable
private fun DietTypeSelector(
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit
) {
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    val cardBg = CardBg
    val dividerColor = DividerColor

    // Prefer showing up to 5 tags as type buttons; scroll if more
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(allTags) { tag ->
            val isSelected = tag.id in selectedTagIds
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) textPrimary else cardBg)
                    .border(
                        1.5.dp,
                        if (isSelected) textPrimary else dividerColor,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onTagToggle(tag.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tag.name,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) cardBg else textMuted
                )
            }
        }
    }
}

// ─── Slot preview row ─────────────────────────────────────────────────────────

@Composable
private fun SlotPreviewRow(
    slot: DefaultMealSlot,
    dotColor: androidx.compose.ui.graphics.Color,
    isLast: Boolean,
    dividerColor: androidx.compose.ui.graphics.Color
) {
    val textMuted = TextMuted

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Colored dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )
            // Slot label
            Text(
                slot.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
                color = textMuted,
                modifier = Modifier.width(70.dp)
            )
            // Placeholder text
            Text(
                "Tap to assign a meal",
                fontSize = 13.sp,
                color = textMuted,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.weight(1f)
            )
            // Add indicator
            Text("+", fontSize = 13.sp, color = textMuted)
        }

        if (!isLast) {
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
        }
    }
}
