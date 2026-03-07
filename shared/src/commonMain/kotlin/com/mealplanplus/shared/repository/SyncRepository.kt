package com.mealplanplus.shared.repository

import com.mealplanplus.shared.model.currentTimeMillis
import com.mealplanplus.shared.network.*
import com.mealplanplus.shared.preferences.PreferencesManager

data class SyncResult(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val timestamp: Long = 0L
)

/**
 * KMP shared sync repository — mirrors Android SyncRepository pattern.
 * Runs push then pull; stores last sync timestamp in preferences.
 * Token provider returns the current Firebase ID token (supplied by iOS/Android layer).
 */
class SyncRepository(
    private val mealRepo: MealRepository,
    private val dietRepo: DietRepository,
    private val healthMetricRepo: HealthMetricRepository,
    private val groceryRepo: GroceryRepository,
    private val preferences: PreferencesManager,
    private val apiClient: MealPlanApiClient
) {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Full sync: push local unsynced data, then pull remote changes.
     * @param firebaseUid  current user's Firebase UID
     * @param userId       local DB user ID
     */
    @Throws(Exception::class)
    suspend fun sync(firebaseUid: String, userId: Long): SyncResult {
        val pushed = push(firebaseUid, userId)
        val pulled = pull(userId)
        val now = currentTimeMillis()
        preferences.setLastSyncTime(now)
        return SyncResult(pushed = pushed, pulled = pulled, timestamp = now)
    }

    @Throws(Exception::class)
    suspend fun push(firebaseUid: String, userId: Long): Int {
        val meals = mealRepo.getUnsyncedMeals(userId).map { meal ->
            MealDto(
                id = meal.id,
                serverId = meal.serverId,
                firebaseUid = firebaseUid,
                name = meal.name,
                slot = meal.slotType,
                updatedAt = meal.updatedAt
            )
        }
        val diets = dietRepo.getUnsyncedDiets(userId).map { diet ->
            DietDto(
                id = diet.id,
                serverId = diet.serverId,
                firebaseUid = firebaseUid,
                name = diet.name,
                description = diet.description,
                updatedAt = diet.updatedAt
            )
        }
        val healthMetrics = healthMetricRepo.getUnsyncedHealthMetrics(userId).map { m ->
            HealthMetricDto(
                id = m.id,
                serverId = m.serverId,
                firebaseUid = firebaseUid,
                date = m.date,
                metricType = m.metricType,
                value = m.value,
                secondaryValue = m.secondaryValue,
                subType = m.subType,
                notes = m.notes,
                updatedAt = m.updatedAt
            )
        }
        val groceryLists = groceryRepo.getUnsyncedGroceryLists(userId).map { gl ->
            GroceryListDto(
                id = gl.id,
                serverId = gl.serverId,
                firebaseUid = firebaseUid,
                name = gl.name,
                startDate = gl.startDate,
                endDate = gl.endDate,
                updatedAt = gl.updatedAt
            )
        }

        if (meals.isEmpty() && diets.isEmpty() && healthMetrics.isEmpty() && groceryLists.isEmpty()) {
            return 0
        }

        val request = SyncPushRequest(
            meals = meals,
            diets = diets,
            healthMetrics = healthMetrics,
            groceryLists = groceryLists
        )

        val response = apiClient.push(request).getOrThrow()
        val now = currentTimeMillis()

        // Mark all pushed items as synced
        mealRepo.getUnsyncedMeals(userId).forEach { mealRepo.updateMealSyncedAt(it.id, now) }
        dietRepo.getUnsyncedDiets(userId).forEach { dietRepo.updateDietSyncedAt(it.id, now) }
        healthMetricRepo.getUnsyncedHealthMetrics(userId).forEach {
            healthMetricRepo.updateHealthMetricSyncedAt(it.id, now)
        }
        groceryRepo.getUnsyncedGroceryLists(userId).forEach {
            groceryRepo.updateGroceryListSyncedAt(it.id, now)
        }

        return response.accepted
    }

    @Throws(Exception::class)
    suspend fun pull(userId: Long): Int {
        val since = preferences.getLastSyncTime()
        val sinceIso = toIsoString(since)

        val response = apiClient.pull(sinceIso).getOrThrow()
        val now = currentTimeMillis()
        var count = 0

        // Merge meals
        response.meals.forEach { dto ->
            if (dto.serverId != null) {
                val existing = mealRepo.getMealByServerId(dto.serverId)
                if (existing == null) {
                    // Insert — create minimal Meal from DTO
                    val newId = mealRepo.insertMeal(
                        com.mealplanplus.shared.model.Meal(
                            userId = userId,
                            name = dto.name,
                            slotType = dto.slot,
                            serverId = dto.serverId,
                            updatedAt = dto.updatedAt ?: now,
                            syncedAt = now
                        )
                    )
                    mealRepo.updateMealSyncState(newId, dto.serverId, now)
                } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                    mealRepo.updateMeal(existing.copy(
                        name = dto.name,
                        slotType = dto.slot,
                        updatedAt = dto.updatedAt ?: now
                    ))
                    mealRepo.updateMealSyncedAt(existing.id, now)
                }
                count++
            }
        }

        // Merge diets
        response.diets.forEach { dto ->
            if (dto.serverId != null) {
                val existing = dietRepo.getDietByServerId(dto.serverId)
                if (existing == null) {
                    val newId = dietRepo.insertDiet(
                        com.mealplanplus.shared.model.Diet(
                            userId = userId,
                            name = dto.name,
                            description = dto.description,
                            serverId = dto.serverId,
                            updatedAt = dto.updatedAt ?: now,
                            syncedAt = now
                        )
                    )
                    dietRepo.updateDietSyncState(newId, dto.serverId, now)
                } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                    dietRepo.updateDiet(existing.copy(
                        name = dto.name,
                        description = dto.description,
                        updatedAt = dto.updatedAt ?: now
                    ))
                    dietRepo.updateDietSyncedAt(existing.id, now)
                }
                count++
            }
        }

        // Merge health metrics
        response.healthMetrics.forEach { dto ->
            if (dto.serverId != null) {
                val existing = healthMetricRepo.getHealthMetricByServerId(dto.serverId)
                if (existing == null) {
                    val newId = healthMetricRepo.insertHealthMetric(
                        com.mealplanplus.shared.model.HealthMetric(
                            userId = userId,
                            date = dto.date,
                            metricType = dto.metricType,
                            value = dto.value,
                            secondaryValue = dto.secondaryValue,
                            subType = dto.subType,
                            notes = dto.notes,
                            serverId = dto.serverId,
                            updatedAt = dto.updatedAt ?: now,
                            syncedAt = now
                        )
                    )
                    healthMetricRepo.updateHealthMetricSyncState(newId, dto.serverId, now)
                } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                    healthMetricRepo.updateHealthMetric(existing.copy(
                        value = dto.value,
                        secondaryValue = dto.secondaryValue,
                        notes = dto.notes,
                        updatedAt = dto.updatedAt ?: now
                    ))
                    healthMetricRepo.updateHealthMetricSyncedAt(existing.id, now)
                }
                count++
            }
        }

        // Merge grocery lists
        response.groceryLists.forEach { dto ->
            if (dto.serverId != null) {
                val existing = groceryRepo.getGroceryListByServerId(dto.serverId)
                if (existing == null) {
                    val newId = groceryRepo.insertGroceryList(
                        com.mealplanplus.shared.model.GroceryList(
                            userId = userId,
                            name = dto.name,
                            startDate = dto.startDate,
                            endDate = dto.endDate,
                            updatedAt = dto.updatedAt ?: now,
                            serverId = dto.serverId,
                            syncedAt = now
                        )
                    )
                    groceryRepo.updateGroceryListSyncState(newId, dto.serverId, now)
                } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                    groceryRepo.updateGroceryList(existing.copy(
                        name = dto.name,
                        startDate = dto.startDate,
                        endDate = dto.endDate,
                        updatedAt = dto.updatedAt ?: now
                    ))
                    groceryRepo.updateGroceryListSyncedAt(existing.id, now)
                }
                count++
            }
        }

        return count
    }

    suspend fun getLastSyncTime(): Long = preferences.getLastSyncTime()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun toIsoString(epochMillis: Long): String {
        if (epochMillis == 0L) return "1970-01-01T00:00:00Z"
        // Simple ISO 8601 UTC string from epoch millis
        val seconds = epochMillis / 1000
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = (seconds / 3600) % 24
        val days = (seconds / 86400).toInt()
        // Use a simple epoch-days calculation for the date part
        val (year, month, day) = epochDaysToDate(days)
        return "${year.pad(4)}-${month.pad(2)}-${day.pad(2)}T${h.pad(2)}:${m.pad(2)}:${s.pad(2)}Z"
    }

    private fun epochDaysToDate(days: Int): Triple<Int, Int, Int> {
        var n = days + 719468
        val era = (if (n >= 0) n else n - 146096) / 146097
        val doe = n - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val mo = mp + (if (mp < 10) 3 else -9)
        return Triple(y + (if (mo <= 2) 1 else 0), mo, d)
    }

    private fun Long.pad(width: Int): String = toString().padStart(width, '0')
    private fun Int.pad(width: Int): String = toString().padStart(width, '0')
}
