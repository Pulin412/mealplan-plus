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
export const getIdToken = async (): Promise<string | null> =>
  a().currentUser ? a().currentUser!.getIdToken() : null;
