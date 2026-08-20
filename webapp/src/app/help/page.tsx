import type { Metadata } from "next";
import { NutritionNav } from "@/components/layout/NutritionNav";

// Public, unauthenticated route (NOT wrapped in AuthGuard) so it can be linked from login,
// onboarding, the app stores and the in-app Help section. Pure static content — no data fetching,
// so it renders instantly and works offline once the PWA has cached it.
export const metadata: Metadata = {
  title: "Help — EatMyPlan",
  description: "Step-by-step guides for everything in EatMyPlan: logging meals, planning, diets, workouts, health tracking, groceries, social and connecting your own AI.",
};

const C = { ink: "#14181b", muted: "#5b666e", teal: "oklch(0.62 0.09 210)", tealSoft: "oklch(0.62 0.09 210 / 0.08)", border: "#eaeef0", bg: "#f7f9fa", surface: "#ffffff" };

type Topic = { id: string; icon: string; title: string; blurb: string };

const TOPICS: Topic[] = [
  { id: "map", icon: "🧭", title: "How it fits together", blurb: "How foods, meals & diets connect" },
  { id: "getting-started", icon: "🚀", title: "Getting started", blurb: "Sign in, onboarding and your targets" },
  { id: "today", icon: "🏠", title: "Today", blurb: "Log meals by slot & track your day" },
  { id: "plan", icon: "📅", title: "Plan", blurb: "Assign diets & plan meals ahead" },
  { id: "diets", icon: "🥗", title: "Diets", blurb: "Reusable day-plan templates" },
  { id: "meals", icon: "🍲", title: "Meals", blurb: "Build reusable meals" },
  { id: "foods", icon: "🍎", title: "Foods", blurb: "Your food library & search" },
  { id: "exercises", icon: "🏋️", title: "Exercises & workouts", blurb: "Build and run workouts" },
  { id: "session", icon: "⏱️", title: "Workout sessions", blurb: "Run a session & log sets" },
  { id: "health", icon: "❤️", title: "Health", blurb: "Weight, glucose & blood pressure" },
  { id: "groceries", icon: "🛒", title: "Groceries", blurb: "Shopping lists from your plan" },
  { id: "social", icon: "👥", title: "Social", blurb: "Follow, share & discover" },
  { id: "profile", icon: "🎯", title: "Profile & targets", blurb: "Goals and macro targets" },
  { id: "settings", icon: "⚙️", title: "Settings & data", blurb: "Reminders, export & account" },
  { id: "install", icon: "📲", title: "Install the app", blurb: "Add to home screen & offline use" },
  { id: "byo-ai", icon: "🤖", title: "Bring your own AI", blurb: "Connect Claude to your data" },
];

function Section({ id, icon, title, children }: { id: string; icon: string; title: string; children: React.ReactNode }) {
  return (
    <section id={id} className="mt-9 scroll-mt-6">
      <h2 className="text-[17px] font-bold mb-2 flex items-center gap-2" style={{ color: C.ink }}>
        <span aria-hidden>{icon}</span>{title}
      </h2>
      <div className="text-[13.5px] leading-relaxed space-y-2" style={{ color: C.muted }}>{children}</div>
    </section>
  );
}

// Numbered how-to steps
function Steps({ items }: { items: React.ReactNode[] }) {
  return (
    <ol className="list-decimal pl-5 space-y-1.5 mt-1">
      {items.map((it, i) => (<li key={i}>{it}</li>))}
    </ol>
  );
}

function Tip({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-[12.5px] rounded-lg px-3 py-2 mt-2" style={{ background: C.tealSoft, color: C.ink }}>
      <b style={{ color: C.teal }}>Tip · </b>{children}
    </p>
  );
}

