package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.*
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.GroceryCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroceryRepository @Inject constructor(
    private val groceryDao: GroceryDao,
    private val planDao: PlanDao,
    private val dietRepository: DietRepository,
    @ApplicationContext private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    // ===== List operations =====

    fun getListsByUser(): Flow<List<GroceryList>> = groceryDao.getListsByUser(getCurrentUserId())

    fun getListWithItems(listId: Long): Flow<GroceryListWithItems?> = groceryDao.getListWithItems(listId)

    suspend fun getListWithItemsOnce(listId: Long): GroceryListWithItems? = groceryDao.getListWithItemsOnce(listId)

    suspend fun createEmptyList(name: String): Long {
        val list = GroceryList(
            userId = getCurrentUserId(),
            name = name
        )
        return groceryDao.insertList(list)
    }

    suspend fun deleteList(list: GroceryList) = groceryDao.deleteList(list)

    suspend fun deleteListById(listId: Long) = groceryDao.deleteListById(listId)

    // ===== Item operations =====

    suspend fun toggleItemChecked(itemId: Long, checked: Boolean) {
        groceryDao.setItemChecked(itemId, checked)
    }

    suspend fun uncheckAllItems(listId: Long) {
        groceryDao.uncheckAllItems(listId)
    }

    suspend fun deleteItem(itemId: Long) {
        groceryDao.deleteItemById(itemId)
    }

    suspend fun updateItemQuantity(itemId: Long, quantity: Double) {
        groceryDao.updateItemQuantity(itemId, quantity)
    }

    suspend fun addCustomItem(listId: Long, name: String, quantity: Double, unit: FoodUnit, category: String? = GroceryCategory.OTHER): Long {
        val item = GroceryItem(
            listId = listId,
            customName = name,
            quantity = quantity,
            unit = unit,
            category = category
        )
        return groceryDao.insertItem(item)
    }

    // ===== Generate from date range =====

    /**
     * Generate grocery list from planned meals for given dates.
     * Combines quantities for same food items and assigns food categories.
     */
    suspend fun generateFromDateRange(
        name: String,
        dates: List<LocalDate>
    ): Long {
        val userId = getCurrentUserId()
        val dateStrings = dates.map { it.format(dateFormatter) }
        val startDate = dates.minOrNull()?.format(dateFormatter)
        val endDate = dates.maxOrNull()?.format(dateFormatter)

        val list = GroceryList(userId = userId, name = name, startDate = startDate, endDate = endDate)
        val listId = groceryDao.insertList(list)

        val aggregatedFoods = mutableMapOf<AggregationKey, AggregatedFood>()

        for (dateStr in dateStrings) {
            val plan = planDao.getPlanForDate(userId, dateStr)
            val dietId = plan?.dietId ?: continue
            val dietWithMeals = dietRepository.getDietWithMeals(dietId) ?: continue

            for ((_, mealWithFoods) in dietWithMeals.meals) {
                if (mealWithFoods == null) continue
                for (item in mealWithFoods.items) {
                    val key = AggregationKey(item.food.id, item.mealFoodItem.unit)
                    val existing = aggregatedFoods[key]
                    if (existing != null) {
                        existing.quantity += item.mealFoodItem.quantity
                    } else {
                        aggregatedFoods[key] = AggregatedFood(
                            foodId = item.food.id,
                            foodName = item.food.name,
                            quantity = item.mealFoodItem.quantity,
                            unit = item.mealFoodItem.unit
                        )
                    }
                }
            }
        }

        val groceryItems = aggregatedFoods.values
            .sortedBy { it.foodName.lowercase() }
            .mapIndexed { index, agg ->
                GroceryItem(
                    listId = listId,
                    foodId = agg.foodId,
                    quantity = agg.quantity,
                    unit = agg.unit,
                    sortOrder = index,
                    category = GroceryCategory.categorize(agg.foodName)
                )
            }

        if (groceryItems.isNotEmpty()) {
            groceryDao.insertItems(groceryItems)
        }

        return listId
    }

    /**
     * Regenerate items for an existing list from its original date range.
     * Deletes current items and re-generates from the same dates.
     */
    suspend fun regenerateList(listId: Long) {
        val listWithItems = groceryDao.getListWithItemsOnce(listId) ?: return
        val list = listWithItems.list
        val startDateStr = list.startDate ?: return
        val endDateStr = list.endDate ?: startDateStr

        val start = LocalDate.parse(startDateStr, dateFormatter)
        val end = LocalDate.parse(endDateStr, dateFormatter)
        val dates = generateSequence(start) { d -> if (d < end) d.plusDays(1) else null }.toList()

        val userId = getCurrentUserId()
        val dateStrings = dates.map { it.format(dateFormatter) }
        val aggregatedFoods = mutableMapOf<AggregationKey, AggregatedFood>()

        for (dateStr in dateStrings) {
            val plan = planDao.getPlanForDate(userId, dateStr)
            val dietId = plan?.dietId ?: continue
            val dietWithMeals = dietRepository.getDietWithMeals(dietId) ?: continue

            for ((_, mealWithFoods) in dietWithMeals.meals) {
                if (mealWithFoods == null) continue
                for (item in mealWithFoods.items) {
                    val key = AggregationKey(item.food.id, item.mealFoodItem.unit)
                    val existing = aggregatedFoods[key]
                    if (existing != null) {
                        existing.quantity += item.mealFoodItem.quantity
                    } else {
                        aggregatedFoods[key] = AggregatedFood(
                            foodId = item.food.id,
                            foodName = item.food.name,
                            quantity = item.mealFoodItem.quantity,
                            unit = item.mealFoodItem.unit
                        )
                    }
                }
            }
        }

        // Delete existing items from plan (keep custom items)
        val existingItems = listWithItems.items
        existingItems.filter { it.item.foodId != null }.forEach { groceryDao.deleteItemById(it.item.id) }

        // Insert new items
        val newItems = aggregatedFoods.values
            .sortedBy { it.foodName.lowercase() }
            .mapIndexed { index, agg ->
                GroceryItem(
                    listId = listId,
                    foodId = agg.foodId,
                    quantity = agg.quantity,
                    unit = agg.unit,
                    sortOrder = index,
                    category = GroceryCategory.categorize(agg.foodName)
                )
            }

        if (newItems.isNotEmpty()) {
            groceryDao.insertItems(newItems)
        }

        // Update updatedAt timestamp
        groceryDao.updateList(list.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Generate grocery list from a single diet
     */
    suspend fun generateFromDiet(name: String, dietId: Long): Long {
        val userId = getCurrentUserId()
        val dietWithMeals = dietRepository.getDietWithMeals(dietId)
            ?: throw IllegalArgumentException("Diet not found")

        val list = GroceryList(userId = userId, name = name)
        val listId = groceryDao.insertList(list)

        val aggregatedFoods = mutableMapOf<AggregationKey, AggregatedFood>()

        for ((_, mealWithFoods) in dietWithMeals.meals) {
            if (mealWithFoods == null) continue
            for (item in mealWithFoods.items) {
                val key = AggregationKey(item.food.id, item.mealFoodItem.unit)
                val existing = aggregatedFoods[key]
                if (existing != null) {
                    existing.quantity += item.mealFoodItem.quantity
                } else {
                    aggregatedFoods[key] = AggregatedFood(
                        foodId = item.food.id,
                        foodName = item.food.name,
                        quantity = item.mealFoodItem.quantity,
                        unit = item.mealFoodItem.unit
                    )
                }
            }
        }

        val groceryItems = aggregatedFoods.values
            .sortedBy { it.foodName.lowercase() }
            .mapIndexed { index, agg ->
                GroceryItem(
                    listId = listId,
                    foodId = agg.foodId,
                    quantity = agg.quantity,
                    unit = agg.unit,
                    sortOrder = index,
                    category = GroceryCategory.categorize(agg.foodName)
                )
            }

        if (groceryItems.isNotEmpty()) {
            groceryDao.insertItems(groceryItems)
        }

        return listId
    }

    // ===== Helper classes =====

    private data class AggregationKey(val foodId: Long, val unit: FoodUnit)

    private data class AggregatedFood(
        val foodId: Long,
        val foodName: String,
        var quantity: Double,
        val unit: FoodUnit
    )
}
