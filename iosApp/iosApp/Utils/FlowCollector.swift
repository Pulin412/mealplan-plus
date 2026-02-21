import Foundation
import shared

// MARK: - ViewModels that wrap shared repository calls for iOS

/// ViewModel for Foods screen - manages food items with async operations
@MainActor
class FoodsViewModel: ObservableObject {
    @Published var foods: [FoodItem] = []
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.foodRepository

    func loadFoods() {
        isLoading = true
        error = nil

        Task {
            do {
                let result = try await repository.getAllFoodsSnapshot()
                self.foods = result
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func searchFoods(query: String) {
        guard !query.isEmpty else {
            loadFoods()
            return
        }

        Task {
            do {
                let result = try await repository.searchByNameSnapshot(query: query, limit: 50)
                self.foods = result
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    func insertFood(_ food: FoodItem) async throws -> Int64 {
        let id = try await repository.insertFood(food: food)
        loadFoods() // Refresh list
        return id.int64Value
    }

    func updateFood(_ food: FoodItem) async throws {
        try await repository.updateFood(food: food)
        loadFoods() // Refresh list
    }

    func deleteFood(id: Int64) async throws {
        try await repository.deleteFood(id: id)
        loadFoods() // Refresh list
    }

    func setFavorite(id: Int64, isFavorite: Bool) async throws {
        try await repository.setFavorite(id: id, isFavorite: isFavorite)
        loadFoods() // Refresh list
    }
}

/// ViewModel for Meals screen
@MainActor
class MealsViewModel: ObservableObject {
    @Published var meals: [Meal] = []
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.mealRepository

    func loadMeals(userId: Int64) {
        isLoading = true
        error = nil

        Task {
            do {
                let result = try await repository.getAllMealsSnapshot(userId: userId)
                self.meals = result
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func getMealWithFoods(mealId: Int64) async throws -> MealWithFoods? {
        return try await repository.getMealWithFoods(mealId: mealId)
    }

    func insertMeal(_ meal: Meal) async throws -> Int64 {
        let id = try await repository.insertMeal(meal: meal)
        return id.int64Value
    }

    func updateMeal(_ meal: Meal) async throws {
        try await repository.updateMeal(meal: meal)
    }

    func deleteMeal(id: Int64) async throws {
        try await repository.deleteMeal(id: id)
    }

    func addFoodToMeal(mealId: Int64, foodId: Int64, quantity: Double, unit: FoodUnit, notes: String? = nil) async throws {
        try await repository.addFoodToMeal(mealId: mealId, foodId: foodId, quantity: quantity, unit: unit, notes: notes)
    }

    func removeFoodFromMeal(mealId: Int64, foodId: Int64) async throws {
        try await repository.removeFoodFromMeal(mealId: mealId, foodId: foodId)
    }
}

/// ViewModel for Diets screen
@MainActor
class DietsViewModel: ObservableObject {
    @Published var diets: [DietSummary] = []
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.dietRepository

    func loadDiets(userId: Int64) {
        isLoading = true
        error = nil

        Task {
            do {
                let result = try await repository.getDietSummariesSnapshot(userId: userId)
                self.diets = result
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func getDietWithMeals(dietId: Int64) async throws -> DietWithMeals? {
        return try await repository.getDietWithMeals(dietId: dietId)
    }

    func insertDiet(_ diet: Diet) async throws -> Int64 {
        let id = try await repository.insertDiet(diet: diet)
        return id.int64Value
    }

    func updateDiet(_ diet: Diet) async throws {
        try await repository.updateDiet(diet: diet)
    }

    func deleteDiet(id: Int64) async throws {
        try await repository.deleteDiet(id: id)
    }

    func setDietMeal(dietId: Int64, slotType: String, mealId: Int64?) async throws {
        try await repository.setDietMeal(dietId: dietId, slotType: slotType, mealId: mealId?.toKotlinLong())
    }

    func getTagsForDiet(dietId: Int64) async throws -> [Tag] {
        return try await repository.getTagsForDietSnapshot(dietId: dietId)
    }

    func getAllTags(userId: Int64) async throws -> [Tag] {
        return try await repository.getAllTagsSnapshot(userId: userId)
    }

    func addTagToDiet(dietId: Int64, tagId: Int64) async throws {
        try await repository.addTagToDiet(dietId: dietId, tagId: tagId)
    }

    func removeTagFromDiet(dietId: Int64, tagId: Int64) async throws {
        try await repository.removeTagFromDiet(dietId: dietId, tagId: tagId)
    }
}

/// ViewModel for DailyLog screen
@MainActor
class DailyLogViewModel: ObservableObject {
    @Published var dailyLog: DailyLog?
    @Published var loggedFoods: [LoggedFoodWithDetails] = []
    @Published var macroSummaries: [DailyMacroSummary] = []
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.dailyLogRepository

    func loadDailyLog(userId: Int64, date: String) {
        isLoading = true
        error = nil

        Task {
            do {
                self.dailyLog = try await repository.getDailyLog(userId: userId, date: date)
                self.loggedFoods = try await repository.getLoggedFoodsSnapshot(userId: userId, date: date)
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func loadMacroSummaries(userId: Int64, startDate: String, endDate: String) {
        Task {
            do {
                let result = try await repository.getDailyMacroSummarySnapshot(userId: userId, startDate: startDate, endDate: endDate)
                self.macroSummaries = result
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    func insertLoggedFood(_ food: LoggedFood) async throws -> Int64 {
        let id = try await repository.insertLoggedFood(loggedFood: food)
        return id.int64Value
    }

    func deleteLoggedFood(id: Int64) async throws {
        try await repository.deleteLoggedFood(id: id)
    }

    func insertLoggedMeal(_ meal: LoggedMeal) async throws -> Int64 {
        let id = try await repository.insertLoggedMeal(loggedMeal: meal)
        return id.int64Value
    }

    func deleteLoggedMeal(id: Int64) async throws {
        try await repository.deleteLoggedMeal(id: id)
    }
}

/// ViewModel for Plans (calendar)
@MainActor
class PlansViewModel: ObservableObject {
    @Published var plans: [PlanWithDietName] = []
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.planRepository

    func loadPlans(userId: Int64, startDate: String, endDate: String) {
        isLoading = true
        error = nil

        Task {
            do {
                let result = try await repository.getPlansWithDietNameSnapshot(userId: userId, startDate: startDate, endDate: endDate)
                self.plans = result
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func getPlanByDate(userId: Int64, date: String) async throws -> Plan? {
        return try await repository.getPlanByDate(userId: userId, date: date)
    }

    func insertOrUpdatePlan(_ plan: Plan) async throws {
        try await repository.insertOrUpdatePlan(plan: plan)
    }

    func updatePlanDiet(userId: Int64, date: String, dietId: Int64?) async throws {
        try await repository.updatePlanDiet(userId: userId, date: date, dietId: dietId?.toKotlinLong())
    }

    func deletePlan(userId: Int64, date: String) async throws {
        try await repository.deletePlan(userId: userId, date: date)
    }
}

/// ViewModel for Grocery Lists
@MainActor
class GroceryViewModel: ObservableObject {
    @Published var groceryLists: [GroceryList] = []
    @Published var currentList: GroceryListWithItems?
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.groceryRepository

    func loadGroceryLists(userId: Int64) {
        isLoading = true
        error = nil

        Task {
            do {
                let result = try await repository.getAllGroceryListsSnapshot(userId: userId)
                self.groceryLists = result
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func loadGroceryListWithItems(listId: Int64) async throws {
        self.currentList = try await repository.getGroceryListWithItems(listId: listId)
    }

    func insertGroceryList(_ list: GroceryList) async throws -> Int64 {
        let id = try await repository.insertGroceryList(list: list)
        return id.int64Value
    }

    func deleteGroceryList(id: Int64) async throws {
        try await repository.deleteGroceryList(id: id)
    }

    func updateItemChecked(id: Int64, isChecked: Bool) async throws {
        try await repository.updateGroceryItemChecked(id: id, isChecked: isChecked)
    }

    func insertGroceryItem(_ item: GroceryItem) async throws -> Int64 {
        let id = try await repository.insertGroceryItem(item: item)
        return id.int64Value
    }

    func deleteGroceryItem(id: Int64) async throws {
        try await repository.deleteGroceryItem(id: id)
    }
}

/// ViewModel for Health Metrics
@MainActor
class HealthMetricsViewModel: ObservableObject {
    @Published var metrics: [HealthMetric] = []
    @Published var customMetricTypes: [CustomMetricType] = []
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.healthMetricRepository

    func loadMetrics(userId: Int64) {
        isLoading = true
        error = nil

        Task {
            do {
                let result = try await repository.getAllHealthMetricsSnapshot(userId: userId)
                self.metrics = result
                self.customMetricTypes = try await repository.getActiveCustomMetricTypesSnapshot(userId: userId)
                self.isLoading = false
            } catch {
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func loadMetricsByType(userId: Int64, metricType: String) async throws -> [HealthMetric] {
        return try await repository.getHealthMetricsByTypeSnapshot(userId: userId, metricType: metricType)
    }

    func loadMetricsForDateRange(userId: Int64, startDate: String, endDate: String) async throws -> [HealthMetric] {
        return try await repository.getHealthMetricsForDateRangeSnapshot(userId: userId, startDate: startDate, endDate: endDate)
    }

    func getLatestMetricByType(userId: Int64, metricType: String) async throws -> HealthMetric? {
        return try await repository.getLatestMetricByType(userId: userId, metricType: metricType)
    }

    func insertMetric(_ metric: HealthMetric) async throws -> Int64 {
        let id = try await repository.insertHealthMetric(metric: metric)
        return id.int64Value
    }

    func deleteMetric(id: Int64) async throws {
        try await repository.deleteHealthMetric(id: id)
    }

    func insertCustomMetricType(_ type: CustomMetricType) async throws -> Int64 {
        let id = try await repository.insertCustomMetricType(type: type)
        return id.int64Value
    }
}

/// ViewModel for User/Auth operations
@MainActor
class UserViewModel: ObservableObject {
    @Published var currentUser: User?
    @Published var isLoading = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.userRepository

    func login(email: String, password: String) async throws -> User? {
        return try await repository.verifyPassword(email: email, password: password)
    }

    func createUser(email: String, password: String, displayName: String?) async throws -> Int64 {
        let id = try await repository.createUser(email: email, password: password, displayName: displayName)
        return id.int64Value
    }

    func getUserById(id: Int64) async throws -> User? {
        return try await repository.getUserById(id: id)
    }

    func getUserByEmail(email: String) async throws -> User? {
        return try await repository.getUserByEmail(email: email)
    }

    func updateUser(_ user: User) async throws {
        try await repository.updateUser(user: user)
    }
}

/// ViewModel for Home screen - aggregates today's data, health metrics, week stats
@MainActor
class HomeViewModel: ObservableObject {
    // Today's macros
    @Published var todayCalories: Double = 0
    @Published var todayProtein: Double = 0
    @Published var todayCarbs: Double = 0
    @Published var todayFat: Double = 0
    let calorieGoal: Double = 2000

    // Health metrics
    @Published var latestWeight: HealthMetric?
    @Published var latestBloodSugar: HealthMetric?
    @Published var latestHba1c: HealthMetric?
    @Published var glucoseHistory: [HealthMetric] = []

    // Week stats
    @Published var weeklyLoggedDates: Set<String> = []
    @Published var dayStreak: Int = 0

    // Macro summaries for weekly data
    @Published var macroSummaries: [DailyMacroSummary] = []

    // Meal slot progress: (slotName, emoji, slotType, logged, total)
    @Published var mealSlots: [(String, String, String, Int, Int)] = []

    // User name
    @Published var userName: String = ""
    @Published var userInitial: String = "?"

    @Published var isLoading = false

    private let logRepo = RepositoryProvider.shared.dailyLogRepository
    private let healthRepo = RepositoryProvider.shared.healthMetricRepository
    private let userRepo = RepositoryProvider.shared.userRepository

    func load(userId: Int64) {
        isLoading = true
        let today = isoToday()
        let sevenDaysAgo = isoDate(daysAgo: 6)

        Task {
            // User name
            if let user = try? await userRepo.getUserById(id: userId) {
                let name = user.displayName?.isEmpty == false ? (user.displayName ?? "") : String(user.email.split(separator: "@").first ?? "")
                self.userName = name
                self.userInitial = String(name.prefix(1)).uppercased()
            }

            // Today's macros from logged foods
            if let foods = try? await logRepo.getLoggedFoodsSnapshot(userId: userId, date: today) {
                self.todayCalories = foods.reduce(0) { $0 + $1.food.calculateCalories(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
                self.todayProtein = foods.reduce(0) { $0 + $1.food.calculateProtein(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
                self.todayCarbs = foods.reduce(0) { $0 + $1.food.calculateCarbs(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
                self.todayFat = foods.reduce(0) { $0 + $1.food.calculateFat(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }

                // Meal slot progress
                let bySlot = Dictionary(grouping: foods) { $0.loggedFood.slotType.uppercased() }
                self.mealSlots = [
                    ("Breakfast", "🍳", "BREAKFAST", bySlot["BREAKFAST"]?.count ?? 0, 3),
                    ("Lunch", "☀️", "LUNCH", bySlot["LUNCH"]?.count ?? 0, 3),
                    ("Dinner", "🌙", "DINNER", bySlot["DINNER"]?.count ?? 0, 3),
                    ("Snacks", "🍎", "SNACK", bySlot["SNACK"]?.count ?? 0, 2)
                ]
            }

            // Health metrics for today
            if let weight = try? await healthRepo.getLatestMetricByType(userId: userId, metricType: "WEIGHT") {
                self.latestWeight = weight
            }
            if let sugar = try? await healthRepo.getLatestMetricByType(userId: userId, metricType: "FASTING_SUGAR") {
                self.latestBloodSugar = sugar
            }
            if let hba1c = try? await healthRepo.getLatestMetricByType(userId: userId, metricType: "HBA1C") {
                self.latestHba1c = hba1c
            }

            // 7-day glucose history
            if let history = try? await healthRepo.getHealthMetricsForDateRangeSnapshot(userId: userId, startDate: sevenDaysAgo, endDate: today) {
                self.glucoseHistory = history.filter { $0.metricType == "FASTING_SUGAR" }
            }

            // Weekly macro summaries for logged dates + streak
            if let summaries = try? await logRepo.getDailyMacroSummarySnapshot(userId: userId, startDate: sevenDaysAgo, endDate: today) {
                self.macroSummaries = summaries
                self.weeklyLoggedDates = Set(summaries.filter { $0.calories > 0 }.map { $0.date })
                self.dayStreak = computeStreak(summaries: summaries)
            }

            self.isLoading = false
        }
    }

    private func computeStreak(summaries: [DailyMacroSummary]) -> Int {
        var streak = 0
        for i in 0...6 {
            let date = isoDate(daysAgo: i)
            if summaries.contains(where: { $0.date == date && $0.calories > 0 }) {
                streak += 1
            } else {
                break
            }
        }
        return streak
    }

    private func isoToday() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    private func isoDate(daysAgo: Int) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        let date = Calendar.current.date(byAdding: .day, value: -daysAgo, to: Date()) ?? Date()
        return f.string(from: date)
    }
}

// MARK: - Helper Extensions

extension Int64 {
    func toKotlinLong() -> KotlinLong {
        return KotlinLong(value: self)
    }
}
