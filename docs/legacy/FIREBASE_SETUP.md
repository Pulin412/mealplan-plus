# Firebase Setup Instructions

To enable authentication in MealPlan+, you need to set up Firebase:

## 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" and follow the wizard
3. Give your project a name (e.g., "MealPlan Plus")

## 2. Add Android App
1. In Firebase Console, click "Add app" → Android
2. Enter package name: `com.mealplanplus`
3. Download `google-services.json`
4. Place it in `android/` folder

## 3. Enable Authentication Methods
1. Go to Authentication → Sign-in method
2. Enable "Email/Password"
3. Enable "Google" provider
4. For Google Sign-In, note the **Web client ID** (not Android client ID)

## 4. Update Web Client ID
1. Open `android/src/main/java/com/mealplanplus/ui/screens/auth/AuthViewModel.kt`
   (previously `app/src/...` — module renamed from `:app` → `:android`)
2. Replace `YOUR_WEB_CLIENT_ID.apps.googleusercontent.com` with your actual Web client ID

## 5. Build and Run
```bash
./gradlew assembleDebug
```

## Troubleshooting

### Google Sign-In not working
- Ensure you added SHA-1 fingerprint in Firebase Console
- Get SHA-1: `./gradlew signingReport`
- Add it to Firebase Console → Project settings → Your apps → SHA certificate fingerprints

### Build fails with google-services.json missing
- Ensure `google-services.json` is in `android/` folder (not `android/src/`)
