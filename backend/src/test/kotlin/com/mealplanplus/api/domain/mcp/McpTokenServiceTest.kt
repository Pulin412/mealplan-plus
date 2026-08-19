package com.mealplanplus.api.domain.mcp

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class McpTokenServiceTest {

    private val service = McpTokenService("test-secret-abc123")

    @Test
    fun `mint then verify round-trips uid and scope`() {
        val token = service.mint("uid-123", McpTokenService.Scope.READ_WRITE)
        val principal = service.verify(token)

        assertThat(principal).isNotNull
        assertThat(principal!!.uid).isEqualTo("uid-123")
        assertThat(principal.scope).isEqualTo(McpTokenService.Scope.READ_WRITE)
    }

    @Test
    fun `a tampered signature is rejected`() {
        val token = service.mint("uid-123", McpTokenService.Scope.READ)
        val tampered = token.dropLast(2) + "xy"

        assertThat(service.verify(tampered)).isNull()
    }

    @Test
    fun `a token signed with a different secret is rejected`() {
        val other = McpTokenService("a-completely-different-secret").mint("uid-123", McpTokenService.Scope.READ)

        assertThat(service.verify(other)).isNull()
    }

    @Test
    fun `malformed tokens are rejected, not thrown`() {
        assertThat(service.verify("garbage")).isNull()
        assertThat(service.verify("a.b.c")).isNull()
        assertThat(service.verify("")).isNull()
    }

    @Test
    fun `an unset secret mints nothing and verifies nothing`() {
        val unset = McpTokenService("")
        assertThat(unset.configured).isFalse()
        assertThat(unset.verify("anything")).isNull()
        assertThatThrownBy { unset.mint("uid", McpTokenService.Scope.READ) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
