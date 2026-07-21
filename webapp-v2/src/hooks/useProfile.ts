"use client";

import { useState, useEffect, useCallback } from "react";
import { getMe, updateMe, type UserResponse, type UserUpdateRequest } from "@/lib/api/user";
import { useAuth } from "@/hooks/useAuth";

export function useProfile() {
  const { signOut } = useAuth();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setLoading(true);
    getMe().then(setUser).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, []);

  const patch = useCallback(async (p: UserUpdateRequest) => {
    setSaving(true);
    try { setUser(await updateMe(p)); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed to save"); }
    finally { setSaving(false); }
  }, []);

  return { user, loading, error, saving, patch, signOut };
}
