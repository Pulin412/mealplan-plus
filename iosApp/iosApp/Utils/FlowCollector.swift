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

// MARK: - Home screen models

enum WeekDayState { case completed, plannedFuture, missed, noData }

struct WeekDayInfo {
    let date: Date
    let isoDate: String
    let dietLabel: String?
    let state: WeekDayState
}

struct TodayPlanSlot: Identifiable {
    let id = UUID()
    let slotType: String
    let slotDisplayName: String
    let emoji: String
    let plannedMealName: String?
    let plannedMealId: Int64?   // meal id from diet (for quick-log)
    let loggedMealId: Int64?    // LoggedMeal row id if already logged
    let isLogged: Bool
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
    @Published var weekDays: [WeekDayInfo] = []
    @Published var weeklyLoggedDates: Set<String> = []
    @Published var dayStreak: Int = 0

    // Macro summaries for weekly data
    @Published var macroSummaries: [DailyMacroSummary] = []

    // Today's plan from assigned diet
    @Published var todayPlanSlots: [TodayPlanSlot] = []

    // User name
    @Published var userName: String = ""
    @Published var userInitial: String = "?"

    @Published var isLoading = false

    private let logRepo = RepositoryProvider.shared.dailyLogRepository
    private let healthRepo = RepositoryProvider.shared.healthMetricRepository
    private let userRepo = RepositoryProvider.shared.userRepository
    private let planRepo = RepositoryProvider.shared.planRepository
    private let dietRepo = RepositoryProvider.shared.dietRepository

    private var currentUserId: Int64?

