import SwiftUI
import shared

struct ForgotPasswordScreen: View {
    @Binding var showForgotPassword: Bool

    @State private var email = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showResultAlert = false

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 60)

                // Logo — green circle + lock icon
                ZStack {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 80, height: 80)
                    Image(systemName: "lock")
                        .font(.system(size: 34, weight: .medium))
                        .foregroundColor(.white)
                }

                Spacer().frame(height: 16)

                Text("Forgot Password?")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)

                Spacer().frame(height: 4)

                Text("Enter your registered email")
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

                // Inline error
                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 24)
                        .padding(.top, 6)
                }

                Spacer().frame(height: 24)

                // Send Password button — filled green
                Button(action: checkEmail) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .padding(.trailing, 4)
                        }
                        Text("Send Password")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(isLoading || email.isEmpty ? Color.green.opacity(0.5) : Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(isLoading || email.isEmpty)
                .padding(.horizontal, 24)

                Spacer().frame(height: 20)

                // Back to Sign In
                Button("Back to Sign In") {
                    showForgotPassword = false
                }
                .font(.subheadline)
                .foregroundColor(.green)

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
        .alert("Password Recovery", isPresented: $showResultAlert) {
            Button("OK") {
                showForgotPassword = false
            }
        } message: {
            Text("For security reasons, your password is stored locally and cannot be sent via email.\n\nTo reset your password, please reinstall the app or contact support.")
        }
    }

    private func checkEmail() {
        guard !email.isEmpty else {
            errorMessage = "Please enter your email address"
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                let userViewModel = UserViewModel()
                let user = try await userViewModel.getUserByEmail(email: email.lowercased().trimmingCharacters(in: .whitespaces))

                await MainActor.run {
                    isLoading = false
                    if user == nil {
                        errorMessage = "No account found with this email"
                    } else {
                        showResultAlert = true
                    }
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = "Something went wrong. Please try again."
                }
            }
        }
    }
}

struct ForgotPasswordScreen_Previews: PreviewProvider {
    static var previews: some View {
        ForgotPasswordScreen(showForgotPassword: .constant(true))
    }
}
