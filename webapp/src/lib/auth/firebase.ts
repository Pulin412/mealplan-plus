import { initializeApp, getApps, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";

// Lazy-initialize so the module is safe to import on the server during pre-render.
let app: FirebaseApp | null = null;
let _auth: Auth | null = null;

function getApp(): FirebaseApp {
  if (!app) {
    app = getApps().length
      ? getApps()[0]
      : initializeApp({
          apiKey:            process.env.NEXT_PUBLIC_FIREBASE_API_KEY!,
          authDomain:        process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN!,
          projectId:         process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID!,
          storageBucket:     process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET!,
          messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID!,
          appId:             process.env.NEXT_PUBLIC_FIREBASE_APP_ID!,
        });
  }
  return app;
}

export function getFirebaseAuth(): Auth {
  if (!_auth) _auth = getAuth(getApp());
  return _auth;
}
