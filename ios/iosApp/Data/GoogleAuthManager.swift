import UIKit
import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import shared

enum GoogleAuthError: LocalizedError {
    case notConfigured
    case noToken
    case noEmail
    case userCreationFailed

    var errorDescription: String? {
        switch self {
        case .notConfigured:      return "Firebase is not configured. Add GoogleService-Info.plist."
        case .noToken:            return "Google did not return a valid token."
        case .noEmail:            return "Google account has no email address."
        case .userCreationFailed: return "Failed to create local account."
        }
    }
}

/// Handles Google Sign-In → Firebase Auth → local KMP user creation/lookup.
/// Platform-specific token acquisition only; user DB logic delegated to shared KMP.
final class GoogleAuthManager {
    static let shared = GoogleAuthManager()

    private let userRepository = RepositoryProvider.shared.userRepository
    private let preferencesManager = RepositoryProvider.shared.preferencesManager

    private init() {}

    /// Signs in with Google and returns the local userId.
    func signIn(presenting viewController: UIViewController) async throws -> Int64 {
        // 1. Configure GIDSignIn using Firebase client ID from GoogleService-Info.plist
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            throw GoogleAuthError.notConfigured
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)

        // 2. Present Google sign-in
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: viewController)
        guard let idToken = result.user.idToken?.tokenString else {
            throw GoogleAuthError.noToken
        }
        let accessToken = result.user.accessToken.tokenString

        // 3. Sign in to Firebase
        let firebaseCredential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
        let authResult = try await Auth.auth().signIn(with: firebaseCredential)
        let firebaseUser = authResult.user

        // 4. Extract identity
        let email = (firebaseUser.email ?? "").lowercased().trimmingCharacters(in: .whitespaces)
        guard !email.isEmpty else { throw GoogleAuthError.noEmail }
        let uid = firebaseUser.uid

        // 5. Check cached provider mapping (KMP shared PreferencesManager)
        let cachedId = try? await preferencesManager.getProviderMapping(provider: "google", subject: uid)
        let localUserId: Int64

        if let cached = cachedId, cached.int64Value > 0 {
            localUserId = cached.int64Value
        } else {
            // 6. Find or create local user via shared KMP UserRepository
            let id = try await userRepository.findOrCreateOAuthUser(
                email: email,
                displayName: firebaseUser.displayName,
                photoUrl: firebaseUser.photoURL?.absoluteString
            )
            localUserId = id.int64Value

            // 7. Store mapping for future logins (KMP shared PreferencesManager)
            try? await preferencesManager.setProviderMapping(
                provider: "google",
                subject: uid,
                userId: localUserId
            )
        }

        guard localUserId > 0 else { throw GoogleAuthError.userCreationFailed }

        // 8. Update FirebaseTokenProvider for sync
        FirebaseTokenProvider.shared.currentFirebaseUid = uid
        FirebaseTokenProvider.shared.currentToken = idToken

        return localUserId
    }
}
