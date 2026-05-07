package com.mealplanplus.api.domain.sync

import java.time.Instant

/**
 * Returns true when the incoming DTO's timestamp is not newer than the stored entity's
 * timestamp — i.e. the local version should win (skip the update).
 *
 * Used in every service's upsert() to implement last-write-wins logic consistently.
 */
fun shouldSkipUpdate(dtoUpdatedAt: Instant?, existingUpdatedAt: Instant): Boolean =
    (dtoUpdatedAt ?: Instant.EPOCH) <= existingUpdatedAt
