package com.mealplanplus.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.mealplanplus.ui.theme.BrandGreen
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.ActivityLevel
import com.mealplanplus.data.model.Gender
import com.mealplanplus.data.model.GoalType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Profile saved!")
            viewModel.clearSaveSuccess()
        }
    }
    LaunchedEffect(uiState.clearSuccess) {
        if (uiState.clearSuccess) {
            snackbarHostState.showSnackbar("All data cleared.")
            viewModel.clearClearSuccess()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Avatar + email ───────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.size(80.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        uiState.user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Personal Info ────────────────────────────────────────────
                ProfileSection("Personal Info") {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.age,
                        onValueChange = { if (it.all(Char::isDigit)) viewModel.updateAge(it) },
                        label = { Text("Age") },
                        leadingIcon = { Icon(Icons.Default.DateRange, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Gender chips
                    Text("Gender", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Gender.entries.forEach { g ->
                            FilterChip(
                                selected = uiState.gender == g,
                                onClick = { viewModel.updateGender(g) },
                                label = { Text(g.displayName) }
                            )
                        }
                    }
                }

                // ── Body Metrics ─────────────────────────────────────────────
                ProfileSection("Body Metrics") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.weightKg,
                            onValueChange = viewModel::updateWeight,
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.heightCm,
                            onValueChange = viewModel::updateHeight,
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Lifestyle ────────────────────────────────────────────────
                ProfileSection("Lifestyle") {
                    ActivityLevelDropdown(
                        selected = uiState.activityLevel,
                        onSelect = viewModel::updateActivityLevel
                    )
                    Text("Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalType.entries.forEach { g ->
                            FilterChip(
                                selected = uiState.goalType == g,
                                onClick = { viewModel.updateGoalType(g) },
                                label = { Text(g.displayName) }
                            )
                        }
                    }
                }

                // ── Health Estimates (only if enough data) ───────────────────
                if (uiState.computedBmr != null) {
                    ProfileSection("Health Estimates") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EstimateCard("BMR", "${uiState.computedBmr} kcal", "basal", Modifier.weight(1f))
                            EstimateCard(
                                "TDEE",
                                if (uiState.computedTdee != null) "${uiState.computedTdee} kcal" else "—",
                                "maintenance",
                                Modifier.weight(1f)
                            )
                            EstimateCard(
                                "Body Fat",
                                if (uiState.computedBodyFatPct != null) "${uiState.computedBodyFatPct}%" else "—",
                                "estimate",
                                Modifier.weight(1f)
                            )
                        }
                        Text(
                            "* Estimates only. BMR via Mifflin-St Jeor; body fat via Deurenberg formula.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Nutrition Goal ───────────────────────────────────────────
                ProfileSection("Nutrition Goal") {
                    OutlinedTextField(
                        value = uiState.targetCalories,
                        onValueChange = { if (it.all(Char::isDigit)) viewModel.updateTargetCalories(it) },
                        label = { Text("Daily Target Calories") },
                        leadingIcon = { Icon(Icons.Default.FitnessCenter, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            val hint = when {
                                uiState.computedTdee != null -> "Blank = use TDEE (~${uiState.computedTdee} kcal)"
                                else -> "Set based on your daily calorie goal"
                            }
                            Text(hint)
                        }
                    )
                }

                // ── Save ─────────────────────────────────────────────────────
                Button(
                    onClick = { viewModel.saveProfile() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }

                // ── Logout ───────────────────────────────────────────────────
                OutlinedButton(
                    onClick = { viewModel.logout(); onLogout() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.ExitToApp, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout")
                }

                // ── Danger Zone ──────────────────────────────────────────────
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Danger Zone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )

                OutlinedButton(
                    onClick = { viewModel.showClearDataDialog() },
                    enabled = !uiState.isClearing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (uiState.isClearing) {
                        CircularProgressIndicator(Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All Data")
                    }
                }

                Button(
                    onClick = { viewModel.showDeleteAccountDialog() },
                    enabled = !uiState.isClearing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Account")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (uiState.showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearDataDialog() },
            title = { Text("Clear All Data?") },
            text = { Text("Deletes all logs, health readings, diets, meals and grocery lists. Your account stays active.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClearData() }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearDataDialog() }) { Text("Cancel") }
            }
        )
    }

    if (uiState.showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAccountDialog() },
            title = { Text("Delete Account?") },
            text = { Text("Permanently deletes your account and all data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteAccount(onLogout) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAccountDialog() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun EstimateCard(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityLevelDropdown(selected: ActivityLevel?, onSelect: (ActivityLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.displayName ?: "Select activity level",
            onValueChange = {},
            readOnly = true,
            label = { Text("Activity Level") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ActivityLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.displayName) },
                    onClick = { onSelect(level); expanded = false }
                )
            }
        }
    }
}
