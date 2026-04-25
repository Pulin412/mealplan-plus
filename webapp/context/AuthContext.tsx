"use client";
import { createContext, useContext, useEffect, useState } from "react";
import { type User } from "firebase/auth";
import { onAuthChange } from "@/lib/firebase/auth";

interface AuthContextValue {
  user: User | null;
  loading: boolean;
}

const AuthContext = createContext<AuthContextValue>({ user: null, loading: true });

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    return onAuthChange((u) => {
      setUser(u);
      setLoading(false);
      // Keep a lightweight cookie so edge middleware can guard routes
      // without needing Firebase SDK (which can't run at the edge)
      if (u) {
        document.cookie = "mp_session=1; path=/; max-age=86400; SameSite=Lax";
      } else {
        document.cookie = "mp_session=; path=/; max-age=0; SameSite=Lax";
      }
    });
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
