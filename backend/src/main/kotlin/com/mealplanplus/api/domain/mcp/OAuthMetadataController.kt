package com.mealplanplus.api.domain.mcp

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * OAuth 2.0 Protected Resource Metadata (RFC 9728) for the MCP server. When a client (e.g. Claude) hits
 * an `/mcp` endpoint without a valid token, [McpAuthFilter] answers `401` with
 * `WWW-Authenticate: Bearer resource_metadata="…/.well-known/oauth-protected-resource"`; the client fetches
 * this document to learn which authorization server to run the OAuth flow against (our Stytch project) via
 * `authorization_servers`. Spec-current clients follow that pointer and talk to Stytch directly, so the
 * authorization-server metadata itself is served by Stytch — we do NOT mirror it here (an on-origin facade
 * can't satisfy both RFC 8414 §3.3 and RFC 9207 for Stytch's default-domain issuer; see PLAN-mcp-server.md).
 *
 * Served for the exact path and any path-suffixed probe (`…/oauth-protected-resource/mcp`).
 */
@RestController
class OAuthMetadataController(
    @Value("\${stytch.issuer:}") private val issuer: String,
    @Value("\${stytch.write-scope:mcp:write}") private val writeScope: String,
) {
    @GetMapping("/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/**")
    fun protectedResourceMetadata(request: HttpServletRequest): Map<String, Any> {
        val base = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString().trimEnd('/')
        return buildMap {
            put("resource", "$base/mcp")
            if (issuer.isNotBlank()) put("authorization_servers", listOf(issuer))
            // Advertise only a scope a Stytch connected app can be granted (openid). `full_access` is a
            // first-party SDK scope and is rejected for third-party/DCR clients.
            put("scopes_supported", listOf(writeScope))
            put("bearer_methods_supported", listOf("header"))
        }
    }
}
