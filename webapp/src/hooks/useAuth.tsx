"use client";

import { createContext, useContext, useEffect, useState, useCallback } from "react";
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  sendPasswordResetEmail,
  signOut as fbSignOut,
  deleteUser,
  type User,
} from "firebase/auth";
import { getFirebaseAuth } from "@/lib/auth/firebase";
import { deleteMe } from "@/lib/api/user";

// Kept in sync with useOnboarding's KEY — cleared on sign-out/delete so the next account re-checks
// the server for onboarding status instead of inheriting this device's cached flag.
const ONBOARDING_KEY = "mp_onboarding_done";

interface AuthState {
  user: User | null;
  loading: boolean;
  signInEmail: (email: string, password: string) => Promise<void>;
  signUpEmail: (email: string, password: string) => Promise<void>;
  signInGoogle: () => Promise<void>;
  sendPasswordReset: (email: string) => Promise<void>;
  signOut: () => Promise<void>;
  deleteAccount: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    return onAuthStateChanged(getFirebaseAuth(), (u) => {
      setUser(u);
      setLoading(false);
    });
  }, []);

  const signInEmail = useCallback(async (email: string, password: string) => {
    await signInWithEmailAndPassword(getFirebaseAuth(), email, password);
  }, []);

  const signUpEmail = useCallback(async (email: string, password: string) => {
    await createUserWithEmailAndPassword(getFirebaseAuth(), email, password);
  }, []);

  const signInGoogle = useCallback(async () => {
    await signInWithPopup(getFirebaseAuth(), new GoogleAuthProvider());
  }, []);

  const sendPasswordReset = useCallback(async (email: string) => {
    await sendPasswordResetEmail(getFirebaseAuth(), email.trim());
  }, []);

  const signOut = useCallback(async () => {
    try { localStorage.removeItem(ONBOARDING_KEY); } catch { /* ignore */ }
    await fbSignOut(getFirebaseAuth());
  }, []);

  // Right-to-erasure: delete server data first, then this device's Firebase auth account (option A).
  const deleteAccount = useCallback(async () => {
    await deleteMe();
    try { localStorage.removeItem(ONBOARDING_KEY); } catch { /* ignore */ }
    const current = getFirebaseAuth().currentUser;
    if (current) {
      // deleteUser may throw auth/requires-recent-login; server data is already gone, so fall back
      // to signing out (the empty auth account can be re-authed & removed later).
      try { await deleteUser(current); } catch { await fbSignOut(getFirebaseAuth()); }
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, signInEmail, signUpEmail, signInGoogle, sendPasswordReset, signOut, deleteAccount }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within <AuthProvider>");
  return ctx;
}
