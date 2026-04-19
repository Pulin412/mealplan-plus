package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import com.mealplanplus.ui.theme.*

@Composable
fun WorkoutTemplateDetailScreen(
    templateId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    var template by remember { mutableStateOf<WorkoutTemplateWithExercises?>(null) }

    LaunchedEffect(templateId) {
        template = viewModel.getTemplateWithExercises(templateId)
    }

    val t = template

    Scaffold(containerColor = BgPage) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
                .padding(padding)
        ) {
            // ── Header ────────────────────────────────────────────────────────
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
                        t?.template?.name ?: "Workout",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp
                    )
                    if (t != null) {
                        val totalSets = t.exercises.sumOf { it.templateExercise.targetSets ?: 0 }
                        Text(
                            "${t.exercises.size} exercise${if (t.exercises.size != 1) "s" else ""} · $totalSets sets · ${t.template.category.displayName()}",
                            fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
                if (t != null) {
                    TextButton(onClick = { onEdit(templateId) }) {
                        Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignGreen)
                    }
                }
            }

            if (t == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Info card ─────────────────────────────────────────────
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(t.template.category.bgColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(t.template.category.emoji(), fontSize = 22.sp)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(t.template.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CategoryBadge(t.template.category.displayName())
                                    }
                                    t.template.notes?.let {
                                        Text(it, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 1.dp))
                                    }
                                }
                            }
                        }
                    }

                    // ── Exercises section ────────────────────────────────────
                    item {
                        Text(
                            "EXERCISES",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                            letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (t.exercises.isEmpty()) {
                                Text(
                                    "No exercises added yet.",
                                    fontSize = 14.sp, color = TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                t.exercises.forEachIndexed { idx, ex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Order number
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(IconBgGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${idx + 1}",
                                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(ex.exercise.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            val meta = buildString {
                                                ex.exercise.muscleGroup?.let { append(it) }
                                                ex.exercise.equipment?.let { if (isNotEmpty()) append(" · "); append(it) }
                                            }
                                            if (meta.isNotBlank()) Text(meta, fontSize = 11.sp, color = TextSecondary)
                                        }

                                        // Set breakdown: group consecutive sets with same reps+weight
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            if (ex.plannedSets.isNotEmpty()) {
                                                // Group into runs of same (reps, weight)
                                                data class SetGroup(val reps: Int?, val weightKg: Double?, val count: Int)
                                                val groups = mutableListOf<SetGroup>()
                                                for (s in ex.plannedSets) {
                                                    val last = groups.lastOrNull()
                                                    if (last != null && last.reps == s.reps && last.weightKg == s.weightKg) {
                                                        groups[groups.lastIndex] = last.copy(count = last.count + 1)
                                                    } else {
                                                        groups.add(SetGroup(s.reps, s.weightKg, 1))
                                                    }
                                                }
                                                groups.forEach { g ->
                                                    val label = buildString {
                                                        g.weightKg?.let {
                                                            val w = if (it % 1 == 0.0) it.toInt().toString() else "%.1f".format(it)
                                                            append("${w}kg · ")
                                                        }
                                                        g.reps?.let { append("${it} reps") }
                                                        if (g.count > 1) append(" ×${g.count}")
                                                    }
                                                    if (label.isNotBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(TagGrayBg)
                                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Fallback: show summary badge
                                                val fallback = buildString {
                                                    ex.templateExercise.targetSets?.let { append("${it}×") }
                                                    ex.templateExercise.targetReps?.let { append("$it") }
                                                }
                                                if (fallback.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(TagGrayBg)
                                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(fallback, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (idx < t.exercises.lastIndex) {
                                        HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 58.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Bottom actions ─────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().background(BgPage).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onStartWorkout(templateId) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                    ) {
                        Text("▶  Start Workout", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CardBg)
                    }
                    OutlinedButton(
                        onClick = { onEdit(templateId) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Workout", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(TagGrayBg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
    }
}

