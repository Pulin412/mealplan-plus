import BackgroundTasks
import Foundation
import shared

/// Manages BGProcessingTask scheduling for periodic cloud sync.
/// Task ID must match Info.plist BGTaskSchedulerPermittedIdentifiers entry.
final class BackgroundSyncScheduler {
    static let shared = BackgroundSyncScheduler()
    private init() {}

    static let taskIdentifier = "com.mealplanplus.sync"

    // MARK: - Registration (call once from app init)

    func registerTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.taskIdentifier,
            using: nil
        ) { task in
            guard let processingTask = task as? BGProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            self.handleSyncTask(processingTask)
        }
    }

    // MARK: - Scheduling

    func scheduleIfNeeded() {
        let request = BGProcessingTaskRequest(identifier: Self.taskIdentifier)
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60) // 15 min

        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Silently fail — sync still works via manual trigger
            print("[BackgroundSyncScheduler] schedule failed: \(error)")
        }
    }

    // MARK: - Task handler

    private func handleSyncTask(_ task: BGProcessingTask) {
        // Re-schedule for next run
        scheduleIfNeeded()

        guard
            let firebaseUid = FirebaseTokenProvider.shared.currentFirebaseUid,
            let userId = currentUserId()
        else {
            task.setTaskCompleted(success: false)
            return
        }

        let syncRepo = RepositoryProvider.shared.syncRepository
        Task {
            task.expirationHandler = { task.setTaskCompleted(success: false) }
            do {
                _ = try await syncRepo.sync(firebaseUid: firebaseUid, userId: userId)
                task.setTaskCompleted(success: true)
            } catch {
                task.setTaskCompleted(success: false)
            }
        }
    }

    private func currentUserId() -> Int64? {
        // Read from AppState via a shared observable — use UserDefaults key set by RepositoryProvider
        let defaults = UserDefaults.standard
        let value = defaults.object(forKey: "user_id")
        return value != nil ? Int64(defaults.integer(forKey: "user_id")) : nil
    }
}
