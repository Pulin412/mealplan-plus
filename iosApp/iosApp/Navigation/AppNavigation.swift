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
    case dailyLog(date: String) // ISO date string
    case calendar
    case groceryLists
    case settings
    case profile
}

class AppState: ObservableObject {
    @Published var isLoggedIn: Bool = false
    @Published var currentUserId: Int64? = nil
    @Published var navigationPath = NavigationPath()

    func login(userId: Int64) {
        self.currentUserId = userId
        self.isLoggedIn = true
    }

    func logout() {
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
            if appState.isLoggedIn {
                MainTabView()
            } else {
                AuthNavigationView()
            }
        }
        .environmentObject(appState)
    }
}

struct AuthNavigationView: View {
    @State private var showSignUp = false

    var body: some View {
        NavigationStack {
            if showSignUp {
                SignUpScreen(showSignUp: $showSignUp)
            } else {
                LoginScreen(showSignUp: $showSignUp)
            }
        }
    }
}

struct MainTabView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeTab()
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(0)

            FoodsTab()
                .tabItem {
                    Label("Foods", systemImage: "leaf.fill")
                }
                .tag(1)

            MealsTab()
                .tabItem {
                    Label("Meals", systemImage: "fork.knife")
                }
                .tag(2)

            DietsTab()
                .tabItem {
                    Label("Diets", systemImage: "calendar")
                }
                .tag(3)

            MoreTab()
                .tabItem {
                    Label("More", systemImage: "ellipsis.circle")
                }
                .tag(4)
        }
        .accentColor(.green)
    }
}

// Tab wrapper views
struct HomeTab: View {
    var body: some View {
        NavigationStack {
            HomeScreen()
        }
    }
}

struct FoodsTab: View {
    var body: some View {
        NavigationStack {
            FoodsScreen()
        }
    }
}

struct MealsTab: View {
    var body: some View {
        NavigationStack {
            MealsScreen()
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

struct MoreTab: View {
    var body: some View {
        NavigationStack {
            MoreScreen()
        }
    }
}
