package com.mealplanplus.api.domain.social

import com.mealplanplus.api.generated.api.SocialApi
import com.mealplanplus.api.generated.model.HandleAvailabilityDto
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import com.mealplanplus.api.generated.model.PublicProfileDto
import com.mealplanplus.api.generated.model.CopyRequest
import com.mealplanplus.api.generated.model.CopyResultDto
import com.mealplanplus.api.generated.model.PublicProfileSummaryDto
import com.mealplanplus.api.generated.model.ReportRequest
import com.mealplanplus.api.generated.model.SharedDietDetailDto
import com.mealplanplus.api.generated.model.SharedMealDetailDto
import com.mealplanplus.api.generated.model.SharedTemplateSummaryDto
import com.mealplanplus.api.generated.model.SharedWorkoutDetailDto
import com.mealplanplus.api.generated.model.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SocialController(private val service: SocialService) : SocialApi {

    override fun updateMyProfile(profileUpdateRequest: ProfileUpdateRequest): ResponseEntity<UserResponse> =
        ResponseEntity.ok(service.updateMyProfile(currentUid(), profileUpdateRequest))

    override fun checkHandleAvailable(handle: String): ResponseEntity<HandleAvailabilityDto> =
        ResponseEntity.ok(service.checkHandleAvailable(currentUid(), handle))

    override fun searchUsers(q: String, page: Int, size: Int): ResponseEntity<List<PublicProfileSummaryDto>> =
        ResponseEntity.ok(service.searchUsers(currentUid(), q, page, size))

    override fun getPublicProfile(handle: String): ResponseEntity<PublicProfileDto> =
        ResponseEntity.ok(service.getPublicProfile(currentUid(), handle))

    override fun followUser(handle: String): ResponseEntity<Unit> {
        service.followUser(currentUid(), handle)
        return ResponseEntity.noContent().build()
    }

    override fun unfollowUser(handle: String): ResponseEntity<Unit> {
        service.unfollowUser(currentUid(), handle)
        return ResponseEntity.noContent().build()
    }

    override fun listFollowers(handle: String): ResponseEntity<List<PublicProfileSummaryDto>> =
        ResponseEntity.ok(service.listFollowers(currentUid(), handle))

    override fun listFollowing(handle: String): ResponseEntity<List<PublicProfileSummaryDto>> =
        ResponseEntity.ok(service.listFollowing(currentUid(), handle))

    override fun blockUser(handle: String): ResponseEntity<Unit> {
        service.blockUser(currentUid(), handle)
        return ResponseEntity.noContent().build()
    }

    override fun unblockUser(handle: String): ResponseEntity<Unit> {
        service.unblockUser(currentUid(), handle)
        return ResponseEntity.noContent().build()
    }

    override fun reportContent(reportRequest: ReportRequest): ResponseEntity<Unit> {
        service.report(currentUid(), reportRequest)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    override fun listSharedDiets(handle: String): ResponseEntity<List<SharedTemplateSummaryDto>> =
        ResponseEntity.ok(service.listSharedDiets(currentUid(), handle))

    override fun listSharedMeals(handle: String): ResponseEntity<List<SharedTemplateSummaryDto>> =
        ResponseEntity.ok(service.listSharedMeals(currentUid(), handle))

    override fun listSharedWorkouts(handle: String): ResponseEntity<List<SharedTemplateSummaryDto>> =
        ResponseEntity.ok(service.listSharedWorkouts(currentUid(), handle))

    override fun getSharedDiet(handle: String, serverId: UUID): ResponseEntity<SharedDietDetailDto> =
        ResponseEntity.ok(service.getSharedDiet(currentUid(), handle, serverId))

    override fun getSharedMeal(handle: String, serverId: UUID): ResponseEntity<SharedMealDetailDto> =
        ResponseEntity.ok(service.getSharedMeal(currentUid(), handle, serverId))

    override fun getSharedWorkout(handle: String, serverId: UUID): ResponseEntity<SharedWorkoutDetailDto> =
        ResponseEntity.ok(service.getSharedWorkout(currentUid(), handle, serverId))

    override fun copyTemplate(copyRequest: CopyRequest): ResponseEntity<CopyResultDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.copy(currentUid(), copyRequest))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
