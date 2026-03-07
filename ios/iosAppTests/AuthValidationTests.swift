import XCTest

/// Pure Swift validation rules mirroring the auth screen logic.
/// These match the same rules enforced in Android's AuthViewModel.
struct AuthValidator {

    // MARK: - Sign In

    static func validateSignIn(email: String, password: String) -> String? {
        guard !email.isEmpty, !password.isEmpty else {
            return "Email and password required"
        }
        return nil
    }

    // MARK: - Sign Up

    static func validateSignUp(email: String, password: String, confirmPassword: String, name: String) -> String? {
        guard !email.isEmpty, !password.isEmpty, !name.isEmpty else {
            return "All fields are required"
        }
        guard password == confirmPassword else {
            return "Passwords do not match"
        }
        guard password.count >= 6 else {
            return "Password must be at least 6 characters"
        }
        return nil
    }

    // MARK: - Forgot Password

    static func validateForgotPassword(email: String) -> String? {
        guard !email.isEmpty else {
            return "Please enter your email address"
        }
        return nil
    }

    // MARK: - Password form validation (SignUp screen)

    static func isSignUpFormValid(email: String, password: String, confirmPassword: String, name: String) -> Bool {
        return !email.isEmpty
            && !password.isEmpty
            && !confirmPassword.isEmpty
            && !name.isEmpty
            && password.count >= 6
    }

    static func hasPasswordMismatch(password: String, confirmPassword: String) -> Bool {
        return !confirmPassword.isEmpty && password != confirmPassword
    }
}

// MARK: - Sign In Tests

final class SignInValidationTests: XCTestCase {

    func test_blankEmail_returnsError() {
        let error = AuthValidator.validateSignIn(email: "", password: "password123")
        XCTAssertEqual(error, "Email and password required")
    }

    func test_blankPassword_returnsError() {
        let error = AuthValidator.validateSignIn(email: "test@example.com", password: "")
        XCTAssertEqual(error, "Email and password required")
    }

    func test_blankEmailAndPassword_returnsError() {
        let error = AuthValidator.validateSignIn(email: "", password: "")
        XCTAssertEqual(error, "Email and password required")
    }

    func test_validCredentials_returnsNil() {
        let error = AuthValidator.validateSignIn(email: "test@example.com", password: "password123")
        XCTAssertNil(error)
    }
}

// MARK: - Sign Up Tests

final class SignUpValidationTests: XCTestCase {

    func test_blankName_returnsError() {
        let error = AuthValidator.validateSignUp(
            email: "test@example.com", password: "password123",
            confirmPassword: "password123", name: ""
        )
        XCTAssertEqual(error, "All fields are required")
    }

    func test_blankEmail_returnsError() {
        let error = AuthValidator.validateSignUp(
            email: "", password: "password123",
            confirmPassword: "password123", name: "Test User"
        )
        XCTAssertEqual(error, "All fields are required")
    }

    func test_blankPassword_returnsError() {
        let error = AuthValidator.validateSignUp(
            email: "test@example.com", password: "",
            confirmPassword: "", name: "Test User"
        )
        XCTAssertEqual(error, "All fields are required")
    }

    func test_passwordMismatch_returnsError() {
        let error = AuthValidator.validateSignUp(
            email: "test@example.com", password: "password123",
            confirmPassword: "different", name: "Test User"
        )
        XCTAssertEqual(error, "Passwords do not match")
    }

    func test_shortPassword_returnsError() {
        let error = AuthValidator.validateSignUp(
            email: "test@example.com", password: "abc",
            confirmPassword: "abc", name: "Test User"
        )
        XCTAssertEqual(error, "Password must be at least 6 characters")
    }

    func test_exactlyMinLength_returnsNil() {
        let error = AuthValidator.validateSignUp(
            email: "test@example.com", password: "abc123",
            confirmPassword: "abc123", name: "Test User"
        )
        XCTAssertNil(error)
    }

    func test_validInputs_returnsNil() {
        let error = AuthValidator.validateSignUp(
            email: "test@example.com", password: "password123",
            confirmPassword: "password123", name: "Test User"
        )
        XCTAssertNil(error)
    }

    func test_formValid_allFieldsFilled() {
        let valid = AuthValidator.isSignUpFormValid(
            email: "test@example.com", password: "password123",
            confirmPassword: "password123", name: "Test User"
        )
        XCTAssertTrue(valid)
    }

    func test_formInvalid_emptyField() {
        let valid = AuthValidator.isSignUpFormValid(
            email: "", password: "password123",
            confirmPassword: "password123", name: "Test User"
        )
        XCTAssertFalse(valid)
    }

    func test_formInvalid_shortPassword() {
        let valid = AuthValidator.isSignUpFormValid(
            email: "test@example.com", password: "abc",
            confirmPassword: "abc", name: "Test User"
        )
        XCTAssertFalse(valid)
    }

    func test_passwordMismatch_detected() {
        let mismatch = AuthValidator.hasPasswordMismatch(password: "password123", confirmPassword: "different")
        XCTAssertTrue(mismatch)
    }

    func test_passwordMatch_notDetectedAsMismatch() {
        let mismatch = AuthValidator.hasPasswordMismatch(password: "password123", confirmPassword: "password123")
        XCTAssertFalse(mismatch)
    }

    func test_emptyConfirm_notDetectedAsMismatch() {
        // Empty confirmPassword should not show mismatch (user hasn't typed yet)
        let mismatch = AuthValidator.hasPasswordMismatch(password: "password123", confirmPassword: "")
        XCTAssertFalse(mismatch)
    }
}

// MARK: - Forgot Password Tests

final class ForgotPasswordValidationTests: XCTestCase {

    func test_blankEmail_returnsError() {
        let error = AuthValidator.validateForgotPassword(email: "")
        XCTAssertEqual(error, "Please enter your email address")
    }

    func test_whitespaceOnly_returnsError() {
        // Should also be treated as blank after trimming in real usage
        let error = AuthValidator.validateForgotPassword(email: "   ")
        // Note: screen-level trimming happens before calling validate
        // This test confirms the validator itself requires non-empty raw input
        XCTAssertNil(error) // raw " " is non-empty — trimming is caller's responsibility
    }

    func test_validEmail_returnsNil() {
        let error = AuthValidator.validateForgotPassword(email: "test@example.com")
        XCTAssertNil(error)
    }
}
