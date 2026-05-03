package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.domain.diet.DietRepository
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.food.toDto
import com.mealplanplus.api.domain.health.HealthMetricRepository
import com.mealplanplus.api.domain.health.toDto
import com.mealplanplus.api.domain.log.DailyLogRepository
import com.mealplanplus.api.domain.log.LoggedFoodRepository
import com.mealplanplus.api.domain.log.toDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DashboardService(
    private val logRepo: DailyLogRepository,
    private val loggedFoodRepo: LoggedFoodRepository,
    private val foodRepo: FoodRepository,
    private val dietRepo: DietRepository,
    private val healthRepo: HealthMetricRepository,
) {
    fun get(firebaseUid: String): DashboardDto {
        // Today's log
        val todayEntity = logRepo.findByFirebaseUidAndDate(firebaseUid, LocalDate.now())
        val todayLog = todayEntity?.let { it.toDto(loggedFoodRepo.findByDailyLogId(it.id)) }

        // Last 5 logs (Spring Data returns them sorted newest-first already)
        val recentLogs = logRepo.findTop5ByFirebaseUidOrderByDateDesc(firebaseUid)
            .map { it.toDto(loggedFoodRepo.findByDailyLogId(it.id)) }

        // Only fetch the food IDs actually referenced by these logs
        val foodIds = (listOfNotNull(todayLog) + recentLogs)
            .flatMap { it.loggedFoods }
            .map { it.foodId }
            .toSet()
        val foods = if (foodIds.isEmpty()) emptyList()
                    else foodRepo.findAllById(foodIds).map { it.toDto() }

        val dietCount = dietRepo.countByFirebaseUid(firebaseUid)

        val latestWeight = healthRepo
            .findTop1ByFirebaseUidAndTypeOrderByRecordedAtDesc(firebaseUid, "WEIGHT")
            ?.toDto()

        return DashboardDto(
            todayLog = todayLog,
            recentLogs = recentLogs,
            foods = foods,
            dietCount = dietCount,
            latestWeight = latestWeight,
        )
    }
}
