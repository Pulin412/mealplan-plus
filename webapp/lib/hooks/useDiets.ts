import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/types.generated";

type DietDto = components["schemas"]["DietDto"];

interface UseDietsResult {
  diets: DietDto[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useDiets(): UseDietsResult {
  const { user } = useAuth();
  const [diets,   setDiets]   = useState<DietDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  const fetch = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<DietDto[]>("/api/v1/diets");
      setDiets(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load diets");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetch(); }, [fetch]);

  return { diets, loading, error, refetch: fetch };
}
