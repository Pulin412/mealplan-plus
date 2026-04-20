package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.WorkoutTemplate
import com.mealplanplus.data.model.WorkoutTemplateCategory
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import com.mealplanplus.ui.theme.*

@Composable
fun WorkoutTemplatesScreen(
    onBack: () -> Unit,
    onCreateTemplate: () -> Unit,
    onViewTemplate: (Long) -> Unit,
    onEditTemplate: (Long) -> Unit,
    onStartFromTemplate: (Long) -> Unit,
    onNavigateToExercises: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var toDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }

    Scaffold(
        containerColor = BgPage,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTemplate,
                containerColor = TextPrimary,
                contentColor = CardBg,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New template", modifier = Modifier.size(22.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgPage)
                        .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "My Workouts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "${state.templates.size} saved workout plans",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    TextButton(onClick = onNavigateToExercises) {
                        Text("Exercises", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignGreen)
                    }
                }
            }

            if (state.templates.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💪", fontSize = 44.sp)
                        Text("No workouts yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            "Create workouts like \"Chest Day\" to quickly\nstart structured sessions.",
                            fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onCreateTemplate,
                            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Create workout", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CardBg)
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "MY WORKOUTS",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = TextSecondary, letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 6.dp)
                    )
                }

                items(state.templates, key = { it.template.id }) { item ->
                    TemplateCard(
                        item = item,
                        onTap = { onViewTemplate(item.template.id) },
                        onEdit = { onEditTemplate(item.template.id) },
                        onDelete = { toDelete = item.template },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete template?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${t.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTemplate(t); toDelete = null }) {
                    Text("Delete", color = TextDestructive, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TemplateCard(
    item: WorkoutTemplateWithExercises,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exerciseCount = item.exercises.size
    val catBg = item.template.category.bgColor()

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(catBg),
                contentAlignment = Alignment.Center
            ) {
                Text(item.template.category.emoji(), fontSize = 20.sp)
            }

            // Name + detail
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.template.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    "$exerciseCount exercise${if (exerciseCount != 1) "s" else ""} · ${item.template.category.displayName()}",
                    fontSize = 12.sp, color = TextSecondary
                )
                item.template.notes?.let {
                    Text(it, fontSize = 11.sp, color = TextMuted, maxLines = 1)
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Exercise preview row
        if (item.exercises.isNotEmpty()) {
            HorizontalDivider(color = DividerColor)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.exercises.take(4).forEach { ex ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(TagGrayBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            ex.exercise.name.split(" ").first(),
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary
                        )
                    }
                }
                if (item.exercises.size > 4) {
                    Text("+${item.exercises.size - 4}", fontSize = 11.sp, color = TextMuted)
                }
            }
        }
    }
}

internal fun WorkoutTemplateCategory.emoji() = when (this) {
    WorkoutTemplateCategory.STRENGTH    -> "💪"
    WorkoutTemplateCategory.CARDIO      -> "🏃"
    WorkoutTemplateCategory.FLEXIBILITY -> "🧘"
    WorkoutTemplateCategory.MIXED       -> "🏋️"
}

@Composable
internal fun WorkoutTemplateCategory.bgColor() = when (this) {
    WorkoutTemplateCategory.STRENGTH    -> IconBgGray
    WorkoutTemplateCategory.CARDIO      -> TagBlueBg
    WorkoutTemplateCategory.FLEXIBILITY -> TagGreenBg
    WorkoutTemplateCategory.MIXED       -> TagOrangeBg
}


internal fun WorkoutTemplateCategory.displayName() = when (this) {
    WorkoutTemplateCategory.STRENGTH    -> "Strength"
    WorkoutTemplateCategory.CARDIO      -> "Cardio"
    WorkoutTemplateCategory.FLEXIBILITY -> "Flexibility"
    WorkoutTemplateCategory.MIXED       -> "Mixed"
}
