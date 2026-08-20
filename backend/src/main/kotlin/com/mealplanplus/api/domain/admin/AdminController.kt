package com.mealplanplus.api.domain.admin

import com.mealplanplus.api.domain.featureflag.FeatureFlag
import com.mealplanplus.api.domain.featureflag.FeatureFlagKey
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import com.mealplanplus.api.domain.mcp.McpTokenService
import com.mealplanplus.api.generated.api.AdminApi
import com.mealplanplus.api.generated.model.FeatureFlagResponse
import com.mealplanplus.api.generated.model.FeatureFlagUpdateRequest
import com.mealplanplus.api.generated.model.McpConnectorTokenResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Admin-only feature-flag management. Every operation is gated by [AdminAccessService] (the email allowlist);
 * non-admins get 403. Auth (a valid Firebase JWT) is already enforced globally by security config.
 */
@RestController
class AdminController(
    private val featureFlags: FeatureFlagService,
    private val adminAccess: AdminAccessService,
    private val mcpTokens: McpTokenService,
) : AdminApi {

    override fun listFeatureFlags(): ResponseEntity<List<FeatureFlagResponse>> {
        requireAdmin()
        return ResponseEntity.ok(featureFlags.all().map { it.toResponse() })
    }

    override fun setFeatureFlag(
        key: String,
        featureFlagUpdateRequest: FeatureFlagUpdateRequest
    ): ResponseEntity<FeatureFlagResponse> {
        requireAdmin()
        val updated = featureFlags.setEnabled(
            key = key,
            enabled = featureFlagUpdateRequest.enabled,
            updatedBy = adminAccess.currentEmail() ?: "unknown"
        )
        return ResponseEntity.ok(updated.toResponse())
    }

    override fun mintMcpConnectorToken(scope: String?): ResponseEntity<McpConnectorTokenResponse> {
        requireAdmin()
        if (!featureFlags.isEnabled(FeatureFlagKey.MCP_SERVER.key)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "MCP server is disabled")
        }
        if (!mcpTokens.configured) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "MCP signing secret is not configured")
        }
        val parsedScope = when (scope?.trim()?.uppercase()) {
            null, "", "READ_WRITE" -> McpTokenService.Scope.READ_WRITE
            "READ" -> McpTokenService.Scope.READ
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid scope '$scope'. Use READ or READ_WRITE.")
        }
        val uid = SecurityContextHolder.getContext().authentication?.name
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return ResponseEntity.ok(
            McpConnectorTokenResponse(
                token = mcpTokens.mint(uid, parsedScope),
                scope = when (parsedScope) {
                    McpTokenService.Scope.READ -> McpConnectorTokenResponse.Scope.READ
                    McpTokenService.Scope.READ_WRITE -> McpConnectorTokenResponse.Scope.READ_WRITE
                },
                sseEndpointPath = MCP_ENDPOINT_PATH,
            )
        )
    }

    private fun requireAdmin() {
        if (!adminAccess.isCurrentUserAdmin()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required")
        }
    }

    private companion object {
        /** The Streamable HTTP MCP endpoint; kept in sync with `spring.ai.mcp.server.streamable-http.mcp-endpoint`. */
        const val MCP_ENDPOINT_PATH = "/mcp"
    }
}

fun FeatureFlag.toResponse() = FeatureFlagResponse(
    key = flagKey,
    enabled = enabled,
    updatedBy = updatedBy,
    updatedAt = updatedAt
)