export default function HelpPage() {
  return (
    <main className="min-h-screen px-5 pt-10 pb-28" style={{ background: C.bg }}>
      <div className="mx-auto max-w-[760px]">
        <div className="text-[22px] font-bold" style={{ color: C.teal }}>EatMyPlan</div>
        <h1 className="text-[26px] font-bold mt-1" style={{ color: C.ink }}>Help</h1>

        {/* Table of contents */}
        <nav aria-label="Topics" className="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {TOPICS.map((t) => (
            <a key={t.id} href={`#${t.id}`}
              className="flex items-start gap-2 rounded-xl px-3 py-2.5 no-underline"
              style={{ background: C.surface, border: `1px solid ${C.border}` }}>
              <span aria-hidden className="text-[16px] leading-none mt-0.5">{t.icon}</span>
              <span className="min-w-0">
                <span className="block text-[13px] font-semibold" style={{ color: C.ink }}>{t.title}</span>
                <span className="block text-[11px] leading-snug" style={{ color: C.muted }}>{t.blurb}</span>
              </span>
            </a>
          ))}
        </nav>

        <Section id="map" icon="🧭" title="How it all fits together">
          <p>The nutrition side builds up from small pieces to your day: <b>foods</b> make up <b>meals</b>, meals make up a <b>diet</b> (one day&apos;s template), diets and meals get scheduled in <b>Plan</b>, and you log against them on <b>Today</b>. <b>Groceries</b> fall out of whatever you planned. Fitness works the same way: <b>exercises → workouts → a session you run</b>.</p>
        </Section>

        <Section id="getting-started" icon="🚀" title="Getting started">
          <p>EatMyPlan is offline-first meal planning and food logging. Everything you create is saved to your account and synced across the web app and Android app.</p>
          <Steps items={[
            <>Create an account or sign in from the <b>login</b> screen (email &amp; password, or Google).</>,
            <>Follow the short <b>onboarding</b> to set your goal, body stats and daily targets. You can change these any time.</>,
            <>Land on <b>Today</b> — your home base for logging what you eat and how you&apos;re tracking.</>,
          ]} />
          <Tip>Want a guided walkthrough inside the app? Go to <b>Settings → Help → Replay app tour</b>.</Tip>
        </Section>

        <Section id="today" icon="🏠" title="Today — log meals & track your day">
          <p>Today shows your calories and macros for the current date, plus every meal slot you can log against.</p>
          <p>There are <b>9 meal slots</b>: Early Morning, Breakfast, Noon, Lunch, Evening, Pre-Workout, Post-Workout, Dinner and Post-Dinner.</p>
          <Steps items={[
            <>Tap a meal slot to log against it. You can log a whole <b>meal</b>, an individual <b>food</b>, or a diet&apos;s planned meal for that slot.</>,
            <>Your calorie ring and protein / carbs / fat totals update instantly as you log.</>,
            <>Expand any logged item to see its ingredients; remove something with the <b>×</b> if you logged it by mistake.</>,
            <>Assigned diet or planned meals appear in their slots — mark them complete as you eat, or <b>Remove from plan</b> for just that day.</>,
          ]} />
          <Tip>&quot;Today&quot; always means your device&apos;s current date — open the app on a new day and it rolls over automatically.</Tip>
        </Section>

        <Section id="plan" icon="📅" title="Plan — schedule meals ahead">
          <p>Plan is your calendar. Assign a diet to a day for a full template, or drop individual meals into specific slots.</p>
          <Steps items={[
            <>Pick a date on the <b>Plan</b> screen.</>,
            <>Assign a <b>diet</b> to fill that day with its meals, or use <b>add meal</b> to plan a single meal into a chosen slot.</>,
            <>The add-meal picker starts on Breakfast; switch the slot selector to filter to meals tagged for that slot, and expand a row to preview its foods.</>,
            <>To take one meal off a day without touching the rest, use <b>Remove from plan</b> — a loose meal is deleted, a diet meal is detached from just that day.</>,
          ]} />
          <Tip>Planning ahead feeds your <b>Groceries</b> list — see that section below.</Tip>
        </Section>

        <Section id="diets" icon="🥗" title="Diets — reusable day-plan templates">
          <p>A diet is a reusable one-day template of meals arranged by slot. Assign it to any day from Today or Plan.</p>
          <Steps items={[
            <>Open <b>Diets</b> (in the <b>More</b> tab) and create a new diet, or browse existing ones.</>,
            <>Add meals to the diet&apos;s slots. Give the diet <b>tags</b> so you can find it later.</>,
            <>Use the filter bar to narrow by <b>tags</b> (match any) and by <b>slot</b> — searching within a selected slot only matches meals in that slot.</>,
          ]} />
        </Section>

        <Section id="meals" icon="🍲" title="Meals — build reusable meals">
          <p>A meal is a named group of foods (with quantities) you eat together — log it in one tap instead of adding foods one by one.</p>
          <Steps items={[
            <>Open <b>Meals</b> (in <b>More</b>) and create a meal.</>,
            <>Add foods from your library and set quantities; the meal&apos;s calories and macros are the sum of its foods.</>,
            <>Tag the meal for the slots it suits (e.g. Breakfast) so it surfaces in the right pickers.</>,
          ]} />
        </Section>

        <Section id="foods" icon="🍎" title="Foods — your food library">
          <p>Foods are the building blocks of meals and logs, each with calories and macros per serving.</p>
          <Steps items={[
            <>Open <b>Foods</b> (in <b>More</b>) to see your library.</>,
            <>Create a food with its nutrition, or <b>search online</b> to pull nutrition from the public Open Food Facts database.</>,
            <>Reuse foods across meals, diets and quick logs.</>,
          ]} />
        </Section>

        <Section id="exercises" icon="🏋️" title="Exercises & workouts">
          <p>Build exercises and group them into workouts you can run and track.</p>
          <p>Each exercise has a <b>type</b> that changes what you log:</p>
          <ul className="list-disc pl-5 space-y-1">
            <li><b>Strength</b> — reps &amp; weight per set.</li>
            <li><b>Cardio</b> — reps &amp; weight, plus optional distance.</li>
            <li><b>Timed</b> — minutes &amp; seconds per set (plus optional distance).</li>
          </ul>
          <Steps items={[
            <>Open <b>Exercises</b>, create exercises and give them tags.</>,
            <>Build a <b>workout</b> from those exercises.</>,
            <>Plan a workout to a day, or start it straight from the Exercises screen.</>,
          ]} />
        </Section>

        <Section id="session" icon="⏱️" title="Workout sessions — run & log">
          <p>Running a workout opens the session runner, which walks you through each exercise and records your sets.</p>
          <Steps items={[
            <>Start a workout to open the <b>runner</b>. The Ready screen shows last time&apos;s sets, reps and any note.</>,
            <>Log each set — reps + weight, or duration for timed exercises.</>,
            <>Add a <b>note</b> per exercise or for the whole session (e.g. how it felt, what to change).</>,
            <>Finish to save the session; it appears in your workout history.</>,
          ]} />
        </Section>

        <Section id="health" icon="❤️" title="Health — metrics & trends">
          <p>Log the health readings you care about and see them trend over time.</p>
          <Steps items={[
            <>Open <b>Health</b> and add a reading: <b>weight</b>, <b>blood glucose</b> or <b>blood pressure</b>.</>,
            <>Switch the range between <b>7 days</b> and <b>30 days</b> to see recent trends.</>,
            <>Your latest weight feeds back into your targets on Profile.</>,
          ]} />
          <Tip>Health data is sensitive and only ever stored because you enter it — see the <a href="/privacy" style={{ color: C.teal }}>Privacy Policy</a>.</Tip>
        </Section>

        <Section id="groceries" icon="🛒" title="Groceries — shopping lists from your plan">
          <p>Turn your planned meals and diets into a shopping list, grouped and ready for the store.</p>
          <Steps items={[
            <>Open <b>Groceries</b> (in <b>More</b>) and pick the date range to cover.</>,
            <><b>Refresh</b> to rebuild the list from everything planned in that range (diets + individual planned meals).</>,
            <>Check items off as you shop. Use <b>Clear list</b> (left of refresh) to empty the whole list and dates.</>,
          ]} />
          <Tip>The list is rebuilt on manual refresh, so add your meals to the plan first, then refresh.</Tip>
        </Section>

        <Section id="social" icon="👥" title="Social — follow, share & discover">
          <p>Optionally connect with other people to share and discover content.</p>
          <Steps items={[
            <>Set up your public handle and profile in <b>Social</b>.</>,
            <>Use <b>Discover</b> to find people, <b>follow</b> them, and share your own content.</>,
            <>You can <b>block</b> or <b>report</b> anyone; blocked users are managed under <b>Settings → Blocked</b>.</>,
          ]} />
        </Section>

        <Section id="profile" icon="🎯" title="Profile & targets">
          <p>Your profile drives your personalised calorie and macro targets.</p>
          <Steps items={[
            <>Open <b>Profile</b> and set your body stats (age, sex, height, weight) and your goal.</>,
            <>We estimate calorie needs (BMR / TDEE) from these; you can override calories, protein, carbs and fat manually.</>,
            <>Targets show up on Today so you can log against them.</>,
          ]} />
        </Section>

        <Section id="settings" icon="⚙️" title="Settings & your data">
          <p>Manage reminders, export your data and control your account under <b>Settings</b>.</p>
          <ul className="list-disc pl-5 space-y-1">
            <li><b>Daily log reminder</b> — toggle a nudge if you haven&apos;t logged.</li>
            <li><b>Export</b> — download your data as a <b>CSV</b> file.</li>
            <li><b>Replay app tour / onboarding</b> — under the Help section.</li>
            <li><b>Send feedback</b> — report a bug or suggest an improvement (your app version is attached automatically).</li>
            <li><b>Delete account</b> — permanently removes your profile, health data and content.</li>
          </ul>
        </Section>

        <Section id="install" icon="📲" title="Install the app (offline use)">
          <p>EatMyPlan is a Progressive Web App — install it to your home screen for a full-screen, app-like experience that works offline for what you&apos;ve already loaded.</p>
          <ul className="list-disc pl-5 space-y-1">
            <li><b>iPhone / iPad (Safari):</b> tap <b>Share</b> → <b>Add to Home Screen</b>.</li>
            <li><b>Android (Chrome):</b> tap the <b>⋮</b> menu → <b>Install app</b> / <b>Add to Home screen</b>. There&apos;s also a native Android app available.</li>
            <li><b>Desktop (Chrome / Edge):</b> use the <b>install</b> icon in the address bar.</li>
          </ul>
          <Tip>Reads are cached, so recently-viewed screens still open without a connection; new changes sync when you&apos;re back online.</Tip>
        </Section>

        <Section id="byo-ai" icon="🤖" title="Bring your own AI">
          <p>Prefer to work with your own AI assistant? EatMyPlan exposes a secure <b>MCP connector</b> so you can connect Claude (or any MCP-compatible client) directly to <i>your</i> data — read your dashboard, search foods, log meals and more.</p>
          <Steps items={[
            <>In Claude, add a custom connector pointing at <b>https://api.eatmyplan.com/mcp</b>.</>,
            <>Sign in when prompted — access uses secure OAuth, and the AI only ever sees your own account&apos;s data.</>,
            <>Ask it to show today&apos;s dashboard, search foods, or log a meal. Write actions require you to grant permission.</>,
          ]} />
          <Tip>This runs on your own AI subscription and is entirely optional — the app works fully without it.</Tip>
        </Section>

        <p className="text-[12.5px] mt-10 pt-5 leading-relaxed" style={{ color: C.muted, borderTop: `1px solid ${C.border}` }}>
          Still stuck? Send feedback from <b>Settings → Send feedback</b>. See also our{" "}
          <a href="/privacy" style={{ color: C.teal }}>Privacy Policy</a>.
        </p>
        <p className="text-[12px] mt-4" style={{ color: C.muted }}>© {new Date().getFullYear()} EatMyPlan</p>
      </div>
      <NutritionNav />
    </main>
  );
}
