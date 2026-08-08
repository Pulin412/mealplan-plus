package com.mealplanplus.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.PublicProfileSummaryDto
import com.mealplanplus.data.repository.SocialRepository
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedLight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val social: SocialRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val handle: String = savedState.get<String>("handle").orEmpty()
    val mode: String = savedState.get<String>("mode").orEmpty()   // "followers" | "following"

    private val _state = MutableStateFlow<List<PublicProfileSummaryDto>?>(null)
    val state: StateFlow<List<PublicProfileSummaryDto>?> = _state

    init {
        viewModelScope.launch {
            _state.update { if (mode == "following") social.following(handle) else social.followers(handle) }
        }
    }
}

@Composable
fun FollowListScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel(),
) {
    val list by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text(
                if (viewModel.mode == "following") "Following" else "Followers",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink,
            )
        }
        when {
            list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            list!!.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(if (viewModel.mode == "following") "Not following anyone yet" else "No followers yet", color = MutedLight)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(list!!, key = { it.handle }) { u -> ProfileRow(u, onClick = { onOpenProfile(u.handle) }) }
            }
        }
    }
}
