package com.mealplanplus.api.domain.social

import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.meal.MealService
import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import com.mealplanplus.api.domain.user.toResponse
import com.mealplanplus.api.domain.workout.WorkoutService
import com.mealplanplus.api.generated.model.CopyRequest
import com.mealplanplus.api.generated.model.CopyResultDto
import com.mealplanplus.api.generated.model.HandleAvailabilityDto
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import com.mealplanplus.api.generated.model.PublicProfileDto
import com.mealplanplus.api.generated.model.PublicProfileSummaryDto
import com.mealplanplus.api.generated.model.ReportRequest
import com.mealplanplus.api.generated.model.SharedDietDetailDto
import com.mealplanplus.api.generated.model.SharedMealDetailDto
import com.mealplanplus.api.generated.model.SharedTemplateSummaryDto
import com.mealplanplus.api.generated.model.SharedWorkoutDetailDto
import com.mealplanplus.api.generated.model.UserResponse
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class SocialService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val blockRepository: BlockRepository,
    private val contentReportRepository: ContentReportRepository,
    private val dietService: DietService,
    private val mealService: MealService,
    private val workoutService: WorkoutService,
    private val foodService: FoodService,
    private val copyService: CopyService,
    private val notificationService: NotificationService,
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
            notificationService.notifyFollow(actorUid = uid, followeeUid = target.firebaseUid)
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

    /** Accounts the caller has blocked, most recent first — the only place a blocked user is
     *  reachable again (they are hidden from search and their profile 403s). */
    fun listBlocks(uid: String): List<PublicProfileSummaryDto> =
        blockRepository.findByBlockerUidOrderByCreatedAtDesc(uid)
            .mapNotNull { userRepository.findByFirebaseUid(it.blockedUid) }
            .map { it.toSummary(uid) }

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

    // ── Shared-library reads (followers-gated) ───────────────────────────────

    fun listSharedDiets(viewerUid: String, handle: String): List<SharedTemplateSummaryDto> {
        val author = requireCanViewLibrary(viewerUid, handle)
        return dietService.sharedDietsOf(author.firebaseUid).map {
            SharedTemplateSummaryDto(
                type = SharedTemplateSummaryDto.Type.DIET,
                serverId = it.serverId,
                name = it.name,
                subtitle = it.targetCalories?.let { c -> "${c.toInt()} kcal" },
            )
        }
    }

    fun listSharedMeals(viewerUid: String, handle: String): List<SharedTemplateSummaryDto> {
        val author = requireCanViewLibrary(viewerUid, handle)
        return mealService.sharedMealsOf(author.firebaseUid).map {
            SharedTemplateSummaryDto(
                type = SharedTemplateSummaryDto.Type.MEAL,
                serverId = it.serverId,
                name = it.name,
                subtitle = it.slots.takeIf { s -> s.isNotEmpty() }?.joinToString(", "),
            )
        }
    }

    fun listSharedWorkouts(viewerUid: String, handle: String): List<SharedTemplateSummaryDto> {
        val author = requireCanViewLibrary(viewerUid, handle)
        return workoutService.sharedTemplatesOf(author.firebaseUid).map {
            SharedTemplateSummaryDto(
                type = SharedTemplateSummaryDto.Type.WORKOUT_TEMPLATE,
                serverId = it.serverId,
                name = it.name,
                subtitle = it.category.takeIf { c -> c.isNotBlank() },
            )
        }
    }

    fun getSharedDiet(viewerUid: String, handle: String, serverId: UUID): SharedDietDetailDto {
        val author = requireCanViewLibrary(viewerUid, handle)
        val diet = dietService.sharedDietDto(author.firebaseUid, serverId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        val meals = mealService.dtosByServerIds(
            author.firebaseUid,
            diet.meals.orEmpty().mapNotNull { it.mealServerId },
        )
        val foodServerIds = (diet.foodItems.orEmpty().mapNotNull { it.foodServerId } +
            meals.flatMap { m -> m.items.orEmpty().mapNotNull { it.foodServerId } }).toSet()
        return SharedDietDetailDto(diet = diet, meals = meals, foods = foodService.dtosByServerIds(foodServerIds))
    }

    fun getSharedMeal(viewerUid: String, handle: String, serverId: UUID): SharedMealDetailDto {
        val author = requireCanViewLibrary(viewerUid, handle)
        val meal = mealService.sharedMealDto(author.firebaseUid, serverId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        val foodServerIds = meal.items.orEmpty().mapNotNull { it.foodServerId }.toSet()
        return SharedMealDetailDto(meal = meal, foods = foodService.dtosByServerIds(foodServerIds))
    }

    fun getSharedWorkout(viewerUid: String, handle: String, serverId: UUID): SharedWorkoutDetailDto {
        val author = requireCanViewLibrary(viewerUid, handle)
        val (workout, exercises) = workoutService.sharedTemplateBundle(author.firebaseUid, serverId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        return SharedWorkoutDetailDto(workout = workout, exercises = exercises)
    }

    // ── Copy ─────────────────────────────────────────────────────────────────

    fun copy(viewerUid: String, req: CopyRequest): CopyResultDto {
        val author = requireCanViewLibrary(viewerUid, req.handle)
        return copyService.copy(viewerUid, author.firebaseUid, req.entityType, req.sourceServerId)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Gate for reading another user's shared library: must follow the author (or be them)
     * and not be in a block relationship either way. 403 otherwise ("Follow to see").
     */
    private fun requireCanViewLibrary(viewerUid: String, handle: String): User {
        val author = requireUser(handle)
        if (author.firebaseUid == viewerUid) return author
        if (blockRepository.blockExistsEitherWay(viewerUid, author.firebaseUid)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Unavailable")
        }
        if (!followRepository.existsByFollowerUidAndFolloweeUid(viewerUid, author.firebaseUid)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Follow to see this user's library")
        }
        return author
    }

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
