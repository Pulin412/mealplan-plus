package com.mealplanplus.api.domain.sync

import com.mealplanplus.api.generated.model.TombstoneDto
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "tombstones",
    indexes = [Index(name = "idx_tombstones_uid_deleted", columnList = "firebaseUid,deletedAt")]
)
class Tombstone(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val entityType: String = "",
    val serverId: UUID = UUID.randomUUID(),
    val deletedAt: Instant = Instant.now()
)

fun Tombstone.toDto() = TombstoneDto(
    entityType = entityType,
    serverId   = serverId,
    deletedAt  = deletedAt
)
