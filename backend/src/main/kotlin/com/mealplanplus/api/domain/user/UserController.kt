package com.mealplanplus.api.domain.user

import com.mealplanplus.api.domain.admin.AdminAccessService
import com.mealplanplus.api.generated.api.UsersApi
import com.mealplanplus.api.generated.model.UserResponse
import com.mealplanplus.api.generated.model.UserUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService,
    private val adminAccess: AdminAccessService
) : UsersApi {

    override fun getMe(): ResponseEntity<UserResponse> {
        val auth = SecurityContextHolder.getContext().authentication
        val user = userService.getOrCreate(auth.name, auth.details as? String)
        return ResponseEntity.ok(user.copy(isAdmin = adminAccess.isCurrentUserAdmin()))
    }

    override fun updateMe(userUpdateRequest: UserUpdateRequest): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.update(currentUid(), userUpdateRequest).copy(isAdmin = adminAccess.isCurrentUserAdmin()))

    override fun deleteMe(): ResponseEntity<Unit> {
        userService.deleteMe(currentUid())
        return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
