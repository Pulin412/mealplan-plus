# MealPlan+ Product Requirements Document

**Version:** 2.0
**Last Updated:** February 2026
**Status:** Active Development (Android Complete, iOS In Progress)

---

## 1. Product Vision

**MealPlan+** is a diabetes-friendly meal planning and nutrition tracking application designed to help users manage their dietary intake through structured meal planning, food logging, and health metrics monitoring.

### Problem Statement
People managing diabetes or following specific dietary requirements struggle to:
- Track daily nutritional intake accurately
- Plan meals that meet their macro targets
- Monitor health metrics alongside diet
- Generate grocery lists from meal plans
- Maintain consistency in their eating habits

### Solution
A cross-platform mobile app (Android + iOS) that provides:
- Comprehensive food database with barcode scanning
- Flexible meal and diet template system
- Daily logging with planned vs actual comparison
- Health metrics tracking with trend visualization
- Automated grocery list generation

---

## 2. User Personas

### Primary: Diabetic Patient (Age 35-65)
- Needs to monitor carb intake and blood sugar
- Wants pre-planned meals to reduce daily decisions
- Values seeing planned vs actual intake comparison
- Tracks fasting sugar and HbA1c regularly

### Secondary: Health-Conscious Meal Planner (Age 25-45)
- Focused on macro tracking (protein/carbs/fat)
- Meal preps weekly
- Uses barcode scanning for quick food entry
- Wants analytics on eating patterns

### Future: Nutritionist/Dietician
- Creates diet plans for multiple clients
- Shares plans via export/import
- Monitors client adherence (future cloud feature)

---

## 3. Core Features

### 3.1 Authentication & User Management
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Local Login | Email/password authentication | ✅ | ✅ |
| Sign Up | Create new account | ✅ | ✅ |
| Profile Management | Edit name, age, contact info | ✅ | ✅ |
| Logout | End session | ✅ | ✅ |
| Password Hashing | SHA-256 secure storage | ✅ | ✅ |

### 3.2 Food Management
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Food Database | Browse all foods with nutrition info | ✅ | ✅ |
| Add Food Manually | Create custom food with macros | ✅ | ✅ |
| Edit Food | Modify existing food entry | ✅ | ❌ |
| Delete Food | Remove food from database | ✅ | ✅ |
| Barcode Scanning | Scan product barcode with camera | ✅ | ✅ |
| Online Search | Search OpenFoodFacts/USDA APIs | ✅ | ✅ |
| Favorites | Mark frequently used foods | ✅ | ❌ |
| Recent Foods | Quick access to recently used | ✅ | ❌ |
| Unit Conversions | g, ml, piece, cup, tbsp, tsp, slice, scoop | ✅ | ✅ |
| Glycemic Index | Optional GI tracking per food | ✅ | ❌ |

**Nutrition Data per Food:**
- Calories (kcal per 100g/100ml)
- Protein (g)
- Carbohydrates (g)
- Fat (g)
- Fiber (g) - optional
- Sugar (g) - optional

### 3.3 Meal Planning
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Create Meal | Combine foods into meal template | ✅ | ✅ |
| Edit Meal | Modify meal composition | ✅ | ❌ |
| Delete Meal | Remove meal template | ✅ | ✅ |
| Meal Slots | Breakfast, Lunch, Dinner, Snacks, etc. | ✅ | ✅ |
| Custom Slots | User-defined meal times | ✅ | ❌ |
| Food Quantities | Specify amount per food in meal | ✅ | ✅ |
| Auto Macro Calculation | Sum of all food macros | ✅ | ✅ |
| Search/Filter Meals | By name, slot type | ✅ | ✅ |

**Default Meal Slots:**
- Breakfast
- Lunch
- Dinner
- Snacks
- Dessert
- Beverages

### 3.4 Diet Templates
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Create Diet | Full-day plan with multiple meals | ✅ | ✅ |
| Edit Diet | Modify diet composition | ✅ | ❌ |
| Delete Diet | Remove diet template | ✅ | ✅ |
| Assign Meals to Slots | Map meals to time slots | ✅ | ❌ |
| Diet Tags | Categorize diets (e.g., "Low Carb", "Keto") | ✅ | ✅ |
| Tag Filtering | Filter diets by tags (AND/OR logic) | ✅ | ❌ |
| Duplicate Diet | Copy existing diet as template | ✅ | ❌ |
| Diet Summary | Total calories, macros, meal count | ✅ | ✅ |

