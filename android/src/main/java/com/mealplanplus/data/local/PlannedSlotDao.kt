package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.PlannedSlot
import com.mealplanplus.data.model.PlannedSlotFood
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedSlotDao {

    // ─── Planned slots ────────────────────────────────────────────────────────

    @Query("SELECT * FROM planned_slots WHERE userId = :userId AND date = :date ORDER BY slotType")
    fun getSlotsForDate(userId: Long, date: Long): Flow<List<PlannedSlot>>

    @Query("SELECT * FROM planned_slots WHERE userId = :userId AND date = :date ORDER BY slotType")
    suspend fun getSlotsForDateOnce(userId: Long, date: Long): List<PlannedSlot>

    @Query("SELECT * FROM planned_slots WHERE userId = :userId AND date = :date AND slotType = :slotType LIMIT 1")
    suspend fun getSlot(userId: Long, date: Long, slotType: String): PlannedSlot?

    @Query("SELECT * FROM planned_slots WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    fun getSlotsInRange(userId: Long, startDate: Long, endDate: Long): Flow<List<PlannedSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlot(slot: PlannedSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlots(slots: List<PlannedSlot>)

    @Query("DELETE FROM planned_slots WHERE userId = :userId AND date = :date AND slotType = :slotType")
    suspend fun deleteSlot(userId: Long, date: Long, slotType: String)

    @Query("DELETE FROM planned_slots WHERE userId = :userId AND date = :date")
    suspend fun deleteSlotsForDate(userId: Long, date: Long)

    // ─── Planned slot foods ───────────────────────────────────────────────────

    @Query("SELECT * FROM planned_slot_foods WHERE plannedSlotId = :slotId")
    suspend fun getFoodsForSlot(slotId: Long): List<PlannedSlotFood>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlotFood(food: PlannedSlotFood): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlotFoods(foods: List<PlannedSlotFood>)

    @Query("DELETE FROM planned_slot_foods WHERE plannedSlotId = :slotId AND foodId = :foodId")
    suspend fun deleteSlotFood(slotId: Long, foodId: Long)

    @Query("DELETE FROM planned_slot_foods WHERE plannedSlotId = :slotId")
    suspend fun clearSlotFoods(slotId: Long)
}
