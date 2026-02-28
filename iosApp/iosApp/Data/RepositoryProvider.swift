import Foundation
import shared

/// Singleton provider for all KMP shared repositories
final class RepositoryProvider {
    static let shared = RepositoryProvider()

    private let database: MealPlanDatabase
    private let httpClient: Ktor_client_coreHttpClient

    private init() {
        database = DatabaseManager.shared.database
        httpClient = HttpClientFactory_iosKt.createHttpClient()
    }

    // MARK: - Repositories

    lazy var userRepository: UserRepository = {
        UserRepository(database: database)
    }()

    lazy var foodRepository: FoodRepository = {
        FoodRepository(database: database)
    }()

    lazy var mealRepository: MealRepository = {
        MealRepository(database: database)
    }()

    lazy var dietRepository: DietRepository = {
        DietRepository(database: database, mealRepository: mealRepository)
    }()

    lazy var dailyLogRepository: DailyLogRepository = {
        DailyLogRepository(database: database)
    }()

    lazy var planRepository: PlanRepository = {
        PlanRepository(database: database)
    }()

    lazy var groceryRepository: GroceryRepository = {
        GroceryRepository(database: database)
    }()

    lazy var healthMetricRepository: HealthMetricRepository = {
        HealthMetricRepository(database: database)
    }()

    // MARK: - Preferences

    lazy var preferencesManager: PreferencesManager = {
        PreferencesManagerFactory().create()
    }()

    // MARK: - APIs

    lazy var openFoodFactsApi: OpenFoodFactsApi = {
        OpenFoodFactsApi(client: httpClient)
    }()

    lazy var usdaFoodApi: UsdaFoodApi = {
        UsdaFoodApi(client: httpClient)
    }()
}
