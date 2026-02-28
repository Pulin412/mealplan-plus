package com.mealplanplus.api.domain.log

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "daily_logs")
class DailyLog(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val date: LocalDate = LocalDate.now(),
    val notes: String? = null
) : SyncableEntity()

@Entity
@Table(name = "logged_foods")
class LoggedFood(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val dailyLogId: Long = 0,
    val foodId: Long = 0,
    val mealSlot: String = "Lunch",
    val quantity: Double = 0.0,
    val unit: String = "GRAM"
)
