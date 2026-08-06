package com.mealplanplus.ui.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal

/**
 * Full-screen spotlight overlay for the guided tour. Draws a scrim with a rounded cutout around the
 * current step's target, plus a tooltip card with Skip / progress / Next. It's rendered above the
 * app's Scaffold (covering the bottom nav) so target bounds — captured in root coordinates by
 * [Modifier.tourTarget] — line up with the scrim.
 *
 * Steps whose target lives on another screen carry a [TourStep.route]; the overlay navigates there
 * first, and the tooltip appears once that target reports its bounds.
 */
@Composable
fun TourOverlay(
    controller: TourController,
    navController: NavHostController,
    onFinish: () -> Unit,
) {
    val step = controller.step ?: return

    // Switch to the step's screen (if any) so its targets compose and report bounds.
    LaunchedEffect(controller.index) {
        step.route?.let { route ->
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
            }
        }
    }

    val density = LocalDensity.current
    val target: Rect? = controller.targets[step.key]
    val scrim = Color.Black.copy(alpha = 0.72f)

    BoxWithConstraints(
        // Swallow every tap on the scrim so the app underneath isn't interactive during the tour.
        Modifier.fillMaxSize().pointerInput(controller.index) { detectTapGestures { } },
    ) {
        val maxHpx = constraints.maxHeight

        // ── Scrim + spotlight cutout ────────────────────────────────────────────
        Canvas(Modifier.fillMaxSize()) {
            if (target == null) {
                drawRect(scrim)
            } else {
                val pad = 6.dp.toPx()
                val radius = 14.dp.toPx()
                val hole = Rect(target.left - pad, target.top - pad, target.right + pad, target.bottom + pad)
                val path = Path().apply { addRoundRect(RoundRect(hole, CornerRadius(radius, radius))) }
                clipPath(path, clipOp = ClipOp.Difference) { drawRect(scrim) }
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(hole.left, hole.top),
                    size = Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // ── Tooltip card ────────────────────────────────────────────────────────
        var cardHpx by remember { mutableStateOf(0) }
        val marginPx = with(density) { 12.dp.toPx() }

        // Anchor the card just below the target if it sits in the top half, otherwise above it.
        // With no target (closing step) centre it vertically.
        val placeBelow = target != null && target.center.y < maxHpx / 2f
        val topOffsetPx = when {
            target == null -> ((maxHpx - cardHpx) / 2f)
            placeBelow -> target.bottom + marginPx
            else -> (target.top - marginPx - cardHpx)
        }.toInt().coerceIn(0, (maxHpx - cardHpx).coerceAtLeast(0))

        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, topOffsetPx) }
                .padding(horizontal = 20.dp)
                .onGloballyPositioned { cardHpx = it.size.height },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(18.dp),
            ) {
                Text(step.title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(step.body, color = MutedDark, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!controller.isLast) {
                        Text(
                            "Skip", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { controller.finish(onFinish) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${controller.index + 1} / ${TOUR_STEPS.size}",
                        color = Muted, fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        if (controller.isLast) "Done" else "Next",
                        color = OnAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Teal)
                            .clickable { controller.next(onFinish) }
                            .padding(horizontal = 20.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}
