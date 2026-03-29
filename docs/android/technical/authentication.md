# Authentication System

MealPlan+ uses **Firebase Authentication** for all credential management. The app supports two sign-in providers.

---

## Supported Providers

| Provider | Implementation | Password reset |
|----------|---------------|---------------|
| Email / Password | `FirebaseAuth.signInWithEmailAndPassword` | Yes — Firebase email |
| Google OAuth | `FirebaseAuth.signInWithCredential(GoogleAuthProvider)` | Via Google account |

---

## Architecture

```
AuthRepository                  (business logic — Hilt @Singleton)
    │
    ├── Firebase Auth SDK        (credential verification, account management)
    ├── UserDao                  (local Room user record)
    └── AuthPreferences          (DataStore — login state + UID mapping)
```

**AuthPreferences** (DataStore) stores:
- `isLoggedIn: Flow<Boolean>` — observed by `NavHost` to drive screen routing
- `currentUserId: Long` — the local Room user ID of the logged-in user
- Provider-subject mappings: `"email:{firebaseUid}" → localUserId`, `"google:{firebaseUid}" → localUserId`

This mapping is what links a Firebase UID to the local Room user record. It allows the same user to sign in across sessions without relying on email lookup every time.

---

## Sign-In Flows

### Email / Password

```
signInWithEmail(email, password)
    │
    ▼
FirebaseAuth.signInWithEmailAndPassword(email, password)
    │
    ├── FirebaseAuthInvalidCredentialsException → "Invalid email or password"
    ├── FirebaseAuthInvalidUserException        → "No account found with this email"
    ├── Other Exception                         → "Sign in failed: <message>"
    │
    └── Success → firebaseUser.uid
                      │
                      ▼
              Look up localUserId via AuthPreferences mapping
              (or fall back to userDao.getUserByEmail)
                      │
                      ▼
              AuthPreferences.setLoggedIn(localUserId)
                      │
                      ▼
              Result.success(localUser)
```

### Google OAuth

```
signInWithGoogle(idToken)
    │
    ▼
FirebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken))
    │
    └── Success → firebaseUser (uid, email, displayName, photoUrl)
                      │
                      ▼
              Look up existing local user by UID mapping or email
                      │
                      ├── Found → use existing localUser
                      └── Not found → create new User in Room
                                      → seed default data (userDataSeeder)
                      │
                      ▼
              setProviderSubjectMapping("google", firebaseUid, localUserId)
              setLoggedIn(localUserId)
```

---

## Sign-Up Flow (Email)

```
signUpWithEmail(email, password, name)
    │
    ▼
FirebaseAuth.createUserWithEmailAndPassword(email, password)
    │
    ├── FirebaseAuthUserCollisionException → "Email already registered"
    ├── FirebaseAuthWeakPasswordException  → "Password too weak (min 6 chars)"
    │
    └── Success → firebaseUser
                      │
                      ▼
              firebaseUser.updateProfile(displayName = name)
                      │
                      ▼
              Insert User(email, passwordHash="", displayName) into Room
                      │
                      ▼
              userDataSeeder.seedUserData(userId)   ← seeds food DB + diets
                      │
                      ▼
              setProviderSubjectMapping("email", firebaseUid, userId)
              setLoggedIn(userId)
```

**Note:** `passwordHash` is always empty `""` for new accounts. Firebase owns the credential — the app never stores or handles plain-text passwords.

---

## Password Reset

```
sendPasswordResetEmail(email)
    │
    ▼
FirebaseAuth.sendPasswordResetEmail(email)
    │
    ├── FirebaseAuthInvalidUserException → "No account found with this email"
    └── Success → Firebase sends reset email with link
```

The user receives a Firebase-generated email with a secure time-limited link. Clicking it opens a Firebase-hosted page to set a new password. The next sign-in uses the new password — no app update required.

---

## Sign-Out

```
signOut()
    │
    ├── FirebaseAuth.getInstance().signOut()
    └── AuthPreferences.clearAuth()    ← clears userId + mappings from DataStore
```

After `clearAuth()`, the `isLoggedIn` DataStore Flow emits `false`. The `NavHost` `LaunchedEffect` detects the `true → false` transition and restarts the Activity:

```kotlin
var previousLoggedIn by remember { mutableStateOf<Boolean?>(null) }
LaunchedEffect(isLoggedIn) {
    if (previousLoggedIn == true && isLoggedIn == false) {
        // Restart activity with FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
        // This resets NavHost, all ViewModels, and the back stack cleanly.
    }
    previousLoggedIn = isLoggedIn
}
```

The `previousLoggedIn` tracker is necessary because simply checking `startDestination` fails when the user launched the app while already logged out.

---

## Delete Account

```
deleteAccount(userId)
    │
    ├── clearAllUserData()       ← deletes all Room data for this user
    ├── userDao.deleteUser(userId)
    ├── FirebaseAuth.currentUser?.delete()   ← removes Firebase record
    └── signOut()
```

The Firebase deletion ensures the email can be reused for a new account and leaves no orphaned credential records.

---

## Security Notes

- Passwords are **never stored locally**. Only Firebase holds credentials.
- The `passwordHash` field in `users` table is always `""` — it is retained for schema compatibility.
- Firebase UID → local user mapping is stored in DataStore (encrypted on Android 6+).
- All email addresses are normalized: `.lowercase().trim()` before any Firebase or DB operation.
