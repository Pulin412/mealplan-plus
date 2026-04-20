package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.ui.theme.*

@Composable
fun AddEditExerciseScreen(
    existingId: Long? = null,
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    var existing by remember { mutableStateOf<Exercise?>(null) }
    var loaded   by remember { mutableStateOf(existingId == null) }

    LaunchedEffect(existingId) {
        if (existingId != null && existingId > 0) {
            existing = viewModel.getExerciseById(existingId)
        }
        loaded = true
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(32.dp))
        }
        return
    }

    val isSystem  = existing?.isSystem == true
    val isNewMode = existing == null

    var name        by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var category    by remember(existing) { mutableStateOf(existing?.category ?: ExerciseCategory.STRENGTH) }
    var muscleGroup by remember(existing) { mutableStateOf(existing?.muscleGroup ?: "") }
    var equipment   by remember(existing) { mutableStateOf(existing?.equipment ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var videoLink   by remember(existing) { mutableStateOf(existing?.videoLink ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && !isSystem
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPage)
                .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        isNewMode -> "Add Exercise"
                        isSystem  -> "Exercise Details"
                        else      -> "Edit Exercise"
                    },
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp
                )
                Text(
                    if (isSystem) "System exercise · read only" else if (!isNewMode) "Custom exercise" else "",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
            if (!isNewMode && !isSystem) {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDestructive)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // ── Main form card ────────────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExFieldBlock(label = "NAME", required = !isSystem) {
                            if (isSystem) {
                                ReadOnlyValue(name)
                            } else {
                                ExTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Bulgarian Split Squat", capitalization = KeyboardCapitalization.Words)
                            }
                        }

                        ExFieldBlock(label = "CATEGORY", required = !isSystem) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(ExerciseCategory.entries) { cat ->
                                    val selected = cat == category
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) TextPrimary else BgPage)
                                            .then(if (!isSystem) Modifier.clickable { category = cat } else Modifier)
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(cat.displayName(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) CardBg else TextSecondary)
                                    }
                                }
                            }
                        }

                        ExFieldBlock(label = "MUSCLE GROUP") {
                            if (isSystem) ReadOnlyValue(muscleGroup.ifBlank { "—" })
                            else ExTextField(value = muscleGroup, onValueChange = { muscleGroup = it }, placeholder = "e.g. Quadriceps, Glutes", capitalization = KeyboardCapitalization.Words)
                        }

                        ExFieldBlock(label = "EQUIPMENT") {
                            if (isSystem) ReadOnlyValue(equipment.ifBlank { "—" })
                            else ExTextField(value = equipment, onValueChange = { equipment = it }, placeholder = "e.g. Dumbbells, Barbell", capitalization = KeyboardCapitalization.Words)
                        }
                    }
                }
            }

            // ── Description / video card ──────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExFieldBlock(label = "DESCRIPTION") {
                            if (isSystem) ReadOnlyValue(description.ifBlank { "—" })
                            else ExTextField(value = description, onValueChange = { description = it }, placeholder = "How to perform, cues, tips…", singleLine = false, minLines = 3)
                        }

                        ExFieldBlock(label = "VIDEO LINK") {
                            if (isSystem) {
                                if (videoLink.isNotBlank()) {
                                    Text(
                                        videoLink,
                                        fontSize = 14.sp,
                                        color = TagBlue,
                                        modifier = Modifier.clickable { uriHandler.openUri(videoLink) }
                                    )
                                } else {
                                    ReadOnlyValue("—")
                                }
                            } else {
                                ExTextField(value = videoLink, onValueChange = { videoLink = it }, placeholder = "https://youtube.com/…", capitalization = KeyboardCapitalization.None)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // ── Bottom actions ────────────────────────────────────────────────────
        if (!isSystem) {
            Column(
                modifier = Modifier.fillMaxWidth().background(BgPage).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveExercise(
                            existingId = existing?.id,
                            name = name, category = category, muscleGroup = muscleGroup,
                            equipment = equipment, description = description, videoLink = videoLink,
                            onDone = onBack
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                ) {
                    Text(if (isNewMode) "Save Exercise" else "Update Exercise", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CardBg)
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().background(BgPage).padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete exercise?", fontWeight = FontWeight.Bold) },
            text  = { Text("\"${existing?.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    existing?.let { viewModel.deleteExercise(it) }
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text("Delete", color = TextDestructive, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Shared form helpers ────────────────────────────────────────────────────────

@Composable
private fun ReadOnlyValue(text: String) {
    Text(text, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
internal fun ExFieldBlock(label: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.6.sp)
            if (required) Text("*", fontSize = 10.sp, color = DesignGreen, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
internal fun ExTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        ),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = BgPage,
            focusedContainerColor = CardBg,
            unfocusedBorderColor = Color(0xFFEBEBEB),
            focusedBorderColor = TextPrimary
        )
    )
}

