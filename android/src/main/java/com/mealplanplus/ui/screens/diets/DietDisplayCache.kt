package com.mealplanplus.ui.screens.diets

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-lifetime cache for the fully-enriched diet list.
 *
 * [DietsViewModel] is scoped to the NavBackStackEntry, so it is destroyed every time
 * the user navigates away from the Diets screen. Without this cache, every re-entry
 * shows a loading spinner while the batch queries re-run.
 *
 * With the cache:
 * - First open: cache is empty → spinner shown, queries run, cache populated.
 * - Subsequent opens: [items] is pre-populated → ViewModel initialises
 *   [DietsViewModel._dietsWithMeals] directly from the cache value, so the list
 *   is visible instantly. Room re-runs in the background and updates silently.
 */
@Singleton
class DietDisplayCache @Inject constructor() {
    /** Last successfully built enriched diet list. Empty until first load completes. */
    val items = MutableStateFlow<List<DietDisplayItem>>(emptyList())
}
