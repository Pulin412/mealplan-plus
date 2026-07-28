"use client";

import { useState } from "react";
import { updateMe } from "@/lib/api/user";

const C = { ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e", border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5", teal: "oklch(0.62 0.09 210)" };
const TOTAL = 4;

type Goal = "LOSE" | "MAINTAIN" | "GAIN";
type Sex = "MALE" | "FEMALE" | "OTHER";

/**
 * Blocking first-run onboarding (shown by AuthGuard before the app + bottom nav):
 *   0 Welcome → 1 Personal details (REQUIRED) → 2 Targets (skippable) → 3 Tips.
 * Personal details can't be skipped or left empty. The global "Skip" only appears once past the
 * required step, and skips the optional remainder. `onDone` unlocks the app.
 */
export function OnboardingFlow({ onDone }: { onDone: () => void }) {
  const [step, setStep] = useState(0);
  const [saving, setSaving] = useState(false);

  // required personal details
  const [name, setName] = useState("");
  const [age, setAge] = useState("");
  const [sex, setSex] = useState<Sex | "">("");
  const [height, setHeight] = useState("");
  const [weight, setWeight] = useState("");

  // optional targets
  const [goal, setGoal] = useState<Goal | "">("");
  const [kcal, setKcal] = useState("");
  const [protein, setProtein] = useState("");
  const [carbs, setCarbs] = useState("");
  const [fat, setFat] = useState("");

  const next = () => setStep((s) => s + 1);

  const detailsValid =
    name.trim().length > 0 && Number(age) > 0 && sex !== "" && Number(height) > 0 && Number(weight) > 0;

  const saveDetails = async () => {
    if (!detailsValid) return;
    setSaving(true);
    try {
      await updateMe({
        displayName: name.trim(),
        age: Math.round(Number(age)),
        gender: sex || null,
        heightCm: Math.round(Number(height)),
        weightKg: Math.round(Number(weight)),
      });
    } catch { /* keep going; they can edit in Profile */ }
    setSaving(false);
    next();
  };

  const saveTargets = async () => {
    setSaving(true);
    try {
      await updateMe({
        goalType: goal || null,
        targetCalories: kcal ? Math.round(Number(kcal)) : null,
        targetProtein: protein ? Math.round(Number(protein)) : null,
        targetCarbs: carbs ? Math.round(Number(carbs)) : null,
        targetFat: fat ? Math.round(Number(fat)) : null,
      });
    } catch { /* non-fatal */ }
    setSaving(false);
    next();
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col" style={{ background: C.bg }}>
      <div className="flex items-center px-5 pt-4">
        <div className="flex gap-[6px]">
          {Array.from({ length: TOTAL }).map((_, i) => (
            <span key={i} className="h-[6px] rounded-full transition-all"
              style={{ width: i === step ? 20 : 6, background: i === step ? C.teal : C.border }} />
          ))}
        </div>
        <span className="flex-1" />
        {/* Global skip only past the required details step — skips the optional remainder. */}
        {step >= 2 && (
          <button onClick={onDone} className="text-[13px] font-semibold" style={{ color: C.muted }}>Skip</button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto flex flex-col justify-center px-7 py-4">
        {step === 0 && <Welcome />}
        {step === 1 && (
          <Details
            name={name} setName={setName} age={age} setAge={setAge} sex={sex} setSex={setSex}
            height={height} setHeight={setHeight} weight={weight} setWeight={setWeight}
          />
        )}
        {step === 2 && (
          <Targets
            goal={goal} setGoal={setGoal} kcal={kcal} setKcal={setKcal}
            protein={protein} setProtein={setProtein} carbs={carbs} setCarbs={setCarbs} fat={fat} setFat={setFat}
          />
        )}
        {step === 3 && <Tips />}
      </div>

      <div className="px-7 pb-10 pt-2">
        {step === 0 && <PrimaryBtn label="Get started" onClick={next} />}
        {step === 1 && (
          <PrimaryBtn label={saving ? "Saving…" : "Continue"} onClick={saveDetails} disabled={!detailsValid || saving} />
        )}
        {step === 2 && (
          <>
            <PrimaryBtn label={saving ? "Saving…" : "Save & continue"} onClick={saveTargets} disabled={saving} />
            <SkipLink label="Skip for now" onClick={next} />
          </>
        )}
        {step === 3 && <PrimaryBtn label="Start using MealPlan+" onClick={onDone} />}
      </div>
    </div>
  );
}

function Welcome() {
  const rows = [
    ["🍽️", "Log meals by slot"],
    ["📋", "Plan diets & groceries"],
    ["💪", "Workouts & health"],
  ];
  return (
    <div>
      <div className="text-center mb-7">
        <div className="text-[28px] font-bold" style={{ color: C.teal }}>MealPlan+</div>
        <div className="text-[13px] mt-1" style={{ color: C.muted3 }}>Let&apos;s get you set up — takes a minute.</div>
      </div>
      <div className="flex flex-col gap-4">
        {rows.map(([icon, title]) => (
          <div key={title} className="flex items-center gap-3">
            <span className="text-[24px]">{icon}</span>
            <div className="text-[14px] font-semibold" style={{ color: C.ink }}>{title}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function Details(p: {
  name: string; setName: (v: string) => void; age: string; setAge: (v: string) => void;
  sex: Sex | ""; setSex: (v: Sex) => void;
  height: string; setHeight: (v: string) => void; weight: string; setWeight: (v: string) => void;
}) {
  const sexes: [Sex, string][] = [["MALE", "Male"], ["FEMALE", "Female"], ["OTHER", "Other"]];
  return (
    <div>
      <div className="text-[20px] font-bold mb-1" style={{ color: C.ink }}>About you</div>
      <div className="text-[12.5px] mb-5" style={{ color: C.muted3 }}>We use this to personalize your targets & health tracking. All fields required.</div>

      <Field label="Name" value={p.name} onChange={p.setName} numeric={false} />

      <div className="text-[11px] font-semibold mb-[6px] mt-1" style={{ color: C.muted }}>Sex</div>
      <div className="flex gap-2 mb-4">
        {sexes.map(([val, label]) => {
          const on = p.sex === val;
          return (
            <button key={val} onClick={() => p.setSex(val)}
              className="flex-1 rounded-[10px] py-[10px] text-[13px] font-semibold"
              style={{ background: on ? C.teal : C.surface, color: on ? "#fff" : C.ink, border: `1px solid ${on ? C.teal : C.border}` }}>
              {label}
            </button>
          );
        })}
      </div>

      <div className="flex gap-[10px]">
        <Field label="Age" value={p.age} onChange={p.setAge} className="flex-1" />
        <Field label="Height (cm)" value={p.height} onChange={p.setHeight} className="flex-1" />
        <Field label="Weight (kg)" value={p.weight} onChange={p.setWeight} className="flex-1" />
      </div>
    </div>
  );
}

function Targets(p: {
  goal: Goal | ""; setGoal: (g: Goal) => void;
  kcal: string; setKcal: (v: string) => void; protein: string; setProtein: (v: string) => void;
  carbs: string; setCarbs: (v: string) => void; fat: string; setFat: (v: string) => void;
}) {
  const goals: Goal[] = ["LOSE", "MAINTAIN", "GAIN"];
  const goalLabel: Record<Goal, string> = { LOSE: "Lose", MAINTAIN: "Maintain", GAIN: "Gain" };
  return (
    <div>
      <div className="text-[20px] font-bold mb-1" style={{ color: C.ink }}>Your targets</div>
      <div className="text-[12.5px] mb-5" style={{ color: C.muted3 }}>Sets your daily goal on the Today ring. Optional — you can change these anytime in Profile.</div>

      <div className="text-[11px] font-semibold mb-[6px]" style={{ color: C.muted }}>Goal</div>
      <div className="flex gap-2 mb-4">
        {goals.map((g) => {
          const on = p.goal === g;
          return (
            <button key={g} onClick={() => p.setGoal(g)}
              className="flex-1 rounded-[10px] py-[10px] text-[13px] font-semibold"
              style={{ background: on ? C.teal : C.surface, color: on ? "#fff" : C.ink, border: `1px solid ${on ? C.teal : C.border}` }}>
              {goalLabel[g]}
            </button>
          );
        })}
      </div>

      <Field label="Calories (kcal)" value={p.kcal} onChange={p.setKcal} />
      <div className="flex gap-[10px]">
        <Field label="Protein (g)" value={p.protein} onChange={p.setProtein} className="flex-1" />
        <Field label="Carbs (g)" value={p.carbs} onChange={p.setCarbs} className="flex-1" />
        <Field label="Fat (g)" value={p.fat} onChange={p.setFat} className="flex-1" />
      </div>
    </div>
  );
}

function Tips() {
  const tips = [
    "Tap a slot on Today to log a meal.",
    "Foods, Meals & Diets live under the More tab.",
    "You already have 100+ foods ready to search.",
  ];
  return (
    <div>
      <div className="text-[36px] text-center mb-3">🎉</div>
      <div className="text-[20px] font-bold text-center mb-1" style={{ color: C.ink }}>You&apos;re all set</div>
      <div className="text-[12.5px] text-center mb-6" style={{ color: C.muted3 }}>A couple of tips to get going:</div>
      <div className="flex flex-col gap-3">
        {tips.map((t) => (
          <div key={t} className="flex items-start gap-2">
            <span style={{ color: C.teal }}>✓</span>
            <span className="text-[13px]" style={{ color: C.muted3 }}>{t}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function Field({ label, value, onChange, className = "", numeric = true }: {
  label: string; value: string; onChange: (v: string) => void; className?: string; numeric?: boolean;
}) {
  return (
    <label className={`flex flex-col gap-1 mb-3 ${className}`}>
      <span className="text-[11px] font-semibold" style={{ color: C.muted }}>{label}</span>
      <input inputMode={numeric ? "numeric" : "text"} value={value}
        onChange={(e) => onChange(numeric ? e.target.value.replace(/[^0-9]/g, "") : e.target.value)}
        className="rounded-[8px] px-3 py-2 text-[13px] outline-none"
        style={{ border: `1px solid ${C.border}`, color: C.ink }} />
    </label>
  );
}

function PrimaryBtn({ label, onClick, disabled }: { label: string; onClick: () => void; disabled?: boolean }) {
  return (
    <button onClick={onClick} disabled={disabled}
      className="w-full rounded-[12px] py-[14px] text-[14px] font-semibold text-white disabled:opacity-50"
      style={{ background: C.teal }}>
      {label}
    </button>
  );
}

function SkipLink({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button onClick={onClick} className="w-full text-center mt-3 text-[12.5px] font-semibold" style={{ color: C.muted }}>
      {label}
    </button>
  );
}
