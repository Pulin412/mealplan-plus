package com.mealplanplus.api.domain.featureflag

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Read/write access to feature flags with a small in-memory cache so a flag check (e.g. gating the MCP
 * endpoint) doesn't hit the DB on every request. The cache is refreshed on write, so a toggle takes effect
 * immediately on the instance that served it. NB: on multiple always-on instances a toggle is eventually
 * consistent (other instances pick it up on their next cold read); acceptable for admin-managed flags on
 * our lean, scale-to-zero deployment. Revisit with a TTL/broadcast if we ever run many warm instances.
 */
@Service
class FeatureFlagService(private val repo: FeatureFlagRepository) {

    private val cache = ConcurrentHashMap<String, Boolean>()

    /** True iff the flag is on. Unknown flags fall back to their registry default, else false. */
    fun isEnabled(key: String): Boolean =
        cache.getOrPut(key) {
            repo.findById(key).map { it.enabled }
                .orElseGet { FeatureFlagKey.fromKey(key)?.defaultEnabled ?: false }
        }

    /** All flags for the admin screen. */
    fun all(): List<FeatureFlag> = repo.findAll()

    @Transactional
    fun setEnabled(key: String, enabled: Boolean, updatedBy: String): FeatureFlag {
        val flag = repo.findById(key).orElseGet { FeatureFlag(flagKey = key) }
        flag.enabled = enabled
        flag.updatedBy = updatedBy
        flag.updatedAt = Instant.now()
        val saved = repo.save(flag)
        cache[key] = enabled
        return saved
    }

    /** Ensure every known flag exists so it's listable/toggleable (idempotent; covers H2 dev where the V16 seed doesn't run). */
    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun seedDefaults() {
        FeatureFlagKey.entries.forEach { known ->
            if (!repo.existsById(known.key)) {
                repo.save(FeatureFlag(flagKey = known.key, enabled = known.defaultEnabled))
            }
        }
    }
}
