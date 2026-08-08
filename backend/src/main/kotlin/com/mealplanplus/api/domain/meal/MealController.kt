package com.mealplanplus.api.domain.meal

import com.mealplanplus.api.generated.api.MealsApi
import com.mealplanplus.api.generated.model.MealDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class MealController(private val service: MealService) : MealsApi {

    override fun listMeals(favorites: Boolean): ResponseEntity<List<MealDto>> =
        ResponseEntity.ok(service.list(currentUid(), favoritesOnly = favorites))

    override fun getMeal(id: Long): ResponseEntity<MealDto> =
        ResponseEntity.ok(service.get(id, currentUid()))

    override fun createMeal(mealDto: MealDto): ResponseEntity<MealDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(mealDto, currentUid()))

    override fun updateMeal(id: Long, mealDto: MealDto): ResponseEntity<MealDto> =
        ResponseEntity.ok(service.update(id, mealDto, currentUid()))

    override fun deleteMeal(id: Long): ResponseEntity<Unit> {
        service.delete(id, currentUid()); return ResponseEntity.noContent().build()
    }

    override fun toggleMealFavorite(id: Long): ResponseEntity<MealDto> =
        ResponseEntity.ok(service.toggleFavorite(id, currentUid()))

    override fun toggleMealShare(serverId: java.util.UUID): ResponseEntity<MealDto> =
        ResponseEntity.ok(service.toggleShare(serverId, currentUid()))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
