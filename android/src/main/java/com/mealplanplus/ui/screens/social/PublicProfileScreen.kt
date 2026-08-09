package com.mealplanplus.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.PublicProfileDto
import com.mealplanplus.data.generated.model.ReportRequest
import com.mealplanplus.data.generated.model.SharedTemplateSummaryDto
import com.mealplanplus.data.repository.SocialRepository
import com.mealplanplus.ui.components.SocialAvatar
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibTab { DIETS, MEALS, WORKOUTS }

data class PublicProfileUiState(
    val loading: Boolean = true,
    val profile: PublicProfileDto? = null,
    val error: String? = null,
    val tab: LibTab = LibTab.DIETS,
    val diets: List<SharedTemplateSummaryDto> = emptyList(),
    val meals: List<SharedTemplateSummaryDto> = emptyList(),
    val workouts: List<SharedTemplateSummaryDto> = emptyList(),
    val followBusy: Boolean = false,
    val blockBusy: Boolean = false,
    val reportBusy: Boolean = false,
    val reportSent: Boolean = false,
)

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val social: SocialRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val handle: String = savedState.get<String>("handle").orEmpty()
    private val _state = MutableStateFlow(PublicProfileUiState())
    val state: StateFlow<PublicProfileUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            social.profile(handle).onSuccess { p ->
                _state.update { it.copy(loading = false, profile = p) }
                if (p.isFollowedByMe || p.isMe) loadLibrary()
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Unavailable") }
            }
        }
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            val d = social.sharedDiets(handle); val m = social.sharedMeals(handle); val w = social.sharedWorkouts(handle)
            _state.update { it.copy(diets = d, meals = m, workouts = w) }
        }
    }

    fun setTab(t: LibTab) = _state.update { it.copy(tab = t) }

    fun toggleFollow() {
        val p = _state.value.profile ?: return
        viewModelScope.launch {
            _state.update { it.copy(followBusy = true) }
            val ok = if (p.isFollowedByMe) social.unfollow(handle) else social.follow(handle)
            _state.update { it.copy(followBusy = false) }
            if (ok) load()
        }
    }

    /** Blocks the user; on success invokes [onBlocked] so the caller can navigate away (a blocked
     *  user's profile 403s, so we can't stay here). */
    fun block(onBlocked: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(blockBusy = true) }
            val ok = social.block(handle)
            _state.update { it.copy(blockBusy = false) }
            if (ok) onBlocked()
        }
    }

    fun report(reason: String, detail: String?) {
        val p = _state.value.profile ?: return
        viewModelScope.launch {
            _state.update { it.copy(reportBusy = true) }
            social.report(
                ReportRequest(
                    entityType = ReportRequest.EntityType.USER,
                    reportedHandle = p.handle,
                    reason = reason,
                    detail = detail,
                ),
            )
            _state.update { it.copy(reportBusy = false, reportSent = true) }
        }
    }

    fun clearReportSent() = _state.update { it.copy(reportSent = false) }
}

@Composable
fun PublicProfileScreen(
    onBack: () -> Unit,
    onOpenShared: (handle: String, type: String, serverId: String) -> Unit,
    onOpenFollows: (handle: String, mode: String) -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("@${viewModel.handle}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            val p = s.profile
            if (p != null && !p.isMe) {
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More options", tint = MutedLight) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Report @${viewModel.handle}") }, onClick = { menuOpen = false; reportOpen = true })
                        DropdownMenuItem(
                            text = { Text(if (s.blockBusy) "Blocking…" else "Block @${viewModel.handle}", color = Danger) },
                            enabled = !s.blockBusy,
                            onClick = { menuOpen = false; viewModel.block(onBack) },
                        )
                    }
                }
            }
        }
        if (reportOpen) {
            ReportDialog(
                subject = "@${viewModel.handle}",
                busy = s.reportBusy,
                onDismiss = { reportOpen = false },
                onSubmit = { reason, detail -> viewModel.report(reason, detail) },
            )
        }
        if (s.reportSent) {
            AlertDialog(
                onDismissRequest = { viewModel.clearReportSent(); reportOpen = false },
                title = { Text("Report received") },
                text = { Text("Thanks — our team will review @${viewModel.handle}. You can also block them to stop seeing their content.") },
                confirmButton = { TextButton(onClick = { viewModel.clearReportSent(); reportOpen = false }) { Text("Done") } },
            )
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Teal) }
            s.error != null || s.profile == null ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.error ?: "Unavailable", color = MutedLight) }
            else -> ProfileBody(s, viewModel, onOpenShared, onOpenFollows)
        }
    }
}

@Composable
private fun ProfileBody(
    s: PublicProfileUiState,
    vm: PublicProfileViewModel,
    onOpenShared: (String, String, String) -> Unit,
    onOpenFollows: (String, String) -> Unit,
) {
    val p = s.profile!!
    Column(Modifier.fillMaxSize()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SocialAvatar(seed = p.avatarSeed, label = p.displayName ?: p.handle, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                p.displayName?.let { Text(it, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Row {
                    Text("${p.followerCount} followers", color = MutedLight, fontSize = 12.5.sp,
                        modifier = Modifier.clickable { onOpenFollows(vm.handle, "followers") })
                    Text("  ·  ", color = MutedLight, fontSize = 12.5.sp)
                    Text("${p.followingCount} following", color = MutedLight, fontSize = 12.5.sp,
                        modifier = Modifier.clickable { onOpenFollows(vm.handle, "following") })
                }
            }
        }
        p.bio?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Ink, fontSize = 13.5.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Spacer(Modifier.height(12.dp))

        if (!p.isMe) {
            val following = p.isFollowedByMe
            Box(Modifier.padding(horizontal = 16.dp)) {
                if (following) {
                    OutlinedButton(onClick = vm::toggleFollow, enabled = !s.followBusy, modifier = Modifier.fillMaxWidth()) {
                        Text("Following")
                    }
                } else {
                    Button(
                        onClick = vm::toggleFollow, enabled = !s.followBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = OnAccent),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Follow", fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Gate for non-followers
        if (!p.isFollowedByMe && !p.isMe) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Follow to see their library", color = MutedLight, fontSize = 14.sp)
            }
            return
        }

        // Tabs
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabChip("Diets", s.tab == LibTab.DIETS) { vm.setTab(LibTab.DIETS) }
            TabChip("Meals", s.tab == LibTab.MEALS) { vm.setTab(LibTab.MEALS) }
            TabChip("Workouts", s.tab == LibTab.WORKOUTS) { vm.setTab(LibTab.WORKOUTS) }
        }
        Spacer(Modifier.height(8.dp))

        val (items, typeName) = when (s.tab) {
            LibTab.DIETS -> s.diets to "DIET"
            LibTab.MEALS -> s.meals to "MEAL"
            LibTab.WORKOUTS -> s.workouts to "WORKOUT_TEMPLATE"
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Nothing shared yet", color = MutedLight) }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(items, key = { it.serverId }) { item ->
                    SharedItemRow(item) { onOpenShared(vm.handle, typeName, item.serverId.toString()) }
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) Teal else Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(label, color = if (selected) OnAccent else Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SharedItemRow(item: SharedTemplateSummaryDto, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            item.subtitle?.let { Text(it, color = MutedLight, fontSize = 12.sp) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MutedFaint)
    }
}
