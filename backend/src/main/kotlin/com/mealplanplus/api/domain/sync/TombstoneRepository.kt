package com.mealplanplus.api.domain.sync

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface TombstoneRepository : JpaRepository<Tombstone, Long> {
    fun findByFirebaseUidAndDeletedAtAfter(firebaseUid: String, since: Instant): List<Tombstone>
}
