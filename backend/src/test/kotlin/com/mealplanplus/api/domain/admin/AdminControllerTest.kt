package com.mealplanplus.api.domain.admin

import com.mealplanplus.api.domain.featureflag.FeatureFlag
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import com.mealplanplus.api.domain.mcp.McpTokenService
import com.mealplanplus.api.generated.model.FeatureFlagUpdateRequest
import com.mealplanplus.api.generated.model.McpConnectorTokenResponse
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
    private val mcpTokens = McpTokenService("test-secret")
    private val controller = AdminController(flags, adminAccess, mcpTokens)

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

    @Test
    fun `an admin mints a read-write connector token that verifies to their uid`() {
        authenticateAs("admin@example.com")
        Mockito.`when`(flags.isEnabled("mcp_server")).thenReturn(true)

        val body = controller.mintMcpConnectorToken("READ_WRITE").body!!

        assertThat(body.scope).isEqualTo(McpConnectorTokenResponse.Scope.READ_WRITE)
        assertThat(body.sseEndpointPath).isEqualTo("/mcp")
        val principal = mcpTokens.verify(body.token)!!
        assertThat(principal.uid).isEqualTo("uid-1")
        assertThat(principal.scope).isEqualTo(McpTokenService.Scope.READ_WRITE)
    }

    @Test
    fun `scope defaults to read-write when omitted`() {
        authenticateAs("admin@example.com")
        Mockito.`when`(flags.isEnabled("mcp_server")).thenReturn(true)

        val body = controller.mintMcpConnectorToken(null).body!!

        assertThat(mcpTokens.verify(body.token)!!.scope).isEqualTo(McpTokenService.Scope.READ_WRITE)
    }

    @Test
    fun `a read scope mints a read-only token`() {
        authenticateAs("admin@example.com")
        Mockito.`when`(flags.isEnabled("mcp_server")).thenReturn(true)

        val body = controller.mintMcpConnectorToken("READ").body!!

        assertThat(body.scope).isEqualTo(McpConnectorTokenResponse.Scope.READ)
        assertThat(mcpTokens.verify(body.token)!!.scope).isEqualTo(McpTokenService.Scope.READ)
    }

    @Test
    fun `minting fails with 409 when the mcp_server flag is off`() {
        authenticateAs("admin@example.com")
        Mockito.`when`(flags.isEnabled("mcp_server")).thenReturn(false)

        assertThatThrownBy { controller.mintMcpConnectorToken("READ_WRITE") }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `minting fails with 409 when the signing secret is unconfigured`() {
        authenticateAs("admin@example.com")
        Mockito.`when`(flags.isEnabled("mcp_server")).thenReturn(true)
        val controllerNoSecret = AdminController(flags, adminAccess, McpTokenService(""))

        assertThatThrownBy { controllerNoSecret.mintMcpConnectorToken("READ_WRITE") }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `an invalid scope is rejected with 400`() {
        authenticateAs("admin@example.com")
        Mockito.`when`(flags.isEnabled("mcp_server")).thenReturn(true)

        assertThatThrownBy { controller.mintMcpConnectorToken("SUPERUSER") }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `a non-admin cannot mint a token`() {
        authenticateAs("random@user.com")

        assertThatThrownBy { controller.mintMcpConnectorToken("READ_WRITE") }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting("statusCode").isEqualTo(HttpStatus.FORBIDDEN)

        Mockito.verifyNoInteractions(flags)
    }
}
