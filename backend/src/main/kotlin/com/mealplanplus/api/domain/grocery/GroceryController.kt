package com.mealplanplus.api.domain.grocery

import com.mealplanplus.api.generated.api.GroceryListsApi
import com.mealplanplus.api.generated.model.GroceryListDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class GroceryController(private val service: GroceryService) : GroceryListsApi {

    override fun listGroceryLists(): ResponseEntity<List<GroceryListDto>> =
        ResponseEntity.ok(service.list(currentUid()))

    override fun getGroceryList(id: Long): ResponseEntity<GroceryListDto> =
        ResponseEntity.ok(service.get(id))

    override fun createGroceryList(groceryListDto: GroceryListDto): ResponseEntity<GroceryListDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(groceryListDto, currentUid()))

    override fun deleteGroceryList(id: Long): ResponseEntity<Unit> {
        service.delete(id, currentUid()); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
