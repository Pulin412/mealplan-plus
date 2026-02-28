import Foundation
import shared

/// Minimal database seeder - just ensures DB is initialized on app launch
/// Actual data import is done via Settings > Import Sample Data
@MainActor
final class DatabaseSeeder {
    static let shared = DatabaseSeeder()
    private init() {}

    /// Just initialize the database connection on app launch
    func initialize() async {
        // Touch the repository to ensure DB is created
        _ = RepositoryProvider.shared.foodRepository
        print("DatabaseSeeder: Database initialized")
    }
}
