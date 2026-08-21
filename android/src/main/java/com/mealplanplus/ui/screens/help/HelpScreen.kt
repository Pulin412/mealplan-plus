package com.mealplanplus.ui.screens.help

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal
import kotlinx.coroutines.launch

/**
 * Native mirror of the webapp's public /help page (webapp/src/app/help/page.tsx).
 * Same content, same section order and wording — rendered with the app's design tokens.
 * Static content only (no data/network), so it opens instantly and works offline.
 *
 * Inline emphasis uses a lightweight `**bold**` marker (see [bolded]) so the copy can be
 * kept verbatim from the web page.
 */

private const val PRIVACY_URL = "https://eatmyplan.com/privacy"

private data class Topic(val id: String, val icon: String, val title: String, val blurb: String)

private sealed interface Block {
    data class Para(val text: String) : Block
    data class Steps(val items: List<String>) : Block
    data class Bullets(val items: List<String>) : Block
    data class Tip(val text: String) : Block
    /** Trailing note that links out to the Privacy Policy. */
    data class ParaPrivacy(val text: String) : Block
}

private data class HelpSection(val id: String, val icon: String, val title: String, val blocks: List<Block>)

private val TOPICS = listOf(
    Topic("map", "🧭", "How it fits together", "How foods, meals & diets connect"),
    Topic("getting-started", "🚀", "Getting started", "Sign in, onboarding and your targets"),
    Topic("today", "🏠", "Today", "Log meals by slot & track your day"),
    Topic("plan", "📅", "Plan", "Assign diets & plan meals ahead"),
    Topic("diets", "🥗", "Diets", "Reusable day-plan templates"),
    Topic("meals", "🍲", "Meals", "Build reusable meals"),
    Topic("foods", "🍎", "Foods", "Your food library & search"),
    Topic("exercises", "🏋️", "Exercises & workouts", "Build and run workouts"),
    Topic("session", "⏱️", "Workout sessions", "Run a session & log sets"),
    Topic("health", "❤️", "Health", "Weight, glucose & blood pressure"),
    Topic("groceries", "🛒", "Groceries", "Shopping lists from your plan"),
    Topic("social", "👥", "Social", "Follow, share & discover"),
    Topic("profile", "🎯", "Profile & targets", "Goals and macro targets"),
    Topic("settings", "⚙️", "Settings & data", "Reminders, export & account"),
    Topic("install", "📲", "Install the app", "Add to home screen & offline use"),
    Topic("byo-ai", "🤖", "Bring your own AI", "Connect Claude to your data"),
)

