package com.mealplanplus.ui.screens.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DietTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPickerScreen(
    date: String,
    onNavigateBack: () -> Unit,
    onDietSelected: (Long, String) -> Unit,
    onNavigateHome: () -> Unit = {},
    viewModel: DietPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDiet by remember { mutableStateOf<DietPickerItem?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success snackbar
    LaunchedEffect(showSuccessSnackbar) {
        if (showSuccessSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Diet logged successfully!",
                actionLabel = "Go Home",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onNavigateHome()
            }
            showSuccessSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Select Diet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search diets...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DietPickerFilter.entries.forEach { filter ->
                    item {
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { viewModel.updateFilter(filter) },
                            label = { Text(filter.label) },
                            leadingIcon = if (uiState.filter == filter) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Count
            Text(
                text = "${uiState.diets.size} diets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.diets.isEmpty()) {
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
                                "No diets match your search"
                            else
                                "No diets available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        uiState.diets.distinctBy { it.diet.id },
                        key = { it.diet.id }
                    ) { item ->
                        DietPickerCard(
                            item = item,
                            onClick = {
                                selectedDiet = item
                                showConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog && selectedDiet != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedDiet = null
            },
            title = { Text("Log Diet") },
            text = {
                Column {
                    Text("Log \"${selectedDiet!!.diet.name}\" for $date?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${selectedDiet!!.totalCalories} cal • ${selectedDiet!!.mealCount} meals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedDiet?.let {
                            onDietSelected(it.diet.id, date)
                            showConfirmDialog = false
                            showSuccessSnackbar = true
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedDiet = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DietPickerCard(
    item: DietPickerItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.diet.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.tags.forEach { tag ->
                            TagChip(tag = tag)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.totalCalories} cal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${item.mealCount} meals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item.diet.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TagChip(tag: DietTag) {
    val color = when (tag) {
        DietTag.REMISSION -> MaterialTheme.colorScheme.tertiary
        DietTag.MAINTENANCE -> MaterialTheme.colorScheme.secondary
        DietTag.SOS -> MaterialTheme.colorScheme.error
        DietTag.CUSTOM -> MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = tag.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
