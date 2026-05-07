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
        val todayEntity = logRepo.findFirstByFirebaseUidAndDateOrderByIdDesc(firebaseUid, LocalDate.now())
        val recentLogs  = logRepo.findTop5ByFirebaseUidOrderByDateDesc(firebaseUid)

        // Batch-fetch all logged foods for today + recent logs in one query
        val allLogIds = (listOfNotNull(todayEntity) + recentLogs).map { it.id }.distinct()
        val allLoggedFoods = if (allLogIds.isEmpty()) emptyList()
                             else loggedFoodRepo.findByDailyLogIdIn(allLogIds)
        val foodsByLogId = allLoggedFoods.groupBy { it.dailyLogId }

        val todayLog = todayEntity?.toDto(foodsByLogId[todayEntity.id] ?: emptyList())
        val recentLogDtos = recentLogs.map { it.toDto(foodsByLogId[it.id] ?: emptyList()) }

        // Batch-fetch only the food rows actually referenced by these logs
        val foodIds = allLoggedFoods.map { it.foodId }.toSet()
        val foods = if (foodIds.isEmpty()) emptyList()
                    else foodRepo.findAllById(foodIds).map { it.toDto() }

        val dietCount   = dietRepo.countByFirebaseUid(firebaseUid)
        val latestWeight = healthRepo
            .findTop1ByFirebaseUidAndTypeOrderByRecordedAtDesc(firebaseUid, "WEIGHT")
            ?.toDto()

        return DashboardDto(
            todayLog = todayLog,
            recentLogs = recentLogDtos,
            foods = foods,
            dietCount = dietCount,
            latestWeight = latestWeight,
        )
    }
}
