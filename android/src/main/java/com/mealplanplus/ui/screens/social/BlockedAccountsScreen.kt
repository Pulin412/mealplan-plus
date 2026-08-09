package com.mealplanplus.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.PublicProfileSummaryDto
import com.mealplanplus.data.repository.SocialRepository
import com.mealplanplus.ui.components.SocialAvatar
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val rows: List<PublicProfileSummaryDto> = emptyList(),
    val busyHandle: String? = null,
)

@HiltViewModel
class BlockedAccountsViewModel @Inject constructor(
    private val social: SocialRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BlockedUiState())
    val state: StateFlow<BlockedUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            // blocks() swallows errors into an empty list; distinguish "empty" from "offline" is not
            // possible here, so an empty result simply shows the empty state.
            val rows = social.blocks()
            _state.update { it.copy(loading = false, rows = rows) }
        }
    }

    fun unblock(handle: String) {
        if (_state.value.busyHandle != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyHandle = handle) }
            val ok = social.unblock(handle)
            _state.update {
                it.copy(
                    busyHandle = null,
                    rows = if (ok) it.rows.filterNot { u -> u.handle == handle } else it.rows,
                )
            }
        }
    }
}

@Composable
fun BlockedAccountsScreen(
    onBack: () -> Unit,
    viewModel: BlockedAccountsViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("Blocked accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            s.rows.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("You haven't blocked anyone.", color = MutedLight)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(s.rows, key = { it.handle }) { u ->
                    BlockedRow(u, busy = s.busyHandle == u.handle) { viewModel.unblock(u.handle) }
                }
            }
        }
    }
}

@Composable
private fun BlockedRow(u: PublicProfileSummaryDto, busy: Boolean, onUnblock: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(seed = u.avatarSeed, label = u.displayName ?: u.handle, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            u.displayName?.let { Text(it, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            Text("@${u.handle}", color = MutedLight, fontSize = 12.5.sp)
        }
        Box(
            Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .clickable(enabled = !busy, onClick = onUnblock).padding(horizontal = 14.dp, vertical = 7.dp),
        ) { Text(if (busy) "…" else "Unblock", color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
    }
}