private val SECTIONS = listOf(
    HelpSection("map", "🧭", "How it all fits together", listOf(
        Block.Para("The nutrition side builds up from small pieces to your day: **foods** make up **meals**, meals make up a **diet** (one day's template), diets and meals get scheduled in **Plan**, and you log against them on **Today**. **Groceries** fall out of whatever you planned. Fitness works the same way: **exercises → workouts → a session you run**."),
    )),
    HelpSection("getting-started", "🚀", "Getting started", listOf(
        Block.Para("EatMyPlan is offline-first meal planning and food logging. Everything you create is saved to your account and synced across the web app and Android app."),
        Block.Steps(listOf(
            "Create an account or sign in from the **login** screen (email & password, or Google).",
            "Follow the short **onboarding** to set your goal, body stats and daily targets. You can change these any time.",
            "Land on **Today** — your home base for logging what you eat and how you're tracking.",
        )),
        Block.Tip("Want a guided walkthrough inside the app? Go to **Settings → Help → Replay app tour**."),
    )),
    HelpSection("today", "🏠", "Today — log meals & track your day", listOf(
        Block.Para("Today shows your calories and macros for the current date, plus every meal slot you can log against."),
        Block.Para("There are **9 meal slots**: Early Morning, Breakfast, Noon, Lunch, Evening, Pre-Workout, Post-Workout, Dinner and Post-Dinner."),
        Block.Steps(listOf(
            "Tap a meal slot to log against it. You can log a whole **meal**, an individual **food**, or a diet's planned meal for that slot.",
            "Your calorie ring and protein / carbs / fat totals update instantly as you log.",
            "Expand any logged item to see its ingredients; remove something with the **×** if you logged it by mistake.",
            "Assigned diet or planned meals appear in their slots — mark them complete as you eat, or **Remove from plan** for just that day.",
        )),
        Block.Tip("\"Today\" always means your device's current date — open the app on a new day and it rolls over automatically."),
    )),
    HelpSection("plan", "📅", "Plan — schedule meals ahead", listOf(
        Block.Para("Plan is your calendar. Assign a diet to a day for a full template, or drop individual meals into specific slots."),
        Block.Steps(listOf(
            "Pick a date on the **Plan** screen.",
            "Assign a **diet** to fill that day with its meals, or use **add meal** to plan a single meal into a chosen slot.",
            "The add-meal picker starts on Breakfast; switch the slot selector to filter to meals tagged for that slot, and expand a row to preview its foods.",
            "To take one meal off a day without touching the rest, use **Remove from plan** — a loose meal is deleted, a diet meal is detached from just that day.",
        )),
        Block.Tip("Planning ahead feeds your **Groceries** list — see that section below."),
    )),
    HelpSection("diets", "🥗", "Diets — reusable day-plan templates", listOf(
        Block.Para("A diet is a reusable one-day template of meals arranged by slot. Assign it to any day from Today or Plan."),
        Block.Steps(listOf(
            "Open **Diets** (in the **More** tab) and create a new diet, or browse existing ones.",
            "Add meals to the diet's slots. Give the diet **tags** so you can find it later.",
            "Use the filter bar to narrow by **tags** (match any) and by **slot** — searching within a selected slot only matches meals in that slot.",
        )),
    )),
    HelpSection("meals", "🍲", "Meals — build reusable meals", listOf(
        Block.Para("A meal is a named group of foods (with quantities) you eat together — log it in one tap instead of adding foods one by one."),
        Block.Steps(listOf(
            "Open **Meals** (in **More**) and create a meal.",
            "Add foods from your library and set quantities; the meal's calories and macros are the sum of its foods.",
            "Tag the meal for the slots it suits (e.g. Breakfast) so it surfaces in the right pickers.",
        )),
    )),
    HelpSection("foods", "🍎", "Foods — your food library", listOf(
        Block.Para("Foods are the building blocks of meals and logs, each with calories and macros per serving."),
        Block.Steps(listOf(
            "Open **Foods** (in **More**) to see your library.",
            "Create a food with its nutrition, or **search online** to pull nutrition from the public Open Food Facts database.",
            "Reuse foods across meals, diets and quick logs.",
        )),
    )),
    HelpSection("exercises", "🏋️", "Exercises & workouts", listOf(
        Block.Para("Build exercises and group them into workouts you can run and track."),
        Block.Para("Each exercise has a **type** that changes what you log:"),
        Block.Bullets(listOf(
            "**Strength** — reps & weight per set.",
            "**Cardio** — reps & weight, plus optional distance.",
            "**Timed** — minutes & seconds per set (plus optional distance).",
        )),
        Block.Steps(listOf(
            "Open **Exercises**, create exercises and give them tags.",
            "Build a **workout** from those exercises.",
            "Plan a workout to a day, or start it straight from the Exercises screen.",
        )),
    )),
    HelpSection("session", "⏱️", "Workout sessions — run & log", listOf(
        Block.Para("Running a workout opens the session runner, which walks you through each exercise and records your sets."),
        Block.Steps(listOf(
            "Start a workout to open the **runner**. The Ready screen shows last time's sets, reps and any note.",
            "Log each set — reps + weight, or duration for timed exercises.",
            "Add a **note** per exercise or for the whole session (e.g. how it felt, what to change).",
            "Finish to save the session; it appears in your workout history.",
        )),
    )),
    HelpSection("health", "❤️", "Health — metrics & trends", listOf(
        Block.Para("Log the health readings you care about and see them trend over time."),
        Block.Steps(listOf(
            "Open **Health** and add a reading: **weight**, **blood glucose** or **blood pressure**.",
            "Switch the range between **7 days** and **30 days** to see recent trends.",
            "Your latest weight feeds back into your targets on Profile.",
        )),
        Block.ParaPrivacy("Health data is sensitive and only ever stored because you enter it — see the Privacy Policy."),
    )),
    HelpSection("groceries", "🛒", "Groceries — shopping lists from your plan", listOf(
        Block.Para("Turn your planned meals and diets into a shopping list, grouped and ready for the store."),
        Block.Steps(listOf(
            "Open **Groceries** (in **More**) and pick the date range to cover.",
            "**Refresh** to rebuild the list from everything planned in that range (diets + individual planned meals).",
            "Check items off as you shop. Use **Clear list** (left of refresh) to empty the whole list and dates.",
        )),
        Block.Tip("The list is rebuilt on manual refresh, so add your meals to the plan first, then refresh."),
    )),
    HelpSection("social", "👥", "Social — follow, share & discover", listOf(
        Block.Para("Optionally connect with other people to share and discover content."),
        Block.Steps(listOf(
            "Set up your public handle and profile in **Social**.",
            "Use **Discover** to find people, **follow** them, and share your own content.",
            "You can **block** or **report** anyone; blocked users are managed under **Settings → Blocked**.",
        )),
    )),
    HelpSection("profile", "🎯", "Profile & targets", listOf(
        Block.Para("Your profile drives your personalised calorie and macro targets."),
        Block.Steps(listOf(
            "Open **Profile** and set your body stats (age, sex, height, weight) and your goal.",
            "We estimate calorie needs (BMR / TDEE) from these; you can override calories, protein, carbs and fat manually.",
            "Targets show up on Today so you can log against them.",
        )),
    )),
    HelpSection("settings", "⚙️", "Settings & your data", listOf(
        Block.Para("Manage reminders, export your data and control your account under **Settings**."),
        Block.Bullets(listOf(
            "**Daily log reminder** — toggle a nudge if you haven't logged.",
            "**Export** — download your data as a **CSV** file.",
            "**Replay app tour / onboarding** — under the Help section.",
            "**Send feedback** — report a bug or suggest an improvement (your app version is attached automatically).",
            "**Delete account** — permanently removes your profile, health data and content.",
        )),
    )),
    HelpSection("install", "📲", "Install the app (offline use)", listOf(
        Block.Para("EatMyPlan is a Progressive Web App — install it to your home screen for a full-screen, app-like experience that works offline for what you've already loaded."),
        Block.Bullets(listOf(
            "**iPhone / iPad (Safari):** tap **Share** → **Add to Home Screen**.",
            "**Android (Chrome):** tap the **⋮** menu → **Install app** / **Add to Home screen**. There's also a native Android app available.",
            "**Desktop (Chrome / Edge):** use the **install** icon in the address bar.",
        )),
        Block.Tip("Reads are cached, so recently-viewed screens still open without a connection; new changes sync when you're back online."),
    )),
    HelpSection("byo-ai", "🤖", "Bring your own AI", listOf(
        Block.Para("Prefer to work with your own AI assistant? EatMyPlan exposes a secure **MCP connector** so you can connect Claude (or any MCP-compatible client) directly to your data — read your dashboard, search foods, log meals and more."),
        Block.Steps(listOf(
            "In Claude, add a custom connector pointing at **https://api.eatmyplan.com/mcp**.",
            "Sign in when prompted — access uses secure OAuth, and the AI only ever sees your own account's data.",
            "Ask it to show today's dashboard, search foods, or log a meal. Write actions require you to grant permission.",
        )),
        Block.Tip("This runs on your own AI subscription and is entirely optional — the app works fully without it."),
    )),
)

