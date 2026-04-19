package com.mealplanplus.data.repository

import com.mealplanplus.data.local.ExerciseDao
import com.mealplanplus.data.local.WorkoutSessionDao
import com.mealplanplus.data.local.WorkoutSetDao
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.data.model.WorkoutSession
import com.mealplanplus.data.model.WorkoutSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao
) {
    fun getSessions(userId: String) = sessionDao.getSessions(userId)

    fun getSessionsForDate(userId: String, date: Long) =
        sessionDao.getSessionsForDate(userId, date)

    fun getSessionsInRange(userId: String, from: Long, to: Long) =
        sessionDao.getSessionsInRange(userId, from, to)

    suspend fun getSessionWithSets(id: Long) = sessionDao.getSessionWithSets(id)

    suspend fun createSession(session: WorkoutSession): Long = sessionDao.insert(session)

    suspend fun updateSession(session: WorkoutSession) = sessionDao.update(session)

    suspend fun deleteSession(session: WorkoutSession) = sessionDao.delete(session)

    fun getAllExercises() = exerciseDao.getAll()

    fun getExercisesByCategory(category: ExerciseCategory) =
        exerciseDao.getByCategory(category.name)

    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun addSet(set: WorkoutSet): Long = setDao.insert(set)

    suspend fun updateSet(set: WorkoutSet) = setDao.update(set)

    suspend fun deleteSet(set: WorkoutSet) = setDao.delete(set)

    suspend fun getUnsyncedSessions(userId: String) = sessionDao.getUnsyncedSessions(userId)
}
