package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.PlannedSlotDao
import com.mealplanplus.data.model.*
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannedSlotRepository @Inject constructor(
    private val plannedSlotDao: PlannedSlotDao,
    private val mealRepository: MealRepository,
    private val foodDao: FoodDao,
    @ApplicationContext private val context: Context
) {
    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    // ─── Slot reads ───────────────────────────────────────────────────────────

    fun getSlotsForDate(date: LocalDate): Flow<List<PlannedSlot>> =
        plannedSlotDao.getSlotsForDate(getCurrentUserId(), date.toEpochMs())

    suspend fun getSlotsForDateOnce(date: LocalDate): List<PlannedSlot> =
        plannedSlotDao.getSlotsForDateOnce(getCurrentUserId(), date.toEpochMs())

    suspend fun getSlot(date: LocalDate, slotType: String): PlannedSlot? =
        plannedSlotDao.getSlot(getCurrentUserId(), date.toEpochMs(), slotType)

    fun getSlotsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<PlannedSlot>> =
        plannedSlotDao.getSlotsInRange(getCurrentUserId(), startDate.toEpochMs(), endDate.toEpochMs())

    /**
     * Resolves a slot's full content: the assigned meal (if any) + ad-hoc foods.
     */
    suspend fun getSlotContent(date: LocalDate, slotType: String): PlannedSlotWithContent? {
        val slot = getSlot(date, slotType) ?: return null
        val meal = slot.mealId?.let { mealRepository.getMealWithFoods(it) }
        val adHocFoods = plannedSlotDao.getFoodsForSlot(slot.id).mapNotNull { psf ->
            foodDao.getFoodById(psf.foodId)?.let { food ->
                PlannedSlotFoodWithDetails(psf, food)
            }
        }
        return PlannedSlotWithContent(slot, meal, adHocFoods)
    }

    // ─── Slot writes ──────────────────────────────────────────────────────────

    /**
     * Assigns a meal to a slot on a given date.
     * Creates the [PlannedSlot] row (or replaces if it already exists).
     */
    suspend fun assignMealToSlot(
        date: LocalDate,
        slotType: String,
        mealId: Long,
        sourceDietId: Long? = null,
        instructions: String? = null
    ) {
        val userId = getCurrentUserId()
        plannedSlotDao.upsertSlot(
            PlannedSlot(
                userId = userId,
                date = date.toEpochMs(),
                slotType = slotType,
                mealId = mealId,
                sourceDietId = sourceDietId,
                instructions = instructions
            )
        )
    }

    /**
     * Eagerly expands a [DietWithMeals] into [PlannedSlot] rows for a given date.
     * Called when "Apply Diet" is tapped on the calendar.
     * Clears any existing planned slots for that date first.
     */
    suspend fun applyDiet(date: LocalDate, diet: DietWithMeals) {
        val userId = getCurrentUserId()
        val epochMs = date.toEpochMs()
        plannedSlotDao.deleteSlotsForDate(userId, epochMs)
        val slots = diet.meals.map { (slotType, mealWithFoods) ->
            PlannedSlot(
                userId = userId,
                date = epochMs,
                slotType = slotType,
                mealId = mealWithFoods?.meal?.id,
                sourceDietId = diet.diet.id,
                instructions = diet.instructions[slotType]
            )
        }
        plannedSlotDao.upsertSlots(slots)
    }

    /** Removes the planned slot (and its ad-hoc foods via CASCADE) for a specific slot on a date. */
    suspend fun removeSlot(date: LocalDate, slotType: String) {
        plannedSlotDao.deleteSlot(getCurrentUserId(), date.toEpochMs(), slotType)
    }

    /** Removes all planned slots for a date (e.g. when a day plan is deleted). */
    suspend fun removeSlotsForDate(date: LocalDate) {
        plannedSlotDao.deleteSlotsForDate(getCurrentUserId(), date.toEpochMs())
    }

    // ─── Ad-hoc food writes ───────────────────────────────────────────────────

    /**
     * Adds an individual food directly to a slot.
     * If no [PlannedSlot] row exists for this slot yet, creates one with mealId = null.
     */
    suspend fun addFoodToSlot(
        date: LocalDate,
        slotType: String,
        foodId: Long,
        quantity: Double,
        unit: FoodUnit = FoodUnit.GRAM,
        notes: String? = null
    ) {
        val userId = getCurrentUserId()
        val epochMs = date.toEpochMs()

        // Ensure slot row exists
        val slotId = plannedSlotDao.getSlot(userId, epochMs, slotType)?.id
            ?: plannedSlotDao.upsertSlot(
                PlannedSlot(userId = userId, date = epochMs, slotType = slotType, mealId = null)
            )

        plannedSlotDao.upsertSlotFood(
            PlannedSlotFood(plannedSlotId = slotId, foodId = foodId, quantity = quantity, unit = unit, notes = notes)
        )
    }

    suspend fun removeAdHocFood(date: LocalDate, slotType: String, foodId: Long) {
        val slot = getSlot(date, slotType) ?: return
        plannedSlotDao.deleteSlotFood(slot.id, foodId)
    }
}
