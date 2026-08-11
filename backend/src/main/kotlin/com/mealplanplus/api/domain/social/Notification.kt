package com.mealplanplus.api.domain.social

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Follow event, or a followee sharing an item to their followers. */
enum class NotificationKind { FOLLOW, SHARE }

/** The kind of shared item a SHARE notification points at. */
enum class NotificationSubjectKind { DIET, MEAL, WORKOUT }

/**
 * A single in-app notification for one recipient. Online-only — never synced to Room (see V12).
 * FOLLOW rows carry no subject; SHARE rows denormalise the item kind/id/name at share time.
 */
@Entity
@Table(name = "notifications")
class Notification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val recipientUid: String = "",
    val actorUid: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationKind = NotificationKind.FOLLOW,

    @Enumerated(EnumType.STRING)
    val subjectKind: NotificationSubjectKind? = null,
    val subjectServerId: UUID? = null,
    val subjectName: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    var readAt: Instant? = null,
)
