import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/types.generated";

type FoodDto = components["schemas"]["FoodDto"];

interface UseFoodsResult {
  foods: FoodDto[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Fetches the full food list for the authenticated user (system + personal).
 * Deduplicate calls: multiple pages mounting this hook in the same render
 * cycle will each get their own fetch today. Use SWR/TanStack Query for
 * deduplication if it becomes a bottleneck.
 */
export function useFoods(): UseFoodsResult {
  const { user } = useAuth();
  const [foods,   setFoods]   = useState<FoodDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  const fetch = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<FoodDto[]>("/api/v1/foods");
      setFoods(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load foods");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetch(); }, [fetch]);

  return { foods, loading, error, refetch: fetch };
}
