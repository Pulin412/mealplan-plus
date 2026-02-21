# Screen 00: Login / Sign Up / Forgot Password

**Version**: 1.0
**Status**: ✅ Android | ✅ iOS
**Branch**: `feature/screen-login-signup`

---

## Screens

- **LoginScreen** — entry point for unauthenticated users
- **SignUpScreen** — new account creation
- **ForgotPasswordScreen** — email lookup with local-only password explanation

---

## Requirements

### Login Screen
- Logo: fork icon in green circle, app name "MealPlan+", tagline "Smart nutrition tracking"
- Email field (envelope icon)
- Password field (lock icon + show/hide toggle)
- "Forgot Password?" link — right-aligned below password field
- "Sign In" primary button — full width, green filled
- "or" divider
- "Sign Up" secondary button — full width, green outlined
- Inline error below password field (replaces snackbar)
- Trademark at bottom: `© 2026 Pulin. All rights reserved.`

### Sign Up Screen
- Back arrow top-left → returns to Login
- Title: "Create Account" / subtitle: "Join MealPlan+ today"
- Name field (person icon)
- Email field (envelope icon)
- Password field (lock + show/hide) — "At least 6 characters" hint
- Confirm Password field (lock + show/hide) — live inline mismatch error
- "Create Account" primary button — full width, green filled
- "Already have an account? Sign In" link at bottom
- Trademark at bottom

### Forgot Password Screen
- Back arrow → returns to Login
- Title: "Forgot Password?" / subtitle: "Enter your registered email"
- Email field (envelope icon)
- "Send Password" button — full width, green filled
- On submit: dialog explaining passwords are locally stored (SHA-256) and cannot be sent via email
- "Back to Sign In" link

---

## Design Spec

| Property | Value |
|----------|-------|
| Background | White (`Color(.systemBackground)` / `Color.White`) |
| Primary color | Green (`#4CAF50` / `Color.green`) |
| Error color | Red (Material error / `.red`) |
| H. padding | 24dp / 24pt |
| Field gap | 16dp / 16pt |
| Button height | 50dp / 50pt, full width, rounded corners (10pt radius) |
| Primary button | Green filled, white text |
| Secondary button | Green outlined, green text |
| Logo | 80dp/pt green circle + white fork icon |
| Trademark | Small gray centered text at bottom |

---

## ASCII Layout — Login

```
┌─────────────────────────────────┐
│                                 │
│         ┌────────┐              │
│         │   🍴   │  ← 80dp green circle
│         └────────┘              │
│          MealPlan+              │  bold, large
│    Smart nutrition tracking     │  gray, small
│                                 │
│  ┌──────────────────────────┐  │
│  │ ✉  Email                 │  │
│  └──────────────────────────┘  │
│                                 │
│  ┌──────────────────────────┐  │
│  │ 🔒  Password         👁  │  │
│  └──────────────────────────┘  │
│  ← inline error (red)          │
│                  Forgot Password?│  ← right-aligned
│                                 │
│  ┌──────────────────────────┐  │
│  │         Sign In          │  │  ← filled green
│  └──────────────────────────┘  │
│                                 │
│  ──────────── or ────────────  │
│                                 │
│  ┌──────────────────────────┐  │
│  │         Sign Up          │  │  ← outlined green
│  └──────────────────────────┘  │
│                                 │
│   © 2026 Pulin. All rights     │
│          reserved.              │
└─────────────────────────────────┘
```

---

## Functionality

| Action | Implementation |
|--------|----------------|
| Sign In | `AuthRepository.signInWithEmail(email, password)` → success: navigate Home, failure: inline error |
| Sign Up | `AuthRepository.signUpWithEmail(email, password, name)` → success: auto-login → Home, failure: inline error |
| Forgot Password | Lookup email via `getUserByEmail()` → if found: show dialog explaining SHA-256 local storage limitation; if not found: inline error "No account found" |

**Auth persistence**:
- Android: `AuthPreferences` (DataStore) → `isLoggedIn` Flow
- iOS: `UserDefaults` → `checkAuthState()` on launch
- Both: logged-in users skip Login and land directly on Home

---

## Files Changed

### Android

| File | Change |
|------|--------|
| `app/src/main/java/com/mealplanplus/ui/screens/auth/LoginScreen.kt` | Full redesign |
| `app/src/main/java/com/mealplanplus/ui/screens/auth/SignUpScreen.kt` | Full redesign |
| `app/src/main/java/com/mealplanplus/ui/screens/auth/AuthViewModel.kt` | Added `forgotPassword()`, `clearForgotPasswordResult()`, `forgotPasswordResult` state |
| `app/src/main/java/com/mealplanplus/data/repository/AuthRepository.kt` | Added `getUserByEmail()` |
| `app/src/main/java/com/mealplanplus/ui/screens/auth/ForgotPasswordScreen.kt` | **NEW** |
| `app/src/main/java/com/mealplanplus/ui/navigation/NavHost.kt` | Added `ForgotPassword` Screen + composable route |
| `app/build.gradle.kts` | BOM → `2024.02.00`, added `material-icons-extended` |

### iOS

| File | Change |
|------|--------|
| `iosApp/iosApp/Screens/Auth/LoginScreen.swift` | Full redesign |
| `iosApp/iosApp/Screens/Auth/SignUpScreen.swift` | Full redesign |
| `iosApp/iosApp/Screens/Auth/ForgotPasswordScreen.swift` | **NEW** |
| `iosApp/iosApp/Navigation/AppNavigation.swift` | Added `showForgotPassword` state + `AuthNavigationView` routing |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | Registered `ForgotPasswordScreen.swift` in Xcode target |

---

## Build Notes

### Android
- Compose BOM `2024.02.00` required for `HorizontalDivider` (Material3 1.2.x)
- `material-icons-extended` required for `Visibility` / `VisibilityOff` icons
- Logo uses `Icons.Default.Restaurant` (XML mipmap drawable cannot be used in `Icon()` composable)
- Uses `Icons.Default.ArrowBack` (not `AutoMirrored.Filled.ArrowBack` — not available in this BOM)

### iOS
- Logo: `ZStack { Circle().fill(Color.green) + Image(systemName: "fork.knife") }` — system image, no asset needed
- Navigation: `AuthNavigationView` uses `@State` booleans to switch between Login / SignUp / ForgotPassword inline (no `NavigationLink` push)
- `ForgotPasswordScreen` must be manually registered in `project.pbxproj` (Xcode does not auto-discover new `.swift` files)

---

## Verification Checklist

- [ ] Login screen shows green circle+fork logo, "MealPlan+", tagline
- [ ] Invalid credentials → inline error below password (no snackbar)
- [ ] "Forgot Password?" → navigates to Forgot Password screen
- [ ] Enter unknown email → inline "No account found" error
- [ ] Enter known email → dialog explains local-only limitation
- [ ] "Back to Sign In" → returns to Login
- [ ] "Sign Up" button → navigates to Sign Up screen
- [ ] Back arrow on Sign Up → returns to Login
- [ ] Password mismatch on Sign Up → live inline error
- [ ] Valid signup → auto-login → Home screen
- [ ] Trademark visible at bottom of all 3 screens
- [ ] Kill app → relaunch → logged-in user goes directly to Home
- [ ] Kill app → relaunch → logged-out user sees Login
