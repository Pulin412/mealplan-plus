package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.FeedbackApi
import com.mealplanplus.data.generated.model.FeedbackRequestDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-backed in-app feedback. A single write endpoint — the app POSTs the user's message plus
 * the client version/platform so submissions can be triaged by build. Nothing is stored locally.
 */
@Singleton
class FeedbackRepository @Inject constructor(
    private val api: FeedbackApi,
) {
    suspend fun submit(message: String, appVersion: String, platform: String = "android"): Result<Unit> =
        runCatching {
            val resp = api.submitFeedback(
                FeedbackRequestDto(message = message, appVersion = appVersion, platform = platform),
            )
            if (!resp.isSuccessful) error("HTTP ${resp.code()}")
            Unit
        }
}
