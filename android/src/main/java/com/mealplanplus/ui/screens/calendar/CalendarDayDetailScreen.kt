package com.mealplanplus.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.util.toEpochMs
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Full-screen day detail view.
 *
 * Uses [CalendarViewModel] with the date provided as `initialDate` nav argument,
 * mirroring the existing CalendarWithDate route so the ViewModel's savedStateHandle
 * wiring works out of the box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDietPicker: (String) -> Unit = {},
    onNavigateToLog: (String) -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val date = uiState.selectedDate

    // Observe diet selection result coming back from DietPickerScreen
    val pickedDietId by (savedStateHandle
        ?.getStateFlow("selected_diet_id", -1L)
        ?.collectAsState() ?: remember { mutableStateOf(-1L) })
    LaunchedEffect(pickedDietId) {
        if (pickedDietId != -1L) {
            viewModel.assignDietById(pickedDietId)
            savedStateHandle?.set("selected_diet_id", -1L)
        }
    }

    // Grocery snapshot sheet
    if (uiState.grocerySnapshot != null) {
        GrocerySnapshotSheet(
            dietName = uiState.selectedDiet?.name ?: "",
            items = uiState.grocerySnapshot!!,
            onDismiss = { viewModel.clearGrocerySnapshot() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        DayDetailTopBar(date = date, onBack = onNavigateBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                val isPlanCompleted = uiState.plans[date.toEpochMs()]?.isCompleted ?: false
                PlanDayDetail(
                    date = date,
                    diet = uiState.selectedDiet,
                    dietWithMeals = uiState.selectedDietWithMeals,
                    tags = uiState.selectedDietTags,
                    isPlanCompleted = isPlanCompleted,
                    todayLoggedSlots = uiState.todayLoggedSlots,
                    onAssignDiet = { onNavigateToDietPicker(date.toString()) },
                    onChangeDiet = { onNavigateToDietPicker(date.toString()) },
                    onRemoveDiet = { viewModel.clearPlan() },
                    onViewLog = { onNavigateToLog(date.toString()) },
                    onSlotToggle = { slotType -> viewModel.toggleSlotLogged(slotType) },
                    onToggleFavourite = { diet -> viewModel.toggleFavourite(diet) },
                    onShowGroceries = { viewModel.generateGroceriesForDiet() },
                    isGeneratingGroceries = uiState.isGeneratingGroceries
                )
            }
        }
    }
}

@Composable
private fun DayDetailTopBar(date: LocalDate, onBack: () -> Unit) {
    val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = Color(0xFF111111),
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = "Day Detail",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111111),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = date.format(dateFmt),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF888888),
            modifier = Modifier.padding(end = 16.dp)
        )
    }
    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
}
