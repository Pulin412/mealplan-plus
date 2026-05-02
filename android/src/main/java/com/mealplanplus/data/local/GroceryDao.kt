package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.GroceryItem
import com.mealplanplus.data.model.GroceryList
import com.mealplanplus.data.model.GroceryListWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {

    // ===== GroceryList operations =====

    @Query("SELECT * FROM grocery_lists WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getListsByUser(userId: Long): Flow<List<GroceryList>>

    @Transaction
    @Query("SELECT * FROM grocery_lists WHERE id = :listId")
    fun getListWithItems(listId: Long): Flow<GroceryListWithItems?>

    @Transaction
    @Query("SELECT * FROM grocery_lists WHERE id = :listId")
    suspend fun getListWithItemsOnce(listId: Long): GroceryListWithItems?

    @Query("SELECT * FROM grocery_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): GroceryList?

    @Insert
    suspend fun insertList(list: GroceryList): Long

    @Update
    suspend fun updateList(list: GroceryList)

    @Delete
    suspend fun deleteList(list: GroceryList)

    @Query("DELETE FROM grocery_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    // ===== GroceryItem operations =====

    @Query("SELECT * FROM grocery_items WHERE listId = :listId ORDER BY isChecked ASC, sortOrder ASC, id ASC")
    fun getItemsByList(listId: Long): Flow<List<GroceryItem>>

    @Insert
    suspend fun insertItem(item: GroceryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: GroceryItem): Long

    @Insert
    suspend fun insertItems(items: List<GroceryItem>)

    @Update
    suspend fun updateItem(item: GroceryItem)

    @Query("UPDATE grocery_items SET isChecked = :checked WHERE id = :itemId")
    suspend fun setItemChecked(itemId: Long, checked: Boolean)

    @Query("UPDATE grocery_items SET isChecked = 0 WHERE listId = :listId")
    suspend fun uncheckAllItems(listId: Long)

    @Query("UPDATE grocery_items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateItemQuantity(itemId: Long, quantity: Double)

    @Delete
    suspend fun deleteItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Long)

    // ===== Statistics =====

    @Query("SELECT COUNT(*) FROM grocery_items WHERE listId = :listId")
    suspend fun getItemCount(listId: Long): Int

    @Query("SELECT COUNT(*) FROM grocery_items WHERE listId = :listId AND isChecked = 1")
    suspend fun getCheckedItemCount(listId: Long): Int

    @Query("DELETE FROM grocery_lists")
    suspend fun deleteAllGroceryLists()

    @Query("DELETE FROM grocery_items")
    suspend fun deleteAllGroceryItems()

    // Sync helpers (v19)
    @Query("SELECT * FROM grocery_lists WHERE userId = :userId AND (syncedAt IS NULL OR updatedAt > syncedAt)")
    suspend fun getUnsyncedGroceryLists(userId: Long): List<GroceryList>

    @Query("SELECT * FROM grocery_lists WHERE serverId = :serverId LIMIT 1")
    suspend fun getGroceryListByServerId(serverId: String): GroceryList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryList(list: GroceryList): Long

    @Update
    suspend fun updateGroceryList(list: GroceryList)
}