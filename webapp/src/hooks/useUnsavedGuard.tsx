"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { UnsavedChangesDialog } from "@/components/UnsavedChangesDialog";

/** The active editor's save/discard hooks. `canSave` mirrors its Save-button validation. */
type Guard = { canSave: boolean; onSave: () => void; onDiscard: () => void };

type GuardCtx = {
  /** Register (or clear with null) the current dirty editor's guard. */
  setGuard: (g: Guard | null) => void;
  /** Run `proceed` now if nothing's dirty; otherwise prompt first. */
  attempt: (proceed: () => void) => void;
};

const Ctx = createContext<GuardCtx>({ setGuard: () => {}, attempt: (p) => p() });

/**
 * App-level guard so navigation away from a dirty create/edit screen (bottom-nav taps, the editor's
 * ✕, browser tab close) prompts to Save / Discard / Keep editing. Wrapped around the whole app in
 * the root layout; editors register a guard via {@link useUnsavedGuard} while dirty.
 */
export function UnsavedGuardProvider({ children }: { children: ReactNode }) {
  // Ref for synchronous reads inside `attempt`; state copy drives the dialog + beforeunload.
  const guardRef = useRef<Guard | null>(null);
  const [guard, setGuardState] = useState<Guard | null>(null);
  const [pending, setPending] = useState<(() => void) | null>(null);

  const setGuard = useCallback((g: Guard | null) => {
    guardRef.current = g;
    setGuardState(g);
  }, []);

  const attempt = useCallback((proceed: () => void) => {
    if (guardRef.current == null) proceed();
    else setPending(() => proceed);
  }, []);

  const finish = () => {
    const p = pending;
    setPending(null);
    p?.();
  };
  const resolveSave = () => { guardRef.current?.onSave(); finish(); };
  const resolveDiscard = () => { guardRef.current?.onDiscard(); finish(); };
  const cancel = () => setPending(null);

  // Warn on hard navigation (tab close / refresh) while there are unsaved changes.
  useEffect(() => {
    if (!guard) return;
    const h = (e: BeforeUnloadEvent) => { e.preventDefault(); e.returnValue = ""; };
    window.addEventListener("beforeunload", h);
    return () => window.removeEventListener("beforeunload", h);
  }, [guard]);

  return (
    <Ctx.Provider value={{ setGuard, attempt }}>
      {children}
      {pending && (
        <UnsavedChangesDialog
          canSave={guard?.canSave ?? false}
          onSave={resolveSave}
          onDiscard={resolveDiscard}
          onKeepEditing={cancel}
        />
      )}
    </Ctx.Provider>
  );
}

export function useUnsavedGuard() {
  return useContext(Ctx);
}