### 3.5 Daily Logging & Planning
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Assign Diet to Date | Schedule diet plan for specific day | ✅ | ❌ |
| Log Meals | Record actual meals eaten | ✅ | ✅ |
| Log Individual Foods | Add foods outside meals | ✅ | ✅ |
| Meal Quantity Adjustment | 0.5x, 1x, 1.5x, 2x servings | ✅ | ✅ |
| Planned vs Actual | Visual comparison of targets | ✅ | ✅ |
| Slot Overrides | Replace planned meal for day | ✅ | ❌ |
| Mark Day Complete | Close day for editing | ✅ | ❌ |
| Date Navigation | Browse past/future days | ✅ | ✅ |
| Clear Plan | Remove diet from date | ✅ | ❌ |

### 3.6 Calendar View
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Month View | See all days at glance | ✅ | ✅ |
| Diet Indicators | Show planned diets on dates | ✅ | ✅ |
| Completion Status | Highlight completed days | ✅ | ✅ |
| Quick Date Selection | Tap to view/edit day | ✅ | ✅ |
| Month Navigation | Browse months | ✅ | ✅ |

### 3.7 Health Metrics
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Log Weight | Track body weight (kg) | ✅ | ❌ |
| Log Fasting Sugar | Blood glucose (mg/dL) | ✅ | ❌ |
| Log HbA1c | Glycated hemoglobin (%) | ✅ | ❌ |
| Custom Metrics | User-defined health measures | ✅ | ❌ |
| Metric History | View all readings sorted by date | ✅ | ❌ |
| Add Notes | Optional notes per reading | ✅ | ❌ |
| Delete Reading | Remove metric entry | ✅ | ❌ |

### 3.8 Analytics & Charts
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Weekly Calorie Chart | Line graph on home screen | ✅ | ❌ |
| Health Metric Trends | Line chart over time | ✅ | ❌ |
| Macro Distribution | Pie/bar chart breakdown | ✅ | ❌ |
| Diet Adherence % | Completed vs planned days | ✅ | ❌ |
| Date Range Filters | 7/30/90 days, all time | ✅ | ❌ |
| Daily Averages | Avg calories, macros | ✅ | ❌ |

### 3.9 Grocery Lists
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Create List | Manual or from date range | ✅ | ✅ |
| Auto-Generate Items | From planned meals | ✅ | ❌ |
| Manual Items | Add custom items | ✅ | ✅ |
| Check Off Items | Mark as purchased | ✅ | ❌ |
| Progress Indicator | X of Y items complete | ✅ | ❌ |
| Delete List | Remove grocery list | ✅ | ✅ |
| Edit Quantities | Modify item amounts | ✅ | ❌ |

### 3.10 Data Import/Export
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Export Food Log CSV | Download eating history | ✅ | ✅ |
| Export Health Metrics CSV | Download health data | ✅ | ❌ |
| Import Diets JSON | Load diet templates | ✅ | ✅ |
| Import Diets CSV | Load from spreadsheet | ✅ | ❌ |
| Share Exports | System share sheet | ✅ | ❌ |

### 3.11 Settings & Preferences
| Feature | Description | Android | iOS |
|---------|-------------|---------|-----|
| Dark Mode | Toggle theme | ✅ | ✅ |
| Follow System Theme | Auto dark/light | ✅ | ❌ |
| Dynamic Colors | Material You (Android 12+) | ✅ | N/A |
| Calorie Goal | Daily target setting | ❌ | ✅ |
| Notifications | Meal reminders | ❌ | ✅ |

---

## 4. Screen Inventory

