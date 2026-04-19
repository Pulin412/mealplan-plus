package com.mealplanplus.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class ExerciseCategory { STRENGTH, CARDIO, FLEXIBILITY, OTHER }

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val muscleGroup: String? = null,
    val equipment: String? = null,
    val description: String? = null,
    val isSystem: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
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
