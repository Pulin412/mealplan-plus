import SwiftUI
import shared

struct SignUpScreen: View {
    @EnvironmentObject var appState: AppState
    @Binding var showSignUp: Bool

    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var showPassword = false
    @State private var showConfirmPassword = false
    @State private var errorMessage: String?
    @State private var isLoading = false

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 40)

                // Logo — green circle + person icon
                ZStack {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 80, height: 80)
                    Image(systemName: "person.badge.plus")
                        .font(.system(size: 30, weight: .medium))
                        .foregroundColor(.white)
                }

                Spacer().frame(height: 16)

                Text("Create Account")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)

                Spacer().frame(height: 4)

                Text("Join MealPlan+ today")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Spacer().frame(height: 32)

                // Name field
                HStack(spacing: 12) {
                    Image(systemName: "person")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    TextField("Full name", text: $name)
                        .textContentType(.name)
                }
                .padding()
                .background(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(.systemGray4), lineWidth: 1)
                )
                .padding(.horizontal, 24)

                Spacer().frame(height: 16)

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
                    .textContentType(.newPassword)
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

                Text("At least 6 characters")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 24)
                    .padding(.top, 4)

                Spacer().frame(height: 16)

                // Confirm Password field
                HStack(spacing: 12) {
                    Image(systemName: "lock")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    Group {
                        if showConfirmPassword {
                            TextField("Confirm password", text: $confirmPassword)
                        } else {
                            SecureField("Confirm password", text: $confirmPassword)
                        }
                    }
                    .textContentType(.newPassword)
                    Button(action: { showConfirmPassword.toggle() }) {
                        Image(systemName: showConfirmPassword ? "eye.slash" : "eye")
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(
                            !confirmPassword.isEmpty && confirmPassword != password ? Color.red : Color(.systemGray4),
                            lineWidth: 1
                        )
                )
                .padding(.horizontal, 24)

                // Inline mismatch error
                if !confirmPassword.isEmpty && confirmPassword != password {
                    Text("Passwords do not match")
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 24)
                        .padding(.top, 4)
                }

                // General error
                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 24)
                        .padding(.top, 6)
                }

                Spacer().frame(height: 24)

                // Create Account button — filled green
                Button(action: signUp) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .padding(.trailing, 4)
                        }
                        Text("Create Account")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(isLoading || !isFormValid ? Color.green.opacity(0.5) : Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(isLoading || !isFormValid)
                .padding(.horizontal, 24)

                Spacer().frame(height: 20)

                // Already have account link
                HStack(spacing: 4) {
                    Text("Already have an account?")
                        .foregroundColor(.secondary)
                    Button("Sign In") {
                        showSignUp = false
                    }
                    .foregroundColor(.green)
                    .fontWeight(.semibold)
                }
                .font(.subheadline)

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

    private var isFormValid: Bool {
        !name.isEmpty && !email.isEmpty && !password.isEmpty &&
        password == confirmPassword && password.count >= 6
    }

    private func signUp() {
        guard password == confirmPassword else {
            errorMessage = "Passwords do not match"
            return
        }
        guard password.count >= 6 else {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                let userViewModel = UserViewModel()

                // Check duplicate email
                if let _ = try await userViewModel.getUserByEmail(email: email) {
                    await MainActor.run {
                        isLoading = false
                        errorMessage = "An account with this email already exists"
                    }
                    return
                }

                let userId = try await userViewModel.createUser(
                    email: email,
                    password: password,
                    displayName: name.isEmpty ? nil : name
                )

                await MainActor.run {
                    isLoading = false
                    appState.login(userId: userId)
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = "Sign up failed: \(error.localizedDescription)"
                }
            }
        }
    }
}

struct SignUpScreen_Previews: PreviewProvider {
    static var previews: some View {
        SignUpScreen(showSignUp: .constant(true))
            .environmentObject(AppState())
    }
}
