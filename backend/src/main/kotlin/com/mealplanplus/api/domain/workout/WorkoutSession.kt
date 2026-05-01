package com.mealplanplus.api.domain.workout

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*
import java.time.LocalDate

// ── Workout Template ──────────────────────────────────────────────────────────

@Entity
@Table(name = "workout_templates")
class WorkoutTemplate(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val name: String = "",
    val category: String = "STRENGTH",
    val notes: String? = null
) : SyncableEntity()

@Entity
@Table(name = "template_exercises")
class TemplateExercise(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val templateId: Long = 0,
    val exerciseId: Long = 0,
    val orderIndex: Int = 0,
    val targetSets: Int = 3,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val notes: String? = null
)

@Entity
@Table(name = "workout_sessions")
class WorkoutSession(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val name: String = "",
    val date: LocalDate = LocalDate.now(),
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false
) : SyncableEntity()

@Entity
@Table(name = "workout_sets")
class WorkoutSet(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val sessionId: Long = 0,
    val exerciseId: Long = 0,
    val setNumber: Int = 0,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val notes: String? = null
)
