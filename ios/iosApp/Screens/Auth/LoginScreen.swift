import SwiftUI
import shared

struct LoginScreen: View {
    @EnvironmentObject var appState: AppState
    @Binding var showSignUp: Bool
    @Binding var showForgotPassword: Bool

    @State private var email = ""
    @State private var password = ""
    @State private var showPassword = false
    @State private var errorMessage: String?
    @State private var isLoading = false
    @State private var isGoogleLoading = false
    @State private var googleErrorMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 60)

                // Logo — green circle + fork icon
                ZStack {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 80, height: 80)
                    Image(systemName: "fork.knife")
                        .font(.system(size: 34, weight: .medium))
                        .foregroundColor(.white)
                }

                Spacer().frame(height: 16)

                Text("MealPlan+")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)

                Spacer().frame(height: 4)

                Text("Smart nutrition tracking")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Spacer().frame(height: 40)

                // Email field
                HStack(spacing: 12) {
                    Image(systemName: "envelope")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    TextField("Email", text: $email)
                        .textContentType(.emailAddress)
                        .autocapitalization(.none)
                        .keyboardType(.emailAddress)
                }
                .padding()
                .background(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(.systemGray4), lineWidth: 1)
                )
                .padding(.horizontal, 24)

                Spacer().frame(height: 16)

                // Password field
                HStack(spacing: 12) {
                    Image(systemName: "lock")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    Group {
                        if showPassword {
                            TextField("Password", text: $password)
                        } else {
                            SecureField("Password", text: $password)
                        }
                    }
                    .textContentType(.password)
                    Button(action: { showPassword.toggle() }) {
                        Image(systemName: showPassword ? "eye.slash" : "eye")
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(.systemGray4), lineWidth: 1)
                )
                .padding(.horizontal, 24)

                // Inline error
                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 24)
                        .padding(.top, 6)
                }

                // Forgot Password — right-aligned
                HStack {
                    Spacer()
                    Button("Forgot Password?") {
                        showForgotPassword = true
                    }
                    .font(.subheadline)
                    .foregroundColor(.green)
                }
                .padding(.horizontal, 24)
                .padding(.top, 8)

                Spacer().frame(height: 24)

                // Sign In button — filled green
                Button(action: login) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .padding(.trailing, 4)
                        }
                        Text("Sign In")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(isLoading || email.isEmpty || password.isEmpty ? Color.green.opacity(0.5) : Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(isLoading || email.isEmpty || password.isEmpty)
                .padding(.horizontal, 24)

                Spacer().frame(height: 20)

                // OR divider
                HStack {
                    Rectangle()
                        .fill(Color(.systemGray4))
                        .frame(height: 1)
                    Text("or")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 8)
                    Rectangle()
                        .fill(Color(.systemGray4))
                        .frame(height: 1)
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 20)

                // Continue with Google
                Button(action: signInWithGoogle) {
                    HStack {
                        if isGoogleLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .primary))
                                .padding(.trailing, 4)
                        } else {
                            Image(systemName: "globe")
                                .foregroundColor(.primary)
                        }
                        Text("Continue with Google")
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color(.systemBackground))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color(.systemGray3), lineWidth: 1.5)
                    )
                }
                .disabled(isLoading || isGoogleLoading)
                .padding(.horizontal, 24)

                if let googleError = googleErrorMessage {
                    Text(googleError)
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 24)
                        .padding(.top, 6)
                }

                Spacer().frame(height: 20)

                // Sign Up — outlined green button
                Button(action: { showSignUp = true }) {
                    Text("Sign Up")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .foregroundColor(.green)
                        .background(Color(.systemBackground))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color.green, lineWidth: 1.5)
                        )
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 40)

                // Trademark
                Text("© 2026 Pulin. All rights reserved.")
                    .font(.caption2)
                    .foregroundColor(.secondary)

                Spacer().frame(height: 24)
            }
        }
        .background(Color(.systemBackground))
        .navigationBarHidden(true)
    }

    private func signInWithGoogle() {
        guard let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
              let rootVC = scene.windows.first?.rootViewController else {
            googleErrorMessage = "Cannot present sign-in screen."
            return
        }

        isGoogleLoading = true
        googleErrorMessage = nil

        Task {
            do {
                let userId = try await GoogleAuthManager.shared.signIn(presenting: rootVC)
                await MainActor.run {
                    isGoogleLoading = false
                    appState.login(userId: userId)
                }
            } catch {
                await MainActor.run {
                    isGoogleLoading = false
                    googleErrorMessage = error.localizedDescription
                }
            }
        }
    }

    private func login() {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Email and password required"
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                let userViewModel = UserViewModel()
                if let user = try await userViewModel.login(email: email, password: password) {
                    await MainActor.run {
                        isLoading = false
                        appState.login(userId: user.id)
                    }
                } else {
                    await MainActor.run {
                        isLoading = false
                        errorMessage = "Invalid email or password"
                    }
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = "Sign in failed: \(error.localizedDescription)"
                }
            }
        }
    }
}

struct LoginScreen_Previews: PreviewProvider {
    static var previews: some View {
        LoginScreen(showSignUp: .constant(false), showForgotPassword: .constant(false))
            .environmentObject(AppState())
    }
}
