package com.mealplanplus.api.domain.diet

import com.mealplanplus.api.generated.api.DietsApi
import com.mealplanplus.api.generated.model.DietDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class DietController(private val service: DietService) : DietsApi {

    override fun listDiets(favorites: Boolean): ResponseEntity<List<DietDto>> =
        ResponseEntity.ok(service.list(currentUid(), favoritesOnly = favorites))

    override fun getDiet(id: Long): ResponseEntity<DietDto> =
        ResponseEntity.ok(service.get(id, currentUid()))

    override fun createDiet(dietDto: DietDto): ResponseEntity<DietDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(dietDto, currentUid()))

    override fun updateDiet(id: Long, dietDto: DietDto): ResponseEntity<DietDto> =
        ResponseEntity.ok(service.update(id, dietDto, currentUid()))

    override fun deleteDiet(id: Long): ResponseEntity<Unit> {
        service.delete(id, currentUid()); return ResponseEntity.noContent().build()
    }

    override fun duplicateDiet(id: Long): ResponseEntity<DietDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.duplicate(id, currentUid()))

    override fun toggleDietFavorite(id: Long): ResponseEntity<DietDto> =
        ResponseEntity.ok(service.toggleFavorite(id, currentUid()))

    override fun toggleDietShare(id: Long): ResponseEntity<DietDto> =
        ResponseEntity.ok(service.toggleShare(id, currentUid()))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
