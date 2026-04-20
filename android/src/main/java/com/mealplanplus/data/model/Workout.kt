package com.mealplanplus.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Kept for legacy references only; category is now stored as a plain String. */
enum class ExerciseCategory { STRENGTH, CARDIO, FLEXIBILITY, OTHER }
enum class WorkoutTemplateCategory { STRENGTH, CARDIO, FLEXIBILITY, MIXED }

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,            // stored as plain text, e.g. "STRENGTH", "CARDIO", custom
    val muscleGroup: String? = null,
    val equipment: String? = null,
    val description: String? = null,
    val videoLink: String? = null,
    val isSystem: Boolean = true,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Workout Templates (like Diet — a reusable plan) ──────────────────────────

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val category: WorkoutTemplateCategory = WorkoutTemplateCategory.STRENGTH,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workout_template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId"), Index("exerciseId")]
)
data class WorkoutTemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val orderIndex: Int = 0,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val targetDurationSeconds: Int? = null,
    val notes: String? = null
)

/** One row per set in a workout template exercise (supports pyramid / varied sets). */
@Entity(
    tableName = "workout_template_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutTemplateExercise::class,
        parentColumns = ["id"],
        childColumns = ["templateExerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("templateExerciseId")]
)
data class WorkoutTemplateSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateExerciseId: Long,
    val setIndex: Int = 0,
    val reps: Int? = null,
    val weightKg: Double? = null
)

data class WorkoutTemplateExerciseWithDetails(
    @Embedded val templateExercise: WorkoutTemplateExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "templateExerciseId")
    val plannedSets: List<WorkoutTemplateSet>
)

data class WorkoutTemplateWithExercises(
    @Embedded val template: WorkoutTemplate,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId",
        entity = WorkoutTemplateExercise::class
    )
    val exercises: List<WorkoutTemplateExerciseWithDetails>
)

// ── Planned Workouts (calendar integration — like DayPlan for meals) ─────────

@Entity(
    tableName = "planned_workouts",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class PlannedWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: Long,
    val templateId: Long
)

data class PlannedWorkoutWithTemplate(
    @Embedded val plannedWorkout: PlannedWorkout,
    @Relation(
        parentColumn = "templateId",
        entityColumn = "id",
        entity = WorkoutTemplate::class
    )
    val template: WorkoutTemplateWithExercises
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val date: Long,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val serverId: String? = null,
    val syncedAt: Long? = null
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val notes: String? = null
)

data class WorkoutSetWithExercise(
    @Embedded val workoutSet: WorkoutSet,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise
)

data class WorkoutSessionWithSets(
    @Embedded val session: WorkoutSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
        entity = WorkoutSet::class
    )
    val sets: List<WorkoutSetWithExercise>
)
