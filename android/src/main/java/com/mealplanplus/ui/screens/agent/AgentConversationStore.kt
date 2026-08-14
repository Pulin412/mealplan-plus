package com.mealplanplus.ui.screens.agent

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of the current assistant conversation so it survives navigating away from the chat
 * screen and back (the ViewModel is scoped to the nav back-stack entry and is recreated on return).
 * Lives for the app process; cleared on sign-out via [clear]. Not persisted across process death —
 * chat history is ephemeral by design in v1.
 */
@Singleton
class AgentConversationStore @Inject constructor() {
    val messages = mutableListOf<ChatMessage>()
    var lastProvider: String? = null

    fun snapshot(): List<ChatMessage> = messages.toList()

    fun replace(newMessages: List<ChatMessage>, provider: String?) {
        messages.clear()
        messages.addAll(newMessages)
        lastProvider = provider
    }

    fun clear() {
        messages.clear()
        lastProvider = null
    }
}
