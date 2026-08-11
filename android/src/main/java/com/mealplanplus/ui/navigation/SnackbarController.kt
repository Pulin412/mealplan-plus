package com.mealplanplus.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * App-level, one-shot user messages (errors and the occasional confirmation). Any screen or
 * ViewModel-driven flow can call [show]; a single [androidx.compose.material3.SnackbarHost] mounted
 * in [NavGraph] renders them, so error surfacing no longer has to be re-plumbed per screen.
 *
 * Backed by a buffered [MutableSharedFlow] (not Compose state) so a message emitted off-screen — or
 * several in quick succession — is queued and shown rather than dropped. Pair with
 * [com.mealplanplus.data.remote.ApiErrors] to turn an API failure into the string passed here.
 */
class SnackbarController {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    /** Queue a user-facing message. Safe to call from any thread; never suspends or drops. */
    fun show(message: String) {
        if (message.isNotBlank()) _messages.tryEmit(message)
    }
}

val LocalSnackbarController = staticCompositionLocalOf { SnackbarController() }
