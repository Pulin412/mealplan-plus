package com.mealplanplus.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFoods: () -> Unit,
    onNavigateToMeals: () -> Unit,
    onNavigateToDiets: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToHealth: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MealPlan+") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Welcome to MealPlan+",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your diabetes-friendly diet tracker",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary action - Today's Log
            Button(
                onClick = onNavigateToLog,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log Today's Food")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Manage",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToFoods,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Foods")
                }
                OutlinedButton(
                    onClick = onNavigateToMeals,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Meals")
                }
            }

            OutlinedButton(
                onClick = onNavigateToDiets,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Diet Templates")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Planning & Health",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToCalendar,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Calendar")
                }
                OutlinedButton(
                    onClick = onNavigateToHealth,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Health")
                }
            }
        }
    }
}
