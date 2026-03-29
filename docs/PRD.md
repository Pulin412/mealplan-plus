# MealPlan+ — Product Requirements Document

**Version:** 3.0
**Last Updated:** March 2026
**Status:** Android — Shipped · iOS — In Progress

---

## 1. Product Vision

**MealPlan+** is a meal planning and nutrition tracking app for Android and iOS. It gives users a structured way to build a personal food database, design reusable meal and diet templates, plan their week on a calendar, log daily intake, and track health metrics over time.

All data is stored locally on the device. Authentication is handled by Firebase. No meal or health data is sent to any server.

### Problem Statement

People with specific nutrition goals struggle to:
- Track daily intake accurately without a cumbersome manual process
- Plan meals ahead of time and reuse proven meal combinations
- Monitor health metrics (weight, blood glucose, etc.) alongside their diet
- Generate grocery lists automatically from planned meals
- Maintain consistency across a week

### Solution

A cross-platform mobile app that provides:
- A personal food database (manual entry, barcode scan, USDA/OpenFoodFacts import)
- Reusable meal and full-day diet templates
- Calendar-based weekly meal planning
- Daily food logging with planned vs actual comparison
- Health metric tracking with trend charts
- Automated grocery list generation from planned diets
- Home screen widgets for at-a-glance summaries (Android)

---

## 2. User Personas

### Primary: Health-Focused Individual (Age 25–55)
- Tracks carbohydrate and calorie intake
- Wants to plan meals in advance to reduce daily decision-making
- Values the planned vs actual comparison to understand adherence
- Tracks health metrics like weight and blood glucose regularly

### Secondary: Meal Prep Enthusiast (Age 20–40)
- Meal preps for the week every Sunday
- Uses barcode scanning for fast food entry
- Wants macro breakdowns at a glance
- Generates grocery lists directly from their plan

### Future: Nutritionist / Dietician
- Creates diet templates for clients
- Shares templates via export
- Monitors client plan adherence (requires cloud sync — on roadmap)

---

## 3. Feature Status

### 3.1 Authentication

| Feature | Android | iOS |
|---------|---------|-----|
| Email / Password sign-in (Firebase) | ✅ | ✅ |
| Google OAuth sign-in (Firebase) | ✅ | ✅ |
| Sign up with email | ✅ | ✅ |
| Forgot password — Firebase reset email | ✅ | ✅ |
| Profile management (name, age, weight, goal) | ✅ | ✅ |
| Logout with full session clear | ✅ | ✅ |
| Delete account (local + Firebase) | ✅ | ✅ |

> Credentials are managed entirely by Firebase Authentication. No passwords are stored locally.

---

### 3.2 Food Database

| Feature | Android | iOS |
|---------|---------|-----|
| Browse food database | ✅ | ✅ |
| Add food manually | ✅ | ✅ |
| Edit food | ✅ | ❌ |
| Delete food | ✅ | ✅ |
| Barcode scanner (OpenFoodFacts / USDA) | ✅ | ✅ |
| Online search (USDA / OpenFoodFacts) | ✅ | ✅ |
| Favourite foods | ✅ | ❌ |
| Recent foods | ✅ | ❌ |
| Unit support: g, ml, piece, cup, tbsp, tsp, slice, scoop | ✅ | ✅ |

**Nutrition fields per food (all per 100g):** Calories · Protein · Carbohydrates · Fat · Fiber (optional) · Sugar (optional)

---

### 3.3 Meals

| Feature | Android | iOS |
|---------|---------|-----|
| Create meal template | ✅ | ✅ |
| Edit meal | ✅ | ❌ |
| Delete meal | ✅ | ✅ |
| Assign foods with quantity and unit | ✅ | ✅ |
| Auto-calculated macro totals | ✅ | ✅ |
| Meal detail with ingredient checklist | ✅ | ✅ |
| Search / filter meals | ✅ | ✅ |

---

### 3.4 Diet Templates

| Feature | Android | iOS |
|---------|---------|-----|
| Create diet template | ✅ | ✅ |
| Edit diet | ✅ | ❌ |
| Delete diet | ✅ | ✅ |
| Assign meals to slots (Breakfast, Lunch, Dinner, etc.) | ✅ | ❌ |
| Custom meal slots | ✅ | ❌ |
| Colour-coded tags | ✅ | ✅ |
| Tag filtering | ✅ | ❌ |
| Diet summary (calories, macros, meal count, food preview) | ✅ | ✅ |
| System (seed) diets — read-only | ✅ | ✅ |

**Default meal slots:** Breakfast · Morning Snack · Lunch · Evening Snack · Dinner

---

### 3.5 Daily Logging

| Feature | Android | iOS |
|---------|---------|-----|
| Log meals to a slot | ✅ | ✅ |
| Log individual foods | ✅ | ✅ |
| Apply a diet template to a day | ✅ | ✅ |
| Custom per-day meal slots | ✅ | ❌ |
| Planned vs actual comparison | ✅ | ✅ |
| Date navigation (past and future) | ✅ | ✅ |
| Edit / remove logged items | ✅ | ✅ |
| Daily nutrition summary bar | ✅ | ✅ |

