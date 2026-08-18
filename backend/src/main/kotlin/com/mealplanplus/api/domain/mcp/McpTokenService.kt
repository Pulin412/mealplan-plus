package com.mealplanplus.api.domain.mcp

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Stateless connector tokens for the MCP server: `base64url(uid:scope).base64url(HMAC-SHA256(uid:scope))`.
 * No storage — a token is valid as long as it verifies against `mcp.token-secret`; rotating the secret
 * revokes every token at once. This is the Phase-1 (personal / header-bearer) scheme; Phase 2 replaces it
 * with OAuth 2.1. If the secret is unset the server mints nothing and rejects everything.
 */
@Service
class McpTokenService(
    @Value("\${mcp.token-secret:}") private val secret: String,
) {
    enum class Scope { READ, READ_WRITE }
    data class Principal(val uid: String, val scope: Scope)

    val configured: Boolean get() = secret.isNotBlank()

    fun mint(uid: String, scope: Scope): String {
        require(configured) { "mcp.token-secret is not set" }
        val payload = "$uid:$scope"
        return "${b64(payload.toByteArray())}.${b64(hmac(payload))}"
    }

    /** Returns the principal iff the token verifies, else null (unset secret, bad shape, or bad signature). */
    fun verify(token: String): Principal? {
        if (!configured) return null
        val parts = token.split(".")
        if (parts.size != 2) return null
        val payload = runCatching { String(b64d(parts[0])) }.getOrNull() ?: return null
        if (!constantTimeEquals(b64(hmac(payload)), parts[1])) return null
        val seg = payload.split(":")
        if (seg.size != 2 || seg[0].isBlank()) return null
        val scope = runCatching { Scope.valueOf(seg[1]) }.getOrNull() ?: return null
        return Principal(seg[0], scope)
    }

    private fun hmac(data: String): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(), "HmacSHA256")) }
            .doFinal(data.toByteArray())

    private fun b64(b: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    private fun b64d(s: String): ByteArray = Base64.getUrlDecoder().decode(s)
    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
