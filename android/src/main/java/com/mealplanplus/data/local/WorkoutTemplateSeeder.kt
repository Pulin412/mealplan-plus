package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mealplanplus.data.model.WorkoutTemplate
import com.mealplanplus.data.model.WorkoutTemplateCategory
import com.mealplanplus.data.model.WorkoutTemplateExercise
import com.mealplanplus.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds personal workout templates (Beginner + Advanced regimes) for the owner's 3 accounts only.
 * New users who sign up fresh will NOT receive these templates.
 *
 * Idempotent: guarded by a per-user DataStore boolean flag (v1).
 * Called from WorkoutViewModel.init() so it fires the first time a user opens the Workouts tab.
 * Exercises must already be seeded (ExerciseSeeder runs first at app startup).
 */
@Singleton
class WorkoutTemplateSeeder @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao
) {
    companion object {
        // Bump this whenever the template list changes to force a re-seed.
        private const val VERSION = 2
        private const val TAG = "WorkoutTemplateSeeder"
        private fun flagKey(uid: String) = booleanPreferencesKey("workout_templates_v${VERSION}_$uid")

        /** Only these accounts receive the personal workout templates. */
        private val OWNER_EMAILS = setOf(
            "pulins412@gmail.com",
            "rajni@gmail.com",
            "pulin4122001@gmail.com"
        )
    }

    suspend fun seedIfNeeded(context: Context, firebaseUid: String) = withContext(Dispatchers.IO) {
        if (firebaseUid.isBlank()) return@withContext

        // Only owner accounts get the personal templates
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
        if (email !in OWNER_EMAILS) {
            Log.d(TAG, "User $email is not an owner account — skipping personal workout seed")
            return@withContext
        }

        val key = flagKey(firebaseUid)
        if (context.dataStore.data.first()[key] == true) {
            Log.d(TAG, "Workout templates already seeded for $email (v$VERSION), skipping")
            return@withContext
        }

        // Guard: exercises must be seeded first. If they're not ready yet, abort WITHOUT
        // setting the flag so we'll retry on the next app launch / screen visit.
        val exerciseCount = exerciseDao.getSystemExerciseCount()
        if (exerciseCount == 0) {
            Log.w(TAG, "Exercises not seeded yet (count=0) — will retry next time")
            return@withContext
        }

        Log.d(TAG, "Seeding personal workout templates for $email ($exerciseCount exercises available)")
        seedAllTemplates(firebaseUid)
        context.dataStore.edit { it[key] = true }
        Log.d(TAG, "Seeded ${TEMPLATES.size} workout templates for $email")
    }

    private suspend fun seedAllTemplates(userId: String) {
        // Remove any previously seeded (empty) templates so we start fresh
        val existing = workoutTemplateDao.getTemplatesForUser(userId).first()
        existing.forEach { twx -> workoutTemplateDao.deleteTemplate(twx.template) }
        if (existing.isNotEmpty()) {
            Log.d(TAG, "Removed ${existing.size} stale empty templates before re-seeding")
        }

        // Build exercise name → ID lookup once
        val exerciseMap = mutableMapOf<String, Long>()

        TEMPLATES.forEach { def ->
            val templateId = workoutTemplateDao.insertTemplate(
                WorkoutTemplate(
                    userId = userId,
                    name = def.name,
                    category = def.category,
                    notes = def.notes
                )
            )

            val templateExercises = def.exercises.mapIndexedNotNull { idx, exDef ->
                val exerciseId = exerciseMap.getOrPut(exDef.name) {
                    exerciseDao.getByName(exDef.name)?.id ?: run {
                        Log.w(TAG, "Exercise not found in DB: '${exDef.name}' — skipping")
                        -1L
                    }
                }
                if (exerciseId < 0) return@mapIndexedNotNull null
                WorkoutTemplateExercise(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderIndex = idx,
                    targetSets = exDef.sets,
                    targetReps = null,
                    targetWeightKg = null,
                    notes = null
                )
            }

            if (templateExercises.isNotEmpty()) {
                workoutTemplateDao.upsertTemplateExercises(templateExercises)
                Log.d(TAG, "  Created '${def.name}' with ${templateExercises.size} exercises")
            } else {
                Log.w(TAG, "  No exercises resolved for '${def.name}'")
            }
        }
    }

    // ── Template definitions ────────────────────────────────────────────────────

    private data class ExerciseDef(val name: String, val sets: Int = 3)

    private data class TemplateDef(
        val name: String,
        val category: WorkoutTemplateCategory,
        val notes: String?,
        val exercises: List<ExerciseDef>
    )

    private val TEMPLATES = listOf(

        // ── BEGINNER ────────────────────────────────────────────────────────────

        TemplateDef(
            name = "Beginner — Chest & Bicep",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Monday · Beginner regime",
            exercises = listOf(
                ExerciseDef("Bench Press"),
                ExerciseDef("Incline Dumbbell Press"),
                ExerciseDef("Flat Dumbbell Press"),
                ExerciseDef("Bicep Curl"),
                ExerciseDef("Cable Bicep Curl"),
                ExerciseDef("Hammer Curl")
            )
        ),

        TemplateDef(
            name = "Beginner — Back & Triceps",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Tuesday · Beginner regime",
            exercises = listOf(
                ExerciseDef("Deadlift"),
                ExerciseDef("Lat Pulldown"),
                ExerciseDef("Reverse Grip Lat Pulldown"),
                ExerciseDef("Seated Cable Row"),
                ExerciseDef("Tricep Bar Pulldown"),
                ExerciseDef("Tricep Rope Pulldown"),
                ExerciseDef("Skull Crushers")
            )
        ),

        TemplateDef(
            name = "Beginner — Shoulders & Legs",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Wednesday · Beginner regime",
            exercises = listOf(
                ExerciseDef("Seated Dumbbell Press"),
                ExerciseDef("Lateral Raises"),
                ExerciseDef("Front Raises"),
                ExerciseDef("Squat"),
                ExerciseDef("Walking Lunges"),
                ExerciseDef("Leg Extension")
            )
        ),

        // ── ADVANCED ────────────────────────────────────────────────────────────

        TemplateDef(
            name = "Advanced — Biceps & Triceps",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Monday · Advanced regime",
            exercises = listOf(
                ExerciseDef("Bicep Curl"),
                ExerciseDef("Hammer Curl"),
                ExerciseDef("Inward Hammer Curl"),
                ExerciseDef("Cable Bicep Curl"),
                ExerciseDef("Reverse Grip Cable Curl"),
                ExerciseDef("Tricep Bar Pulldown"),
                ExerciseDef("Tricep Rope Pulldown"),
                ExerciseDef("Skull Crushers")
            )
        ),

        TemplateDef(
            name = "Advanced — Chest",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Tuesday · Advanced regime",
            exercises = listOf(
                ExerciseDef("Push-Up"),
                ExerciseDef("Bench Press"),
                ExerciseDef("Incline Bench Press"),
                ExerciseDef("Decline Bench Press"),
                ExerciseDef("Cable Crossover")
            )
        ),

        TemplateDef(
            name = "Advanced — Shoulders",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Wednesday · Advanced regime",
            exercises = listOf(
                ExerciseDef("Seated Dumbbell Press"),
                ExerciseDef("Front Raises"),
                ExerciseDef("Lateral Raises")
            )
        ),

        TemplateDef(
            name = "Advanced — Back",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Thursday · Advanced regime",
            exercises = listOf(
                ExerciseDef("Lat Pulldown"),
                ExerciseDef("Deadlift"),
                ExerciseDef("Reverse Grip Lat Pulldown"),
                ExerciseDef("Seated Cable Row")
            )
        ),

        TemplateDef(
            name = "Advanced — Legs",
            category = WorkoutTemplateCategory.STRENGTH,
            notes = "Friday · Advanced regime",
            exercises = listOf(
                ExerciseDef("Squat"),
                ExerciseDef("Leg Extension"),
                ExerciseDef("Walking Lunges")
            )
        )
    )
}
