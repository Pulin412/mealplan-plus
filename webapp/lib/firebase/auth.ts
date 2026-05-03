import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  type User,
} from "firebase/auth";
import { getFirebase } from "./config";

const googleProvider = new GoogleAuthProvider();

const a = () => getFirebase().auth;

export const signInWithGoogle = () => signInWithPopup(a(), googleProvider);
export const signInWithEmail = (email: string, password: string) =>
  signInWithEmailAndPassword(a(), email, password);
export const signUpWithEmail = (email: string, password: string) =>
  createUserWithEmailAndPassword(a(), email, password);
export const logout = () => signOut(a());
export const onAuthChange = (cb: (user: User | null) => void) =>
  onAuthStateChanged(a(), cb);
/**
 * Returns the current user's ID token.
 * Waits for Firebase Auth to restore session state (async on page load)
 * before giving up — prevents spurious 403s on hard refresh.
 */
export const getIdToken = (): Promise<string | null> =>
  new Promise((resolve) => {
    const auth = a();
    // If already initialized, return immediately
    if (auth.currentUser) {
      resolve(auth.currentUser.getIdToken());
      return;
    }
    // Otherwise wait for the first auth state event (fires within ~1s on reload)
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      unsubscribe();
      resolve(user ? user.getIdToken() : null);
    });
  });
