package com.mealplanplus.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.mealplanplus.data.generated.model.ProviderStatus
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal

private val ReadyGreen = Color(0xFF34A853)

@Composable
fun AiProvidersScreen(onBack: () -> Unit = {}, viewModel: AiProvidersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
                Text("AI providers", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "The assistant tries these in order and fails over automatically. Order and keys are " +
                        "configured on the server; keys are never shown here.",
                    fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(vertical = 8.dp),
                )

                when {
                    state.loading -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Teal, strokeWidth = 2.dp)
                    }
                    state.error != null -> Text(
                        state.error ?: "", fontSize = 13.sp, color = MutedDark,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    else -> Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    ) {
                        state.providers.forEachIndexed { i, p ->
                            ProviderRow(i + 1, p)
                            if (i < state.providers.lastIndex) {
                                Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(CardBorder))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(true); Spacer(Modifier.width(6.dp))
                    Text("ready", fontSize = 11.5.sp, color = MutedFaint)
                    Spacer(Modifier.width(16.dp))
                    StatusDot(false); Spacer(Modifier.width(6.dp))
                    Text("not configured", fontSize = 11.5.sp, color = MutedFaint)
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(order: Int, p: ProviderStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text("$order", fontSize = 12.sp, color = MutedFaint, modifier = Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(p.model, fontSize = 11.5.sp, color = MutedLight)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(p.ready)
            Spacer(Modifier.width(6.dp))
            Text(
                if (!p.enabled) "disabled" else if (p.ready) "ready" else "no key",
                fontSize = 11.5.sp,
                color = if (p.ready && p.enabled) ReadyGreen else MutedFaint,
            )
        }
    }
}

@Composable
private fun StatusDot(on: Boolean) {
    Box(Modifier.size(9.dp).clip(CircleShape).background(if (on) ReadyGreen else MutedFaint))
}
