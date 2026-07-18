package com.mealplanplus.api.domain.food

import com.mealplanplus.api.generated.api.FoodsApi
import com.mealplanplus.api.generated.model.FoodDto
import com.mealplanplus.api.generated.model.FoodPage
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class FoodController(private val service: FoodService) : FoodsApi {

    override fun listFoods(favorites: Boolean): ResponseEntity<List<FoodDto>> =
        ResponseEntity.ok(service.list(currentUid(), favoritesOnly = favorites))

    override fun searchFoods(q: String, page: Int, size: Int): ResponseEntity<FoodPage> {
        val p = service.search(q, currentUid(), PageRequest.of(page, size.coerceIn(1, 100), Sort.by("name")))
        return ResponseEntity.ok(FoodPage(
            content       = p.content,
            totalElements = p.totalElements,
            totalPages    = p.totalPages,
            number        = p.number,
            propertySize  = p.propertySize
        ))
    }

    override fun getFood(id: Long): ResponseEntity<FoodDto> =
        ResponseEntity.ok(service.get(id, currentUid()))

    override fun createFood(foodDto: FoodDto): ResponseEntity<FoodDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(foodDto, currentUid()))

    override fun updateFood(id: Long, foodDto: FoodDto): ResponseEntity<FoodDto> =
        ResponseEntity.ok(service.update(id, foodDto, currentUid()))

    override fun deleteFood(id: Long): ResponseEntity<Unit> {
        service.delete(id, currentUid()); return ResponseEntity.noContent().build()
    }

    override fun toggleFoodFavorite(id: Long): ResponseEntity<FoodDto> =
        ResponseEntity.ok(service.toggleFavorite(id, currentUid()))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
