package com.mealplanplus.api.domain.feedback

import com.mealplanplus.api.generated.model.FeedbackDto
import com.mealplanplus.api.generated.model.FeedbackRequestDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeedbackService(private val repo: FeedbackRepository) {

    @Transactional
    fun submit(dto: FeedbackRequestDto, firebaseUid: String): FeedbackDto {
        val saved = repo.save(
            Feedback(
                firebaseUid = firebaseUid,
                message = dto.message,
                appVersion = dto.appVersion,
                platform = dto.platform
            )
        )
        return saved.toDto()
    }
}

fun Feedback.toDto() = FeedbackDto(
    id = id,
    message = message,
    appVersion = appVersion,
    platform = platform,
    createdAt = createdAt
)
