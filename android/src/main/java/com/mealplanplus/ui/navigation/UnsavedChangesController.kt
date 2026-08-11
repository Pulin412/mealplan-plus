package com.mealplanplus.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-level guard so navigation that happens OUTSIDE an editor (bottom-nav tab taps, etc.) still
 * prompts for unsaved changes. An open create/edit screen registers a [Guard] while it's dirty;
 * navigation routed through [attempt] is deferred until the user resolves the prompt.
 *
 * The system-back button and the editor's own X are handled locally inside each editor; this covers
 * the leave-vectors an editor can't intercept itself.
 */
class UnsavedChangesController {
    /** The active editor's save/discard hooks. [canSave] mirrors its Save-button validation. */
    data class Guard(val canSave: Boolean, val onSave: () -> Unit, val onDiscard: () -> Unit)

    var guard by mutableStateOf<Guard?>(null)
    var pending by mutableStateOf<(() -> Unit)?>(null)
        private set

    /** Run [proceed] now if nothing's dirty; otherwise stash it and let the prompt decide. */
    fun attempt(proceed: () -> Unit) {
        if (guard == null) proceed() else pending = proceed
    }

    fun resolveSave() { guard?.onSave?.invoke(); proceedAndClear() }
    fun resolveDiscard() { guard?.onDiscard?.invoke(); proceedAndClear() }
    fun cancel() { pending = null }

    private fun proceedAndClear() {
        val p = pending
        pending = null
        p?.invoke()
    }
}

val LocalUnsavedChangesController = staticCompositionLocalOf { UnsavedChangesController() }
