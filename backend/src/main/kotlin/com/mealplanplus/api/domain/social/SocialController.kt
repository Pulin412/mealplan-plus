package com.mealplanplus.api.domain.social

import com.mealplanplus.api.generated.api.SocialApi
import com.mealplanplus.api.generated.model.HandleAvailabilityDto
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import com.mealplanplus.api.generated.model.PublicProfileDto
import com.mealplanplus.api.generated.model.PublicProfileSummaryDto
import com.mealplanplus.api.generated.model.ReportRequest
import com.mealplanplus.api.generated.model.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

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

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
