# MealPlan+ Web / PWA — Design System

> Mirrors the Android app 1:1 so iPhone Safari users get the same visual experience as Android users.
> All values are derived directly from `Color.kt`, `DesignTokens.kt`, `Type.kt`, `Shape.kt`, and `NavHost.kt`.

---

## 1. Color Tokens

### Light theme (only — no dark mode on web for now)

| Token | Hex | Usage |
|-------|-----|-------|
| `--bg-page` | `#F7F7F7` | Page / scaffold background |
| `--bg-card` | `#FFFFFF` | Card, bottom bar, sheet backgrounds |
| `--divider` | `#EEEEEE` | Horizontal rules, separators |
| `--outline` | `#DEDEDE` | Input borders, card outlines |
| `--outline-variant` | `#CCCCCC` | Subtle outlines |

### Text hierarchy

| Token | Hex | Usage |
|-------|-----|-------|
| `--text-primary` | `#111111` | Headlines, body copy, nav selected |
| `--text-secondary` | `#444444` | Subheadings, supporting text |
| `--text-muted` | `#666666` | Nav unselected, placeholders, metadata |
| `--text-placeholder` | `#999999` | Input placeholder text |
| `--text-destructive` | `#E53E3E` | Delete, error text |

### Brand green

| Token | Hex | Usage |
|-------|-----|-------|
| `--green` | `#2E7D52` | Primary action, active states, links |
| `--green-light` | `#E8F5EE` | Green pill backgrounds, selected chips |
| `--green-container` | `#E8F5EE` | Primary container (buttons, badges) |

### Macro nutrition colors (fixed — never theme-dependent)

| Token | Hex | Usage |
|-------|-----|-------|
| `--macro-protein` | `#2E7D52` | Protein label / bar |
| `--macro-carbs` | `#C05200` | Carbs label / bar |
| `--macro-fat` | `#1E4FBF` | Fat label / bar |
| `--macro-cal` | `#F59E0B` | Calories label / ring |

### Meal slot dot colors

| Token | Hex | Usage |
|-------|-----|-------|
| `--slot-breakfast` | `#F59E0B` | Breakfast indicator dot / badge |
| `--slot-lunch` | `#2E7D52` | Lunch indicator |
| `--slot-dinner` | `#7C3AED` | Dinner indicator |
| `--slot-snack` | `#2196F3` | Snack indicator |

### Semantic category tag colors

| Color | Fg | Bg |
|-------|----|----|
| Green | `#2E7D52` | `#E8F5EE` |
| Blue | `#1E4FBF` | `#E8EEFF` |
| Orange | `#C05200` | `#FFF0E6` |
| Purple | `#7C3AED` | `#F3EEFF` |
| Gray | `#888888` | `#F0F0F0` |

### Section / icon tint colors (used in More sheet + Quick-add)

| Section | Icon color |
|---------|-----------|
| Diets | `#2E7D52` |
| Meals | `#C05200` |
| Foods | `#1E4FBF` |
| Health | `#D32F2F` |
| Workouts | `#00796B` |
| Grocery | `#6A1B9A` |
| Settings | `#555555` |
| Log meals | `#2E7D52` |
| Add food | `#F59E0B` |

---

## 2. Typography

Base font: system-ui (matches Android's `FontFamily.Default`). Scale factor: `1.1×` (matches Android's `fontScale * 1.1f`).

| Role | Size | Weight | Line height | Letter spacing | Tailwind class |
|------|------|--------|-------------|----------------|----------------|
| `headlineLarge` | 32px / 2rem | 600 | 40px | 0 | `text-4xl font-semibold` |
| `headlineMedium` | 28px / 1.75rem | 600 | 36px | 0 | `text-3xl font-semibold` |
| `headlineSmall` | 24px / 1.5rem | 600 | 32px | 0 | `text-2xl font-semibold` |
| `titleLarge` | 22px / 1.375rem | 600 | 28px | 0 | `text-xl font-semibold` |
| `titleMedium` | 16px / 1rem | 500 | 24px | 0.15px | `text-base font-medium` |
| `titleSmall` | 14px / 0.875rem | 500 | 20px | 0.1px | `text-sm font-medium` |
| `bodyLarge` | 16px / 1rem | 400 | 24px | 0.5px | `text-base` |
| `bodyMedium` | 14px / 0.875rem | 400 | 20px | 0.25px | `text-sm` |
| `bodySmall` | 12px / 0.75rem | 400 | 16px | 0.4px | `text-xs` |
| `labelLarge` | 14px / 0.875rem | 500 | 20px | 0.1px | `text-sm font-medium` |
| `labelMedium` | 12px / 0.75rem | 500 | 16px | 0.5px | `text-xs font-medium` |
| `labelSmall` | 11px / 0.6875rem | 500 | 16px | 0.5px | `text-[11px] font-medium` |

---

## 3. Shape / Border Radius

| Token | Value | Android equiv | Tailwind |
|-------|-------|---------------|---------|
| `--radius-xs` | 4px | `extraSmall` | `rounded` |
| `--radius-sm` | 8px | `small` | `rounded-lg` |
| `--radius-md` | 12px | `medium` | `rounded-xl` |
| `--radius-lg` | 16px | `large` | `rounded-2xl` |
| `--radius-xl` | 24px | `extraLarge` | `rounded-3xl` |

