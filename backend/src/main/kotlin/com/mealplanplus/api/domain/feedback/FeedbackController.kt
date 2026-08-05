package com.mealplanplus.api.domain.feedback

import com.mealplanplus.api.generated.api.FeedbackApi
import com.mealplanplus.api.generated.model.FeedbackDto
import com.mealplanplus.api.generated.model.FeedbackRequestDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedbackController(private val service: FeedbackService) : FeedbackApi {

    override fun submitFeedback(feedbackRequestDto: FeedbackRequestDto): ResponseEntity<FeedbackDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.submit(feedbackRequestDto, currentUid()))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
