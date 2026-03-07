# MealPlan+ Technical Architecture Document

**Version:** 1.0
**Last Updated:** February 2026
**Audience:** Architects, Senior Developers

---

## 1. System Overview

MealPlan+ is a **Kotlin Multiplatform (KMP)** application supporting Android and iOS from a shared codebase. The architecture prioritizes code sharing while allowing platform-specific UI implementations.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                          │
├─────────────────────────────┬───────────────────────────────────┤
│     Android (Jetpack Compose)│         iOS (SwiftUI)             │
│  ┌─────────────────────────┐│  ┌─────────────────────────────┐  │
│  │ ViewModels + StateFlow  ││  │ @State + @Observable        │  │
│  │ Hilt DI                 ││  │ Manual DI                   │  │
│  │ Navigation Compose      ││  │ NavigationStack             │  │
│  └─────────────────────────┘│  └─────────────────────────────┘  │
├─────────────────────────────┴───────────────────────────────────┤
│                      SHARED LAYER (KMP)                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Repositories                           │   │
│  │  FoodRepository | MealRepository | DietRepository         │   │
│  │  DailyLogRepository | HealthMetricRepository              │   │
│  │  GroceryRepository | UserRepository | PlanRepository      │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    SQLDelight Queries                     │   │
│  │  Type-safe SQL | Generated Kotlin | Coroutines Flow      │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Network (Ktor)                         │   │
│  │  OpenFoodFactsApi | UsdaFoodApi                          │   │
│  └──────────────────────────────────────────────────────────┘   │
├─────────────────────────────┬───────────────────────────────────┤
│      androidMain            │           iosMain                  │
│  ┌─────────────────────────┐│  ┌─────────────────────────────┐  │
│  │ AndroidSqliteDriver     ││  │ NativeSqliteDriver          │  │
│  │ OkHttp Engine           ││  │ Darwin Engine               │  │
│  │ DataStore Preferences   ││  │ NSUserDefaults              │  │
│  └─────────────────────────┘│  └─────────────────────────────┘  │
├─────────────────────────────┴───────────────────────────────────┤
│                      DATA LAYER                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              SQLite Database (mealplan.db)                │   │
│  │                     17 Tables                             │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Project Structure

```
mealplan-plus/
├── shared/                           # KMP shared module
│   └── src/
│       ├── commonMain/               # Shared code (all platforms)
│       │   ├── kotlin/com/mealplanplus/shared/
│       │   │   ├── db/               # Database factories
│       │   │   ├── model/            # Domain models
│       │   │   ├── network/          # Ktor HTTP clients
│       │   │   ├── preferences/      # Settings interface
│       │   │   ├── repository/       # Business logic
│       │   │   └── util/             # Utilities
│       │   └── sqldelight/           # .sq schema files
│       ├── androidMain/              # Android-specific
│       │   └── kotlin/com/mealplanplus/shared/
│       │       ├── db/               # AndroidSqliteDriver
│       │       ├── network/          # OkHttp engine
│       │       └── preferences/      # DataStore impl
│       └── iosMain/                  # iOS-specific
│           └── kotlin/com/mealplanplus/shared/
│               ├── db/               # NativeSqliteDriver
│               ├── network/          # Darwin engine
│               └── preferences/      # NSUserDefaults
├── android/                          # Android application (formerly app/)
│   └── src/main/
│       ├── java/com/mealplanplus/
│       │   ├── data/                 # Room entities (legacy)
│       │   ├── di/                   # Hilt modules
│       │   ├── ui/                   # Compose screens
│       │   └── viewmodel/            # ViewModels
│       └── res/                      # Resources
└── ios/                              # iOS application (formerly iosApp/)
    └── iosApp/
        ├── Screens/                  # SwiftUI views
        ├── Data/                     # Data loaders
        └── Resources/                # Assets
```

---

## 3. Technology Stack

