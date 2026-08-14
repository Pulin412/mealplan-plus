package com.mealplanplus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mealplanplus.ui.components.UnsavedChangesDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mealplanplus.ui.tour.LocalTourController
import com.mealplanplus.ui.tour.TourOverlay
import com.mealplanplus.ui.tour.TourViewModel
import com.mealplanplus.ui.tour.rememberTourController
import com.mealplanplus.ui.tour.tourTarget
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mealplanplus.ui.onboarding.OnboardingScreen
import com.mealplanplus.ui.onboarding.OnboardingViewModel
import com.mealplanplus.ui.screens.auth.AuthScreen
import com.mealplanplus.ui.screens.auth.AuthViewModel
import com.mealplanplus.ui.screens.auth.ForgotPasswordScreen
import com.mealplanplus.ui.screens.diets.DietsScreen
import com.mealplanplus.ui.screens.exercises.ExercisesScreen
import com.mealplanplus.ui.screens.foods.FoodsScreen
import com.mealplanplus.ui.screens.groceries.GroceryScreen
import com.mealplanplus.ui.screens.health.HealthScreen
import com.mealplanplus.ui.screens.meals.MealsScreen
import com.mealplanplus.ui.screens.misc.MiscScreen
import com.mealplanplus.ui.screens.home.HomeScreen
import com.mealplanplus.ui.screens.plan.PlanScreen
import com.mealplanplus.ui.screens.profile.ProfileScreen
import com.mealplanplus.ui.screens.runner.SessionRunnerScreen
import com.mealplanplus.ui.screens.social.BlockedAccountsScreen
import com.mealplanplus.ui.screens.social.DiscoverScreen
import com.mealplanplus.ui.screens.social.FollowListScreen
import com.mealplanplus.ui.screens.social.NotificationsScreen
import com.mealplanplus.ui.screens.social.ProfileEditScreen
import com.mealplanplus.ui.screens.social.PublicProfileScreen
import com.mealplanplus.ui.screens.social.SharedDetailScreen
import com.mealplanplus.ui.screens.settings.SettingsScreen
import com.mealplanplus.ui.screens.settings.AiProvidersScreen
import com.mealplanplus.ui.screens.agent.AgentChatScreen
import java.net.URLEncoder

sealed class Screen(val route: String, val label: String) {
    object Today     : Screen("today",     "Today")
    object Plan      : Screen("plan",      "Plan")
    object Exercises : Screen("exercises", "Exercises")
    object Health    : Screen("health",    "Health")
    object Foods     : Screen("foods",     "Foods")
    object Meals     : Screen("meals",     "Meals")
    object Diets     : Screen("diets",     "Diets")
    object Groceries : Screen("groceries", "Groceries")
    object Misc      : Screen("misc",      "More")
    object Profile   : Screen("profile",   "Profile")
    object Settings  : Screen("settings",  "Settings")
    object AgentChat : Screen("agentChat", "Assistant")
    object AiProviders : Screen("aiProviders", "AI providers")
}

private val bottomNavItems = listOf(
    Screen.Today     to Icons.Default.Home,
    Screen.Plan      to Icons.Default.CalendarMonth,
    Screen.Exercises to Icons.Default.FitnessCenter,
    Screen.Health    to Icons.Default.MonitorHeart,
    Screen.Misc      to Icons.Default.GridView,
)

/**
 * Top-level gate: unauthenticated users see the auth flow; signed-in users get the app.
 * The switch is automatic — [AuthViewModel.authState] flips on sign-in/out.
 */
@Composable
fun AppRoot() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val user by authViewModel.authState.collectAsState()
    if (user == null) {
        AuthNavHost(authViewModel)
        return
    }
    // Signed in: gate the app (no bottom nav) behind first-run onboarding until it's done/skipped.
    // Completion is server-authoritative (once per account); wait for the check before showing it.
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingDone by onboardingViewModel.done.collectAsState()
    val checkingOnboarding by onboardingViewModel.checking.collectAsState()
    when {
        checkingOnboarding -> Box(Modifier.fillMaxSize())
        !onboardingDone    -> OnboardingScreen(onboardingViewModel)
        else               -> MealPlanNavHost()
    }
}

@Composable
private fun AuthNavHost(vm: AuthViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "auth") {
        composable("auth")   { AuthScreen(vm, onForgotPassword = { nav.navigate("forgot") }) }
        composable("forgot") { ForgotPasswordScreen(vm, onBack = { nav.popBackStack() }) }
    }
}