---

### 3.6 Meal Planning (Calendar)

| Feature | Android | iOS |
|---------|---------|-----|
| Month calendar view | ✅ | ✅ |
| Assign diet to a date | ✅ | ✅ |
| Change or remove diet from date | ✅ | ✅ |
| Planned-diet indicator on calendar dates | ✅ | ✅ |
| Tap date to view / log | ✅ | ✅ |
| Month navigation | ✅ | ✅ |

---

### 3.7 Health Metrics

| Feature | Android | iOS |
|---------|---------|-----|
| Log weight | ✅ | ❌ |
| Log blood glucose (Fasting / Post-meal / Random) | ✅ | ❌ |
| Log blood pressure | ✅ | ❌ |
| Log HbA1c | ✅ | ❌ |
| Custom metric types (user-defined name + unit) | ✅ | ❌ |
| Metric history list | ✅ | ❌ |
| Normal range min/max per metric type | ✅ | ❌ |
| Delete readings | ✅ | ❌ |

---

### 3.8 Charts & Analytics

| Feature | Android | iOS |
|---------|---------|-----|
| Health metric trend charts (line) | ✅ | ❌ |
| Normal range bands on charts | ✅ | ❌ |
| Date range filter (7 / 30 / 90 days) | ✅ | ❌ |
| Weekly calorie summary on Home | ✅ | ❌ |

---

### 3.9 Grocery Lists

| Feature | Android | iOS |
|---------|---------|-----|
| Auto-generate list from date-range plan | ✅ | ❌ |
| Create list manually | ✅ | ✅ |
| Check off items while shopping | ✅ | ❌ |
| Items grouped by food category | ✅ | ❌ |
| Progress indicator (X of Y checked) | ✅ | ❌ |
| Edit item quantities | ✅ | ❌ |
| Delete list | ✅ | ✅ |

---

### 3.10 Widgets (Android only)

| Widget | Status |
|--------|--------|
| Today's Plan — shows planned meals by slot | ✅ |
| Diet Summary — calorie ring + macro totals | ✅ |
| Mini Calendar — month view with plan indicators | ✅ |
| Reactive updates (Room Flow → Glance collectAsState) | ✅ |
| Deep links into app screens | ✅ |
| Appearance settings (theme, compact/expanded) | ✅ |

---

### 3.11 Settings & Preferences

| Feature | Android | iOS |
|---------|---------|-----|
| Dark / Light / System theme | ✅ | ✅ |
| Widget appearance settings | ✅ | N/A |
| Export data as CSV | ✅ | ⚠️ Partial |
| Clear all data | ✅ | ✅ |

---

## 4. Screen Inventory

### Android (29 routes)

| Screen | Purpose |
|--------|---------|
| LoginScreen | Email/password + Google sign-in |
| SignUpScreen | New account creation |
| ForgotPasswordScreen | Firebase password reset |
| HomeScreen | Dashboard — today's summary, quick actions |
| ProfileScreen | Edit user profile |
| FoodsScreen | Browse food database (also picker mode) |
| AddFoodScreen | Create custom food |
| BarcodeScannerScreen | Camera barcode detection |
| OnlineSearchScreen | USDA / OpenFoodFacts search |
| MealsScreen | Browse meal templates |
| AddMealScreen | Create meal |
| EditMealScreen | Edit existing meal |
| FoodPickerScreen | Select food for meal / diet slot |
| DietsScreen | Browse diet templates |
| AddDietScreen | Create diet |
| DietDetailScreen | View / edit diet with slot assignments |
| DietMealSlotScreen | Manage a specific meal slot in a diet |
| DietMealPickerScreen | Select meal to assign to a slot |
| DailyLogScreen | Log daily intake (with and without date param) |
| LogMealPickerScreen | Select meal when logging |
| DietPickerScreen | Choose diet template to apply to a date |
| CalendarScreen | Month calendar with plan assignment |
| HealthScreen | Log and browse health metrics |
| ChartsScreen | Health metric trend charts |
| GroceryListsScreen | Browse all grocery lists |
| CreateGroceryListScreen | Create grocery list (manual or from plan) |
| GroceryDetailScreen | View and check off grocery items |
| SettingsScreen | App preferences |
| WidgetSettingsScreen | Home screen widget appearance |

### iOS (21 screens — in progress)

| Screen | Status |
|--------|--------|
| LoginScreen | ✅ |
| SignUpScreen | ✅ |
| ForgotPasswordScreen | ✅ |
| HomeScreen | ✅ (no charts) |
| ProfileScreen | ✅ |
| FoodsScreen | ✅ |
| AddFoodScreen | ✅ |
| OnlineSearchScreen | ✅ |
| BarcodeScannerScreen | ✅ |
| MealsScreen | ✅ |
| AddMealScreen | ✅ |
| MealDetailScreen | ✅ (ingredient checklist) |
| DietsScreen | ✅ |
| AddDietScreen | ✅ |
| DietDetailScreen | ✅ |
| DailyLogScreen | ✅ |
| CalendarScreen | ✅ |
| GroceryListsScreen | ✅ (basic — no auto-generate) |
| MoreScreen | ✅ (navigation hub) |
| SettingsScreen | ✅ |
| HealthScreen | ❌ Not yet implemented |

