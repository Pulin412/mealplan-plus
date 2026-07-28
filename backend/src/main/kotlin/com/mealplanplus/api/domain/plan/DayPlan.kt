package com.mealplanplus.api.domain.plan

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "day_plans",
    uniqueConstraints = [UniqueConstraint(columnNames = ["firebase_uid", "date"])]
)
class DayPlan(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val date: LocalDate = LocalDate.now(),
    val dietId: Long? = null
) : SyncableEntity()

@Entity
@Table(name = "planned_workouts")
class PlannedWorkout(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val dayPlanId: Long = 0,
    val workoutTemplateId: Long? = null,
    val activityName: String = ""
)
