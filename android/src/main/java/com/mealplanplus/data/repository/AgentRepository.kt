package com.mealplanplus.data.repository

import android.content.Context
import android.util.Log
import com.mealplanplus.data.remote.AgentChatRequest
import com.mealplanplus.data.remote.MealPlanApi
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MealPlanApi,
    private val syncRepository: SyncRepository
) {
    private val TAG = "AgentRepository"

    // Push local changes first so the agent sees the latest state, then chat,
    // then pull so any food the agent logged appears immediately in the local DB.
    suspend fun chat(message: String, date: String? = null, slot: String? = null): Result<String> =
        runCatching {
            val userId = resolveUserId()
            if (userId != null) {
                syncRepository.push(userId).onFailure {
                    Log.w(TAG, "Pre-chat push failed (non-fatal): ${it.message}")
                }
            }

            val reply = api.agentChat(AgentChatRequest(message, date, slot)).reply

            if (userId != null) {
                syncRepository.pull(userId, since = 0L).onFailure {
                    Log.w(TAG, "Post-chat pull failed (non-fatal): ${it.message}")
                }
            }

            reply
        }

    private suspend fun resolveUserId(): Long? =
        AuthPreferences.getUserId(context).firstOrNull()
}
