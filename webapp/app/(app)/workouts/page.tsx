"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback } from "react";
import {
  Plus, Trash2, X, ChevronDown, ChevronUp, Dumbbell, Clock, CalendarDays,
  LayoutTemplate, Play, Pencil,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";

// ─── Types ────────────────────────────────────────────────────────────────────

interface ExerciseDto {
  id?: number;
  name: string;
  category: string;
  muscleGroup?: string;
}

interface WorkoutSetDto {
  exerciseId: number;
  setNumber: number;
  reps?: number;
  weightKg?: number;
  durationSeconds?: number;
  notes?: string;
}

interface WorkoutSessionDto {
  id?: number;
  name: string;
  date?: string;
  durationMinutes?: number;
  notes?: string;
  isCompleted?: boolean;
  sets: WorkoutSetDto[];
}

interface TemplateExerciseDto {
  id?: number;
  exerciseId: number;
  orderIndex: number;
  targetSets: number;
  targetReps?: number;
  targetWeightKg?: number;
  notes?: string;
  exerciseName?: string;
  exerciseCategory?: string;
}

interface WorkoutTemplateDto {
  id?: number;
  name: string;
  category: string;
  notes?: string;
  exercises: TemplateExerciseDto[];
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatDate(dateStr?: string | null) {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short", year: "numeric" });
}

function todayStr() { return new Date().toISOString().split("T")[0]; }

// ─── Set row inside the log form ──────────────────────────────────────────────
interface SetRowProps {
  idx: number;
  set: WorkoutSetDto;
  exercises: ExerciseDto[];
  onChange: (s: WorkoutSetDto) => void;
  onRemove: () => void;
}
function SetRow({ idx, set, exercises, onChange, onRemove }: SetRowProps) {
  return (
    <div className="bg-bg-page rounded-lg border border-divider p-3 space-y-2">
      <div className="flex items-center justify-between">
        <p className="text-[11px] font-bold text-text-muted uppercase">Set {idx + 1}</p>
        <button onClick={onRemove} className="text-text-muted hover:text-red-500 transition-colors"><X size={13} /></button>
      </div>
      <div>
        <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Exercise</label>
        <select
          value={set.exerciseId || ""}
          onChange={(e) => onChange({ ...set, exerciseId: parseInt(e.target.value) || 0 })}
          className="w-full rounded-lg border border-divider bg-bg-card px-3 py-2 text-sm text-text-primary outline-none"
        >
          <option value="">– Select –</option>
          {exercises.map((ex) => (
            <option key={ex.id} value={ex.id}>{ex.name}{ex.muscleGroup ? ` (${ex.muscleGroup})` : ""}</option>
          ))}
        </select>
      </div>
      <div className="grid grid-cols-3 gap-2">
        <div>
          <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Reps</label>
          <input type="number" min={0} value={set.reps ?? ""} placeholder="—"
            onChange={(e) => onChange({ ...set, reps: e.target.value ? parseInt(e.target.value) : undefined })}
            className="w-full rounded-lg border border-divider bg-bg-card px-2 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary" />
        </div>
        <div>
          <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Weight (kg)</label>
          <input type="number" min={0} step="0.5" value={set.weightKg ?? ""} placeholder="—"
            onChange={(e) => onChange({ ...set, weightKg: e.target.value ? parseFloat(e.target.value) : undefined })}
            className="w-full rounded-lg border border-divider bg-bg-card px-2 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary" />
        </div>
        <div>
          <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Duration (s)</label>
          <input type="number" min={0} value={set.durationSeconds ?? ""} placeholder="—"
            onChange={(e) => onChange({ ...set, durationSeconds: e.target.value ? parseInt(e.target.value) : undefined })}
            className="w-full rounded-lg border border-divider bg-bg-card px-2 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary" />
        </div>
      </div>
      <div>
        <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Notes</label>
        <input value={set.notes ?? ""} placeholder="Optional"
          onChange={(e) => onChange({ ...set, notes: e.target.value || undefined })}
          className="w-full rounded-lg border border-divider bg-bg-card px-3 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
      </div>
    </div>
  );
}

// ─── Template exercise row inside the template form ───────────────────────────
interface TemplateExRowProps {
  idx: number;
  te: TemplateExerciseDto;
  exercises: ExerciseDto[];
  onChange: (t: TemplateExerciseDto) => void;
  onRemove: () => void;
}
function TemplateExRow({ idx, te, exercises, onChange, onRemove }: TemplateExRowProps) {
  return (
    <div className="bg-bg-page rounded-lg border border-divider p-3 space-y-2">
      <div className="flex items-center justify-between">
        <p className="text-[11px] font-bold text-text-muted uppercase">Exercise {idx + 1}</p>
        <button type="button" onClick={onRemove} className="text-text-muted hover:text-red-500 transition-colors"><X size={13} /></button>
      </div>
      <div>
        <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Exercise</label>
        <select
          value={te.exerciseId || ""}
          onChange={(e) => onChange({ ...te, exerciseId: parseInt(e.target.value) || 0 })}
          className="w-full rounded-lg border border-divider bg-bg-card px-3 py-2 text-sm text-text-primary outline-none"
        >
          <option value="">– Select –</option>
          {exercises.map((ex) => (
            <option key={ex.id} value={ex.id}>{ex.name}{ex.muscleGroup ? ` (${ex.muscleGroup})` : ""}</option>
          ))}
        </select>
      </div>
      <div className="grid grid-cols-3 gap-2">
        <div>
          <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Sets</label>
          <input type="number" min={1} value={te.targetSets}
            onChange={(e) => onChange({ ...te, targetSets: parseInt(e.target.value) || 3 })}
            className="w-full rounded-lg border border-divider bg-bg-card px-2 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary" />
        </div>
        <div>
          <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Reps</label>
          <input type="number" min={0} value={te.targetReps ?? ""} placeholder="—"
            onChange={(e) => onChange({ ...te, targetReps: e.target.value ? parseInt(e.target.value) : undefined })}
            className="w-full rounded-lg border border-divider bg-bg-card px-2 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary" />
        </div>
        <div>
          <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Weight (kg)</label>
          <input type="number" min={0} step="0.5" value={te.targetWeightKg ?? ""} placeholder="—"
            onChange={(e) => onChange({ ...te, targetWeightKg: e.target.value ? parseFloat(e.target.value) : undefined })}
            className="w-full rounded-lg border border-divider bg-bg-card px-2 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary" />
        </div>
      </div>
      <div>
        <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Notes</label>
        <input value={te.notes ?? ""} placeholder="Optional"
          onChange={(e) => onChange({ ...te, notes: e.target.value || undefined })}
          className="w-full rounded-lg border border-divider bg-bg-card px-3 py-1.5 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
      </div>
    </div>
  );
}

// ─── Session card ─────────────────────────────────────────────────────────────
function SessionCard({
  session, exercises, onDelete, deleting,
}: {
  session: WorkoutSessionDto;
  exercises: ExerciseDto[];
  onDelete: () => void;
  deleting: boolean;
}) {
  const [expanded, setExpanded] = useState(false);
  const exerciseCounts = new Map<number, number>();
  for (const s of session.sets ?? []) {
    exerciseCounts.set(s.exerciseId, (exerciseCounts.get(s.exerciseId) ?? 0) + 1);
  }

  return (
    <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
      <div className="flex items-start gap-3 px-4 py-3 cursor-pointer" onClick={() => setExpanded((v) => !v)}>
        <div className="w-9 h-9 rounded-xl bg-green-light flex items-center justify-center shrink-0 mt-0.5">
          <Dumbbell size={16} className="text-green" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-[14px] font-semibold text-text-primary">{session.name}</p>
          <div className="flex items-center gap-3 mt-0.5 flex-wrap">
            <span className="flex items-center gap-1 text-[11px] text-text-muted">
              <CalendarDays size={11} /> {formatDate(session.date)}
            </span>
            {session.durationMinutes && (
              <span className="flex items-center gap-1 text-[11px] text-text-muted">
                <Clock size={11} /> {session.durationMinutes} min
              </span>
            )}
            {(session.sets?.length ?? 0) > 0 && (
              <span className="text-[11px] text-text-muted">{session.sets?.length} set{session.sets?.length !== 1 ? "s" : ""}</span>
            )}
            {session.isCompleted && (
              <span className="text-[9px] font-bold px-1.5 py-0.5 rounded-full bg-green-light text-green">Completed</span>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {expanded ? <ChevronUp size={14} className="text-text-muted" /> : <ChevronDown size={14} className="text-text-muted" />}
          <button
            disabled={deleting}
            onClick={(e) => { e.stopPropagation(); onDelete(); }}
            className="text-text-muted hover:text-red-500 transition-colors p-1 -mr-1"
          >
            <Trash2 size={14} />
          </button>
        </div>
      </div>

      {expanded && (
        <div className="border-t border-divider px-4 py-3 space-y-2">
          {session.notes && <p className="text-[13px] text-text-secondary mb-2">{session.notes}</p>}
          {exerciseCounts.size === 0 ? (
            <p className="text-[12px] text-text-muted">No sets logged</p>
          ) : (
            Array.from(exerciseCounts.entries()).map(([exerciseId, count]) => {
              const ex = exercises.find((e) => e.id === exerciseId);
              const exSets = (session.sets ?? []).filter((s) => s.exerciseId === exerciseId);
              return (
                <div key={exerciseId} className="space-y-1.5">
                  <p className="text-[12px] font-semibold text-text-primary">
                    {ex?.name ?? `Exercise #${exerciseId}`}
                    <span className="text-text-muted font-normal ml-1.5">· {count} set{count !== 1 ? "s" : ""}</span>
                  </p>
                  <div className="grid grid-cols-1 gap-1">
                    {exSets.map((s, i) => (
                      <div key={i} className="flex items-center gap-3 text-[11px] text-text-muted">
                        <span className="text-text-placeholder">Set {s.setNumber}</span>
                        {s.reps != null && <span>{s.reps} reps</span>}
                        {s.weightKg != null && <span>{s.weightKg} kg</span>}
                        {s.durationSeconds != null && <span>{s.durationSeconds}s</span>}
                        {s.notes && <span className="italic">{s.notes}</span>}
                      </div>
                    ))}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}

// ─── Template card ────────────────────────────────────────────────────────────
function TemplateCard({
  template, onEdit, onDelete, onStart, starting, deleting,
}: {
  template: WorkoutTemplateDto;
  onEdit: () => void;
  onDelete: () => void;
  onStart: () => void;
  starting: boolean;
  deleting: boolean;
}) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
      <div className="flex items-start gap-3 px-4 py-3 cursor-pointer" onClick={() => setExpanded((v) => !v)}>
        <div className="w-9 h-9 rounded-xl bg-blue-50 dark:bg-blue-950 flex items-center justify-center shrink-0 mt-0.5">
          <LayoutTemplate size={16} className="text-blue-500" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-[14px] font-semibold text-text-primary">{template.name}</p>
          <div className="flex items-center gap-2 mt-0.5 flex-wrap">
            <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-full bg-bg-page text-text-muted uppercase">{template.category}</span>
            <span className="text-[11px] text-text-muted">{template.exercises.length} exercise{template.exercises.length !== 1 ? "s" : ""}</span>
          </div>
        </div>
        <div className="flex items-center gap-1.5 shrink-0">
          {expanded ? <ChevronUp size={14} className="text-text-muted" /> : <ChevronDown size={14} className="text-text-muted" />}
          <button
            disabled={deleting}
            onClick={(e) => { e.stopPropagation(); onEdit(); }}
            className="text-text-muted hover:text-text-primary transition-colors p-1"
          ><Pencil size={13} /></button>
          <button
            disabled={deleting}
            onClick={(e) => { e.stopPropagation(); onDelete(); }}
            className="text-text-muted hover:text-red-500 transition-colors p-1"
          ><Trash2 size={13} /></button>
        </div>
      </div>

      {expanded && (
        <div className="border-t border-divider px-4 py-3 space-y-3">
          {template.notes && <p className="text-[13px] text-text-secondary">{template.notes}</p>}
          {template.exercises.length === 0 ? (
            <p className="text-[12px] text-text-muted">No exercises in this template</p>
          ) : (
            <div className="space-y-1.5">
              {template.exercises.map((te, i) => (
                <div key={i} className="flex items-center gap-2 text-[12px] text-text-muted">
                  <span className="w-4 text-text-placeholder text-right">{i + 1}.</span>
                  <span className="text-text-primary font-medium">{te.exerciseName || `Exercise #${te.exerciseId}`}</span>
                  <span className="text-text-placeholder">—</span>
                  <span>{te.targetSets} × {te.targetReps ?? "?"} reps{te.targetWeightKg ? ` @ ${te.targetWeightKg}kg` : ""}</span>
                </div>
              ))}
            </div>
          )}
          <button
            onClick={onStart}
            disabled={starting || template.exercises.length === 0}
            className="w-full flex items-center justify-center gap-2 rounded-xl bg-green py-2 text-sm font-semibold text-white disabled:opacity-40 hover:opacity-90 transition-opacity"
          >
            <Play size={14} />
            {starting ? "Starting…" : "Start session from template"}
          </button>
        </div>
      )}
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────
type Tab = "log" | "templates" | "history";

const CATEGORIES = ["STRENGTH", "CARDIO", "FLEXIBILITY", "HIIT", "SPORT", "OTHER"];

export default function WorkoutsPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState<Tab>("history");

  // Data
  const [sessions, setSessions] = useState<WorkoutSessionDto[]>([]);
  const [exercises, setExercises] = useState<ExerciseDto[]>([]);
  const [templates, setTemplates] = useState<WorkoutTemplateDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Session log form
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [date, setDate] = useState(todayStr());
  const [duration, setDuration] = useState("");
  const [notes, setNotes] = useState("");
  const [isCompleted, setIsCompleted] = useState(true);
  const [sets, setSets] = useState<WorkoutSetDto[]>([]);

  // Template form
  const [showTemplateForm, setShowTemplateForm] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<WorkoutTemplateDto | null>(null);
  const [tName, setTName] = useState("");
  const [tCategory, setTCategory] = useState("STRENGTH");
  const [tNotes, setTNotes] = useState("");
  const [tExercises, setTExercises] = useState<TemplateExerciseDto[]>([]);
  const [savingTemplate, setSavingTemplate] = useState(false);
  const [deletingTemplateId, setDeletingTemplateId] = useState<number | null>(null);
  const [startingTemplateId, setStartingTemplateId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true); setError(null);
    try {
      const [sess, exs, tmplts] = await Promise.all([
        api.get<WorkoutSessionDto[]>("/api/v1/workout-sessions"),
        api.get<ExerciseDto[]>("/api/v1/exercises"),
        api.get<WorkoutTemplateDto[]>("/api/v1/workout-templates"),
      ]);
      setSessions(sess.sort((a, b) => (b.date ?? "").localeCompare(a.date ?? "")));
      setExercises(exs);
      setTemplates(tmplts);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { load(); }, [load]);

  // ── Session log ──
  const addSet = () => setSets((p) => [...p, { exerciseId: 0, setNumber: p.length + 1 }]);
  const updateSet = (idx: number, updated: WorkoutSetDto) =>
    setSets((p) => p.map((s, i) => (i === idx ? updated : s)));
  const removeSet = (idx: number) =>
    setSets((p) => p.filter((_, i) => i !== idx).map((s, i) => ({ ...s, setNumber: i + 1 })));
  const resetLogForm = () => { setName(""); setDate(todayStr()); setDuration(""); setNotes(""); setIsCompleted(true); setSets([]); };

  const submitSession = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    const validSets = sets.filter((s) => s.exerciseId > 0);
    setSaving(true); setError(null);
    try {
      const payload: WorkoutSessionDto = {
        name: name.trim(), date,
        durationMinutes: duration ? parseInt(duration) : undefined,
        notes: notes.trim() || undefined, isCompleted, sets: validSets,
      };
      const created = await api.post<WorkoutSessionDto>("/api/v1/workout-sessions", payload);
      setSessions((p) => [created, ...p]);
      resetLogForm(); setTab("history");
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally { setSaving(false); }
  };

  const deleteSession = async (id: number) => {
    if (!confirm("Delete this workout session?")) return;
    setDeletingId(id);
    try {
      await api.delete(`/api/v1/workout-sessions/${id}`);
      setSessions((p) => p.filter((s) => s.id !== id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    } finally { setDeletingId(null); }
  };

  // ── Templates ──
  const openNewTemplate = () => {
    setEditingTemplate(null);
    setTName(""); setTCategory("STRENGTH"); setTNotes(""); setTExercises([]);
    setShowTemplateForm(true);
  };

  const openEditTemplate = (t: WorkoutTemplateDto) => {
    setEditingTemplate(t);
    setTName(t.name); setTCategory(t.category); setTNotes(t.notes ?? ""); setTExercises(t.exercises);
    setShowTemplateForm(true);
  };

  const cancelTemplateForm = () => { setShowTemplateForm(false); setEditingTemplate(null); };

  const addTemplateExercise = () =>
    setTExercises((p) => [...p, { exerciseId: 0, orderIndex: p.length, targetSets: 3 }]);

  const updateTemplateExercise = (idx: number, te: TemplateExerciseDto) =>
    setTExercises((p) => p.map((x, i) => (i === idx ? te : x)));

  const removeTemplateExercise = (idx: number) =>
    setTExercises((p) => p.filter((_, i) => i !== idx).map((x, i) => ({ ...x, orderIndex: i })));

  const submitTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tName.trim()) return;
    const validExs = tExercises.filter((te) => te.exerciseId > 0);
    setSavingTemplate(true); setError(null);
    try {
      const payload: WorkoutTemplateDto = {
        name: tName.trim(), category: tCategory,
        notes: tNotes.trim() || undefined, exercises: validExs,
      };
      if (editingTemplate?.id) {
        const updated = await api.put<WorkoutTemplateDto>(`/api/v1/workout-templates/${editingTemplate.id}`, payload);
        setTemplates((p) => p.map((t) => (t.id === editingTemplate.id ? updated : t)));
      } else {
        const created = await api.post<WorkoutTemplateDto>("/api/v1/workout-templates", payload);
        setTemplates((p) => [created, ...p]);
      }
      cancelTemplateForm();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save template");
    } finally { setSavingTemplate(false); }
  };

  const deleteTemplate = async (id: number) => {
    if (!confirm("Delete this template?")) return;
    setDeletingTemplateId(id);
    try {
      await api.delete(`/api/v1/workout-templates/${id}`);
      setTemplates((p) => p.filter((t) => t.id !== id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete template");
    } finally { setDeletingTemplateId(null); }
  };

  const startFromTemplate = async (id: number) => {
    setStartingTemplateId(id); setError(null);
    try {
      const session = await api.post<WorkoutSessionDto>(`/api/v1/workout-templates/${id}/start`, {});
      setSessions((p) => [session, ...p]);
      setTab("history");
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to start session");
    } finally { setStartingTemplateId(null); }
  };

  // Group sessions by month
  const grouped = sessions.reduce<Record<string, WorkoutSessionDto[]>>((acc, s) => {
    const key = s.date
      ? new Date(s.date).toLocaleDateString("en-GB", { month: "long", year: "numeric" })
      : "Unknown";
    if (!acc[key]) acc[key] = [];
    acc[key].push(s);
    return acc;
  }, {});

  const tabs: { key: Tab; label: string }[] = [
    { key: "log", label: "Log" },
    { key: "templates", label: "Templates" },
    { key: "history", label: "History" },
  ];

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between pt-1">
        <h1 className="text-[22px] font-semibold text-text-primary">Workouts</h1>
        {tab === "templates" && !showTemplateForm && (
          <button
            onClick={openNewTemplate}
            className="flex items-center gap-1.5 rounded-xl bg-text-primary px-4 py-2 text-sm font-semibold text-bg-card"
          >
            <Plus size={14} /> New template
          </button>
        )}
      </div>

      {/* Tab switcher */}
      <div className="flex gap-1 bg-bg-card rounded-xl p-1 border border-divider">
        {tabs.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => { setTab(key); if (key !== "templates") setShowTemplateForm(false); }}
            className={`flex-1 rounded-lg py-1.5 text-[13px] font-semibold transition-colors ${
              tab === key ? "bg-text-primary text-bg-card" : "text-text-muted hover:text-text-primary"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>}

      {/* ── Log tab ── */}
      {tab === "log" && (
        <form onSubmit={submitSession} className="bg-bg-card rounded-xl border border-divider p-4 space-y-4">
          <p className="text-[13px] font-semibold text-text-primary">Log workout session</p>
          <div>
            <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Workout name *</label>
            <input autoFocus value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Chest Day"
              className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Date</label>
              <input type="date" value={date} onChange={(e) => setDate(e.target.value)}
                className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
            </div>
            <div>
              <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Duration (min)</label>
              <input type="number" min={0} value={duration} onChange={(e) => setDuration(e.target.value)} placeholder="e.g. 60"
                className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
            </div>
          </div>
          <div>
            <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Notes</label>
            <input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Optional"
              className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
          </div>
          <label className="flex items-center gap-2.5 cursor-pointer select-none">
            <div
              onClick={() => setIsCompleted((v) => !v)}
              className={`w-10 h-6 rounded-full transition-colors relative ${isCompleted ? "bg-green" : "bg-text-placeholder"}`}
            >
              <div className={`absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-all ${isCompleted ? "left-5" : "left-1"}`} />
            </div>
            <span className="text-[13px] text-text-primary">Mark as completed</span>
          </label>

          {/* Sets */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <p className="text-[12px] font-semibold text-text-primary">Sets <span className="text-text-muted font-normal">({sets.length})</span></p>
              <button type="button" onClick={addSet}
                className="flex items-center gap-1 text-[11px] font-semibold text-green hover:opacity-80">
                <Plus size={12} /> Add set
              </button>
            </div>
            {sets.length === 0 && (
              <p className="text-[12px] text-text-muted py-2 text-center">No sets yet. Tap &quot;Add set&quot; to start.</p>
            )}
            {sets.map((s, i) => (
              <SetRow key={i} idx={i} set={s} exercises={exercises}
                onChange={(updated) => updateSet(i, updated)}
                onRemove={() => removeSet(i)} />
            ))}
          </div>

          <button type="submit" disabled={saving || !name.trim()}
            className="w-full rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-40">
            {saving ? "Saving…" : "Save session"}
          </button>
        </form>
      )}

      {/* ── Templates tab ── */}
      {tab === "templates" && (
        <div className="space-y-3">
          {/* Template create/edit form */}
          {showTemplateForm && (
            <form onSubmit={submitTemplate} className="bg-bg-card rounded-xl border border-divider p-4 space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-[13px] font-semibold text-text-primary">
                  {editingTemplate ? "Edit template" : "New template"}
                </p>
                <button type="button" onClick={cancelTemplateForm} className="text-text-muted hover:text-text-primary">
                  <X size={16} />
                </button>
              </div>
              <div>
                <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Name *</label>
                <input autoFocus value={tName} onChange={(e) => setTName(e.target.value)} placeholder="e.g. Upper Body A"
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
              </div>
              <div>
                <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Category</label>
                <select value={tCategory} onChange={(e) => setTCategory(e.target.value)}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none">
                  {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div>
                <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Notes</label>
                <input value={tNotes} onChange={(e) => setTNotes(e.target.value)} placeholder="Optional"
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
              </div>

              {/* Template exercises */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <p className="text-[12px] font-semibold text-text-primary">
                    Exercises <span className="text-text-muted font-normal">({tExercises.length})</span>
                  </p>
                  <button type="button" onClick={addTemplateExercise}
                    className="flex items-center gap-1 text-[11px] font-semibold text-green hover:opacity-80">
                    <Plus size={12} /> Add exercise
                  </button>
                </div>
                {tExercises.length === 0 && (
                  <p className="text-[12px] text-text-muted py-2 text-center">
                    No exercises yet. Tap &quot;Add exercise&quot; to start.
                  </p>
                )}
                {tExercises.map((te, i) => (
                  <TemplateExRow key={i} idx={i} te={te} exercises={exercises}
                    onChange={(updated) => updateTemplateExercise(i, updated)}
                    onRemove={() => removeTemplateExercise(i)} />
                ))}
              </div>

              <button type="submit" disabled={savingTemplate || !tName.trim()}
                className="w-full rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-40">
                {savingTemplate ? "Saving…" : editingTemplate ? "Update template" : "Create template"}
              </button>
            </form>
          )}

          {/* Templates list */}
          {loading ? (
            <div className="space-y-3">{[1, 2].map((i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)}</div>
          ) : templates.length === 0 && !showTemplateForm ? (
            <div className="bg-bg-card rounded-xl border border-divider flex flex-col items-center py-16 gap-3">
              <span className="text-4xl">📋</span>
              <p className="text-sm text-text-muted">No templates yet</p>
              <p className="text-xs text-text-placeholder">Tap &quot;New template&quot; to create a reusable workout plan</p>
            </div>
          ) : (
            templates.map((t) => (
              <TemplateCard
                key={t.id}
                template={t}
                onEdit={() => openEditTemplate(t)}
                onDelete={() => { if (t.id) deleteTemplate(t.id); }}
                onStart={() => { if (t.id) startFromTemplate(t.id); }}
                starting={startingTemplateId === t.id}
                deleting={deletingTemplateId === t.id}
              />
            ))
          )}
        </div>
      )}

      {/* ── History tab ── */}
      {tab === "history" && (
        loading ? (
          <div className="space-y-3">{[1, 2, 3].map((i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)}</div>
        ) : sessions.length === 0 ? (
          <div className="bg-bg-card rounded-xl border border-divider flex flex-col items-center py-16 gap-3">
            <span className="text-4xl">💪</span>
            <p className="text-sm text-text-muted">No workouts logged yet</p>
            <p className="text-xs text-text-placeholder">Use the Log tab to record your first session</p>
          </div>
        ) : (
          <div className="space-y-5">
            {Object.entries(grouped).map(([month, monthSessions]) => (
              <div key={month} className="space-y-2">
                <p className="text-[11px] font-bold text-text-muted uppercase tracking-wider px-0.5">{month}</p>
                {monthSessions.map((s) => (
                  <SessionCard
                    key={s.id}
                    session={s}
                    exercises={exercises}
                    onDelete={() => { if (s.id) deleteSession(s.id); }}
                    deleting={deletingId === s.id}
                  />
                ))}
              </div>
            ))}
          </div>
        )
      )}
    </div>
  );
}
