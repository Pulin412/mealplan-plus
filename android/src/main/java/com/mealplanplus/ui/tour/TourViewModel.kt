package com.mealplanplus.ui.tour

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the local "tour seen" flag to the UI (first-run trigger + Settings replay). */
@HiltViewModel
class TourViewModel @Inject constructor(private val store: TourStore) : ViewModel() {
    val seen: StateFlow<Boolean> = store.seen
    fun markSeen() = store.markSeen()
    fun replay() = store.reset()
}
