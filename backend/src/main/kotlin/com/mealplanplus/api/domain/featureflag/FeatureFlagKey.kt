package com.mealplanplus.api.domain.featureflag

/**
 * Registry of the flags the app knows about, with their defaults. Seeded on startup so every known flag
 * exists (and is listable in the admin screen) on both Postgres and H2. Add a new flag by adding an entry.
 */
enum class FeatureFlagKey(val key: String, val defaultEnabled: Boolean) {
    /** External MCP connector server (users' own Claude). Off until deliberately enabled. */
    MCP_SERVER("mcp_server", false);

    companion object {
        fun fromKey(key: String): FeatureFlagKey? = entries.firstOrNull { it.key == key }
    }
}
