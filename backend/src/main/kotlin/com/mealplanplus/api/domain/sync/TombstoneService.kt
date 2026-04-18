package com.mealplanplus.api.domain.sync

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TombstoneService(private val repo: TombstoneRepository) {

    fun record(firebaseUid: String, entityType: String, serverId: UUID) {
        repo.save(Tombstone(firebaseUid = firebaseUid, entityType = entityType, serverId = serverId))
    }

    fun since(firebaseUid: String, since: Instant): List<TombstoneDto> =
        repo.findByFirebaseUidAndDeletedAtAfter(firebaseUid, since).map { it.toDto() }
}
