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

/**
 * The live (unsaved) working state — which days are picked and how much of each item is ticked.
 * Persisted so leaving the screen and coming back keeps your checks (else a checked 5 that grows
 * to 6 would reset to 6 unchecked instead of 5 bought + 1 to buy).
 */
/** One persisted row of the live list — an independent line that's either checked (bought) or not. */
data class WorkRow(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val unit: String = "GRAM",
    val qty: Double = 0.0,
    val checked: Boolean = false,
)

data class GroceryWork(
    val selected: List<String> = emptyList(),
    val activeId: String? = null,
    /** The live list's rows — kept as-is until the user hits refresh or changes days, so a plan
     *  edit elsewhere doesn't silently reshuffle the list. */
    val rows: List<WorkRow> = emptyList(),
)

/** Persists saved grocery lists + the live working state locally (JSON in SharedPreferences). */
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

    fun loadWork(): GroceryWork? =
        prefs.getString(WORK_KEY, null)?.let { runCatching { gson.fromJson(it, GroceryWork::class.java) }.getOrNull() }

    fun saveWork(work: GroceryWork) =
        prefs.edit().putString(WORK_KEY, gson.toJson(work)).apply()

    private companion object {
        const val KEY = "saved_lists"
        const val WORK_KEY = "work_state"
    }
}
