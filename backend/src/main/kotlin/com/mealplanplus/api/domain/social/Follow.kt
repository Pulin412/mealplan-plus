package com.mealplanplus.api.domain.social

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

/**
 * Directed follow edge: [followerUid] follows [followeeUid]. Open follow (no approval).
 * See V9 migration + issue #175. This is an online-only relationship — never synced to Room.
 */
data class FollowId(
    val followerUid: String = "",
    val followeeUid: String = ""
) : Serializable

@Entity
@Table(name = "follows")
@IdClass(FollowId::class)
class Follow(
    @Id val followerUid: String = "",
    @Id val followeeUid: String = "",
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

data class BlockId(
    val blockerUid: String = "",
    val blockedUid: String = ""
) : Serializable

@Entity
@Table(name = "blocks")
@IdClass(BlockId::class)
class Block(
    @Id val blockerUid: String = "",
    @Id val blockedUid: String = "",
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "content_reports")
class ContentReport(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val reporterUid: String = "",
    /** DIET | MEAL | WORKOUT_TEMPLATE | USER */
    val entityType: String = "",
    /** null when entityType = USER */
    val entityServerId: java.util.UUID? = null,
    val reportedUid: String? = null,
    val reason: String? = null,
    val detail: String? = null,
    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