Cards use `--radius-lg` (16px). Input fields use `--radius-sm` (8px). Pills/badges use `--radius-xl` (24px, full pill). FAB uses `rounded-full`.

---

## 4. Spacing & Layout

| Token | Value | Usage |
|-------|-------|-------|
| Page horizontal padding | `16px` | Left/right edge padding |
| Card padding | `16px` | Inside card content |
| Section gap | `16px` | Between cards in a list |
| List item vertical padding | `12px` | Per row in a list |
| Bottom bar height | `64px` | Fixed bottom navigation |
| Safe area bottom | CSS `env(safe-area-inset-bottom)` | iPhone home indicator |

---

## 5. Bottom Navigation Bar

**5 slots:** Home · Log · ＋ (FAB) · Plan · More

| Slot | Icon | Route | Color when active |
|------|------|-------|-------------------|
| Home | `Home` | `/dashboard` | `--text-primary` |
| Log | `FileText` | `/log` | `--text-primary` |
| ＋ | `Plus` | Quick-add sheet | `--bg-card` on `--text-primary` bg |
| Plan | `Calendar` | `/plan` | `--text-primary` |
| More | `Settings` | More sheet | `--text-primary` |

**Styling rules:**
- Container: `--bg-card` (`#FFFFFF`), `border-t` `--divider`
- Height: `64px` + `safe-area-inset-bottom`
- Active icon + label: `--text-primary` (`#111111`)
- Inactive icon + label: `--text-muted` (`#666666`)
- Active indicator pill: `--bg-card` → `#F0F0F0` pill behind icon
- Icon size: `22px`
- Label font: `11px` weight `500`
- FAB: `48px` circle, bg `--text-primary`, icon `--bg-card`, no label

**More sheet** (slide-up modal): lists Diets, Meals, Foods, Health, Workouts, Grocery, Settings as `QuickActionRow` items.

**Quick-add sheet** (slide-up modal): Log Today's Meals, Add a Food, Create a Meal, Build a Diet.

Each row has: 42px rounded-square icon bubble (color at 13% opacity bg), 14px spacer, label (15px SemiBold) + subtitle (12px muted), chevron right.

---

## 6. Card Component

```
bg: --bg-card (#FFFFFF)
border-radius: 16px
border: 1px solid --outline (#DEDEDE)   ← subtle, or shadow-sm instead
padding: 16px
```

No drop shadow by default — rely on border + background contrast against `--bg-page`.  
Use `shadow-sm` only for elevated sheets / modals.

---

## 7. Input Fields

```
height: 40px (h-10) for standard, 32px (h-8) for compact inline
border: 1px solid --outline (#DEDEDE)
border-radius: 8px
padding: 0 12px
font-size: 14px
placeholder: --text-placeholder (#999999)
focus border: --green (#2E7D52)
```

---

## 8. Buttons

### Primary (filled)
```
bg: --green (#2E7D52)
text: white
border-radius: 8px
height: 40px
padding: 0 16px
font: 14px medium
```

### Secondary (outline)
```
bg: transparent
border: 1px solid --outline (#DEDEDE)
text: --text-primary
same size as primary
```

### Ghost
```
bg: transparent, no border
text: --text-muted
hover: bg --bg-page
```

### Destructive
```
bg: --text-destructive (#E53E3E)
text: white
```

### Icon button
```
size: 28px × 28px
border-radius: 8px
icon: 16px
```

---

## 9. Badge / Pill

```
border-radius: 24px (full pill)
padding: 2px 8px
font: 11px medium
```

Variants: green (`--green` fg, `--green-light` bg), gray (`#888888` fg, `#F0F0F0` bg), amber, etc. — match tag colors in §1.

---

## 10. Screen Layout Pattern

Every app screen follows this structure:

```
<page bg #F7F7F7>
  <header>            ← 16px horizontal padding, 16px top, screen title (22px semibold)
  <content area>      ← vertical scroll, 16px horizontal padding, 16px gap between cards
  <bottom nav bar>    ← fixed, 64px + safe-area
</page>
```

No top app bar on main tab screens (just a title text). Sub-screens (detail pages) use a back-arrow row at the top.

---

## 11. Skeleton / Loading State

Use a pulsing gray placeholder matching the shape of the content:
```
bg: #E0E0E0  →  animate-pulse
border-radius: matches content (card=16px, text line=4px, etc.)
```

---

## 12. Error State

```
bg: #FFF0F0  (Red/10% tint)
border: 1px solid #FFCDD2
text: --text-destructive (#E53E3E)
border-radius: 8px
padding: 8px 12px
font: 13px
```

---

## 13. Slot color helper (CSS)

```css
.slot-breakfast { color: #F59E0B; }
.slot-lunch     { color: #2E7D52; }
.slot-dinner    { color: #7C3AED; }
.slot-snack     { color: #2196F3; }
```

---

## 14. Implementation Checklist

- [ ] Rewrite `globals.css` with all CSS custom properties from §1
- [ ] Rewrite `tailwind.config.ts` with full color + radius tokens
- [ ] Build `BottomNav` component matching §5 exactly
- [ ] Build `AppShell` layout component (page bg + content + bottom nav)
- [ ] Apply card styles from §6 to all `<Card>` usages
- [ ] Apply button variants from §8
- [ ] Apply typography scale from §2
- [ ] Redesign each screen: Dashboard, Log, Plan, Diets, Health, Grocery, Settings
