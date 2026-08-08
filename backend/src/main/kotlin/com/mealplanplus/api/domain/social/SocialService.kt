package com.mealplanplus.api.domain.social

import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import com.mealplanplus.api.domain.user.toResponse
import com.mealplanplus.api.generated.model.HandleAvailabilityDto
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import com.mealplanplus.api.generated.model.PublicProfileDto
import com.mealplanplus.api.generated.model.PublicProfileSummaryDto
import com.mealplanplus.api.generated.model.ReportRequest
import com.mealplanplus.api.generated.model.UserResponse
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class SocialService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val blockRepository: BlockRepository,
    private val contentReportRepository: ContentReportRepository,
) {
    private val handleRegex = Regex("^[a-z0-9_]{3,20}$")

    // ── Profile ──────────────────────────────────────────────────────────────

    @Transactional
    fun updateMyProfile(uid: String, req: ProfileUpdateRequest): UserResponse {
        val user = userRepository.findByFirebaseUid(uid)
            ?: userRepository.save(User(firebaseUid = uid))

        req.handle?.let { raw ->
            val normalized = raw.trim().lowercase()
            if (!handleRegex.matches(normalized)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Handle must match ^[a-z0-9_]{3,20}$")
            }
            // Allow re-submitting the same handle (no-op); reject if another user holds it.
            if (!normalized.equals(user.handle, ignoreCase = true) &&
                userRepository.existsByHandleIgnoreCase(normalized)
            ) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Handle already taken")
            }
            user.handle = normalized
        }
        req.bio?.let { user.bio = it.trim().ifBlank { null } }
        req.avatarSeed?.let { user.avatarSeed = it.trim().ifBlank { null } }
        req.isSearchable?.let { user.isSearchable = it }

        return userRepository.save(user).toResponse()
    }

    fun checkHandleAvailable(uid: String, handle: String): HandleAvailabilityDto {
        val normalized = handle.trim().lowercase()
        val valid = handleRegex.matches(normalized)
        // A handle you already own reads as "available" to you.
        val ownedByMe = userRepository.findByFirebaseUid(uid)?.handle?.equals(normalized, true) == true
        val available = valid && (ownedByMe || !userRepository.existsByHandleIgnoreCase(normalized))
        return HandleAvailabilityDto(handle = normalized, available = available, valid = valid)
    }

    // ── Discovery ────────────────────────────────────────────────────────────

    fun searchUsers(uid: String, q: String, page: Int = 0, size: Int = 20): List<PublicProfileSummaryDto> {
        val term = q.trim()
        if (term.isEmpty()) return emptyList()
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 50))
        return userRepository.searchProfiles(term, uid, pageable).map { it.toSummary(uid) }
    }

    fun getPublicProfile(uid: String, handle: String): PublicProfileDto {
        val target = requireVisibleUser(uid, handle)
        return PublicProfileDto(
            handle = target.handle!!,
            followerCount = followRepository.countByFolloweeUid(target.firebaseUid),
            followingCount = followRepository.countByFollowerUid(target.firebaseUid),
            isFollowedByMe = followRepository.existsByFollowerUidAndFolloweeUid(uid, target.firebaseUid),
            isMe = target.firebaseUid == uid,
            displayName = target.displayName,
            bio = target.bio,
            avatarSeed = target.avatarSeed,
        )
    }

    // ── Follow graph ─────────────────────────────────────────────────────────

    @Transactional
    fun followUser(uid: String, handle: String) {
        val target = requireUser(handle)
        if (target.firebaseUid == uid) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot follow yourself")
        }
        if (blockRepository.blockExistsEitherWay(uid, target.firebaseUid)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot follow this user")
        }
        if (!followRepository.existsByFollowerUidAndFolloweeUid(uid, target.firebaseUid)) {
            followRepository.save(Follow(followerUid = uid, followeeUid = target.firebaseUid))
        }
    }

    @Transactional
    fun unfollowUser(uid: String, handle: String) {
        val target = requireUser(handle)
        followRepository.deleteByFollowerUidAndFolloweeUid(uid, target.firebaseUid)
    }

    fun listFollowers(uid: String, handle: String): List<PublicProfileSummaryDto> {
        val target = requireVisibleUser(uid, handle)
        return followRepository.findByFolloweeUidOrderByCreatedAtDesc(target.firebaseUid)
            .mapNotNull { userRepository.findByFirebaseUid(it.followerUid) }
            .filter { !blockRepository.blockExistsEitherWay(uid, it.firebaseUid) }
            .map { it.toSummary(uid) }
    }

    fun listFollowing(uid: String, handle: String): List<PublicProfileSummaryDto> {
        val target = requireVisibleUser(uid, handle)
        return followRepository.findByFollowerUidOrderByCreatedAtDesc(target.firebaseUid)
            .mapNotNull { userRepository.findByFirebaseUid(it.followeeUid) }
            .filter { !blockRepository.blockExistsEitherWay(uid, it.firebaseUid) }
            .map { it.toSummary(uid) }
    }

    // ── Safety ───────────────────────────────────────────────────────────────

    @Transactional
    fun blockUser(uid: String, handle: String) {
        val target = requireUser(handle)
        if (target.firebaseUid == uid) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot block yourself")
        }
        if (!blockRepository.existsByBlockerUidAndBlockedUid(uid, target.firebaseUid)) {
            blockRepository.save(Block(blockerUid = uid, blockedUid = target.firebaseUid))
        }
        // A block severs any existing follow edges in both directions.
        followRepository.deleteByFollowerUidAndFolloweeUid(uid, target.firebaseUid)
        followRepository.deleteByFollowerUidAndFolloweeUid(target.firebaseUid, uid)
    }

    @Transactional
    fun unblockUser(uid: String, handle: String) {
        val target = requireUser(handle)
        blockRepository.deleteByBlockerUidAndBlockedUid(uid, target.firebaseUid)
    }

    @Transactional
    fun report(uid: String, req: ReportRequest) {
        val reportedUid = req.reportedHandle?.let { userRepository.findByHandle(it.trim())?.firebaseUid }
        contentReportRepository.save(
            ContentReport(
                reporterUid = uid,
                entityType = req.entityType.value,
                entityServerId = req.entityServerId,
                reportedUid = reportedUid,
                reason = req.reason,
                detail = req.detail,
            )
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun requireUser(handle: String): User =
        userRepository.findByHandle(handle.trim())
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No such user")

    /** Resolve a user by handle, 404 if missing, 403 if a block exists either way. */
    private fun requireVisibleUser(uid: String, handle: String): User {
        val target = requireUser(handle)
        if (target.firebaseUid != uid &&
            blockRepository.blockExistsEitherWay(uid, target.firebaseUid)
        ) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Unavailable")
        }
        return target
    }

    private fun User.toSummary(viewerUid: String) = PublicProfileSummaryDto(
        handle = handle!!,
        isFollowedByMe = followRepository.existsByFollowerUidAndFolloweeUid(viewerUid, firebaseUid),
        displayName = displayName,
        avatarSeed = avatarSeed,
    )
}