**Missing iOS screens:** EditFoodScreen · EditMealScreen · EditDietScreen · DietMealSlotScreen · DietMealPickerScreen · DietPickerScreen · HealthScreen · ChartsScreen · GroceryDetailScreen · CreateGroceryListScreen (full) · WidgetKit widgets

---

## 5. Data Model

| Entity | Key Fields |
|--------|-----------|
| **User** | email, displayName, age, weight, height, activityLevel, goal |
| **FoodItem** | name, calories, protein, carbs, fat, servingSize, servingUnit, category, barcode |
| **Meal** | name, description, userId |
| **MealFoodItem** | mealId, foodItemId, quantity, unit |
| **Diet** | name, description, isSystem, userId |
| **DietMeal** | dietId, mealId, slotType, instructions |
| **Tag** | name, color, userId |
| **DietTagCrossRef** | dietId, tagId |
| **DailyLog** | userId, date, plannedDietId |
| **LoggedFood** | logId, foodItemId, quantity, unit, slotType, loggedAt |
| **Plan** | userId, date, dietId, isCompleted |
| **HealthMetric** | userId, type, value, subType, unit, recordedAt |
| **CustomMetricType** | userId, name, unit, minValue, maxValue |
| **GroceryList** | userId, name, startDate, endDate |
| **GroceryItem** | listId, foodItemId, name, quantity, unit, category, isChecked |
| **CustomMealSlot** | userId, date, name, order |

Database: Room (Android) / SQLDelight (iOS) · Current version: 21 · Full schema: [database-schema.md](android/technical/database-schema.md)

---

## 6. Seed Data

On first sign-in, every new account is seeded with:
- **~55 common foods** — everyday ingredients (eggs, grains, vegetables, proteins, dairy)
- **5 meal slots** — Breakfast, Morning Snack, Lunch, Evening Snack, Dinner
- **Sample diet templates** — covering common patterns (low-carb, balanced, high-protein)

Users can delete or modify all seed data.

---

## 7. Roadmap

### Now — Android polish & iOS parity
- Complete missing iOS screens (Health, Charts, Grocery Detail, Edit flows)
- iOS diet meal slot assignment
- Performance and stability work

### Near-term — Firebase integrations
| Item | Purpose | Priority |
|------|---------|---------|
| Firebase Crashlytics | Crash visibility before public launch | 🔴 P1 |
| Firebase Remote Config | Feature flags for safe rollouts | 🔴 P1 |
| Firebase Analytics | Usage insights once real users arrive | 🟡 P2 |
| Cloud Firestore sync | Cross-device data backup and restore | 🟡 P2 |
| Firebase Cloud Messaging | Push notification reminders | 🟢 P3 |

Full implementation details: [roadmap/future-improvements.md](roadmap/future-improvements.md)

### Later
- iOS WidgetKit widgets
- Web app (React / Next.js)
- Nutritionist / multi-user sharing
- AI meal suggestions

---

## 8. Non-Functional Requirements

### Performance
| Metric | Target |
|--------|--------|
| App cold start | < 2 seconds |
| Screen transition | < 300ms |
| Food search results | < 500ms |
| Barcode detection | < 1 second |
| DB query (Diets screen, 20+ diets) | < 100ms (batch queries, cache) |

### Security
- Credentials managed by **Firebase Authentication** — no passwords stored locally
- `passwordHash` field in DB is always empty for all accounts
- Firebase UID → local user mapping stored in DataStore (encrypted on Android 6+)
- No meal or health data leaves the device

### Compatibility
- Android: API 26+ (Android 8.0+)
- iOS: iOS 16+
- Offline-first: all features work without a network connection

### Accessibility
- TalkBack / VoiceOver support
- Minimum touch target 44pt
- Content descriptions on all icons and images
- Scalable text

---

## 9. API Dependencies

| API | Purpose | Limits |
|-----|---------|--------|
| OpenFoodFacts | Barcode lookup, food search | Unlimited (open data) |
| USDA FoodData Central | Nutrition data search | 1,000 req/hour (demo key — upgrade before launch) |
| Firebase Authentication | Sign-in, sign-up, password reset | 10,000 MAU free (Spark plan) |

> **Action required before launch:** Replace the USDA `DEMO_KEY` with a registered API key. Demo keys are rate-limited and can be revoked without notice.

---

## 10. Appendix

### Export CSV Format
```
date,meal_slot,food_name,quantity,unit,calories,protein,carbs,fat
2026-03-29,Breakfast,Oatmeal,100,g,389,16.9,66.3,6.9
```

### Technical Reference
- Architecture: [android/technical/architecture.md](android/technical/architecture.md)
- Database schema: [android/technical/database-schema.md](android/technical/database-schema.md)
- Authentication flows: [android/technical/authentication.md](android/technical/authentication.md)
- Contributing guide: [android/technical/contributing.md](android/technical/contributing.md)
