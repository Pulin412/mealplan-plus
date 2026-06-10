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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.Tag
import com.mealplanplus.ui.components.*
import com.mealplanplus.ui.theme.*

private val SLOT_COLORS = mapOf(
    DefaultMealSlot.EARLY_MORNING  to SlotBreakfast,
    DefaultMealSlot.BREAKFAST      to SlotBreakfast,
    DefaultMealSlot.MID_MORNING    to SlotDefault,
    DefaultMealSlot.NOON           to SlotDefault,
    DefaultMealSlot.LUNCH          to SlotLunch,
    DefaultMealSlot.PRE_WORKOUT    to Color(0xFF0EA5E9),
    DefaultMealSlot.EVENING        to SlotDinner,
    DefaultMealSlot.EVENING_SNACK  to SlotDefault,
    DefaultMealSlot.POST_WORKOUT   to Color(0xFF0EA5E9),
    DefaultMealSlot.DINNER         to SlotDinner,
    DefaultMealSlot.POST_DINNER    to SlotDefault
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDietScreen(
    onNavigateBack: () -> Unit,
    onDietSaved: (Long) -> Unit,
    onNavigateToMealPicker: (slotType: String) -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: AddDietViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedDietId) {
        uiState.savedDietId?.let { onDietSaved(it) }
    }

    // Receive meal selection result from DietMealPickerScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            handle.getStateFlow<Long?>("selected_meal_id", null).collect { mealId ->
                if (mealId != null) {
                    val slotType = handle.get<String>("selected_slot_type") ?: return@collect
                    viewModel.assignMealToSlotById(slotType, mealId)
                    handle.remove<Long>("selected_meal_id")
                    handle.remove<String>("selected_slot_type")
                }
            }
        }
    }

    val cardBg    = CardBg
    val textMuted = TextMuted
    val divColor  = DividerColor

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        ScreenCloseHeader(title = "Create Diet", onClose = onNavigateBack)
        ScreenSubtitle(text = "Fill in the details and assign meals to slots")

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

            // Type selector
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

            // Meal slots section
            FormSectionLabel("Meal Slots", modifier = Modifier.padding(top = 6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
            ) {
                DefaultMealSlot.entries.forEachIndexed { index, slot ->
                    val assigned = uiState.slotAssignments[slot.name]
                    val dotColor = SLOT_COLORS[slot] ?: SlotDefault
                    val isLast   = index == DefaultMealSlot.entries.lastIndex
                    SlotAssignRow(
                        slot         = slot,
                        dotColor     = dotColor,
                        assignedMeal = assigned,
                        isLast       = isLast,
                        dividerColor = divColor,
                        onTap        = { onNavigateToMealPicker(slot.name) },
                        onClear      = { viewModel.clearSlotAssignment(slot.name) }
                    )
                }
            }

            // Error
            uiState.error?.let {
                Text(it, fontSize = 12.sp, color = TextDestructive)
            }

            Spacer(Modifier.height(4.dp))

            PrimaryButton(
                text = "Save Diet",
                onClick = viewModel::saveDiet,
                isLoading = uiState.isLoading
            )
        }
    }

}

// ─── Slot assign row ─────────────────────────────────────────────────────────

@Composable
private fun SlotAssignRow(
    slot: DefaultMealSlot,
    dotColor: Color,
    assignedMeal: Meal?,
    isLast: Boolean,
    dividerColor: Color,
    onTap: () -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )
            Text(
                slot.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
                color = TextMuted,
                modifier = Modifier.width(80.dp)
            )
            if (assignedMeal != null) {
                Text(
                    assignedMeal.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Text(
                    "Tap to assign a meal",
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.weight(1f)
                )
                Text("+", fontSize = 13.sp, color = TextMuted)
            }
        }
        if (!isLast) HorizontalDivider(color = dividerColor, thickness = 1.dp)
    }
}

// ─── Add new type row ─────────────────────────────────────────────────────────

@Composable
private fun AddTypeRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
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
                .background(if (value.isNotBlank()) TextPrimary else IconBgGray)
                .clickable(enabled = value.isNotBlank()) { onAdd() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (value.isNotBlank()) CardBg else TextMuted
            )
        }
    }
}

// ─── Type selector ────────────────────────────────────────────────────────────

@Composable
private fun DietTypeSelector(
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(allTags) { tag ->
            val isSelected = tag.id in selectedTagIds
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) TextPrimary else CardBg)
                    .border(1.5.dp, if (isSelected) TextPrimary else DividerColor, RoundedCornerShape(10.dp))
                    .clickable { onTagToggle(tag.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tag.name,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) CardBg else TextMuted
                )
            }
        }
    }
}
