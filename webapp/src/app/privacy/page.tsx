import type { Metadata } from "next";
import { PRIVACY_POLICY_VERSION, PRIVACY_CONTACT_EMAIL } from "@/lib/legal";

// Public, unauthenticated route (NOT wrapped in AuthGuard) so it can be linked from the app stores
// and from the onboarding consent step. This is a first-pass v1 — have it reviewed before launch.
export const metadata: Metadata = {
  title: "Privacy Policy — MealPlan+",
  description: "How MealPlan+ collects, uses, and protects your data.",
};

const C = { ink: "#14181b", muted: "#5b666e", teal: "oklch(0.62 0.09 210)", border: "#eaeef0", bg: "#f7f9fa" };

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-7">
      <h2 className="text-[16px] font-bold mb-2" style={{ color: C.ink }}>{title}</h2>
      <div className="text-[13.5px] leading-relaxed space-y-2" style={{ color: C.muted }}>{children}</div>
    </section>
  );
}

export default function PrivacyPolicyPage() {
  return (
    <main className="min-h-screen px-5 py-10" style={{ background: C.bg }}>
      <div className="mx-auto max-w-[720px]">
        <div className="text-[22px] font-bold" style={{ color: C.teal }}>MealPlan+</div>
        <h1 className="text-[26px] font-bold mt-1" style={{ color: C.ink }}>Privacy Policy</h1>
        <p className="text-[12.5px] mt-1" style={{ color: C.muted }}>Last updated: {PRIVACY_POLICY_VERSION}</p>

        <p className="text-[13.5px] leading-relaxed mt-5" style={{ color: C.muted }}>
          MealPlan+ (&quot;we&quot;, &quot;us&quot;) is a meal-planning and health-tracking app. This policy explains what
          personal data we collect, why we collect it, how we protect it, and the choices and rights you have. By
          creating an account and using the app you agree to this policy.
        </p>

        <Section title="Data we collect">
          <p>We collect only what the app needs to work:</p>
          <ul className="list-disc pl-5 space-y-1">
            <li><b>Account data:</b> your email address, and an authentication identifier from Firebase Authentication.</li>
            <li><b>Profile data:</b> display name, age, sex, height, weight, and your goals and nutrition targets (calories, protein, carbs, fat). We use these to personalise calorie/macro targets (e.g. BMR/TDEE estimates).</li>
            <li><b>Health data you choose to log:</b> blood glucose, blood pressure, and weight history. This is sensitive health-related data and is only stored because you enter it.</li>
            <li><b>App content:</b> meals, diets, foods, workouts, plans, grocery lists and logs you create.</li>
            <li><b>Diagnostics &amp; analytics:</b> crash reports and basic usage analytics via Firebase (Crashlytics and Analytics) to keep the app stable. We do not sell your data or use it for advertising.</li>
          </ul>
          <p>We do <b>not</b> collect payment information, precise location, or contacts.</p>
        </Section>

        <Section title="How we use your data">
          <ul className="list-disc pl-5 space-y-1">
            <li>To provide the core features: logging meals, planning diets, tracking workouts and health metrics.</li>
            <li>To calculate personalised targets and summaries.</li>
            <li>To send on-device reminders you enable (these are scheduled locally on your device).</li>
            <li>To diagnose crashes and improve reliability.</li>
          </ul>
          <p>
            Where the law requires a legal basis, we rely on your <b>consent</b> for health-related data and on the
            performance of our service for the rest. You can withdraw consent at any time by deleting your account.
          </p>
        </Section>

        <Section title="Who we share it with (processors)">
          <p>We do not sell your data. We use a small number of service providers to run the app:</p>
          <ul className="list-disc pl-5 space-y-1">
            <li><b>Google Firebase</b> — authentication, crash reporting, analytics.</li>
            <li><b>Google Cloud Run</b> — hosts our backend API.</li>
            <li><b>Neon</b> — hosts our PostgreSQL database where your account, profile, health and app content are stored.</li>
            <li><b>Vercel</b> — hosts the web app.</li>
            <li><b>Open Food Facts</b> — when you search foods online, your search query is sent to their public database to return nutrition results. No account data is shared.</li>
          </ul>
        </Section>

        <Section title="Storage, security & retention">
          <p>
            Your data is encrypted in transit (HTTPS) and encrypted at rest by our database provider. We keep your data
            for as long as your account exists. When you delete your account, we delete your profile, health data and app
            content from our database.
          </p>
        </Section>

        <Section title="Your rights">
          <ul className="list-disc pl-5 space-y-1">
            <li><b>Access &amp; correction:</b> view and edit your profile and data in the app.</li>
            <li><b>Export / portability:</b> export your data as a CSV file from Settings.</li>
            <li><b>Deletion (right to erasure):</b> permanently delete your account and associated data from within the app.</li>
            <li>Depending on where you live (e.g. EEA/UK under GDPR, California under CCPA), you may have additional rights; contact us to exercise them.</li>
          </ul>
        </Section>

        <Section title="Children">
          <p>
            MealPlan+ is not intended for children under 16, and we do not knowingly collect data from them. If you
            believe a child has provided us data, contact us and we will delete it.
          </p>
        </Section>

        <Section title="International transfers">
          <p>
            Our providers may process data on servers outside your country. Where required, transfers are covered by
            appropriate safeguards such as Standard Contractual Clauses.
          </p>
        </Section>

        <Section title="Changes to this policy">
          <p>
            We may update this policy. When we make material changes we will update the &quot;Last updated&quot; date and,
            where appropriate, ask you to review it again.
          </p>
        </Section>

        <Section title="Contact">
          <p>
            Questions or privacy requests: <a href={`mailto:${PRIVACY_CONTACT_EMAIL}`} style={{ color: C.teal }}>{PRIVACY_CONTACT_EMAIL}</a>.
          </p>
        </Section>

        <p className="text-[12px] mt-10 pt-5" style={{ color: C.muted, borderTop: `1px solid ${C.border}` }}>
          © {new Date().getFullYear()} MealPlan+
        </p>
      </div>
    </main>
  );
}
