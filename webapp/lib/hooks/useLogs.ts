import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/types.generated";

type DailyLogDto = components["schemas"]["DailyLogDto"];

interface UseLogsResult {
  logs: DailyLogDto[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Fetches all daily logs for the authenticated user, newest first.
 */
export function useLogs(): UseLogsResult {
  const { user } = useAuth();
  const [logs,    setLogs]    = useState<DailyLogDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  const fetch = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<DailyLogDto[]>("/api/v1/logs");
      setLogs(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load logs");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetch(); }, [fetch]);

  return { logs, loading, error, refetch: fetch };
}
