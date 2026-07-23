package com.mealplanplus.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One item in a saved grocery list — a frozen snapshot of a combined ingredient
 * (name + unit + summed quantity + how many source entries merged into it).
 */
data class SavedGroceryItem(
    val key: String,
    val name: String,
    val unit: String,
    val total: Double,
    val count: Int,
)

/**
 * A named grocery list the user saved from a date selection. Local-only convenience
 * data — never synced — so it lives in SharedPreferences (JSON) rather than Room,
 * avoiding a schema migration for data the backend never sees.
 */
data class SavedGroceryList(
    val id: String,
    val name: String,
    val dateKeys: List<String>,
    val items: List<SavedGroceryItem>,
    val checked: Map<String, Boolean> = emptyMap(),
    val days: Int,
)

/** Persists saved grocery lists locally (JSON in SharedPreferences). Most-recent first. */
@Singleton
class GroceryStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("groceries", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<SavedGroceryList>>() {}.type

    fun load(): List<SavedGroceryList> =
        prefs.getString(KEY, null)?.let { runCatching { gson.fromJson<List<SavedGroceryList>>(it, listType) }.getOrNull() }
            ?: emptyList()

    fun save(lists: List<SavedGroceryList>) =
        prefs.edit().putString(KEY, gson.toJson(lists, listType)).apply()

    private companion object {
        const val KEY = "saved_lists"
    }
}
