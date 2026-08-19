package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.featureflag.FeatureFlagKey
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Authenticates MCP requests (the `/mcp` paths) from a user's own agent, and runtime-gates the whole
 * surface behind the `mcp_server` feature flag.
 *
 * Two token kinds are accepted, disambiguated by shape (both arrive as `Authorization: Bearer …`):
 * - a **connector token** ([McpTokenService], one `.` → HMAC `payload.sig`) — personal, for Inspector /
 *   the Messages API;
 * - a **Stytch OAuth access token** ([StytchTokenService], two `.` → JWT `header.payload.sig`) — the
 *   real add-connector flow for a user's own Claude (Phase 2).
 *
 * - flag off  → 404 (the surface simply doesn't exist)
 * - no/invalid token → 401 + `WWW-Authenticate: Bearer resource_metadata="…"` so an OAuth client can
 *   discover the authorization server (RFC 9728 Protected Resource Metadata)
 * - valid token → SecurityContext is populated with the uid + scope authority, so the MCP tools resolve
 *   `uid` exactly like the rest of the API. Header-bearer (not URL) so the token rides every request,
 *   including the SSE message POSTs where tool calls actually arrive.
 */
@Component
class McpAuthFilter(
    private val tokens: McpTokenService,
    private val stytch: StytchTokenService,
    private val featureFlags: FeatureFlagService,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/mcp/")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (!featureFlags.isEnabled(FeatureFlagKey.MCP_SERVER.key)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        val token = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")?.trim()
        val principal = token?.let { resolve(it) }
        if (principal == null) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge(request))
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }

        val authorities = buildList {
            add(SimpleGrantedAuthority("SCOPE_MCP_READ"))
            if (principal.scope == McpTokenService.Scope.READ_WRITE) add(SimpleGrantedAuthority("SCOPE_MCP_WRITE"))
        }
        val auth = UsernamePasswordAuthenticationToken(principal.uid, null, authorities)
        SecurityContextHolder.getContext().authentication = auth
        try {
            chain.doFilter(request, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    /** A JWT (two dots) is a Stytch access token; anything else is a connector HMAC token (one dot). */
    private fun resolve(token: String): McpTokenService.Principal? =
        if (token.count { it == '.' } == 2) stytch.verify(token) else tokens.verify(token)

    private fun challenge(request: HttpServletRequest): String {
        val base = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString().trimEnd('/')
        return "Bearer resource_metadata=\"$base/.well-known/oauth-protected-resource\""
    }
}
