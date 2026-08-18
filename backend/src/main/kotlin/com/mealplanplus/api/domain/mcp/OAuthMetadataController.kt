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
 * this document to learn which authorization server to run the OAuth flow against (our Stytch project).
 *
 * The `resource` and metadata URL are derived from the incoming request so they're correct on both localhost
 * and the deployed host (behind Cloud Run this relies on forwarded headers; set later if needed).
 */
@RestController
class OAuthMetadataController(
    @Value("\${stytch.issuer:}") private val issuer: String,
    @Value("\${stytch.write-scope:mcp:write}") private val writeScope: String,
) {
    @GetMapping("/.well-known/oauth-protected-resource")
    fun protectedResourceMetadata(request: HttpServletRequest): Map<String, Any> {
        val base = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString().trimEnd('/')
        return buildMap {
            put("resource", "$base/mcp")
            if (issuer.isNotBlank()) put("authorization_servers", listOf(issuer))
            put("scopes_supported", listOf("mcp:read", writeScope))
            put("bearer_methods_supported", listOf("header"))
        }
    }
}
