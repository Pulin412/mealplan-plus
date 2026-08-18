package com.mealplanplus.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal
import androidx.compose.foundation.border

/** Human-facing label + blurb for known flags; unknown keys fall back to the raw key. */
private fun flagLabel(key: String): String = when (key) {
    "mcp_server" -> "MCP server"
    else -> key
}

private fun flagDescription(key: String): String = when (key) {
    "mcp_server" -> "Let users connect their own Claude to read/write their meal plan."
    else -> "Feature flag: $key"
}

@Composable
fun AdminScreen(onBack: () -> Unit = {}, viewModel: AdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(AppBg)) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("Admin", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            SectionLabel("Feature flags")

            state.error?.let { msg ->
                Text(msg, fontSize = 12.sp, color = Color_Error, modifier = Modifier.padding(vertical = 6.dp))
            }

            when {
                state.loading && state.flags.isEmpty() ->
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Teal)
                    }

                state.flags.isEmpty() ->
                    Text("No feature flags.", fontSize = 13.sp, color = MutedLight, modifier = Modifier.padding(vertical = 16.dp))

                else -> Card {
                    state.flags.forEachIndexed { i, flag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(flagLabel(flag.key), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(flagDescription(flag.key), fontSize = 11.5.sp, color = MutedLight)
                            }
                            Spacer(Modifier.width(10.dp))
                            Switch(
                                checked = flag.enabled,
                                onCheckedChange = { on -> viewModel.setEnabled(flag.key, on) },
                                enabled = !state.pending.contains(flag.key),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                    checkedTrackColor = Teal,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private val Color_Error = androidx.compose.ui.graphics.Color(0xFFD9534F)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        verticalArrangement = Arrangement.Top,
        content = content,
    )
}