### Android App (27 Screens)
| # | Screen | Purpose |
|---|--------|---------|
| 1 | LoginScreen | User authentication |
| 2 | SignUpScreen | New user registration |
| 3 | HomeScreen | Dashboard with summary + charts |
| 4 | FoodsScreen | Browse food database |
| 5 | AddFoodScreen | Create/edit food |
| 6 | FoodDetailScreen | View food nutrition |
| 7 | BarcodeScannerScreen | Camera barcode detection |
| 8 | OnlineSearchScreen | API food search |
| 9 | MealsScreen | Browse meal templates |
| 10 | AddMealScreen | Create/edit meal |
| 11 | MealDetailScreen | View meal composition |
| 12 | FoodPickerScreen | Select foods for meal |
| 13 | DietsScreen | Browse diet templates |
| 14 | AddDietScreen | Create/edit diet |
| 15 | DietDetailScreen | View diet breakdown |
| 16 | DietMealSlotScreen | Assign meals to slots |
| 17 | DietMealPickerScreen | Select meal for slot |
| 18 | DietPickerScreen | Select diet for date |
| 19 | DailyLogScreen | Log daily intake |
| 20 | CalendarScreen | Month planning view |
| 21 | HealthMetricsScreen | Log health readings |
| 22 | ChartsScreen | Analytics dashboards |
| 23 | GroceryListsScreen | View all lists |
| 24 | CreateGroceryListScreen | Create new list |
| 25 | GroceryDetailScreen | View/edit list items |
| 26 | SettingsScreen | App preferences |
| 27 | ProfileScreen | User profile |

### iOS App (17 Screens) - Current State
| # | Screen | Status |
|---|--------|--------|
| 1 | LoginScreen | ✅ |
| 2 | SignUpScreen | ✅ |
| 3 | HomeScreen | ✅ (no charts) |
| 4 | FoodsScreen | ✅ |
| 5 | AddFoodScreen | ✅ |
| 6 | FoodDetailScreen | ✅ |
| 7 | BarcodeScannerScreen | ✅ |
| 8 | OnlineSearchScreen | ✅ |
| 9 | MealsScreen | ✅ |
| 10 | AddMealScreen | ✅ |
| 11 | MealDetailScreen | ✅ |
| 12 | FoodPickerScreen | ✅ |
| 13 | DietsScreen | ✅ |
| 14 | AddDietScreen | ✅ |
| 15 | DietDetailScreen | ✅ |
| 16 | DailyLogScreen | ✅ |
| 17 | CalendarScreen | ✅ |
| 18 | GroceryListsScreen | ✅ (basic) |
| 19 | MoreScreen | ✅ |
| 20 | SettingsScreen | ✅ |
| 21 | ProfileScreen | ✅ |

### Missing iOS Screens (10)
- EditFoodScreen
- EditMealScreen
- EditDietScreen
- DietMealSlotScreen
- DietMealPickerScreen
- DietPickerScreen
- HealthMetricsScreen
- ChartsScreen
- GroceryDetailScreen
- CreateGroceryListScreen (full)

---

## 5. Feature Parity Matrix

| Category | Feature | Android | iOS | Web |
|----------|---------|---------|-----|-----|
| **Auth** | Login/Signup | ✅ | ✅ | Future |
| **Foods** | CRUD | ✅ | ⚠️ Add only | Future |
| **Foods** | Barcode Scan | ✅ | ✅ | N/A |
| **Foods** | Online Search | ✅ | ✅ | Future |
| **Foods** | Favorites/Recent | ✅ | ❌ | Future |
| **Meals** | CRUD | ✅ | ⚠️ Add only | Future |
| **Meals** | Slot Filtering | ✅ | ✅ | Future |
| **Diets** | CRUD | ✅ | ⚠️ Add only | Future |
| **Diets** | Meal Assignment | ✅ | ❌ | Future |
| **Diets** | Tags | ✅ | ✅ | Future |
| **Planning** | Assign Diet to Date | ✅ | ❌ | Future |
| **Logging** | Log Meals/Foods | ✅ | ✅ | Future |
| **Logging** | Planned vs Actual | ✅ | ✅ | Future |
| **Calendar** | Month View | ✅ | ✅ | Future |
| **Health** | Metric Logging | ✅ | ❌ | Future |
| **Health** | Custom Metrics | ✅ | ❌ | Future |
| **Charts** | Calorie Trends | ✅ | ❌ | Future |
| **Charts** | Health Trends | ✅ | ❌ | Future |
| **Grocery** | Full CRUD | ✅ | ⚠️ Basic | Future |
| **Grocery** | Auto-generate | ✅ | ❌ | Future |
| **Export** | CSV Export | ✅ | ⚠️ Partial | Future |
| **Import** | JSON/CSV Import | ✅ | ⚠️ Partial | Future |
| **Settings** | Theme/Dark Mode | ✅ | ✅ | Future |

