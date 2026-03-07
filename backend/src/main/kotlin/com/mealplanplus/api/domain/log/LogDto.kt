package com.mealplanplus.api.domain.log

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class LoggedFoodDto(
    val id: Long = 0,
    val dailyLogId: Long = 0,
    val foodId: Long = 0,
    val mealSlot: String = "Lunch",
    val quantity: Double = 0.0,
    val unit: String = "GRAM"
)

data class DailyLogDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val date: LocalDate? = null,
    val notes: String? = null,
    val loggedFoods: List<LoggedFoodDto> = emptyList(),
    val updatedAt: Instant? = null
)

fun LoggedFood.toDto() = LoggedFoodDto(id, dailyLogId, foodId, mealSlot, quantity, unit)
fun DailyLog.toDto(loggedFoods: List<LoggedFood>) =
    DailyLogDto(id, serverId, firebaseUid, date, notes, loggedFoods.map { it.toDto() }, updatedAt)