/** Splits on `**` and bolds every odd segment, so verbatim web copy keeps its emphasis. */
private fun bolded(text: String, ink: Color): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = ink)) { append(part) }
        else append(part)
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit = {}) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val ink = Ink
    // Item layout in the LazyColumn: 0 = header, 1 = topic grid, then one item per section, then footer.
    val firstSectionIndex = 2

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("Help", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item {
                Column {
                    Text("EatMyPlan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Teal)
                    Spacer(Modifier.height(14.dp))
                }
            }
            item {
                TopicGrid(onTopic = { id ->
                    val idx = SECTIONS.indexOfFirst { it.id == id }
                    if (idx >= 0) scope.launch { listState.animateScrollToItem(firstSectionIndex + idx) }
                })
            }
            itemsIndexed(SECTIONS) { _, section -> SectionView(section, ink, onOpenPrivacy = { uriHandler.openUri(PRIVACY_URL) }) }
            item { Footer(ink, onOpenPrivacy = { uriHandler.openUri(PRIVACY_URL) }) }
        }
    }
}

@Composable
private fun TopicGrid(onTopic: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TOPICS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t ->
                    Row(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface)
                            .clickable { onTopic(t.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(t.icon, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(t.blurb, fontSize = 11.sp, color = MutedLight)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionView(section: HelpSection, ink: Color, onOpenPrivacy: () -> Unit) {
    Column(Modifier.padding(top = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.icon, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(section.title, fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(6.dp))
        section.blocks.forEach { block ->
            when (block) {
                is Block.Para -> Text(bolded(block.text, ink), fontSize = 13.5.sp, color = MutedLight,
                    lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
                is Block.ParaPrivacy -> Text(
                    bolded(block.text, ink), fontSize = 13.5.sp, color = MutedLight, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp).clickable { onOpenPrivacy() })
                is Block.Steps -> Column(Modifier.padding(top = 4.dp)) {
                    block.items.forEachIndexed { i, it ->
                        NumberedRow(i + 1, bolded(it, ink))
                    }
                }
                is Block.Bullets -> Column(Modifier.padding(top = 4.dp)) {
                    block.items.forEach { BulletRow(bolded(it, ink)) }
                }
                is Block.Tip -> TipBox(block.text, ink)
            }
        }
    }
}

@Composable
private fun NumberedRow(n: Int, text: AnnotatedString) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text("$n.", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
            modifier = Modifier.width(20.dp))
        Text(text, fontSize = 13.5.sp, color = MutedLight, lineHeight = 20.sp)
    }
}

@Composable
private fun BulletRow(text: AnnotatedString) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text("•", fontSize = 13.5.sp, color = MutedFaint, modifier = Modifier.width(20.dp))
        Text(text, fontSize = 13.5.sp, color = MutedLight, lineHeight = 20.sp)
    }
}

@Composable
private fun TipBox(text: String, ink: Color) {
    val body = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Teal)) { append("Tip · ") }
        append(bolded(text, ink))
    }
    Box(
        Modifier.padding(top = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Teal.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(body, fontSize = 12.5.sp, color = ink, lineHeight = 18.sp)
    }
}

@Composable
private fun Footer(ink: Color, onOpenPrivacy: () -> Unit) {
    Column(Modifier.padding(top = 28.dp)) {
        Text(
            bolded("Still stuck? Send feedback from **Settings → Send feedback**. See also our Privacy Policy.", ink),
            fontSize = 12.5.sp, color = MutedLight, lineHeight = 18.sp,
            modifier = Modifier.clickable { onOpenPrivacy() },
        )
        Text("© 2026 EatMyPlan", fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(top = 12.dp))
    }
}
