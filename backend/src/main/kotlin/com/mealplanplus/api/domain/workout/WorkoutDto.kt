package com.mealplanplus.api.domain.workout

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class WorkoutSetDto(
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

data class WorkoutSessionDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val date: LocalDate? = null,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val sets: List<WorkoutSetDto> = emptyList(),
    val updatedAt: Instant? = null
)

data class ExerciseDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val name: String = "",
    val category: String = "STRENGTH",
    val muscleGroup: String? = null,
    val equipment: String? = null,
    val description: String? = null,
    val videoLink: String? = null,
    val isSystem: Boolean = false,
    val updatedAt: Instant? = null
)

fun WorkoutSet.toDto() = WorkoutSetDto(id, sessionId, exerciseId, setNumber, reps, weightKg, durationSeconds, distanceMeters, notes)
fun WorkoutSession.toDto(sets: List<WorkoutSet>) = WorkoutSessionDto(id, serverId, firebaseUid, name, date, durationMinutes, notes, isCompleted, sets.map { it.toDto() }, updatedAt)
fun Exercise.toDto() = ExerciseDto(id, serverId, name, category, muscleGroup, equipment, description, videoLink, isSystem, updatedAt)

// ── Template DTOs ─────────────────────────────────────────────────────────────

data class TemplateExerciseDto(
    val id: Long = 0,
    val templateId: Long = 0,
    val exerciseId: Long = 0,
    val orderIndex: Int = 0,
    val targetSets: Int = 3,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val notes: String? = null,
    // Denormalised for convenience — populated from Exercise table
    val exerciseName: String = "",
    val exerciseCategory: String = "STRENGTH"
)

data class WorkoutTemplateDto(
    val id: Long = 0,
    val firebaseUid: String = "",
    val name: String = "",
    val category: String = "STRENGTH",
    val notes: String? = null,
    val exercises: List<TemplateExerciseDto> = emptyList()
)

fun TemplateExercise.toDto(exercise: Exercise?) = TemplateExerciseDto(
    id, templateId, exerciseId, orderIndex, targetSets, targetReps, targetWeightKg, notes,
    exercise?.name ?: "", exercise?.category ?: "STRENGTH"
)
fun WorkoutTemplate.toDto(exercises: List<TemplateExerciseDto>) =
    WorkoutTemplateDto(id, firebaseUid, name, category, notes, exercises)
