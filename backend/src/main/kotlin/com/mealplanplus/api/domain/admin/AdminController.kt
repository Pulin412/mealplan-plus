package com.mealplanplus.api.domain.admin

import com.mealplanplus.api.domain.featureflag.FeatureFlag
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import com.mealplanplus.api.generated.api.AdminApi
import com.mealplanplus.api.generated.model.FeatureFlagResponse
import com.mealplanplus.api.generated.model.FeatureFlagUpdateRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Admin-only feature-flag management. Every operation is gated by [AdminAccessService] (the email allowlist);
 * non-admins get 403. Auth (a valid Firebase JWT) is already enforced globally by security config.
 */
@RestController
class AdminController(
    private val featureFlags: FeatureFlagService,
    private val adminAccess: AdminAccessService
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

    private fun requireAdmin() {
        if (!adminAccess.isCurrentUserAdmin()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required")
        }
    }
}

fun FeatureFlag.toResponse() = FeatureFlagResponse(
    key = flagKey,
    enabled = enabled,
    updatedBy = updatedBy,
    updatedAt = updatedAt
)
