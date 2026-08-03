package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Generic read-through cache for server-computed read-models (Home/Today dashboard, Plan,
 * Groceries …). On screen open the UI paints [payload] instantly from disk, then a background
 * refresh overwrites it — so a Cloud Run cold start is never visible to a returning user.
 *
 * Keyed by "<uid>|<screen>|<params>" so different accounts never read each other's cache. This is
 * a disposable cache: it always re-fetches from the server (the source of truth), so losing it
 * (uninstall, [fallbackToDestructiveMigration]) is recoverable.
 */
@Entity(tableName = "cached_response")
data class CachedResponse(
    @PrimaryKey val key: String,
    val payload: String,      // DTO serialized via apiGson()
    val fetchedAt: Long,      // epoch millis — for staleness checks / debugging
)
