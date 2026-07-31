package com.mealplanplus.api.domain.push

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.Subscription
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Security
import java.time.Instant
import java.time.LocalDate
import nl.martijndwars.webpush.PushService as WebPushService

/**
 * Stores Web Push subscriptions and sends "you haven't logged yet" reminders via VAPID.
 * Uses the standards-based `web-push` library (not Firebase FCM) so it stays inside the zero-billing
 * guardrail. If VAPID keys aren't configured, subscriptions are still stored but sends are skipped.
 */
@Service
class PushNotificationService(
    private val repo: PushSubscriptionRepository,
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${push.vapid.public-key:}") private val publicKey: String,
    @Value("\${push.vapid.private-key:}") private val privateKey: String,
    @Value("\${push.vapid.subject:mailto:reminders@mealplan.plus}") private val subject: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var pushService: WebPushService? = null

    @PostConstruct
    fun init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        pushService = if (publicKey.isNotBlank() && privateKey.isNotBlank()) {
            WebPushService(publicKey, privateKey, subject)
        } else {
            log.warn("VAPID keys not configured — Web Push sending disabled (subscriptions still stored).")
            null
        }
    }

    @Transactional
    fun subscribe(uid: String, endpoint: String, p256dh: String, auth: String, userAgent: String?) {
        val existing = repo.findByEndpoint(endpoint)
        if (existing != null) {
            existing.firebaseUid = uid
            existing.p256dh = p256dh
            existing.auth = auth
            existing.userAgent = userAgent
            repo.save(existing)
        } else {
            repo.save(
                PushSubscription(
                    firebaseUid = uid, endpoint = endpoint,
                    p256dh = p256dh, auth = auth, userAgent = userAgent,
                ),
            )
        }
    }

    @Transactional
    fun unsubscribe(endpoint: String) = repo.deleteByEndpoint(endpoint)

    /** Result triple: (subscriptions checked, reminders sent, stale subscriptions pruned). */
    fun runReminders(): Triple<Int, Int, Int> {
        val subs = repo.findAll()
        if (subs.isEmpty()) return Triple(0, 0, 0)
        // NOTE: MVP uses the server's date (Cloud Run = UTC). Per-user timezones can be added later.
        val today = LocalDate.now()
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "title" to "MealPlan+",
                "body" to "You haven't logged any meals today — tap to add one.",
                "url" to "/today",
            ),
        )
        var checked = 0
        var sent = 0
        var pruned = 0
        for ((uid, userSubs) in subs.groupBy { it.firebaseUid }) {
            checked += userSubs.size
            if (hasLoggedToday(uid, today)) continue
            for (sub in userSubs) {
                when (sendOne(sub, payload)) {
                    SendResult.SENT -> { sub.lastUsedAt = Instant.now(); repo.save(sub); sent++ }
                    SendResult.GONE -> { repo.delete(sub); pruned++ }
                    SendResult.ERROR -> { /* transient — leave subscription in place */ }
                }
            }
        }
        return Triple(checked, sent, pruned)
    }

    private fun hasLoggedToday(uid: String, today: LocalDate): Boolean {
        val slots = jdbc.queryForObject(
            "SELECT COUNT(*) FROM logged_meal_slots WHERE firebase_uid = ? AND date = ? AND is_logged = true",
            Long::class.java, uid, today,
        ) ?: 0
        if (slots > 0) return true
        val foods = jdbc.queryForObject(
            "SELECT COUNT(*) FROM daily_logs dl JOIN logged_foods lf ON lf.daily_log_id = dl.id " +
                "WHERE dl.firebase_uid = ? AND dl.date = ?",
            Long::class.java, uid, today,
        ) ?: 0
        return foods > 0
    }

    private enum class SendResult { SENT, GONE, ERROR }

    private fun sendOne(sub: PushSubscription, payload: String): SendResult {
        val svc = pushService ?: return SendResult.ERROR
        return try {
            val subscription = Subscription(sub.endpoint, Subscription.Keys(sub.p256dh, sub.auth))
            val response = svc.send(Notification(subscription, payload))
            when (val status = response.statusLine.statusCode) {
                in 200..299 -> SendResult.SENT
                404, 410 -> SendResult.GONE // subscription expired/unsubscribed — prune it
                else -> { log.warn("Push send returned $status for ${sub.endpoint}"); SendResult.ERROR }
            }
        } catch (e: Exception) {
            log.warn("Push send failed for ${sub.endpoint}: ${e.message}")
            SendResult.ERROR
        }
    }
}
