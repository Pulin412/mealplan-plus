package com.mealplanplus.api.domain.sync

import com.mealplanplus.api.generated.api.SyncApi
import com.mealplanplus.api.generated.model.SyncPullResponse
import com.mealplanplus.api.generated.model.SyncPushRequest
import com.mealplanplus.api.generated.model.SyncPushResponse
import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.grocery.GroceryService
import com.mealplanplus.api.domain.health.HealthMetricService
import com.mealplanplus.api.domain.log.DailyLogService
import com.mealplanplus.api.domain.log.LoggingService
import com.mealplanplus.api.domain.meal.MealService
import com.mealplanplus.api.domain.plan.DayPlanService
import com.mealplanplus.api.domain.workout.WorkoutService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate

@RestController
class SyncController(
    private val foodService: FoodService,
    private val mealService: MealService,
    private val dietService: DietService,
    private val healthService: HealthMetricService,
    private val groceryService: GroceryService,
    private val logService: DailyLogService,
    private val loggingService: LoggingService,
    private val workoutService: WorkoutService,
    private val dayPlanService: DayPlanService,
    private val tombstoneService: TombstoneService
) : SyncApi {

    override fun syncPush(syncPushRequest: SyncPushRequest): ResponseEntity<SyncPushResponse> {
        val uid = currentUid()
        val savedFoods     = (syncPushRequest.foods ?: emptyList()).map { foodService.upsert(it, uid) }
        val savedLogs      = (syncPushRequest.dailyLogs ?: emptyList()).map { logService.upsert(it, uid) }
        val savedExercises = (syncPushRequest.exercises ?: emptyList()).map { workoutService.upsertExercise(it, uid) }
        val savedSessions  = (syncPushRequest.workoutSessions ?: emptyList()).map { workoutService.upsertSession(it, uid) }
        val savedMeals     = (syncPushRequest.meals ?: emptyList()).map { mealService.upsert(it, uid) }
        val savedDiets     = (syncPushRequest.diets ?: emptyList()).map { dietService.upsert(it, uid) }
        val savedMetrics   = (syncPushRequest.healthMetrics ?: emptyList()).map { healthService.upsert(it, uid) }
        val savedGroceries = (syncPushRequest.groceryLists ?: emptyList()).map { groceryService.upsert(it, uid) }
        val savedPlans     = (syncPushRequest.dayPlans ?: emptyList()).map { dayPlanService.upsert(uid, it.date, it) }
        val savedSlots     = (syncPushRequest.loggedMealSlots ?: emptyList()).map { loggingService.upsertSlot(uid, it) }
        val accepted = savedFoods.size + savedLogs.size + savedExercises.size +
            savedSessions.size + savedMeals.size + savedDiets.size +
            savedMetrics.size + savedGroceries.size + savedPlans.size + savedSlots.size
        return ResponseEntity.ok(SyncPushResponse(
            accepted        = accepted,
            foods           = savedFoods,
            meals           = savedMeals,
            diets           = savedDiets,
            healthMetrics   = savedMetrics,
            groceryLists    = savedGroceries,
            dailyLogs       = savedLogs,
            exercises       = savedExercises,
            workoutSessions = savedSessions,
            dayPlans        = savedPlans,
            loggedMealSlots = savedSlots
        ))
    }

    override fun syncPull(since: Instant): ResponseEntity<SyncPullResponse> {
        val uid = currentUid()
        return ResponseEntity.ok(SyncPullResponse(
            foods           = foodService.since(uid, since),
            meals           = mealService.since(uid, since),
            diets           = dietService.since(uid, since),
            healthMetrics   = healthService.since(uid, since),
            groceryLists    = groceryService.since(uid, since),
            dailyLogs       = logService.since(uid, since),
            exercises       = workoutService.exercisesSince(uid, since),
            workoutSessions = workoutService.sessionsSince(uid, since),
            dayPlans        = dayPlanService.since(uid, since),
            loggedMealSlots = loggingService.slotsSince(uid, since),
            tombstones      = tombstoneService.since(uid, since),
            serverTime      = Instant.now()
        ))
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
