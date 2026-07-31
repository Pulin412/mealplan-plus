package com.mealplanplus.api.domain.push

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * One browser/device Web Push subscription that opted in to reminders. Natural key is [endpoint]
 * (unique per subscription). Not a syncable entity — it never leaves the server.
 */
@Entity
@Table(name = "push_subscriptions")
class PushSubscription(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    var firebaseUid: String = "",
    @Column(length = 2048)
    val endpoint: String = "",
    var p256dh: String = "",
    var auth: String = "",
    var userAgent: String? = null,
    val createdAt: Instant = Instant.now(),
    var lastUsedAt: Instant? = null,
)
