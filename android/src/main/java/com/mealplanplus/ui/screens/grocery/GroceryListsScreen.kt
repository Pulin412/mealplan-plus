package com.mealplanplus.ui.screens.grocery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mealplanplus.data.model.GroceryList
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DesignGreenLight
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: GroceryListsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var listToDelete by remember { mutableStateOf<GroceryList?>(null) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = { Text("Grocery Lists", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.Add, contentDescription = "Create list", tint = TextPrimary)
                    }
                },
                colors = minimalTopAppBarColors()
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.lists.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No grocery lists yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap + to create one from your meal plans",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.lists, key = { it.list.id }) { item ->
                        GroceryListCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.list.id) },
                            onDelete = { listToDelete = item.list }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Delete List") },
            text = { Text("Delete \"${list.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(list)
                    listToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GroceryListCard(
    item: GroceryListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(DesignGreenLight),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ShoppingCart, null, tint = DesignGreen, modifier = Modifier.size(20.dp)) }

                Column(modifier = Modifier.weight(1f)) {
                    Text(item.list.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.list.dateRangeDisplay?.let { dateRange ->
                        Text(dateRange, fontSize = 11.sp, color = TextMuted)
                    }
                    Text("${item.checkedCount} of ${item.itemCount} items", fontSize = 11.sp, color = TextMuted)
                }

                if (item.itemCount > 0 && item.checkedCount == item.itemCount) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(DesignGreenLight).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) { Text("Done", fontSize = 11.sp, color = DesignGreen, fontWeight = FontWeight.SemiBold) }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${item.itemCount}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("items", fontSize = 10.sp, color = TextMuted)
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            if (item.itemCount > 0) {
                LinearProgressIndicator(
                    progress = item.progressPercent,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = DesignGreen,
                    trackColor = TagGrayBg
                )
            }
        }
    }
}
