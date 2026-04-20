package com.mealplanplus.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DesignGreenLight
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.ActivityLevel
import com.mealplanplus.data.model.Gender
import com.mealplanplus.data.model.GoalType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onSaveSuccess: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Each import type gets its own launcher
    val importDietsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importDietsFromJson(it) } }

    val importFoodsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importFoodsFromJson(it) } }

    val importExercisesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importExercisesFromJson(it) } }

    val importTemplatesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importWorkoutTemplatesFromJson(it) } }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importBackupFromJson(it) } }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            onSaveSuccess()
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
    LaunchedEffect(importResult) {
        importResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportResult()
        }
    }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = minimalTopAppBarColors()
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
                    .background(BgPage)
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
                        colors = CardDefaults.cardColors(containerColor = DesignGreenLight)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person, null,
                                modifier = Modifier.size(40.dp),
                                tint = TextPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        uiState.user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
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
                    Text("Gender", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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
                    Text("Goal", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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
                            color = TextSecondary
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

                // ── Data Management ──────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Import Data",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Pick a JSON file from your device to import. Files are available in the app assets.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        ImportButton("Foods", Icons.Default.LocalDining) {
                            importFoodsLauncher.launch("application/json")
                        }
                        ImportButton("Exercises", Icons.Default.FitnessCenter) {
                            importExercisesLauncher.launch("application/json")
                        }
                        ImportButton("Workout Templates", Icons.Default.ViewList) {
                            importTemplatesLauncher.launch("application/json")
                        }
                        ImportButton("Diets", Icons.Default.Restaurant) {
                            importDietsLauncher.launch("application/json")
                        }
                        ImportButton("Full Backup", Icons.Default.RestoreFromTrash) {
                            importBackupLauncher.launch("application/json")
                        }
                    }
                }

                // ── Save ─────────────────────────────────────────────────────
                Button(
                    onClick = { viewModel.saveProfile() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignGreen)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }

                // ── Logout ───────────────────────────────────────────────────
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(13.dp),
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
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear All Data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will permanently delete:")
                    listOf(
                        "All meals and diets you created",
                        "All food logs and daily records",
                        "All health readings",
                        "All grocery lists"
                    ).forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.error)
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "⚠\uFE0F Your data is stored on this device only. " +
                        "There is no cloud backup. Once deleted, it cannot be recovered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmClearData() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Delete Everything")
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
            icon = {
                Icon(
                    Icons.Default.PersonOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your account and all associated data will be permanently deleted, including:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    listOf(
                        "All meals and diets you created",
                        "All food logs and daily records",
                        "All health readings",
                        "Your profile and settings"
                    ).forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.error)
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "⚠\uFE0F Your data is stored on this device only. " +
                        "There is no cloud backup. Deleting your account is permanent " +
                        "and there is no way to restore your data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteAccount(onLogout) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Delete My Account")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            content()
        }
    }
}

@Composable
private fun EstimateCard(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DesignGreenLight)
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

@Composable
private fun ImportButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Import $label (JSON)", fontSize = 14.sp)
    }
}