| Layer | Android | iOS | Shared (KMP) |
|-------|---------|-----|--------------|
| **Language** | Kotlin | Swift | Kotlin |
| **UI Framework** | Jetpack Compose | SwiftUI | - |
| **State Management** | ViewModel + StateFlow | @State/@Observable | - |
| **Navigation** | Navigation Compose | NavigationStack | - |
| **Database** | SQLDelight | SQLDelight | SQLDelight |
| **HTTP Client** | - | - | Ktor |
| **DI** | Hilt | Manual | - |
| **Async** | Coroutines | async/await | Coroutines |
| **Charts** | Vico | - (planned) | - |
| **Barcode** | ML Kit | Vision | - |
| **Build** | Gradle | Xcode | Gradle |

---

## 4. Database Architecture

### 4.1 SQLDelight Schema

SQLDelight generates type-safe Kotlin code from SQL definitions, enabling shared database access across platforms.

**Schema Location:** `shared/src/commonMain/sqldelight/com/mealplanplus/db/`

### 4.2 Tables (17 Total)

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| **users** | User accounts | id, email, passwordHash, displayName, age, contact |
| **food_items** | Food database | id, name, brand, barcode, calories, protein, carbs, fat, unit, gramsPerPiece, gramsPerCup, glycemicIndex, isFavorite |
| **meals** | Meal templates | id, userId, name, slotType, customSlotId, createdAt |
| **meal_food_items** | Meal ↔ Food junction | mealId, foodId, quantity, unit, notes |
| **custom_meal_slots** | User-defined slots | id, userId, name, orderNum |
| **diets** | Diet templates | id, userId, name, description, createdAt |
| **diet_meals** | Diet ↔ Meal per slot | dietId, slotType, mealId |
| **tags** | Diet categories | id, userId, name, color, createdAt |
| **diet_tags** | Diet ↔ Tag junction | dietId, tagId |
| **plans** | Scheduled diets | userId, date, dietId, notes, isCompleted |
| **daily_logs** | Daily intake logs | userId, date, plannedDietId, notes, createdAt |
| **daily_log_slot_overrides** | Meal overrides | userId, logDate, slotType, overrideMealId |
| **logged_foods** | Individual food logs | id, userId, logDate, foodId, quantity, unit, slotType |
| **logged_meals** | Meal logs | id, userId, logDate, mealId, quantity, timestamp |
| **health_metrics** | Health readings | id, userId, date, metricType, customTypeId, value, notes |
| **custom_metric_types** | User-defined metrics | id, userId, name, unit, minValue, maxValue, isActive |
| **grocery_lists** | Shopping lists | id, userId, name, startDate, endDate, createdAt |
| **grocery_items** | List items | id, listId, foodId, customName, quantity, unit, isChecked |

### 4.3 Entity Relationship Diagram

```
┌──────────┐       ┌──────────┐       ┌──────────┐
│  users   │──1:N──│  diets   │──M:N──│   tags   │
└──────────┘       └──────────┘       └──────────┘
     │                   │
     │              diet_meals
     │                   │
     │              ┌────▼────┐       ┌────────────┐
     └──────1:N─────│  meals  │──M:N──│ food_items │
                    └─────────┘       └────────────┘
                         │                   │
                    meal_food_items          │
                                             │
┌──────────┐       ┌─────────────┐           │
│  plans   │       │ daily_logs  │───────────┘
└──────────┘       └─────────────┘
     │                   │
     └───────────────────┼───────────────────┐
                         │                   │
                  ┌──────▼──────┐     ┌──────▼──────┐
                  │ logged_foods │     │ logged_meals│
                  └─────────────┘     └─────────────┘
```

### 4.4 Key Design Patterns

| Pattern | Implementation | Rationale |
|---------|----------------|-----------|
| **Composite Keys** | `(userId, date)` for plans/logs | Multi-tenant isolation, efficient queries |
| **Junction Tables** | diet_tags, meal_food_items | Clean M:N relationships |
| **Soft References** | Nullable FKs for customSlotId | Graceful handling of deleted entities |
| **Cascading Deletes** | ON DELETE CASCADE | Maintain referential integrity |
| **Indexed Columns** | userId, date, barcode | Query performance |

