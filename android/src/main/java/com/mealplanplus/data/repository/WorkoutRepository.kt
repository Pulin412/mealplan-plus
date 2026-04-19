package com.mealplanplus.data.repository

import com.mealplanplus.data.local.ExerciseDao
import com.mealplanplus.data.local.PlannedWorkoutDao
import com.mealplanplus.data.local.WorkoutSessionDao
import com.mealplanplus.data.local.WorkoutSetDao
import com.mealplanplus.data.local.WorkoutTemplateDao
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.data.model.PlannedWorkout
import com.mealplanplus.data.model.WorkoutSession
import com.mealplanplus.data.model.WorkoutSet
import com.mealplanplus.data.model.WorkoutTemplate
import com.mealplanplus.data.model.WorkoutTemplateExercise
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao,
    private val templateDao: WorkoutTemplateDao,
    private val plannedWorkoutDao: PlannedWorkoutDao
) {
    // ── Sessions ─────────────────────────────────────────────────────────────
    fun getSessions(userId: String) = sessionDao.getSessions(userId)
    fun getSessionsForDate(userId: String, date: Long) = sessionDao.getSessionsForDate(userId, date)
    fun getSessionsInRange(userId: String, from: Long, to: Long) = sessionDao.getSessionsInRange(userId, from, to)
    suspend fun getSessionWithSets(id: Long) = sessionDao.getSessionWithSets(id)
    suspend fun createSession(session: WorkoutSession): Long = sessionDao.insert(session)
    suspend fun updateSession(session: WorkoutSession) = sessionDao.update(session)
    suspend fun deleteSession(session: WorkoutSession) = sessionDao.delete(session)
    suspend fun getUnsyncedSessions(userId: String) = sessionDao.getUnsyncedSessions(userId)

    // ── Sets ─────────────────────────────────────────────────────────────────
    suspend fun addSet(set: WorkoutSet): Long = setDao.insert(set)
    suspend fun updateSet(set: WorkoutSet) = setDao.update(set)
    suspend fun deleteSet(set: WorkoutSet) = setDao.delete(set)

    // ── Exercises ─────────────────────────────────────────────────────────────
    fun getAllExercisesForUser(userId: String) = exerciseDao.getAllForUser(userId)
    fun getAllExercises() = exerciseDao.getAll()
    fun getExercisesByCategory(category: ExerciseCategory) = exerciseDao.getByCategory(category.name)
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getById(id)
    suspend fun getExerciseByName(name: String): Exercise? = exerciseDao.getByName(name)
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)
    suspend fun upsertSystemExercises(exercises: List<Exercise>) = exerciseDao.upsertAll(exercises)

    // ── Templates ─────────────────────────────────────────────────────────────
    fun getTemplatesForUser(userId: String) = templateDao.getTemplatesForUser(userId)
    suspend fun getTemplateWithExercises(id: Long) = templateDao.getTemplateWithExercises(id)
    suspend fun insertTemplate(template: WorkoutTemplate): Long = templateDao.insertTemplate(template)
    suspend fun upsertTemplateExercises(exercises: List<WorkoutTemplateExercise>) =
        templateDao.upsertTemplateExercises(exercises)

    suspend fun saveTemplate(
        template: WorkoutTemplate,
        exercises: List<WorkoutTemplateExercise>
    ): Long {
        val templateId = if (template.id == 0L) {
            templateDao.insertTemplate(template)
        } else {
            templateDao.updateTemplate(template.copy(updatedAt = System.currentTimeMillis()))
            template.id
        }
        templateDao.clearTemplateExercises(templateId)
        templateDao.upsertTemplateExercises(exercises.map { it.copy(templateId = templateId) })
        return templateId
    }

    suspend fun deleteTemplate(template: WorkoutTemplate) = templateDao.deleteTemplate(template)

    // ── Planned workouts ──────────────────────────────────────────────────────
    fun getPlannedForDate(userId: String, date: Long) = plannedWorkoutDao.getPlannedForDate(userId, date)
    fun getPlannedInRange(userId: String, from: Long, to: Long) = plannedWorkoutDao.getPlannedInRange(userId, from, to)
    suspend fun planWorkout(plannedWorkout: PlannedWorkout) = plannedWorkoutDao.plan(plannedWorkout)
    suspend fun unplanWorkout(userId: String, date: Long, templateId: Long) =
        plannedWorkoutDao.unplanByKey(userId, date, templateId)
}
