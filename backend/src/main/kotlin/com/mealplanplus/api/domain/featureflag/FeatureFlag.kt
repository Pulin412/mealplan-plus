package com.mealplanplus.api.domain.featureflag

import jakarta.persistence.*
import java.time.Instant

/**
 * A single runtime on/off toggle, keyed by name. Mirrors the `feature_flags` table (V16); on H2 dev the
 * schema is built from this entity and rows are seeded by [FeatureFlagService.seedDefaults].
 */
@Entity
@Table(name = "feature_flags")
class FeatureFlag(
    @Id
    val flagKey: String = "",
    var enabled: Boolean = false,
    @Column(columnDefinition = "text")
    var updatedBy: String? = null,
    var updatedAt: Instant = Instant.now()
)