---

## 5. Repository Layer

### 5.1 Repository Pattern

All data access goes through repository classes that:
- Accept `MealPlanDatabase` via constructor
- Return `Flow<List<T>>` for reactive reads
- Use `suspend` for write operations
- Map DB entities to domain models

### 5.2 Repository Inventory

```kotlin
// FoodRepository
├── getAllFoods(): Flow<List<FoodItem>>
├── searchByName(query, limit): Flow<List<FoodItem>>
├── getByBarcode(barcode): FoodItem?
├── getFavorites(): Flow<List<FoodItem>>
├── getRecent(limit): Flow<List<FoodItem>>
├── insertFood(food): Long
├── updateFood(food)
├── deleteFood(id)
└── setFavorite(id, isFavorite)

// MealRepository
├── getAllMeals(userId): Flow<List<Meal>>
├── getMealsBySlot(userId, slotType): Flow<List<Meal>>
├── getMealWithFoods(mealId): MealWithFoods?
├── insertMeal(meal): Long
├── updateMeal(meal)
├── deleteMeal(id)
├── addFoodToMeal(mealId, foodId, quantity, unit)
├── removeFoodFromMeal(mealId, foodId)
├── getCustomSlots(userId): Flow<List<CustomMealSlot>>
└── createCustomSlot(userId, name)

// DietRepository
├── getAllDiets(userId): Flow<List<Diet>>
├── getDietWithMeals(dietId): DietWithMeals?
├── getDietSummaries(userId): Flow<List<DietSummary>>
├── insertDiet(diet): Long
├── updateDiet(diet)
├── deleteDiet(id)
├── setDietMeal(dietId, slotType, mealId)
├── getAllTags(userId): Flow<List<Tag>>
├── createTag(userId, name, color): Long
├── addTagToDiet(dietId, tagId)
└── removeTagFromDiet(dietId, tagId)

// DailyLogRepository
├── getDailyLog(userId, date): DailyLog?
├── getDailyLogsForRange(userId, start, end): Flow<List<DailyLog>>
├── createDailyLog(userId, date, plannedDietId?): Long
├── insertLoggedFood(loggedFood): Long
├── updateLoggedFood(loggedFood)
├── deleteLoggedFood(id)
├── insertLoggedMeal(loggedMeal): Long
├── updateLoggedMealQuantity(id, quantity)
├── deleteLoggedMeal(id)
├── getDailyMacroSummary(userId, start, end): Flow<List<DailyMacroSummary>>
├── getSlotOverrides(userId, date): List<DailyLogSlotOverride>
└── setSlotOverride(userId, date, slot, mealId)

// PlanRepository
├── getPlanByDate(userId, date): Plan?
├── getPlansForRange(userId, start, end): Flow<List<Plan>>
├── insertPlan(plan)
├── updatePlan(plan)
├── deletePlan(userId, date)
└── markPlanComplete(userId, date, isComplete)

// HealthMetricRepository
├── getHealthMetrics(userId): Flow<List<HealthMetric>>
├── getMetricsByType(userId, type): Flow<List<HealthMetric>>
├── getMetricsForRange(userId, start, end): Flow<List<HealthMetric>>
├── insertHealthMetric(metric): Long
├── deleteHealthMetric(id)
├── getCustomMetricTypes(userId): Flow<List<CustomMetricType>>
└── createCustomMetricType(userId, name, unit): Long

// GroceryRepository
├── getAllGroceryLists(userId): Flow<List<GroceryList>>
├── getGroceryListWithItems(listId): GroceryListWithItems?
├── insertGroceryList(list): Long
├── deleteGroceryList(id)
├── insertGroceryItem(item): Long
├── updateGroceryItemChecked(itemId, isChecked)
├── deleteGroceryItem(id)
└── generateItemsFromDateRange(userId, start, end): List<GroceryItem>

// UserRepository
├── getUserByEmail(email): User?
├── getUserById(id): User?
├── insertUser(user): Long
├── updateUser(user)
└── validatePassword(email, password): User?
```

