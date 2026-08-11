package com.mealplanplus.api.domain.social

import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import com.mealplanplus.api.generated.model.NotificationDto
import com.mealplanplus.api.generated.model.NotificationListDto
import com.mealplanplus.api.generated.model.NotificationPrefsDto
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Generates and serves the in-app social notification feed (follow + share-to-followers).
 *
 * Deliberately a **leaf** service: it depends only on repositories, never on
 * Diet/Meal/WorkoutService or SocialService, so those can call [notifyShare]/[notifyFollow]
 * without creating a dependency cycle. Every generation path is gated by [receives] — the
 * recipient's master switch and the mutual-block check.
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val followRepository: FollowRepository,
    private val blockRepository: BlockRepository,
    private val userRepository: UserRepository,
) {
    // ── Generation ───────────────────────────────────────────────────────────

    /** [actorUid] started following [followeeUid]. No-op if the recipient can't receive it. */
    @Transactional
    fun notifyFollow(actorUid: String, followeeUid: String) {
        if (!receives(followeeUid, actorUid)) return
        notificationRepository.save(
            Notification(recipientUid = followeeUid, actorUid = actorUid, type = NotificationKind.FOLLOW),
        )
    }

    /** [actorUid] shared an item — fan out one notification to each eligible follower. */
    @Transactional
    fun notifyShare(actorUid: String, kind: NotificationSubjectKind, serverId: UUID, name: String?) {
        val rows = followRepository.findByFolloweeUidOrderByCreatedAtDesc(actorUid)
            .map { it.followerUid }
            .filter { receives(it, actorUid) }
            .map {
                Notification(
                    recipientUid = it, actorUid = actorUid, type = NotificationKind.SHARE,
                    subjectKind = kind, subjectServerId = serverId, subjectName = name,
                )
            }
        if (rows.isNotEmpty()) notificationRepository.saveAll(rows)
    }

    /** A recipient receives notifications from an actor only if enabled, not self, and no block. */
    private fun receives(recipientUid: String, actorUid: String): Boolean {
        if (recipientUid == actorUid) return false
        val user = userRepository.findByFirebaseUid(recipientUid) ?: return false
        if (!user.socialNotificationsEnabled) return false
        return !blockRepository.blockExistsEitherWay(recipientUid, actorUid)
    }

    // ── Feed reads ─────────────────────────────────────────────────────────────

    fun list(uid: String, limit: Int): NotificationListDto {
        val rows = notificationRepository.findByRecipientUidOrderByCreatedAtDesc(
            uid, PageRequest.of(0, limit.coerceIn(1, 100)),
        )
        val actors = rows.map { it.actorUid }.toSet()
            .mapNotNull { userRepository.findByFirebaseUid(it) }
            .associateBy { it.firebaseUid }
        return NotificationListDto(
            items = rows.map { it.toDto(actors[it.actorUid]) },
            unreadCount = notificationRepository.countByRecipientUidAndReadAtIsNull(uid).toInt(),
        )
    }

    @Transactional
    fun markAllRead(uid: String) {
        notificationRepository.markAllRead(uid, Instant.now())
    }

    // ── Preference ─────────────────────────────────────────────────────────────

    fun getPrefs(uid: String): NotificationPrefsDto =
        NotificationPrefsDto(enabled = userRepository.findByFirebaseUid(uid)?.socialNotificationsEnabled ?: true)

    @Transactional
    fun setPrefs(uid: String, enabled: Boolean): NotificationPrefsDto {
        val user = userRepository.findByFirebaseUid(uid) ?: userRepository.save(User(firebaseUid = uid))
        user.socialNotificationsEnabled = enabled
        userRepository.save(user)
        return NotificationPrefsDto(enabled = enabled)
    }

    private fun Notification.toDto(actor: User?) = NotificationDto(
        id = id,
        type = NotificationDto.Type.forValue(type.name),
        read = readAt != null,
        createdAt = createdAt,
        actorHandle = actor?.handle,
        actorDisplayName = actor?.displayName,
        actorAvatarSeed = actor?.avatarSeed,
        subjectKind = subjectKind?.let { NotificationDto.SubjectKind.forValue(it.name) },
        subjectServerId = subjectServerId,
        subjectName = subjectName,
    )
}
