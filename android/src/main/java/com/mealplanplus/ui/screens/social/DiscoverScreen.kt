package com.mealplanplus.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<PublicProfileSummaryDto> = emptyList(),
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val social: SocialRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state
    private var job: Job? = null

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        job?.cancel()
        if (q.isBlank()) { _state.update { it.copy(results = emptyList(), loading = false) }; return }
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            delay(350)
            val r = social.search(q.trim())
            _state.update { it.copy(results = r, loading = false) }
        }
    }
}

@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("Discover people", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        OutlinedTextField(
            value = s.query,
            onValueChange = viewModel::onQuery,
            singleLine = true,
            placeholder = { Text("Search by handle or name") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            s.query.isNotBlank() && s.results.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No users found", color = MutedLight) }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(s.results, key = { it.handle }) { u ->
                    ProfileRow(u, onClick = { onOpenProfile(u.handle) })
                }
            }
        }
    }
}

@Composable
fun ProfileRow(u: PublicProfileSummaryDto, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(seed = u.avatarSeed, label = u.displayName ?: u.handle, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("@${u.handle}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            u.displayName?.let { Text(it, color = MutedLight, fontSize = 12.5.sp) }
        }
        if (u.isFollowedByMe) Text("Following", color = MutedLight, fontSize = 12.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
}
