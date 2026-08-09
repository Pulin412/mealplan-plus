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
    val notes: String? = null,
    // Social (V9): per-item share toggle + copy provenance.
    var isShared: Boolean = false,
    var copiedFromUid: String? = null,
    var copiedFromServerId: java.util.UUID? = null
) : SyncableEntity()

@Entity
@Table(name = "template_exercises")
class TemplateExercise(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val templateId: Long = 0,
    val exerciseId: Long = 0,
    val orderIndex: Int = 0,
    val notes: String? = null
)

/** One target set within a [TemplateExercise] — per-set reps + optional weight. */
@Entity
@Table(name = "template_exercise_sets")
class TemplateExerciseSet(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val templateExerciseId: Long = 0,
    val setNumber: Int = 0,
    val reps: Int? = null,
    val weightKg: Double? = null
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

/**
 * One free-text note per exercise within a session — a note-to-self (form cues, how it felt) kept
 * across repeats and surfaced on the next "copy last". Distinct from per-set [WorkoutSet.notes] and
 * the whole-workout [WorkoutSession.notes]; it is never copied when copying sets/reps.
 */
@Entity
@Table(name = "workout_session_exercise_notes")
class WorkoutSessionExerciseNote(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val sessionId: Long = 0,
    val exerciseId: Long = 0,
    val note: String? = null
)

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
