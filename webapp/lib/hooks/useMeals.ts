import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/types.generated";

type MealDto = components["schemas"]["MealDto"];

interface UseMealsResult {
  meals: MealDto[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useMeals(): UseMealsResult {
  const { user } = useAuth();
  const [meals,   setMeals]   = useState<MealDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  const fetch = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<MealDto[]>("/api/v1/meals");
      setMeals(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load meals");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetch(); }, [fetch]);

  return { meals, loading, error, refetch: fetch };
}
