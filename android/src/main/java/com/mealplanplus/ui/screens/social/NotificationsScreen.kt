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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.NotificationDto
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
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class NotificationsUiState(
    val loading: Boolean = true,
    val rows: List<NotificationDto> = emptyList(),
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val social: SocialRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val rows = social.notifications(50)?.items.orEmpty()
            _state.update { it.copy(loading = false, rows = rows) }
            // Opening the feed clears the unread badge server-side.
            if (rows.any { !it.read }) social.markNotificationsRead()
        }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenShared: (handle: String, type: String, serverId: String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            s.rows.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Nothing here yet.", color = MutedLight)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(s.rows, key = { it.id }) { n ->
                    NotificationRow(n) { onTapNotification(n, onOpenProfile, onOpenShared) }
                }
            }
        }
    }
}

private fun onTapNotification(
    n: NotificationDto,
    onOpenProfile: (String) -> Unit,
    onOpenShared: (String, String, String) -> Unit,
) {
    val handle = n.actorHandle ?: return
    when (n.type) {
        NotificationDto.Type.FOLLOW -> onOpenProfile(handle)
        NotificationDto.Type.SHARE -> {
            val serverId = n.subjectServerId?.toString() ?: return
            // The shared-detail route expects WORKOUT_TEMPLATE, but the notification carries WORKOUT.
            val type = when (n.subjectKind) {
                NotificationDto.SubjectKind.DIET -> "DIET"
                NotificationDto.SubjectKind.MEAL -> "MEAL"
                NotificationDto.SubjectKind.WORKOUT -> "WORKOUT_TEMPLATE"
                null -> return
            }
            onOpenShared(handle, type, serverId)
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationDto, onClick: () -> Unit) {
    val name = n.actorDisplayName ?: n.actorHandle?.let { "@$it" } ?: "Someone"
    val text = when (n.type) {
        NotificationDto.Type.FOLLOW -> "$name started following you"
        NotificationDto.Type.SHARE -> {
            val kind = when (n.subjectKind) {
                NotificationDto.SubjectKind.DIET -> "a diet"
                NotificationDto.SubjectKind.MEAL -> "a meal"
                NotificationDto.SubjectKind.WORKOUT -> "a workout"
                null -> "an item"
            }
            val what = n.subjectName?.let { ": $it" } ?: ""
            "$name shared $kind$what"
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(if (n.read) Surface else UnreadTint).border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialAvatar(seed = n.actorAvatarSeed, label = n.actorDisplayName ?: n.actorHandle ?: "?", size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = Ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(relativeTime(n.createdAt), color = MutedLight, fontSize = 11.5.sp)
        }
        if (!n.read) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D6B)))
        }
    }
}

private val UnreadTint = Color(0x1A2E7D6B)

private fun relativeTime(ts: Instant): String {
    val d = Duration.between(ts, Instant.now())
    return when {
        d.toMinutes() < 1 -> "just now"
        d.toMinutes() < 60 -> "${d.toMinutes()}m ago"
        d.toHours() < 24 -> "${d.toHours()}h ago"
        d.toDays() < 7 -> "${d.toDays()}d ago"
        else -> "${d.toDays() / 7}w ago"
    }
}
