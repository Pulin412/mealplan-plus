package com.mealplanplus.api.domain.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management")
class UserController(private val userService: UserService) {

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user, auto-creating on first call")
    fun getMe(auth: Authentication): UserResponse =
        userService.getOrCreate(auth.name)

    @PutMapping("/me")
    @Operation(summary = "Update profile", description = "Update editable profile fields for the current user")
    fun updateMe(@RequestBody req: UpdateUserRequest, auth: Authentication): UserResponse =
        userService.update(auth.name, req)
}
