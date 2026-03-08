package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.CustomMealSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomMealSlotDao {
    @Query("SELECT * FROM custom_meal_slots WHERE userId = :userId AND date = :date ORDER BY slotOrder ASC")
    fun getSlotsForDate(userId: Long, date: String): Flow<List<CustomMealSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: CustomMealSlot): Long

    @Query("DELETE FROM custom_meal_slots WHERE id = :id")
    suspend fun deleteById(id: Long)
}
