import Foundation

/// Bridge between Firebase Auth (when available) and KMP SyncRepository.
/// Update `currentToken` when user signs in via Firebase.
/// Currently returns nil — will be populated after I6 (Firebase Auth for iOS).
final class FirebaseTokenProvider {
    static let shared = FirebaseTokenProvider()
    private init() {}

    /// Firebase ID token for the currently signed-in user, or nil if not available.
    var currentToken: String? = nil

    /// Firebase UID of the currently signed-in user, or nil.
    var currentFirebaseUid: String? = nil
}
