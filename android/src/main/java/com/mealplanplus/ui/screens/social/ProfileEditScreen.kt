package com.mealplanplus.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mealplanplus.data.generated.api.UsersApi
import com.mealplanplus.data.generated.model.ProfileUpdateRequest
import com.mealplanplus.data.repository.HandleTakenException
import com.mealplanplus.data.repository.InvalidHandleException
import com.mealplanplus.data.repository.SocialRepository
import com.mealplanplus.ui.components.SocialAvatar
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Teal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileEditUiState(
    val loading: Boolean = true,
    val handle: String = "",
    val bio: String = "",
    val avatarSeed: String = "",
    val searchable: Boolean = true,
    val displayName: String? = null,
    val availability: String? = null,   // null = idle; "ok" | "taken" | "invalid" | "checking"
    val saving: Boolean = false,
    val savedOk: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val usersApi: UsersApi,
    private val social: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state
    private var checkJob: Job? = null

    init {
        viewModelScope.launch {
            val me = runCatching { usersApi.getMe().body() }.getOrNull()
            _state.update {
                it.copy(
                    loading = false,
                    handle = me?.handle ?: "",
                    bio = me?.bio ?: "",
                    avatarSeed = me?.avatarSeed ?: (me?.handle ?: ""),
                    searchable = me?.isSearchable ?: true,
                    displayName = me?.displayName,
                )
            }
        }
    }

    fun onHandle(v: String) {
        val h = v.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(20)
        _state.update { it.copy(handle = h, availability = if (h.isBlank()) null else "checking", savedOk = false) }
        checkJob?.cancel()
        if (h.isBlank()) return
        checkJob = viewModelScope.launch {
            delay(400)   // debounce
            val res = social.handleAvailable(h)
            _state.update {
                it.copy(availability = when {
                    res == null -> null
                    !res.valid -> "invalid"
                    res.available -> "ok"
                    else -> "taken"
                })
            }
        }
    }

    fun onBio(v: String) = _state.update { it.copy(bio = v.take(300), savedOk = false) }
    fun onSearchable(v: Boolean) = _state.update { it.copy(searchable = v, savedOk = false) }
    fun randomizeAvatar() = _state.update { it.copy(avatarSeed = UUID.randomUUID().toString().take(8), savedOk = false) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, savedOk = false) }
            social.updateProfile(
                ProfileUpdateRequest(
                    handle = s.handle.ifBlank { null },
                    bio = s.bio.ifBlank { null },
                    avatarSeed = s.avatarSeed.ifBlank { null },
                    isSearchable = s.searchable,
                )
            ).onSuccess {
                _state.update { st -> st.copy(saving = false, savedOk = true, error = null) }
            }.onFailure { e ->
                val msg = when (e) {
                    is HandleTakenException -> "That handle is already taken"
                    is InvalidHandleException -> "Handle must be 3–20 chars: a–z, 0–9, _"
                    else -> e.message ?: "Couldn't save"
                }
                _state.update { st -> st.copy(saving = false, error = msg) }
            }
        }
    }
}

@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("Public profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        if (s.loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Teal) }
            return@Column
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SocialAvatar(seed = s.avatarSeed, label = s.displayName ?: s.handle, size = 84.dp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::randomizeAvatar) { Icon(Icons.Default.Refresh, "Shuffle avatar", tint = Teal) }
                        Text("Shuffle avatar", color = Teal, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Text("Handle", color = MutedLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = s.handle,
                onValueChange = viewModel::onHandle,
                singleLine = true,
                prefix = { Text("@") },
                modifier = Modifier.fillMaxWidth(),
            )
            val hint = when (s.availability) {
                "ok" -> "@${s.handle} is available" to Teal
                "taken" -> "@${s.handle} is taken" to androidx.compose.ui.graphics.Color(0xFFEF4444)
                "invalid" -> "3–20 chars: a–z, 0–9, _" to androidx.compose.ui.graphics.Color(0xFFEF4444)
                "checking" -> "Checking…" to MutedLight
                else -> "Your unique @handle so others can find you" to MutedLight
            }
            Text(hint.first, color = hint.second, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))

            Text("Bio", color = MutedLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = s.bio,
                onValueChange = viewModel::onBio,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                keyboardOptions = KeyboardOptions.Default,
            )
            Text("${s.bio.length}/300", color = MutedLight, fontSize = 11.sp, modifier = Modifier.align(Alignment.End).padding(top = 2.dp))

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show me in search", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Let others discover you by handle or name", color = MutedLight, fontSize = 11.5.sp)
                }
                Switch(checked = s.searchable, onCheckedChange = viewModel::onSearchable)
            }

            Spacer(Modifier.height(16.dp))
            s.error?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFEF4444), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
            if (s.savedOk) Text("Saved ✓", color = Teal, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))

            Button(
                onClick = viewModel::save,
                enabled = !s.saving && s.availability != "taken" && s.availability != "invalid",
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = OnAccent),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (s.saving) CircularProgressIndicator(color = OnAccent, modifier = Modifier.height(20.dp))
                else Text("Save", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
