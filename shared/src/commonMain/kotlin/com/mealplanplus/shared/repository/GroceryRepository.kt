package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroceryRepository(private val database: MealPlanDatabase) {

    private val queries = database.groceryListQueries

    fun getAllGroceryLists(userId: Long): Flow<List<GroceryList>> {
        return queries.selectAllGroceryLists(userId).asFlowList().map { list ->
            list.map { it.toGroceryList() }
        }
    }

    suspend fun getGroceryListById(id: Long): GroceryList? {
        return queries.selectGroceryListById(id).executeAsOneOrNull()?.toGroceryList()
    }

    suspend fun getGroceryListWithItems(listId: Long): GroceryListWithItems? {
        val list = getGroceryListById(listId) ?: return null
        val items = queries.selectGroceryItems(listId).executeAsList().map { row ->
            GroceryItemWithFood(
                item = GroceryItem(
                    id = row.id,
                    listId = row.listId,
                    foodId = row.foodId,
                    customName = row.customName,
                    quantity = row.quantity,
                    unit = FoodUnit.fromString(row.unit),
                    isChecked = row.isChecked == 1L,
                    sortOrder = row.sortOrder.toInt(),
                    category = row.category
                ),
                food = row.id_?.let {
                    FoodItem(
                        id = it,
                        name = row.name ?: "",
                        brand = row.brand,
                        barcode = row.barcode,
                        caloriesPer100 = row.caloriesPer100 ?: 0.0,
                        proteinPer100 = row.proteinPer100 ?: 0.0,
                        carbsPer100 = row.carbsPer100 ?: 0.0,
                        fatPer100 = row.fatPer100 ?: 0.0,
                        gramsPerPiece = row.gramsPerPiece,
                        gramsPerCup = row.gramsPerCup,
                        gramsPerTbsp = row.gramsPerTbsp,
                        gramsPerTsp = row.gramsPerTsp,
                        glycemicIndex = row.glycemicIndex?.toInt(),
                        isFavorite = row.isFavorite == 1L,
                        lastUsed = row.lastUsed,
                        createdAt = row.createdAt ?: 0L,
                        isSystemFood = row.isSystemFood == 1L
                    )
                }
            )
        }
        return GroceryListWithItems(list, items)
    }

    suspend fun insertGroceryList(list: GroceryList): Long {
        queries.insertGroceryList(
            userId = list.userId,
            name = list.name,
            startDate = list.startDate,
            endDate = list.endDate,
            createdAt = list.createdAt,
            updatedAt = list.updatedAt
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateGroceryList(list: GroceryList) {
        queries.updateGroceryList(
            name = list.name,
            startDate = list.startDate,
            endDate = list.endDate,
            updatedAt = list.updatedAt,
            id = list.id
        )
    }

    suspend fun deleteGroceryList(id: Long) {
        queries.deleteGroceryList(id)
    }

    // Grocery items
    suspend fun insertGroceryItem(item: GroceryItem): Long {
        queries.insertGroceryItem(
            listId = item.listId,
            foodId = item.foodId,
            customName = item.customName,
            quantity = item.quantity,
            unit = item.unit.name,
            isChecked = if (item.isChecked) 1L else 0L,
            sortOrder = item.sortOrder.toLong(),
            category = item.category
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateGroceryItem(item: GroceryItem) {
        queries.updateGroceryItem(
            foodId = item.foodId,
            customName = item.customName,
            quantity = item.quantity,
            unit = item.unit.name,
            isChecked = if (item.isChecked) 1L else 0L,
            sortOrder = item.sortOrder.toLong(),
            category = item.category,
            id = item.id
        )
    }

    suspend fun updateGroceryItemChecked(id: Long, isChecked: Boolean) {
        queries.updateGroceryItemChecked(if (isChecked) 1L else 0L, id)
    }

    suspend fun deleteGroceryItem(id: Long) {
        queries.deleteGroceryItem(id)
    }

    suspend fun clearGroceryItems(listId: Long) {
        queries.deleteGroceryItemsByListId(listId)
    }

    // MARK: - Snapshot functions for iOS
    suspend fun getAllGroceryListsSnapshot(userId: Long): List<GroceryList> {
        return queries.selectAllGroceryLists(userId).executeAsList().map { it.toGroceryList() }
    }

    private fun com.mealplanplus.shared.db.Grocery_lists.toGroceryList(): GroceryList {
        return GroceryList(
            id = id,
            userId = userId,
            name = name,
            startDate = startDate,
            endDate = endDate,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
