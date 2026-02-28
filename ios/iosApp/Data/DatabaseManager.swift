import Foundation
import shared

/// Singleton manager for KMP shared database
final class DatabaseManager {
    static let shared = DatabaseManager()

    private let driverFactory: DatabaseDriverFactory
    private var _database: MealPlanDatabase?
    private let lock = NSLock()

    private init() {
        driverFactory = DatabaseDriverFactory()
    }

    /// Get or create the database instance (thread-safe)
    var database: MealPlanDatabase {
        lock.lock()
        defer { lock.unlock() }

        if _database == nil {
            _database = DatabaseProvider.shared.getDatabase(driverFactory: driverFactory)
        }
        return _database!
    }

    /// Close database connection (call on app termination if needed)
    func close() {
        lock.lock()
        defer { lock.unlock() }

        DatabaseProvider.shared.closeDatabase()
        _database = nil
    }
}