### 5.3 Reactive Data Flow

```kotlin
// Extension function for Flow conversion
fun <T : Any> Query<T>.asFlowList(): Flow<List<T>> {
    return this.asFlow().mapToList(Dispatchers.Default)
}

// Usage in Repository
class FoodRepository(private val database: MealPlanDatabase) {
    fun getAllFoods(): Flow<List<FoodItem>> {
        return database.foodItemQueries
            .selectAll()
            .asFlowList()
            .map { list -> list.map { it.toFoodItem() } }
    }
}
```

---

## 6. Network Layer

### 6.1 Ktor HTTP Client

```kotlin
// commonMain - shared configuration
val sharedJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// expect/actual for platform engines
expect fun createHttpClient(): HttpClient

// androidMain
actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) { json(sharedJson) }
    }
}

// iosMain
actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) { json(sharedJson) }
    }
}
```

### 6.2 API Integrations

**OpenFoodFacts API:**
```kotlin
class OpenFoodFactsApi(private val client: HttpClient) {
    private val baseUrl = "https://world.openfoodfacts.org"

    suspend fun getProductByBarcode(barcode: String): ProductResponse? {
        return client.get("$baseUrl/api/v0/product/$barcode.json").body()
    }

    suspend fun searchProducts(query: String, page: Int = 1): SearchResponse {
        return client.get("$baseUrl/cgi/search.pl") {
            parameter("search_terms", query)
            parameter("search_simple", 1)
            parameter("json", 1)
            parameter("page", page)
        }.body()
    }
}
```

**USDA FoodData Central API:**
```kotlin
class UsdaFoodApi(private val client: HttpClient) {
    private val baseUrl = "https://api.nal.usda.gov/fdc/v1"
    private val apiKey = "DEMO_KEY"  // Replace with actual key

    suspend fun searchFoods(query: String): FoodSearchResponse {
        return client.get("$baseUrl/foods/search") {
            parameter("api_key", apiKey)
            parameter("query", query)
            parameter("dataType", "Foundation,SR Legacy")
        }.body()
    }

    suspend fun getFoodDetails(fdcId: Int): FoodDetailsResponse {
        return client.get("$baseUrl/food/$fdcId") {
            parameter("api_key", apiKey)
        }.body()
    }
}
```

---

## 7. Platform-Specific Implementations

### 7.1 Database Drivers

```kotlin
// commonMain - expect declaration
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = MealPlanDatabase.Schema,
            context = context,
            name = "mealplan.db"
        )
    }
}

// iosMain
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = MealPlanDatabase.Schema,
            name = "mealplan.db"
        )
    }
}
```

### 7.2 Preferences

```kotlin
// commonMain
expect class PreferencesManager {
    suspend fun getUserId(): Long?
    suspend fun setUserId(id: Long)
    suspend fun clearUserId()
    suspend fun isDarkMode(): Boolean
    suspend fun setDarkMode(enabled: Boolean)
}

// androidMain - uses DataStore
actual class PreferencesManager(private val context: Context) {
    private val dataStore = context.dataStore

    actual suspend fun getUserId(): Long? {
        return dataStore.data.first()[USER_ID_KEY]
    }

    actual suspend fun setUserId(id: Long) {
        dataStore.edit { it[USER_ID_KEY] = id }
    }
}

// iosMain - uses NSUserDefaults
actual class PreferencesManager {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun getUserId(): Long? {
        val value = defaults.objectForKey("user_id") as? NSNumber
        return value?.longValue
    }

    actual suspend fun setUserId(id: Long) {
        defaults.setObject(NSNumber(long = id), forKey = "user_id")
    }
}
```

---

## 8. Build Configuration

