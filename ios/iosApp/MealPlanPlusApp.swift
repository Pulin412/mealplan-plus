import SwiftUI
import shared
import UIKit

@main
struct MealPlanPlusApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            AppNavigation()
        }
    }
}

// MARK: - AppDelegate for background task registration

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Register background sync task (must run before app finishes launching)
        BackgroundSyncScheduler.shared.registerTask()
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Schedule next background sync when app goes to background
        BackgroundSyncScheduler.shared.scheduleIfNeeded()
    }
}