@Composable
fun MealPlanNavHost() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination

    // Persistent bottom nav on every in-app screen, except full-screen ones (Session Runner, Assistant chat).
    val fullScreenRoutes = setOf(Screen.AgentChat.route)
    val showBottomBar = currentDest?.route?.startsWith("runner") != true &&
        currentDest?.route !in fullScreenRoutes

    // First-run guided tour: a spotlight overlay drawn above the Scaffold (so it can dim the bottom
    // nav). The controller holds live target bounds, provided to inner screens via LocalTourController.
    val tourViewModel: TourViewModel = hiltViewModel()
    val tourSeen by tourViewModel.seen.collectAsState()
    val tour = rememberTourController()
    LaunchedEffect(Unit) { if (!tourSeen) tour.start() }

    // Guards navigation away from a dirty create/edit screen via routes those screens can't intercept
    // themselves (bottom-nav tab taps). System-back and the editor's X are handled inside each editor.
    val unsavedController = remember { UnsavedChangesController() }

    // App-level snackbar host: any screen/VM flow surfaces user-facing errors via LocalSnackbarController
    // instead of swallowing them or re-plumbing a host per screen.
    val snackbarController = remember { SnackbarController() }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarController) {
        snackbarController.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    CompositionLocalProvider(
        LocalTourController provides tour,
        LocalUnsavedChangesController provides unsavedController,
        LocalSnackbarController provides snackbarController,
    ) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, icon) ->
                        NavigationBarItem(
                            modifier = Modifier.tourTarget("nav_${screen.route}", tour),
                            selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                            onClick  = {
                                // Tapping a tab returns to that top-level screen, clearing any
                                // transient screens (Profile, Settings, Meals…) pushed on top.
                                // No saveState/restoreState — that captured non-tab screens into a
                                // tab's state and resurrected them (e.g. Home re-opening Profile).
                                // Routed through the guard so a dirty editor prompts first.
                                unsavedController.attempt {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon  = { Icon(icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            // TEMP (no bottom nav yet): launch on Today; ☰/back cycle Today → Meals → Diets → Foods → Today.
            startDestination = Screen.Today.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route)     {
                HomeScreen(onMenu = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    onProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } },
                    onAssistant = { navController.navigate(Screen.AgentChat.route) { launchSingleTop = true } },
                    onNotifications = { navController.navigate("notifications") { launchSingleTop = true } },
                    onOpenRunner = { templateId, name ->
                        navController.navigate("runner?templateId=$templateId&name=${URLEncoder.encode(name, "UTF-8")}")
                    },
                    onOpenExerciseRunner = { exerciseId, name ->
                        navController.navigate("runner?exerciseId=$exerciseId&name=${URLEncoder.encode(name, "UTF-8")}")
                    })
            }
            composable(
                route = "runner?templateId={templateId}&exerciseId={exerciseId}&name={name}",
                arguments = listOf(
                    navArgument("templateId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("exerciseId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                SessionRunnerScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Profile.route)   {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onEditPublicProfile = { navController.navigate("profileEdit") },
                    onOpenPublicProfile = { handle -> navController.navigate("u/$handle") },
                    onDiscover = { navController.navigate("discover") },
                    onBlockedAccounts = { navController.navigate("blockedAccounts") },
                )
            }
            composable(Screen.Settings.route)  {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onAiProviders = { navController.navigate(Screen.AiProviders.route) { launchSingleTop = true } },
                )
            }
            composable(Screen.AgentChat.route) { AgentChatScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.AiProviders.route) { AiProvidersScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Plan.route)      { PlanScreen() }
            composable(Screen.Exercises.route) { ExercisesScreen() }
            composable(Screen.Health.route)    { HealthScreen() }
            composable(Screen.Misc.route)      {
                MiscScreen(
                    onFoods = { navController.navigate(Screen.Foods.route) },
                    onMeals = { navController.navigate(Screen.Meals.route) },
                    onDiets = { navController.navigate(Screen.Diets.route) },
                    onGroceries = { navController.navigate(Screen.Groceries.route) },
                )
            }
            composable(Screen.Meals.route)     {
                MealsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Diets.route)     {
                DietsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Foods.route)     {
                FoodsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Groceries.route) {
                GroceryScreen(onMenu = { navController.popBackStack() })
            }

            // ── Social ──────────────────────────────────────────────────────────
            composable("profileEdit") {
                ProfileEditScreen(onBack = { navController.popBackStack() })
            }
            composable("discover") {
                DiscoverScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { handle -> navController.navigate("u/$handle") },
                )
            }
            composable(
                route = "u/{handle}",
                arguments = listOf(navArgument("handle") { type = NavType.StringType }),
            ) {
                PublicProfileScreen(
                    onBack = { navController.popBackStack() },
                    onOpenShared = { handle, type, serverId, own -> navController.navigate("shared/$handle/$type/$serverId?own=$own") },
                    onOpenFollows = { handle, mode -> navController.navigate("follows/$handle/$mode") },
                )
            }
            composable(
                route = "follows/{handle}/{mode}",
                arguments = listOf(
                    navArgument("handle") { type = NavType.StringType },
                    navArgument("mode") { type = NavType.StringType },
                ),
            ) {
                FollowListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { handle -> navController.navigate("u/$handle") },
                )
            }
            composable(
                route = "shared/{handle}/{type}/{serverId}?own={own}",
                arguments = listOf(
                    navArgument("handle") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType },
                    navArgument("serverId") { type = NavType.StringType },
                    navArgument("own") { type = NavType.BoolType; defaultValue = false },
                ),
            ) {
                SharedDetailScreen(onBack = { navController.popBackStack() })
            }
            composable("blockedAccounts") {
                BlockedAccountsScreen(onBack = { navController.popBackStack() })
            }
            composable("notifications") {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { handle -> navController.navigate("u/$handle") },
                    onOpenShared = { handle, type, serverId -> navController.navigate("shared/$handle/$type/$serverId?own=false") },
                )
            }
        }
    }

        if (tour.running) {
            TourOverlay(tour, navController, onFinish = { tourViewModel.markSeen() })
        }

        // Prompt when a dirty editor is being left via guarded navigation (bottom-nav tab tap).
        if (unsavedController.pending != null) {
            UnsavedChangesDialog(
                canSave = unsavedController.guard?.canSave == true,
                onSave = { unsavedController.resolveSave() },
                onDiscard = { unsavedController.resolveDiscard() },
                onDismiss = { unsavedController.cancel() },
            )
        }
    }
    }
}
