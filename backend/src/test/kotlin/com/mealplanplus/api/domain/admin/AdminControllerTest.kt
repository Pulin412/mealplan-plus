package com.mealplanplus.api.domain.admin

import com.mealplanplus.api.domain.featureflag.FeatureFlag
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import com.mealplanplus.api.generated.model.FeatureFlagUpdateRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException

class AdminControllerTest {

    private val flags = Mockito.mock(FeatureFlagService::class.java)
    private val adminAccess = AdminAccessService("admin@example.com")
    private val controller = AdminController(flags, adminAccess)

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    private fun authenticateAs(email: String?) {
        val auth = UsernamePasswordAuthenticationToken("uid-1", null, emptyList())
        auth.details = email
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun `a non-admin is forbidden from listing flags`() {
        authenticateAs("random@user.com")

        assertThatThrownBy { controller.listFeatureFlags() }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting("statusCode").isEqualTo(HttpStatus.FORBIDDEN)

        Mockito.verifyNoInteractions(flags)
    }

    @Test
    fun `an admin lists flags`() {
        authenticateAs("admin@example.com")
        Mockito.`when`(flags.all()).thenReturn(listOf(FeatureFlag(flagKey = "mcp_server", enabled = true)))

        val body = controller.listFeatureFlags().body!!

        assertThat(body).hasSize(1)
        assertThat(body[0].key).isEqualTo("mcp_server")
        assertThat(body[0].enabled).isTrue()
    }

    @Test
    fun `an admin toggles a flag and the updater email is recorded`() {
        authenticateAs("admin@example.com")
        // Concrete stub (no matchers) — also asserts the exact updater email is threaded through.
        Mockito.`when`(flags.setEnabled("mcp_server", true, "admin@example.com"))
            .thenReturn(FeatureFlag(flagKey = "mcp_server", enabled = true, updatedBy = "admin@example.com"))

        val resp = controller.setFeatureFlag("mcp_server", FeatureFlagUpdateRequest(enabled = true)).body!!

        assertThat(resp.enabled).isTrue()
        assertThat(resp.updatedBy).isEqualTo("admin@example.com")
        Mockito.verify(flags).setEnabled("mcp_server", true, "admin@example.com")
    }

    @Test
    fun `a non-admin cannot toggle a flag`() {
        authenticateAs(null)

        assertThatThrownBy { controller.setFeatureFlag("mcp_server", FeatureFlagUpdateRequest(enabled = true)) }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting("statusCode").isEqualTo(HttpStatus.FORBIDDEN)

        Mockito.verifyNoInteractions(flags)
    }
}
