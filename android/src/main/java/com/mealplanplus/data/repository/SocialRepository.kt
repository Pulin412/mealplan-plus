package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.SocialApi
import com.mealplanplus.data.generated.model.CopyRequest
import com.mealplanplus.data.generated.model.CopyResultDto
import com.mealplanplus.data.generated.model.HandleAvailabilityDto
import com.mealplanplus.data.generated.model.ProfileUpdateRequest
import com.mealplanplus.data.generated.model.PublicProfileDto
import com.mealplanplus.data.generated.model.PublicProfileSummaryDto
import com.mealplanplus.data.generated.model.SharedDietDetailDto
import com.mealplanplus.data.generated.model.SharedMealDetailDto
import com.mealplanplus.data.generated.model.SharedTemplateSummaryDto
import com.mealplanplus.data.generated.model.SharedWorkoutDetailDto
import com.mealplanplus.data.generated.model.UserResponse
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The social layer is an ONLINE read layer over the offline-first personal data — others' data is
 * fetched live over REST and never written into Room / the sync tables (mirrors
 * [WorkoutSessionRepository], the existing "direct REST, outside sync" precedent). Copy is the only
 * thing that crosses into owned/synced data; after a copy the caller triggers a normal sync-pull.
 */
@Singleton
class SocialRepository @Inject constructor(
    private val api: SocialApi,
) {
    // ── Profile ──────────────────────────────────────────────────────────────
    suspend fun updateProfile(req: ProfileUpdateRequest): Result<UserResponse> = runCatching {
        val r = api.updateMyProfile(req)
        if (r.isSuccessful) r.body()!!
        else when (r.code()) {
            409 -> throw HandleTakenException()
            400 -> throw InvalidHandleException()
            else -> throw IllegalStateException("Couldn't save profile (${r.code()})")
        }
    }

    suspend fun handleAvailable(handle: String): HandleAvailabilityDto? =
        runCatching { api.checkHandleAvailable(handle).body() }.getOrNull()

    // ── Discovery ──────────────────────────────────────────────────────────────
    suspend fun search(q: String): List<PublicProfileSummaryDto> =
        runCatching { api.searchUsers(q).body().orEmpty() }.getOrDefault(emptyList())

    suspend fun profile(handle: String): Result<PublicProfileDto> = runCatching {
        val r = api.getPublicProfile(handle)
        if (r.isSuccessful) r.body()!! else throw IllegalStateException("Profile unavailable (${r.code()})")
    }

    suspend fun followers(handle: String): List<PublicProfileSummaryDto> =
        runCatching { api.listFollowers(handle).body().orEmpty() }.getOrDefault(emptyList())

    suspend fun following(handle: String): List<PublicProfileSummaryDto> =
        runCatching { api.listFollowing(handle).body().orEmpty() }.getOrDefault(emptyList())

    // ── Follow / safety ────────────────────────────────────────────────────────
    suspend fun follow(handle: String): Boolean =
        runCatching { api.followUser(handle).isSuccessful }.getOrDefault(false)

    suspend fun unfollow(handle: String): Boolean =
        runCatching { api.unfollowUser(handle).isSuccessful }.getOrDefault(false)

    suspend fun block(handle: String): Boolean =
        runCatching { api.blockUser(handle).isSuccessful }.getOrDefault(false)

    suspend fun unblock(handle: String): Boolean =
        runCatching { api.unblockUser(handle).isSuccessful }.getOrDefault(false)

    // ── Shared library reads ─────────────────────────────────────────────────────
    suspend fun sharedDiets(handle: String): List<SharedTemplateSummaryDto> =
        runCatching { api.listSharedDiets(handle).body().orEmpty() }.getOrDefault(emptyList())

    suspend fun sharedMeals(handle: String): List<SharedTemplateSummaryDto> =
        runCatching { api.listSharedMeals(handle).body().orEmpty() }.getOrDefault(emptyList())

    suspend fun sharedWorkouts(handle: String): List<SharedTemplateSummaryDto> =
        runCatching { api.listSharedWorkouts(handle).body().orEmpty() }.getOrDefault(emptyList())

    suspend fun sharedDiet(handle: String, serverId: UUID): Result<SharedDietDetailDto> = runCatching {
        api.getSharedDiet(handle, serverId).let { if (it.isSuccessful) it.body()!! else error("Unavailable (${it.code()})") }
    }

    suspend fun sharedMeal(handle: String, serverId: UUID): Result<SharedMealDetailDto> = runCatching {
        api.getSharedMeal(handle, serverId).let { if (it.isSuccessful) it.body()!! else error("Unavailable (${it.code()})") }
    }

    suspend fun sharedWorkout(handle: String, serverId: UUID): Result<SharedWorkoutDetailDto> = runCatching {
        api.getSharedWorkout(handle, serverId).let { if (it.isSuccessful) it.body()!! else error("Unavailable (${it.code()})") }
    }

    // ── Copy ─────────────────────────────────────────────────────────────────
    suspend fun copy(req: CopyRequest): Result<CopyResultDto> = runCatching {
        val r = api.copyTemplate(req)
        if (r.isSuccessful) r.body()!! else throw IllegalStateException("Copy failed (${r.code()})")
    }
}

class HandleTakenException : Exception("That handle is already taken")
class InvalidHandleException : Exception("Handle must be 3–20 chars: a–z, 0–9, _")
