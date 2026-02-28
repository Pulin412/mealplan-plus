package com.mealplanplus.api.domain.sync

import com.mealplanplus.api.domain.diet.DietDto
import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.food.FoodDto
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.grocery.GroceryListDto
import com.mealplanplus.api.domain.grocery.GroceryService
import com.mealplanplus.api.domain.health.HealthMetricDto
import com.mealplanplus.api.domain.health.HealthMetricService
import com.mealplanplus.api.domain.log.DailyLogDto
import com.mealplanplus.api.domain.log.DailyLogService
import com.mealplanplus.api.domain.meal.MealDto
import com.mealplanplus.api.domain.meal.MealService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Sync")
class SyncController(
    private val foodService: FoodService,
    private val mealService: MealService,
    private val dietService: DietService,
    private val healthService: HealthMetricService,
    private val groceryService: GroceryService,
    private val logService: DailyLogService
) {
    data class PushRequest(
        val foods: List<FoodDto> = emptyList(),
        val meals: List<MealDto> = emptyList(),
        val diets: List<DietDto> = emptyList(),
        val healthMetrics: List<HealthMetricDto> = emptyList(),
        val groceryLists: List<GroceryListDto> = emptyList(),
        val dailyLogs: List<DailyLogDto> = emptyList()
    )

    data class PushResponse(val accepted: Int)

    data class PullResponse(
        val foods: List<FoodDto>,
        val meals: List<MealDto>,
        val diets: List<DietDto>,
        val healthMetrics: List<HealthMetricDto>,
        val groceryLists: List<GroceryListDto>,
        val dailyLogs: List<DailyLogDto>,
        val serverTime: Instant
    )

    @PostMapping("/push")
    fun push(@RequestBody req: PushRequest, auth: Authentication): PushResponse {
        var count = 0
        req.foods.forEach { foodService.upsert(it, auth.name); count++ }
        req.meals.forEach { mealService.upsert(it, auth.name); count++ }
        req.diets.forEach { dietService.upsert(it, auth.name); count++ }
        req.healthMetrics.forEach { healthService.upsert(it, auth.name); count++ }
        req.groceryLists.forEach { groceryService.upsert(it, auth.name); count++ }
        req.dailyLogs.forEach { logService.upsert(it, auth.name); count++ }
        return PushResponse(count)
    }

    @GetMapping("/pull")
    fun pull(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) since: Instant,
        auth: Authentication
    ): PullResponse = PullResponse(
        foods = foodService.since(auth.name, since),
        meals = mealService.since(auth.name, since),
        diets = dietService.since(auth.name, since),
        healthMetrics = healthService.since(auth.name, since),
        groceryLists = groceryService.since(auth.name, since),
        dailyLogs = logService.since(auth.name, since),
        serverTime = Instant.now()
    )
}