**Legend:** ✅ Complete | ⚠️ Partial | ❌ Missing | Future = Planned

---

## 6. Data Entities

### User-Facing Data
| Entity | Description | Key Fields |
|--------|-------------|------------|
| **Food** | Nutritional item | name, calories, protein, carbs, fat, unit |
| **Meal** | Collection of foods | name, slot, foods[], totalCalories |
| **Diet** | Full day plan | name, description, meals[], tags[], totalCalories |
| **Plan** | Diet scheduled for date | date, dietId, isCompleted |
| **DailyLog** | Actual intake for day | date, loggedMeals[], loggedFoods[] |
| **HealthMetric** | Health reading | type, value, date, notes |
| **GroceryList** | Shopping list | name, items[], progress |
| **Tag** | Diet category | name, color |

### Seed Data Included
- **55 Foods**: Common ingredients (Egg, Paneer, Rice, Vegetables, etc.)
- **38 Diets**: Pre-configured meal plans
  - Diet-1 to Diet-17: Remission phase
  - Diet-M1 to Diet-M21: Maintenance phase
- **5 Meal Slots per Diet**: Breakfast, Lunch, Noon, Evening, Dinner

---

## 7. Future Roadmap

### Phase 1: iOS Feature Parity (Current)
- Complete all missing iOS screens
- Integrate shared KMP repositories
- Add edit functionality
- Implement health metrics screen
- Add charts/analytics

### Phase 2: Web Application
- React/Next.js web client
- Responsive design
- Use shared business logic via Kotlin/JS
- Desktop-optimized UI
- Keyboard shortcuts

### Phase 3: Cloud Sync
- User account cloud storage
- Cross-device synchronization
- Backup/restore functionality
- Sharing diet plans between users

### Phase 4: AI Features
- AI meal suggestions based on macros
- Photo-based food recognition
- Personalized recommendations
- Meal plan auto-generation

### Phase 5: Professional Features
- Nutritionist dashboard
- Client management
- Meal plan sharing
- Progress reports for clients

---

## 8. Success Metrics

### Engagement
- Daily Active Users (DAU)
- Meals logged per user per week
- Foods added via barcode vs manual
- Time spent in app per session

### Health Outcomes
- Diet adherence % (logged vs planned)
- Health metric logging frequency
- Weight trend (if tracking)
- HbA1c improvement over time

### Feature Usage
- Most used meal slots
- Popular diet templates
- Barcode scan success rate
- Export feature usage

### Technical
- App crash rate < 0.1%
- API response time < 500ms
- Database query time < 100ms
- App startup time < 2s

---

## 9. Non-Functional Requirements

### Performance
- App launch: < 2 seconds
- Screen transitions: < 300ms
- Search results: < 500ms
- Barcode detection: < 1 second

### Security
- Passwords hashed (SHA-256)
- No cloud storage of health data (currently)
- Local-only authentication
- No third-party analytics

### Accessibility
- VoiceOver/TalkBack support
- Minimum touch target 44pt
- High contrast mode support
- Scalable text

### Compatibility
- Android: API 26+ (Android 8.0+)
- iOS: iOS 16+
- Offline-first architecture

---

## 10. Appendix

### API Dependencies
| API | Purpose | Rate Limit |
|-----|---------|------------|
| OpenFoodFacts | Barcode lookup, food search | Unlimited (free) |
| USDA FoodData Central | Food nutrition database | 1000/hour (demo key) |

### File Formats
**Export CSV Format:**
```csv
date,meal_slot,food_name,quantity,unit,calories,protein,carbs,fat
2024-01-15,Breakfast,Oatmeal,100,g,389,16.9,66.3,6.9
```

**Import JSON Format:**
```json
{
  "diets": [
    {
      "name": "Low Carb Day",
      "description": "Under 100g carbs",
      "meals": {
        "BREAKFAST": {"name": "Eggs & Avocado", "items": [...]}
      }
    }
  ]
}
```

---

*Document maintained by Product Team. For technical details, see ARCHITECTURE.md*
