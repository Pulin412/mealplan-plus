package com.mealplanplus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mealplanplus.data.model.WorkoutTemplate
import com.mealplanplus.data.model.WorkoutTemplateExercise
import com.mealplanplus.data.model.WorkoutTemplateSet
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE userId = :userId OR userId = '' ORDER BY name ASC")
    fun getTemplatesForUser(userId: String): Flow<List<WorkoutTemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId LIMIT 1")
    suspend fun getTemplateWithExercises(templateId: Long): WorkoutTemplateWithExercises?

    @Insert
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)

    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplateExercises(exercises: List<WorkoutTemplateExercise>)

    @Query("DELETE FROM workout_template_exercises WHERE templateId = :templateId")
    suspend fun clearTemplateExercises(templateId: Long)

    @Delete
    suspend fun deleteTemplateExercise(exercise: WorkoutTemplateExercise)

    // ── Per-set pyramid data ──────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateSets(sets: List<WorkoutTemplateSet>)

    @Query("DELETE FROM workout_template_sets WHERE templateExerciseId = :templateExerciseId")
    suspend fun clearSetsForExercise(templateExerciseId: Long)

    @Query("""
        DELETE FROM workout_template_sets
        WHERE templateExerciseId IN (
            SELECT id FROM workout_template_exercises WHERE templateId = :templateId
        )
    """)
    suspend fun clearSetsForTemplate(templateId: Long)
}
