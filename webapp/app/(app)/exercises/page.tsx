"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback } from "react";
import { Plus, Search, Trash2, X, ChevronDown, ChevronUp, ExternalLink } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";

interface ExerciseDto {
  id?: number;
  name: string;
  category: string;
  muscleGroup?: string;
  equipment?: string;
  description?: string;
  videoLink?: string;
  isSystem?: boolean;
}

const CATEGORIES = ["STRENGTH", "CARDIO", "FLEXIBILITY", "BALANCE", "SPORT"] as const;
const MUSCLE_GROUPS = [
  "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Forearms",
  "Core", "Glutes", "Quads", "Hamstrings", "Calves", "Full Body",
] as const;
const EQUIPMENT_OPTIONS = ["Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight", "Kettlebell", "Bands", "Other"] as const;

type Category = typeof CATEGORIES[number];

const CATEGORY_COLORS: Record<Category, { bg: string; text: string }> = {
  STRENGTH:    { bg: "#FFF3E0", text: "#E65100" },
  CARDIO:      { bg: "#E8F5E9", text: "#2E7D32" },
  FLEXIBILITY: { bg: "#F3E5F5", text: "#7B1FA2" },
  BALANCE:     { bg: "#E3F2FD", text: "#1565C0" },
  SPORT:       { bg: "#FBE9E7", text: "#BF360C" },
};

interface ExerciseForm {
  name: string;
  category: Category;
  muscleGroup: string;
  equipment: string;
  description: string;
  videoLink: string;
}

const emptyForm: ExerciseForm = {
  name: "", category: "STRENGTH", muscleGroup: "", equipment: "", description: "", videoLink: "",
};

