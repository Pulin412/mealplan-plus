package com.mealplanplus.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal

private val SUGGESTIONS = listOf(
    "What did I eat today?",
    "How am I tracking against my goals?",
    "Log 100g oats for breakfast",
)

@Composable
fun AgentChatScreen(onBack: () -> Unit = {}, viewModel: AgentChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Keep the newest message in view as the conversation grows / while "thinking".
    LaunchedEffect(state.messages.size, state.sending) {
        val count = state.messages.size + if (state.sending) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    val send: () -> Unit = { viewModel.send(input); input = "" }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            // ── Top bar ──────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
                Text("Assistant", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                state.provider?.let { p ->
                    Text(
                        "via $p", fontSize = 11.sp, color = MutedFaint,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceMuted)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder))

            // ── Messages ─────────────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item { EmptyState() }
                }
                items(state.messages) { msg -> MessageBubble(msg) }
                if (state.sending) {
                    item { ThinkingBubble() }
                }
            }

            // ── Suggestion chips (only before the first message) ──────────────────
            if (state.messages.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SUGGESTIONS.take(2).forEach { s ->
                        SuggestionChip(s) { viewModel.send(s) }
                    }
                }
            }

            // ── Input ────────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask or log a meal…", color = MutedFaint) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 4,
                    enabled = !state.sending,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface, unfocusedContainerColor = Surface,
                        disabledContainerColor = Surface, cursorColor = Teal,
                        focusedIndicatorColor = Teal, unfocusedIndicatorColor = CardBorder,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                val canSend = input.isNotBlank() && !state.sending
                IconButton(
                    onClick = send, enabled = canSend,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (canSend) Teal else SurfaceMuted),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send, "Send",
                        tint = if (canSend) androidx.compose.ui.graphics.Color.White else MutedFaint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == ChatRole.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Text(
            msg.text,
            fontSize = 14.sp,
            color = if (isUser) androidx.compose.ui.graphics.Color.White else Ink,
            modifier = Modifier.widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) Teal else Surface)
                .then(if (isUser) Modifier else Modifier.border(1.dp, CardBorder, RoundedCornerShape(16.dp)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ThinkingBubble() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            "● ● ●", fontSize = 12.sp, color = MutedFaint,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Surface)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🥗", fontSize = 40.sp)
        Spacer(Modifier.height(10.dp))
        Text("Your nutrition assistant", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Ask about your diets, meals, metrics and logs —\nor just say what you ate.",
            fontSize = 12.5.sp, color = MutedLight,
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Text(
        text, fontSize = 12.sp, color = MutedDark,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceMuted)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
