# MealPlan+ - Product Requirements Document

## Vision
Personal diet & health tracking app focused on **diabetes management** with **plan-first** approach.

---

## Core Concepts

### 1. Food Items (Base Unit)
- Name, brand (optional)
- Macros: calories, protein, carbs, fat
- **Glycemic Index (GI)** - low/medium/high or numeric value
- Serving size + unit (g, ml, piece, cup, etc.)
- Custom fields support (fiber, sugar, sodium - user-defined)
- Source: manual entry → later: barcode scan / online fetch

### 2. Meals (Composition)
- Collection of food items with quantities
- Predefined slots: breakfast, lunch, dinner
- Custom slots: user-defined (early_morning, post_dinner, snack, etc.)
- Auto-calculated macros from constituent foods

### 3. Diets (Templates)
- Named diet template (e.g., "Low Carb Day", "Fasting Day")
- Contains meals for each slot
- Reusable across multiple days
- CRUD operations: create, update, delete, duplicate

### 4. Plans (Calendar)
- Assign diet to any future date
- Calendar view for planning
- Copy/paste days or weeks
- No enforcement - just planning aid

### 5. Daily Log (Reality)
- What user actually consumed
- Can start from planned diet or blank
- Add/remove/modify foods from plan
- Timestamp per meal (optional)

### 6. Health Metrics
- **Built-in**: weight, fasting sugar, HbA1c
- **Custom fields**: user-defined (BP, post-meal sugar, steps, etc.)
- Flexible logging - whenever measured
- Historical tracking

---

## Feature Phases

### Phase 1: Foundation (MVP)
- [ ] Food item CRUD with manual macro entry
- [ ] Meal composition with predefined + custom slots
- [ ] Diet template CRUD
- [ ] Basic daily logging
- [ ] Local SQLite storage
- [ ] Simple home screen with today's view

### Phase 2: Planning & Metrics
- [ ] Calendar-based diet planning
- [ ] Health metrics logging (weight, fasting sugar, HbA1c)
- [ ] Custom health metric fields
- [ ] Basic line charts (weight over time, sugar trends)

### Phase 3: Insights & Intelligence
- [ ] Macro summaries (daily/weekly/monthly)
- [ ] Trend analysis charts
- [ ] Carb-sugar correlation charts
- [ ] Plan vs actual comparison view
- [ ] Export data (CSV/PDF) - for sharing with doctors

### Phase 4: Smart Food Entry
- [ ] Barcode scanner integration
- [ ] Online food database lookup (OpenFoodFacts API)
- [ ] Favorite foods / recent foods
- [ ] Quick-add from history

### Phase 5: Advanced (Future)
- [ ] Cloud backup/sync
- [ ] Reminders for logging
- [ ] Goal setting & tracking
- [ ] Integration with fitness apps

---

## Data Model (Conceptual)

```
FoodItem
├── id, name, brand
├── servingSize, servingUnit
├── calories, protein, carbs, fat
├── glycemicIndex (optional)
└── customFields: Map<String, Double>

Meal
├── id, name, slotType (breakfast/lunch/custom)
└── items: List<FoodItem + quantity>

Diet
├── id, name, description
└── meals: Map<SlotType, Meal>

Plan
├── date
└── dietId

DailyLog
├── date
├── meals: Map<SlotType, List<FoodItem + quantity>>
└── notes

HealthMetric
├── date, timestamp
├── type (weight/sugar/custom)
└── value, unit

CustomField (for extensibility)
├── id, name, unit
├── category (food/health)
└── dataType (number/text)
```

---

## UI Screens (High-level)

1. **Home/Dashboard** - Today's plan, quick log, recent metrics
2. **Foods** - Browse/search/add food items
3. **Diets** - Manage diet templates
4. **Calendar** - Plan view, assign diets to dates
5. **Log** - Daily food log entry
6. **Health** - Log & view health metrics
7. **Charts** - Visualizations & trends
8. **Settings** - Custom fields, preferences

---

## Decisions Made

| Question | Decision |
|----------|----------|
| Primary focus | Diabetes management |
| Tracking approach | Plan-first (create plans, then log actual) |
| Macros to track | Basic: calories, protein, carbs, fat |
| User support | Single user (no family profiles) |
| Plan flexibility | Calendar-based (any diet → any date) |
| Deviation handling | Log actual only (no alerts) |
| Health metric frequency | Flexible (log when measured) |
| Data import | Not needed (starting fresh) |
| Sharing with doctors | Future phase (via export) |
| Meal photos | No (text/data only) |
| Platform | TBD - discuss in tech phase |
| GI tracking | Yes, in Phase 1 |
| Correlation charts | Defer to later phase |
| App name | MealPlan+ |
