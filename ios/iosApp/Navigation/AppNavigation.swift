import SwiftUI
import shared

enum AppScreen: Hashable {
    case login
    case signUp
    case home
    case foods
    case addFood
    case editFood(id: Int64)
    case meals
    case addMeal
    case editMeal(id: Int64)
    case diets
    case dietDetail(id: Int64)
    case addDiet
    case editDiet(id: Int64)
    case dietMealSlot(dietId: Int64, slot: String)
    case dailyLog(date: String) // ISO date string
    case dietPicker(date: String)
    case calendar
    case groceryLists
    case groceryDetail(id: Int64)
    case healthMetrics
    case addMetric
    case charts
    case settings
    case profile
}

@MainActor
class AppState: ObservableObject {
    @Published var isLoggedIn: Bool = false
    @Published var currentUserId: Int64? = nil
    @Published var navigationPath = NavigationPath()
    @Published var isLoading: Bool = true  // Start true - show loading while DB inits

    private let userDefaults = UserDefaults.standard

    // Lazy preferencesManager - don't access during init
    private var _preferencesManager: PreferencesManager?
    private var preferencesManager: PreferencesManager {
        if _preferencesManager == nil {
            _preferencesManager = RepositoryProvider.shared.preferencesManager
        }
        return _preferencesManager!
    }

    private let isLoggedInKey = "is_logged_in"
    private let userIdKey = "user_id"
    private let darkModeKey = "dark_mode_enabled"
    @Published var isDarkMode: Bool

    init() {
        self.isDarkMode = UserDefaults.standard.bool(forKey: "dark_mode_enabled")
        // Start async initialization - don't block main thread
        Task { await initializeApp() }
    }

    func setDarkMode(_ enabled: Bool) {
        isDarkMode = enabled
        userDefaults.set(enabled, forKey: darkModeKey)
    }

    private func initializeApp() async {
        // Initialize database on background thread to avoid blocking main thread
        await Task.detached(priority: .userInitiated) {
            // This triggers DatabaseManager initialization off main thread
            _ = RepositoryProvider.shared.foodRepository
        }.value

        // Initialize database
        await DatabaseSeeder.shared.initialize()

        // Check auth state (uses UserDefaults, fast)
        checkAuthState()

        // Done loading
        isLoading = false
    }

    private func checkAuthState() {
        // Check persisted auth state on launch via UserDefaults directly
        // (PreferencesManager uses NSUserDefaults underneath)
        isLoggedIn = userDefaults.bool(forKey: isLoggedInKey)
        if userDefaults.object(forKey: userIdKey) != nil {
            currentUserId = Int64(userDefaults.integer(forKey: userIdKey))
        }
    }

    func login(userId: Int64) {
        Task {
            try? await preferencesManager.setLoggedIn(userId: userId)
        }
        // Also update local state immediately
        userDefaults.set(true, forKey: isLoggedInKey)
        userDefaults.set(Int(userId), forKey: userIdKey)
        self.currentUserId = userId
        self.isLoggedIn = true
    }

    func logout() {
        Task {
            try? await preferencesManager.clearAuth()
        }
        // Also update local state immediately
        userDefaults.set(false, forKey: isLoggedInKey)
        userDefaults.removeObject(forKey: userIdKey)
        self.currentUserId = nil
        self.isLoggedIn = false
        self.navigationPath = NavigationPath()
    }

    func navigate(to screen: AppScreen) {
        navigationPath.append(screen)
    }

    func goBack() {
        if !navigationPath.isEmpty {
            navigationPath.removeLast()
        }
    }
}

struct AppNavigation: View {
    @StateObject private var appState = AppState()

    var body: some View {
        Group {
            if appState.isLoading {
                ProgressView("Loading...")
            } else if appState.isLoggedIn {
                MainTabView()
            } else {
                AuthNavigationView()
            }
        }
        .environmentObject(appState)
        .preferredColorScheme(appState.isDarkMode ? .dark : .light)
    }
}

struct AuthNavigationView: View {
    @State private var showSignUp = false
    @State private var showForgotPassword = false

    var body: some View {
        NavigationStack {
            if showSignUp {
                SignUpScreen(showSignUp: $showSignUp)
            } else if showForgotPassword {
                ForgotPasswordScreen(showForgotPassword: $showForgotPassword)
            } else {
                LoginScreen(showSignUp: $showSignUp, showForgotPassword: $showForgotPassword)
            }
        }
    }
}

struct MainTabView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeTab(onNavigateToLogWithDate: { date in
                selectedTab = 2
                // date navigation handled inside LogTab via notification
                NotificationCenter.default.post(name: .navigateToDate, object: date)
            })
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(0)

            MealPlanTab()
                .tabItem {
                    Label("Meal Plan", systemImage: "calendar")
                }
                .tag(1)

            LogTab()
                .tabItem {
                    Label("Log", systemImage: "square.and.pencil")
                }
                .tag(2)

            DietsTab()
                .tabItem {
                    Label("Diets", systemImage: "fork.knife")
                }
                .tag(3)

            HealthTab()
                .tabItem {
                    Label("Health", systemImage: "heart.fill")
                }
                .tag(4)

            GroceryTab()
                .tabItem {
                    Label("Grocery", systemImage: "cart.fill")
                }
                .tag(5)
        }
        .accentColor(Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0))
        .onReceive(NotificationCenter.default.publisher(for: .navigateToLog)) { notification in
            selectedTab = 2
            if let isoDate = notification.object as? String {
                NotificationCenter.default.post(name: .navigateToDate, object: isoDate)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .navigateToTab)) { notification in
            if let tab = notification.object as? Int {
                selectedTab = tab
            }
        }
    }
}

// MARK: - Notification names for log navigation
extension Notification.Name {
    static let navigateToDate    = Notification.Name("navigateToDate")
    /// Post with an ISO date String to switch to the Log tab and jump to that date.
    static let navigateToLog     = Notification.Name("navigateToLog")
    /// Post with an Int as object to switch to that tab index.
    static let navigateToTab     = Notification.Name("navigateToTab")
    static let navigateToFoods   = Notification.Name("navigateToFoods")
    static let navigateToMeals   = Notification.Name("navigateToMeals")
    static let navigateToSettings = Notification.Name("navigateToSettings")
}

// Tab wrapper views
struct HomeTab: View {
    var onNavigateToLogWithDate: ((String) -> Void)?

    var body: some View {
        NavigationStack {
            HomeScreen(onNavigateToLogWithDate: onNavigateToLogWithDate)
        }
    }
}

struct MealPlanTab: View {
    var body: some View {
        MealPlanScreen()
    }
}

struct LogTab: View {
    var body: some View {
        NavigationStack {
            DailyLogScreen()
        }
    }
}

struct HealthTab: View {
    var body: some View {
        NavigationStack {
            HealthMetricsScreen()
        }
    }
}

struct GroceryTab: View {
    var body: some View {
        NavigationStack {
            GroceryListsScreen()
        }
    }
}

struct DietsTab: View {
    var body: some View {
        NavigationStack {
            DietsScreen()
        }
    }
}
