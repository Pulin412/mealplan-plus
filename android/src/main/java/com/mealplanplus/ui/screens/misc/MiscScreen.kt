package com.mealplanplus.ui.screens.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted

/** The "More" tab — links to the secondary library pages. */
@Composable
fun MiscScreen(
    onFoods: () -> Unit = {},
    onMeals: () -> Unit = {},
    onDiets: () -> Unit = {},
    onGroceries: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("More", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            Text("Nutrition library & shopping", fontSize = 12.5.sp, color = MutedLight, modifier = Modifier.padding(bottom = 16.dp))

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            ) {
                MiscRow("🍎", "Foods", "Your food library", onFoods)
                Divider()
                MiscRow("🍲", "Meals", "Reusable meals", onMeals)
                Divider()
                MiscRow("🥗", "Diets", "Day-plan templates", onDiets)
                Divider()
                MiscRow("🛒", "Groceries", "Shopping lists from your plan", onGroceries)
            }
        }
    }
}

@Composable
private fun MiscRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(SurfaceMuted), Alignment.Center) {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(subtitle, fontSize = 11.5.sp, color = MutedLight)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MutedFaint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(CardBorder))
