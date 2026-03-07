import SwiftUI
import shared
import UIKit
import FirebaseCore
import GoogleSignIn

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
        FirebaseApp.configure()
        BackgroundSyncScheduler.shared.registerTask()
        return true
    }

    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        BackgroundSyncScheduler.shared.scheduleIfNeeded()
    }
}
