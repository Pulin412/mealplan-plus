package com.mealplanplus.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.HealthMetric
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Design tokens
private val PrimaryGreen = Color(0xFF2E7D52)
private val LightGreenBg = Color(0xFFF0F9F4)
private val CardBg = Color.White
private val CarboColor = Color(0xFFF5A623)
private val ProteinColor = Color(0xFF4A90D9)
private val FatColor = Color(0xFFE91E8C)
private val CaloriesColor = Color(0xFF2E7D52)
private val ProgressBarGreen = Color(0xFF2E7D52)
private val WeekOrange = Color(0xFFF57C00)
private val WeekRed = Color(0xFFD32F2F)

@Composable
fun HomeScreen(
    onNavigateToLog: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToGroceryLists: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDiets: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGreenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Green header hero ──────────────────────────────────
            HomeHeaderSection(
                userName = uiState.userName,
                userInitial = uiState.userInitial,
                caloriesConsumed = uiState.todaySummary.calories,
                calorieGoal = uiState.calorieGoal,
                onProfileClick = onNavigateToProfile
            )

            // ── Macro rings card ───────────────────────────────────
            MacroRingsCard(
                calories = uiState.todaySummary.calories,
                calorieGoal = uiState.calorieGoal,
                protein = uiState.todaySummary.protein,
                carbs = uiState.todaySummary.carbs,
                fat = uiState.todaySummary.fat
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Quick Log Food button ──────────────────────────────
            QuickLogFoodButton(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onNavigateToLog
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── This Week mini-calendar ────────────────────────────
            ThisWeekCard(
                weekDays = uiState.weekDays,
                onFullLogClick = onNavigateToLog,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Today's Plan (moved above Blood Glucose) ───────────
            TodaysPlanCard(
                slots = uiState.todayPlanSlots,
                onEditPlanClick = onNavigateToCalendar,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Blood Glucose card ─────────────────────────────────
            BloodGlucoseCard(
                latestSugar = uiState.latestSugar,
                glucoseHistory = uiState.glucoseHistory,
                onDetailsClick = onNavigateToHealth,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Stats row (A1C / Weight / Streak / Diets) ─────────
            StatsRow(
                latestHba1c = uiState.latestHba1c,
                latestWeight = uiState.latestWeight,
                dayStreak = uiState.dayStreak,
                onNavigateToDiets = onNavigateToDiets,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
fun HomeHeaderSection(
    userName: String,
    userInitial: String,
    caloriesConsumed: Int,
    calorieGoal: Int,
    onProfileClick: () -> Unit
) {
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryGreen)
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 24.dp)
    ) {
        Column {
            // Greeting row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting,",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${userName.ifBlank { "there" }} 👋",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable(onClick = onProfileClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitial,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calorie target card — shows surplus if over goal
            val diff = caloriesConsumed - calorieGoal
            val isOver = diff > 0
            val progress = (caloriesConsumed.toFloat() / calorieGoal).coerceIn(0f, 1f)
            val statusText = if (isOver) "🔴 ${diff} kcal over" else "🔥 ${(calorieGoal - caloriesConsumed)} kcal remaining"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Today's Target", color = Color.White, fontSize = 14.sp)
                        Text(
                            "$caloriesConsumed / $calorieGoal kcal",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isOver) Color(0xFFFF7043) else Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(statusText, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Macro rings ───────────────────────────────────────────────────────────────

@Composable
fun MacroRingsCard(calories: Int, calorieGoal: Int, protein: Int, carbs: Int, fat: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroRingItem(value = carbs, unit = "g", label = "Carbs",
                percent = ((carbs.toFloat() / 120f) * 100).toInt().coerceAtMost(100), color = CarboColor)
            MacroRingItem(value = protein, unit = "g", label = "Protein",
                percent = ((protein.toFloat() / 90f) * 100).toInt().coerceAtMost(100), color = ProteinColor)
            MacroRingItem(value = fat, unit = "g", label = "Fat",
                percent = ((fat.toFloat() / 60f) * 100).toInt().coerceAtMost(100), color = FatColor)
            MacroRingItem(value = calories, unit = "kcal", label = "Calories",
                percent = ((calories.toFloat() / calorieGoal) * 100).toInt().coerceAtMost(100), color = CaloriesColor)
        }
    }
}

@Composable
fun MacroRingItem(value: Int, unit: String, label: String, percent: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
            CircularProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 6.dp,
                trackColor = color.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                Text(text = unit, fontSize = 10.sp, color = Color(0xFF888888))
            }
        }
        Text(text = label, fontSize = 12.sp, color = Color(0xFF444444))
        Text(text = "$percent%", fontSize = 11.sp, color = Color(0xFF888888))
    }
}

// ── Quick Log Food ────────────────────────────────────────────────────────────

@Composable
fun QuickLogFoodButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Quick Log Food", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

// ── This Week ─────────────────────────────────────────────────────────────────

@Composable
fun ThisWeekCard(
    weekDays: List<WeekDayInfo>,
    onFullLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    // Fallback: if weekDays not yet loaded, build basic list
    val displayDays = if (weekDays.isEmpty()) {
        (6 downTo 0).map { WeekDayInfo(today.minusDays(it.toLong()), null, WeekDayState.NO_DATA) }
    } else weekDays

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("This Week", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A))
                    Text("$monthName ${today.year}", fontSize = 12.sp, color = Color(0xFF888888))
                }
                TextButton(onClick = onFullLogClick) {
                    Text("Full Log >", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day letter row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                displayDays.forEach { info ->
                    val letter = info.date.dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        .take(1)
                        .uppercase()
                    Text(
                        text = letter,
                        fontSize = 12.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Day circle row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                displayDays.forEach { info ->
                    val isToday = info.date == today
                    val bgColor = when {
                        isToday && info.state == WeekDayState.COMPLETED -> PrimaryGreen
                        isToday -> PrimaryGreen.copy(alpha = 0.7f)
                        info.state == WeekDayState.COMPLETED -> PrimaryGreen
                        info.state == WeekDayState.PLANNED_FUTURE -> WeekOrange
                        info.state == WeekDayState.MISSED -> WeekRed
                        else -> Color.Transparent
                    }
                    val textColor = when {
                        bgColor == Color.Transparent -> Color(0xFFCCCCCC)
                        else -> Color.White
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = info.date.dayOfMonth.toString(),
                                fontSize = 13.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                        // Diet label below circle
                        if (info.dietLabel != null) {
                            Text(
                                text = info.dietLabel,
                                fontSize = 9.sp,
                                color = when (info.state) {
                                    WeekDayState.COMPLETED -> PrimaryGreen
                                    WeekDayState.PLANNED_FUTURE -> WeekOrange
                                    WeekDayState.MISSED -> WeekRed
                                    else -> Color(0xFF888888)
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(color = PrimaryGreen, label = "Done")
                LegendDot(color = WeekOrange, label = "Planned")
                LegendDot(color = WeekRed, label = "Missed")
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, fontSize = 11.sp, color = Color(0xFF666666))
    }
}

// ── Today's Plan ──────────────────────────────────────────────────────────────

@Composable
fun TodaysPlanCard(
    slots: List<TodayPlanSlot>,
    onEditPlanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Plan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
                TextButton(onClick = onEditPlanClick) {
                    Text("Edit Plan >", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            if (slots.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No diet planned for today.\nTap Edit Plan to assign a diet.",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                slots.forEach { slot ->
                    TodayPlanSlotRow(slot = slot)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun TodayPlanSlotRow(slot: TodayPlanSlot) {
    val slotBgColor = when (slot.slotType.uppercase()) {
        "BREAKFAST" -> Color(0xFFFFF3E0)
        "LUNCH" -> Color(0xFFE3F2FD)
        "DINNER" -> Color(0xFFEDE7F6)
        "PRE_WORKOUT", "POST_WORKOUT" -> Color(0xFFE8F5E9)
        "EARLY_MORNING", "NOON", "MID_MORNING" -> Color(0xFFFFF9C4)
        "EVENING", "EVENING_SNACK", "POST_DINNER" -> Color(0xFFFCE4EC)
        else -> Color(0xFFE8F5E9)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Emoji in colored rounded box
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(slotBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(slot.emoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Slot name + meal name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                slot.slotDisplayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF1A1A1A)
            )
            if (slot.plannedMealName != null) {
                Text(
                    slot.plannedMealName,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    maxLines = 1
                )
            }
        }

        // Tick or unticked circle
        if (slot.isLogged) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Logged",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                // empty circle for "not yet logged"
            }
        }
    }
}

// ── Blood Glucose ─────────────────────────────────────────────────────────────

@Composable
fun BloodGlucoseCard(
    latestSugar: HealthMetric?,
    glucoseHistory: List<HealthMetric>,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔥", fontSize = 14.sp)
                    }
                    Text("Blood Glucose", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A))
                }
                TextButton(onClick = onDetailsClick) {
                    Text("Details >", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (latestSugar != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(latestSugar.value.toInt().toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text("mg/dL", fontSize = 14.sp, color = Color(0xFF888888), modifier = Modifier.padding(bottom = 6.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val inRange = latestSugar.value in 80.0..130.0
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (inRange) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = if (inRange) "In Range" else "Out of Range",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (inRange) PrimaryGreen else Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target Range", fontSize = 11.sp, color = Color(0xFF888888))
                        Text("80–130", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF444444))
                    }
                }

                if (glucoseHistory.size >= 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlucoseChart(history = glucoseHistory)
                }
            } else {
                Text(
                    "No glucose reading today.\nLog a reading in Health.",
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun GlucoseChart(history: List<HealthMetric>) {
    val entries = remember(history) {
        history.sortedBy { it.date }.mapIndexed { index, metric ->
            entryOf(index.toFloat(), metric.value.toFloat())
        }
    }
    val producer = remember(entries) { ChartEntryModelProducer(entries) }
    val labels = remember(history) { history.sortedBy { it.date }.map { it.date.takeLast(5) } }
    val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        labels.getOrElse(value.toInt()) { "" }
    }

    Chart(
        chart = lineChart(),
        chartModelProducer = producer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = formatter),
        modifier = Modifier.fillMaxWidth().height(140.dp)
    )
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
fun StatsRow(
    latestHba1c: HealthMetric?,
    latestWeight: HealthMetric?,
    dayStreak: Int,
    onNavigateToDiets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            emoji = "🏅",
            value = latestHba1c?.let { "${it.value}%" } ?: "--",
            label = "A1C Level",
            iconBg = Color(0xFFEDE7F6),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            emoji = "⚡",
            value = latestWeight?.let { "${it.value} lbs" } ?: "--",
            label = "Weight",
            iconBg = Color(0xFFE3F2FD),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            emoji = "🕐",
            value = "$dayStreak",
            label = "Streak",
            iconBg = Color(0xFFFFF8E1),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            emoji = "🥗",
            value = "Diets",
            label = "View All",
            iconBg = Color(0xFFE8F5E9),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToDiets
        )
    }
}

@Composable
fun StatCard(
    emoji: String,
    value: String,
    label: String,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 16.sp)
            }
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), maxLines = 1)
            Text(text = label, fontSize = 10.sp, color = Color(0xFF888888))
        }
    }
}