### 8.1 Root build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform") version "1.9.24" apply false
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("app.cash.sqldelight") version "2.0.1" apply false
}
```

### 8.2 Shared Module build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("app.cash.sqldelight")
    kotlin("plugin.serialization") version "1.9.24"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("app.cash.sqldelight:runtime:2.0.1")
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            implementation("io.ktor:ktor-client-core:2.3.7")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
        }
        androidMain.dependencies {
            implementation("app.cash.sqldelight:android-driver:2.0.1")
            implementation("io.ktor:ktor-client-okhttp:2.3.7")
            implementation("androidx.datastore:datastore-preferences:1.0.0")
        }
        iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:2.0.1")
            implementation("io.ktor:ktor-client-darwin:2.3.7")
        }
    }
}

sqldelight {
    databases {
        create("MealPlanDatabase") {
            packageName.set("com.mealplanplus.shared.db")
        }
    }
}

android {
    namespace = "com.mealplanplus.shared"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
}
```

### 8.3 iOS Framework Generation

```bash
# Debug framework for simulator (Apple Silicon)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Release framework for device
./gradlew :shared:linkReleaseFrameworkIosArm64

# Universal framework (all architectures)
./gradlew :shared:assembleXCFramework
```

---

## 9. Key Design Decisions

| Decision | Options Considered | Chosen | Rationale |
|----------|-------------------|--------|-----------|
| **Cross-platform** | Flutter, React Native, KMP | KMP | Native UI, shared business logic |
| **Database** | Room, SQLDelight, Realm | SQLDelight | KMP support, type-safe SQL |
| **HTTP Client** | Retrofit, Ktor | Ktor | KMP support, coroutines |
| **Android UI** | Views, Compose | Compose | Modern, declarative |
| **iOS UI** | UIKit, SwiftUI | SwiftUI | Modern, declarative |
| **State** | LiveData, StateFlow | StateFlow | KMP-friendly, coroutines |
| **DI (Android)** | Koin, Hilt | Hilt | Google-supported, compile-safe |
| **Reactive Queries** | LiveData, Flow | Flow | KMP-compatible |
| **Password Storage** | Plain, SHA-256, bcrypt | SHA-256 | Simple local auth (no cloud) |
| **Date Format** | Various | ISO 8601 (yyyy-MM-dd) | Universal standard |

---

## 10. Security Considerations

### Authentication
- Local-only authentication (no cloud backend)
- SHA-256 password hashing (hex-encoded)
- No password recovery (local-only)
- Session stored in DataStore/NSUserDefaults

### Data Protection
- All data stored locally on device
- No data transmitted to external servers (except food APIs)
- Health data never leaves device
- No third-party analytics

### API Security
- OpenFoodFacts: Public API, no auth required
- USDA: DEMO_KEY for development (rate-limited)
- HTTPS for all API calls

---

## 11. Testing Strategy

### Unit Tests (Shared)
- Repository logic
- Nutrition calculations
- Data mapping functions
- SQL query correctness

### Integration Tests
- Database operations
- API client responses
- Repository ↔ Database flow

### UI Tests
- **Android**: Compose UI tests, Espresso
- **iOS**: XCTest, SwiftUI previews

### Manual Testing
- Barcode scanning
- Cross-platform data consistency
- Performance benchmarks

---

## 12. Performance Considerations

### Database
- Indexed columns for frequent queries
- Batch inserts for seed data
- Lazy loading for large lists
- Flow-based queries (no polling)

### Network
- Caching for API responses
- Debounced search queries
- Offline-first architecture

### UI
- Lazy lists (LazyColumn/LazyVStack)
- Image caching (future)
- Minimal re-composition

---

## 13. Future Technical Considerations

### Web Support
- Kotlin/JS for shared logic
- React/Next.js frontend
- SQLDelight Web driver (SQLite WASM)

### Cloud Sync
- Backend API (likely Ktor server)
- Conflict resolution strategy
- Offline queue for sync

### CI/CD
- GitHub Actions for builds
- Fastlane for iOS deployment
- Play Console API for Android

---

*Document maintained by Engineering Team. For product requirements, see PRD.md*
