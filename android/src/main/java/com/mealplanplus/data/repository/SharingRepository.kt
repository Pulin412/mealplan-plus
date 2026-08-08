package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.DietsApi
import com.mealplanplus.data.generated.api.MealsApi
import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct-REST helper for the per-item Share toggle. Keyed by serverId (which the offline-first
 * library models already use as their primary id), so toggling never touches Room or the sync
 * tables — it just flips the server-side is_shared flag. Read helpers fetch the current shared set
 * so the library screens can render the correct toggle state.
 */
@Singleton
class SharingRepository @Inject constructor(
    private val dietsApi: DietsApi,
    private val mealsApi: MealsApi,
    private val templatesApi: WorkoutTemplatesApi,
) {
    /** serverIds that are shared with followers, and serverIds that are copies from others. */
    data class Flags(val shared: Set<UUID> = emptySet(), val imported: Set<UUID> = emptySet())

    suspend fun dietFlags(): Flags = runCatching {
        val list = dietsApi.listDiets().body().orEmpty()
        Flags(
            shared = list.filter { it.isShared == true }.mapNotNull { it.serverId }.toSet(),
            imported = list.filter { it.imported == true }.mapNotNull { it.serverId }.toSet(),
        )
    }.getOrDefault(Flags())

    suspend fun mealFlags(): Flags = runCatching {
        val list = mealsApi.listMeals().body().orEmpty()
        Flags(
            shared = list.filter { it.isShared == true }.mapNotNull { it.serverId }.toSet(),
            imported = list.filter { it.imported == true }.mapNotNull { it.serverId }.toSet(),
        )
    }.getOrDefault(Flags())

    /** Toggle share; returns the new isShared state, or null on failure. */
    suspend fun toggleDiet(serverId: UUID): Boolean? =
        runCatching { dietsApi.toggleDietShare(serverId).body()?.isShared }.getOrNull()

    suspend fun toggleMeal(serverId: UUID): Boolean? =
        runCatching { mealsApi.toggleMealShare(serverId).body()?.isShared }.getOrNull()

    suspend fun toggleWorkout(serverId: UUID): Boolean? =
        runCatching { templatesApi.toggleWorkoutTemplateShare(serverId).body()?.isShared }.getOrNull()
}