export default function ExercisesPage() {
  const { user } = useAuth();
  const [exercises, setExercises] = useState<ExerciseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [filterCategory, setFilterCategory] = useState<Category | "ALL">("ALL");
  const [filterMuscle, setFilterMuscle] = useState<string>("All");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<ExerciseForm>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true); setError(null);
    try {
      setExercises(await api.get<ExerciseDto[]>("/api/v1/exercises"));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { load(); }, [load]);

  const filtered = exercises.filter((ex) => {
    if (filterCategory !== "ALL" && ex.category !== filterCategory) return false;
    if (filterMuscle !== "All" && ex.muscleGroup !== filterMuscle) return false;
    if (query && !ex.name.toLowerCase().includes(query.toLowerCase()) &&
        !(ex.muscleGroup?.toLowerCase().includes(query.toLowerCase()) ?? false)) return false;
    return true;
  });

  const setField = (k: keyof ExerciseForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
      setForm((p) => ({ ...p, [k]: e.target.value }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) return;
    setSaving(true); setError(null);
    try {
      const payload: ExerciseDto = {
        name: form.name.trim(),
        category: form.category,
        muscleGroup: form.muscleGroup || undefined,
        equipment: form.equipment || undefined,
        description: form.description.trim() || undefined,
        videoLink: form.videoLink.trim() || undefined,
      };
      const created = await api.post<ExerciseDto>("/api/v1/exercises", payload);
      setExercises((p) => [created, ...p]);
      setForm(emptyForm); setShowForm(false);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally { setSaving(false); }
  };

  const deleteExercise = async (id: number) => {
    if (!confirm("Delete this exercise?")) return;
    setDeletingId(id);
    try {
      await api.delete(`/api/v1/exercises/${id}`);
      setExercises((p) => p.filter((ex) => ex.id !== id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    } finally { setDeletingId(null); }
  };

  const uniqueMuscles = ["All", ...Array.from(new Set(exercises.map((e) => e.muscleGroup).filter(Boolean)))];

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between pt-1">
        <h1 className="text-[22px] font-semibold text-text-primary">Exercises</h1>
        <button
          onClick={() => { setShowForm((v) => !v); setForm(emptyForm); }}
          className="flex items-center gap-1.5 rounded-xl bg-text-primary px-4 py-2 text-sm font-semibold text-bg-card"
        >
          {showForm ? <X size={14} /> : <Plus size={14} />}
          {showForm ? "Cancel" : "Add exercise"}
        </button>
      </div>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>}

      {/* Add form */}
      {showForm && (
        <form onSubmit={submit} className="bg-bg-card rounded-xl border border-divider p-4 space-y-4">
          <p className="text-[13px] font-semibold text-text-primary">New exercise</p>
          <div>
            <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Name *</label>
            <input autoFocus value={form.name} onChange={setField("name")} placeholder="e.g. Flat Bench Press"
              className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Category</label>
              <select value={form.category} onChange={setField("category")}
                className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none">
                {CATEGORIES.map((c) => <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>)}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Muscle group</label>
              <select value={form.muscleGroup} onChange={setField("muscleGroup")}
                className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none">
                <option value="">– Select –</option>
                {MUSCLE_GROUPS.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Equipment</label>
              <select value={form.equipment} onChange={setField("equipment")}
                className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none">
                <option value="">– Select –</option>
                {EQUIPMENT_OPTIONS.map((e) => <option key={e} value={e}>{e}</option>)}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Video link</label>
              <input value={form.videoLink} onChange={setField("videoLink")} placeholder="https://…"
                className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
            </div>
          </div>
          <div>
            <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Description</label>
            <textarea value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
              rows={3} placeholder="Optional instructions or notes…"
              className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder resize-none" />
          </div>
          <button type="submit" disabled={saving || !form.name.trim()}
            className="w-full rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-40">
            {saving ? "Saving…" : "Save exercise"}
          </button>
        </form>
      )}

      {/* Search */}
      <div className="flex items-center gap-2 bg-bg-card rounded-xl border border-divider px-3">
        <Search size={14} className="text-text-muted shrink-0" />
        <input placeholder="Search exercises…" value={query} onChange={(e) => setQuery(e.target.value)}
          className="flex-1 py-2.5 text-sm bg-transparent outline-none text-text-primary placeholder:text-text-placeholder" />
        {query && <button onClick={() => setQuery("")} className="text-text-muted"><X size={14} /></button>}
      </div>

      {/* Category filter chips */}
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
        {(["ALL", ...CATEGORIES] as (Category | "ALL")[]).map((cat) => (
          <button
            key={cat}
            onClick={() => setFilterCategory(cat)}
            className="shrink-0 rounded-full px-3 py-1 text-[11px] font-semibold transition-colors"
            style={filterCategory === cat
              ? { background: cat === "ALL" ? "#1A1A1A" : CATEGORY_COLORS[cat as Category].bg,
                  color: cat === "ALL" ? "#FFFFFF" : CATEGORY_COLORS[cat as Category].text,
                  border: "1px solid transparent" }
              : { background: "transparent", color: "#888888", border: "1px solid #E0E0E0" }
            }
          >
            {cat === "ALL" ? "All" : cat.charAt(0) + cat.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Muscle filter */}
      {uniqueMuscles.length > 2 && (
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
          {uniqueMuscles.map((m) => (
            <button
              key={m}
              onClick={() => setFilterMuscle(m ?? "All")}
              className="shrink-0 rounded-full px-3 py-1 text-[11px] font-medium border transition-colors"
              style={{ background: filterMuscle === m ? "#F0FDF4" : "transparent",
                       color: filterMuscle === m ? "#2E7D52" : "#888888",
                       borderColor: filterMuscle === m ? "#2E7D52" : "#E0E0E0" }}
            >
              {m}
            </button>
          ))}
        </div>
      )}

      <p className="text-xs text-text-muted px-0.5">
        {filtered.length} exercise{filtered.length !== 1 ? "s" : ""}
      </p>

      {/* List */}
      {loading ? (
        <div className="space-y-2">{[1, 2, 3].map((i) => <Skeleton key={i} className="h-16 w-full rounded-xl" />)}</div>
      ) : filtered.length === 0 ? (
        <div className="bg-bg-card rounded-xl border border-divider flex flex-col items-center py-12 gap-3">
          <span className="text-4xl">🏋️</span>
          <p className="text-sm text-text-muted">{query ? "No exercises match" : "No exercises yet"}</p>
          {!query && <p className="text-xs text-text-placeholder">Import from Profile or tap &quot;Add exercise&quot;</p>}
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map((ex) => {
            const isExpanded = expandedId === ex.id;
            const catColors = CATEGORY_COLORS[ex.category as Category] ?? { bg: "#F5F5F5", text: "#555" };
            return (
              <div key={ex.id} className="bg-bg-card rounded-xl border border-divider overflow-hidden">
                <div className="flex items-center gap-3 px-4 py-3 cursor-pointer"
                  onClick={() => setExpandedId(isExpanded ? null : (ex.id ?? null))}>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-[14px] font-semibold text-text-primary">{ex.name}</p>
                      <span className="text-[9px] font-bold px-1.5 py-0.5 rounded-full"
                        style={{ background: catColors.bg, color: catColors.text }}>
                        {ex.category}
                      </span>
                      {ex.isSystem && (
                        <span className="text-[9px] font-bold px-1.5 py-0.5 rounded-full bg-green-light text-green">SYSTEM</span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 mt-0.5">
                      {ex.muscleGroup && <span className="text-[11px] text-text-muted">{ex.muscleGroup}</span>}
                      {ex.equipment && <span className="text-[11px] text-text-placeholder">· {ex.equipment}</span>}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {isExpanded ? <ChevronUp size={14} className="text-text-muted" /> : <ChevronDown size={14} className="text-text-muted" />}
                    {!ex.isSystem && (
                      <button
                        disabled={deletingId === ex.id}
                        onClick={(e) => { e.stopPropagation(); if (ex.id) deleteExercise(ex.id); }}
                        className="text-text-muted hover:text-red-500 transition-colors p-1 -mr-1"
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                </div>

                {isExpanded && (
                  <div className="border-t border-divider px-4 py-3 space-y-2">
                    {ex.description && (
                      <p className="text-[13px] text-text-secondary leading-relaxed">{ex.description}</p>
                    )}
                    {ex.videoLink && (
                      <a href={ex.videoLink} target="_blank" rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 text-[12px] font-medium text-green hover:underline">
                        <ExternalLink size={12} /> Watch video
                      </a>
                    )}
                    {!ex.description && !ex.videoLink && (
                      <p className="text-[12px] text-text-muted">No additional details</p>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