    func load(userId: Int64) {
        currentUserId = userId
        isLoading = true
        let today = isoToday()
        let sevenDaysAgo = isoDate(daysAgo: 6)

        // Set safe defaults before async block
        self.userName = "User"
        self.userInitial = "U"

        Task {
            // User name — try by id first, fall back to first user
            var user = try? await userRepo.getUserById(id: userId)
            if user == nil {
                user = (try? await userRepo.getAllUsersSnapshot())?.first
            }
            if let user = user {
                let name: String
                if let display = user.displayName, !display.isEmpty {
                    name = display
                } else {
                    name = String(user.email.split(separator: "@").first ?? "User")
                }
                self.userName = name.isEmpty ? "User" : name
                self.userInitial = String(name.prefix(1)).uppercased()
            }

            // Today's macros from logged foods
            let foods = (try? await logRepo.getLoggedFoodsSnapshot(userId: userId, date: today)) ?? []
            self.todayCalories = foods.reduce(0) { $0 + $1.food.calculateCalories(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            self.todayProtein = foods.reduce(0) { $0 + $1.food.calculateProtein(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            self.todayCarbs = foods.reduce(0) { $0 + $1.food.calculateCarbs(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            self.todayFat = foods.reduce(0) { $0 + $1.food.calculateFat(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            let loggedBySlot = Dictionary(grouping: foods) { $0.loggedFood.slotType.uppercased() }

            // Today's plan from diet
            await loadTodayPlanSlots(userId: userId, date: today, loggedBySlot: loggedBySlot)

            // Health metrics
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

            // Weekly macro summaries + week days
            if let summaries = try? await logRepo.getDailyMacroSummarySnapshot(userId: userId, startDate: sevenDaysAgo, endDate: today) {
                self.macroSummaries = summaries
                self.weeklyLoggedDates = Set(summaries.filter { $0.calories > 0 }.map { $0.date })
                self.dayStreak = computeStreak(summaries: summaries)
            }

            // Week plans for colours
            if let weekPlans = try? await planRepo.getPlansWithDietNameSnapshot(userId: userId, startDate: sevenDaysAgo, endDate: today) {
                let plansByDate = Dictionary(grouping: weekPlans) { $0.date }.mapValues { $0.first }
                self.weekDays = buildWeekDays(plansByDate: plansByDate)
            } else {
                self.weekDays = buildWeekDays(plansByDate: [:])
            }

            self.isLoading = false
        }
    }

    private func loadTodayPlanSlots(userId: Int64, date: String, loggedBySlot: [String: [LoggedFoodWithDetails]]) async {
        // Also load logged meals to get loggedMealId for toggle
        let loggedMeals = (try? await logRepo.getLoggedMealsSnapshot(userId: userId, date: date)) ?? []
        let loggedMealBySlot = Dictionary(grouping: loggedMeals) { $0.slotType.uppercased() }
            .mapValues { $0.first?.id }

        let plan = try? await planRepo.getPlanByDate(userId: userId, date: date)
        guard let dietId = plan?.dietId?.int64Value else {
            // No diet: show only logged slots
            self.todayPlanSlots = loggedBySlot.keys.sorted().map { slotType in
                TodayPlanSlot(
                    slotType: slotType,
                    slotDisplayName: displayName(for: slotType),
                    emoji: slotEmoji(slotType),
                    plannedMealName: nil,
                    plannedMealId: nil,
                    loggedMealId: loggedMealBySlot[slotType.uppercased()] ?? nil,
                    isLogged: true
                )
            }
            return
        }

        guard let dietWithMeals = try? await dietRepo.getDietWithMeals(dietId: dietId) else { return }
        // KMP Map<String, MealWithFoods?> bridges to NSDictionary in Swift
        // Cast to NSDictionary first, then iterate with string keys
        let rawDict = dietWithMeals.meals as AnyObject
        let nativeDict: [String: MealWithFoods?]
        if let nd = rawDict as? NSDictionary {
            var tmp: [String: MealWithFoods?] = [:]
            for (k, v) in nd {
                if let key = k as? String {
                    tmp[key] = v as? MealWithFoods  // nil if NSNull or not castable
                }
            }
            nativeDict = tmp
        } else {
            nativeDict = [:]
        }
        let mealEntries = nativeDict.compactMap { (key, value) -> (String, String?, Int64?)? in
            guard let meal = value else { return (key, nil, nil) }
            return (key, meal.meal.name, meal.meal.id)
        }
        .sorted { slotOrder($0.0) < slotOrder($1.0) }

        self.todayPlanSlots = mealEntries.map { (slotType, mealName, mealId) in
            let upperSlot = slotType.uppercased()
            return TodayPlanSlot(
                slotType: slotType,
                slotDisplayName: displayName(for: slotType),
                emoji: slotEmoji(slotType),
                plannedMealName: mealName,
                plannedMealId: mealId,
                loggedMealId: loggedMealBySlot[upperSlot] ?? nil,
                isLogged: loggedBySlot[upperSlot] != nil || loggedMealBySlot[upperSlot] != nil
            )
        }
    }

    private func buildWeekDays(plansByDate: [String: PlanWithDietName?]) -> [WeekDayInfo] {
        let cal = Calendar.current
        let todayDate = Date()
        let todayIso = isoToday()
        return (0...6).reversed().compactMap { daysAgo -> WeekDayInfo? in
            guard let date = cal.date(byAdding: .day, value: -daysAgo, to: todayDate) else { return nil }
            let iso = isoDate(from: date)
            let isFuture = iso > todayIso
            let plan = plansByDate[iso] ?? nil
            let hasPlan = plan?.dietId != nil
            let isCompleted = plan?.isCompleted == true
            let hasCalories = weeklyLoggedDates.contains(iso)
            let dietLabel = plan?.dietName.map { extractShortDietName($0) }

            let state: WeekDayState
            if !isFuture && (isCompleted || hasCalories) {
                state = .completed
            } else if isFuture && hasPlan {
                state = .plannedFuture
            } else if !isFuture && iso < todayIso {
                state = .missed
            } else {
                state = .noData
            }
            return WeekDayInfo(date: date, isoDate: iso, dietLabel: dietLabel, state: state)
        }
    }

    private func computeStreak(summaries: [DailyMacroSummary]) -> Int {
        var streak = 0
        for i in 0...6 {
            let date = isoDate(daysAgo: i)
            if summaries.contains(where: { $0.date == date && $0.calories > 0 }) { streak += 1 } else { break }
        }
        return streak
    }

    // MARK: - Helpers

    private func isoToday() -> String { isoDate(from: Date()) }

    private func isoDate(daysAgo: Int) -> String {
        let date = Calendar.current.date(byAdding: .day, value: -daysAgo, to: Date()) ?? Date()
        return isoDate(from: date)
    }

    private func isoDate(from date: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    func slotEmoji(_ slotType: String) -> String {
        switch slotType.uppercased() {
        case "BREAKFAST": return "🍳"
        case "LUNCH": return "☀️"
        case "DINNER": return "🌙"
        case "SNACK", "EVENING_SNACK": return "🍎"
        case "PRE_WORKOUT": return "💪"
        case "POST_WORKOUT": return "🥤"
        case "EARLY_MORNING": return "🌅"
        case "NOON": return "🌞"
        case "MID_MORNING": return "☕"
        case "EVENING": return "🌆"
        case "POST_DINNER": return "🍵"
        default: return "🍽️"
        }
    }

    func displayName(for slotType: String) -> String {
        switch slotType.uppercased() {
        case "BREAKFAST": return "Breakfast"
        case "LUNCH": return "Lunch"
        case "DINNER": return "Dinner"
        case "SNACK": return "Snacks"
        case "EVENING_SNACK": return "Evening Snack"
        case "PRE_WORKOUT": return "Pre-Workout"
        case "POST_WORKOUT": return "Post-Workout"
        case "EARLY_MORNING": return "Early Morning"
        case "NOON": return "Noon"
        case "MID_MORNING": return "Mid Morning"
        case "EVENING": return "Evening"
        case "POST_DINNER": return "Post Dinner"
        default: return slotType.replacingOccurrences(of: "_", with: " ").capitalized
        }
    }

    private func slotOrder(_ slotType: String) -> Int {
        switch slotType.uppercased() {
        case "EARLY_MORNING": return 0
        case "BREAKFAST": return 1
        case "NOON": return 2
        case "MID_MORNING": return 3
        case "LUNCH": return 4
        case "PRE_WORKOUT": return 5
        case "EVENING": return 6
        case "EVENING_SNACK": return 7
        case "POST_WORKOUT": return 8
        case "DINNER": return 9
        case "POST_DINNER": return 10
        default: return 99
        }
    }

    /// Toggle a slot: log the planned meal if not logged, delete if already logged.
    func toggleSlotLogged(slot: TodayPlanSlot) {
        guard let userId = currentUserId else { return }
        let today = isoToday()
        Task {
            if slot.isLogged, let loggedMealId = slot.loggedMealId {
                try? await logRepo.deleteLoggedMeal(id: loggedMealId)
            } else if !slot.isLogged, let mealId = slot.plannedMealId {
                let meal = LoggedMeal(
                    id: 0,
                    userId: userId,
                    logDate: today,
                    mealId: mealId,
                    slotType: slot.slotType,
                    quantity: 1.0,
                    timestamp: nil,
                    notes: nil
                )
                _ = try? await logRepo.insertLoggedMeal(loggedMeal: meal)
            }
            // Reload plan slots to reflect updated state
            let foods = (try? await logRepo.getLoggedFoodsSnapshot(userId: userId, date: today)) ?? []
            let loggedBySlot = Dictionary(grouping: foods) { $0.loggedFood.slotType.uppercased() }
            await loadTodayPlanSlots(userId: userId, date: today, loggedBySlot: loggedBySlot)
        }
    }

    private func extractShortDietName(_ name: String) -> String {
        let words = name.split(separator: " ")
        if words.count == 1 { return String(name.prefix(4)) }
        let initials = words.prefix(2).map { String($0.prefix(1)) }.joined()
        let num = name.components(separatedBy: CharacterSet.decimalDigits.inverted).joined()
        return initials.uppercased() + num.prefix(2)
    }
}

// MARK: - Helper Extensions

extension Int64 {
    func toKotlinLong() -> KotlinLong {
        return KotlinLong(value: self)
    }
}
