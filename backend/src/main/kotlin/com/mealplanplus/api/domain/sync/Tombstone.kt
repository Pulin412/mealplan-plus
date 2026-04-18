package com.mealplanplus.api.domain.sync

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

data class TombstoneDto(
    val entityType: String,
    val serverId: UUID,
    val deletedAt: Instant
)

fun Tombstone.toDto() = TombstoneDto(entityType, serverId, deletedAt)
