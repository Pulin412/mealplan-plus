package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDietScreen(
    onNavigateBack: () -> Unit,
    onDietSaved: (Long) -> Unit,
    // kept for NavHost compat but no longer used in this screen
    onNavigateToFoodPicker: () -> Unit = {},
    viewModel: AddDietViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedDietId) {
        uiState.savedDietId?.let { onDietSaved(it) }
    }

    Scaffold(
        topBar = {
            DietFormTopBar(
                title = "New Diet",
                onNavigateBack = onNavigateBack,
                onSave = viewModel::saveDiet,
                isSaving = uiState.isLoading
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DietInfoCard(
                    name = uiState.name,
                    description = uiState.description,
                    allTags = uiState.allTags,
                    selectedTagIds = uiState.selectedTagIds,
                    onNameChange = viewModel::updateName,
                    onDescriptionChange = viewModel::updateDescription,
                    onTagToggle = viewModel::toggleTag,
                    newTagName = uiState.newTagName,
                    onNewTagNameChange = viewModel::updateNewTagName,
                    onCreateTag = viewModel::createAndSelectTag
                )
            }

            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
