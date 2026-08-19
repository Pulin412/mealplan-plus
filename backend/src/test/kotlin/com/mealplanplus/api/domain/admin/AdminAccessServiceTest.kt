package com.mealplanplus.api.domain.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class AdminAccessServiceTest {

    private val service = AdminAccessService("pulins412@gmail.com, second.admin@example.com")

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    @Test
    fun `an allowlisted email is admin, case- and whitespace-insensitively`() {
        assertThat(service.isAdmin("pulins412@gmail.com")).isTrue()
        assertThat(service.isAdmin("  PULINS412@Gmail.com ")).isTrue()
        assertThat(service.isAdmin("second.admin@example.com")).isTrue()
    }

    @Test
    fun `a non-allowlisted or null email is not admin`() {
        assertThat(service.isAdmin("random@user.com")).isFalse()
        assertThat(service.isAdmin(null)).isFalse()
    }

    @Test
    fun `an empty allowlist grants nobody`() {
        assertThat(AdminAccessService("").isAdmin("pulins412@gmail.com")).isFalse()
    }

    @Test
    fun `isCurrentUserAdmin reads the email off the authentication details`() {
        val auth = UsernamePasswordAuthenticationToken("uid-1", null, emptyList())
        auth.details = "pulins412@gmail.com"
        SecurityContextHolder.getContext().authentication = auth

        assertThat(service.isCurrentUserAdmin()).isTrue()
    }
}
