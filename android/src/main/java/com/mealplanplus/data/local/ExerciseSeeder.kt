package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private data class ExerciseJson(
    val name: String,
    val category: String,
    @SerializedName("muscleGroup") val muscleGroup: String?,
    val equipment: String?
)

@Singleton
class ExerciseSeeder @Inject constructor(
    private val exerciseDao: ExerciseDao
) {
    companion object {
        const val EXERCISE_DATA_VERSION = 2
        private val EXERCISE_DATA_VERSION_KEY = intPreferencesKey("exercise_data_version")
        private const val TAG = "ExerciseSeeder"
    }

    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val storedVersion = context.dataStore.data.first()[EXERCISE_DATA_VERSION_KEY] ?: 0
        val actualCount = exerciseDao.getSystemExerciseCount()
        if (storedVersion == EXERCISE_DATA_VERSION && actualCount > 0) {
            Log.d(TAG, "Exercises up-to-date (v$EXERCISE_DATA_VERSION, count=$actualCount), skipping")
            return@withContext
        }
        if (storedVersion == EXERCISE_DATA_VERSION && actualCount == 0) {
            Log.w(TAG, "Exercise flag says v$EXERCISE_DATA_VERSION but DB is empty — re-seeding")
        }

        Log.d(TAG, "Seeding exercises (stored=$storedVersion, current=$EXERCISE_DATA_VERSION)")
        val json = context.assets.open("exercises.json").bufferedReader().readText()
        val type = object : TypeToken<List<ExerciseJson>>() {}.type
        val items: List<ExerciseJson> = Gson().fromJson(json, type)

        val exercises = items.map { e ->
            Exercise(
                name = e.name,
                category = ExerciseCategory.valueOf(e.category),
                muscleGroup = e.muscleGroup,
                equipment = e.equipment,
                isSystem = true
            )
        }
        exerciseDao.upsertAll(exercises)
        Log.d(TAG, "Seeded ${exercises.size} exercises")

        context.dataStore.edit { prefs ->
            prefs[EXERCISE_DATA_VERSION_KEY] = EXERCISE_DATA_VERSION
        }
    }
}
