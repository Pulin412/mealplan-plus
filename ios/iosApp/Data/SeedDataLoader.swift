import Foundation

// MARK: - JSON Models

struct IngredientsFile: Codable {
    let foods: [IngredientJSON]
}

struct IngredientJSON: Codable {
    let name: String
    let calories: Double
    let protein: Double
    let carbs: Double
    let fat: Double
}

struct SeedDataFile: Codable {
    let diets: [DietJSON]
}

struct DietJSON: Codable {
    let name: String
    let meal_type: String
    let description: String
    let meals: [String: MealJSON]
}

struct MealJSON: Codable {
    let name: String
    let items: [MealItemJSON]
}

struct MealItemJSON: Codable {
    let food: String
    let quantity: Double
    let unit: String
}

// MARK: - Seed Data Loader

class SeedDataLoader {
    static let shared = SeedDataLoader()

    private var cachedFoods: [FoodItemUI]?
    private var cachedDiets: [DietUI]?
    private var cachedMeals: [MealUI]?
    private var foodLookup: [String: IngredientJSON] = [:]

    private init() {
        loadFoodLookup()
    }

    private func loadFoodLookup() {
        guard let url = Bundle.main.url(forResource: "ingredients", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let file = try? JSONDecoder().decode(IngredientsFile.self, from: data) else {
            return
        }

        for food in file.foods {
            foodLookup[food.name] = food
        }
    }

    // MARK: - Load Foods

    func loadFoods() -> [FoodItemUI] {
        if let cached = cachedFoods {
            return cached
        }

        guard let url = Bundle.main.url(forResource: "ingredients", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let file = try? JSONDecoder().decode(IngredientsFile.self, from: data) else {
            return []
        }

        let foods = file.foods.enumerated().map { index, ingredient in
            FoodItemUI(
                id: Int64(index + 1),
                name: ingredient.name,
                calories: Int(ingredient.calories),
                protein: ingredient.protein,
                carbs: ingredient.carbs,
                fat: ingredient.fat,
                unit: "100g"
            )
        }

        cachedFoods = foods
        return foods
    }

    // MARK: - Load Diets

    func loadDiets() -> [DietUI] {
        if let cached = cachedDiets {
            return cached
        }

        guard let url = Bundle.main.url(forResource: "seed_data", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let file = try? JSONDecoder().decode(SeedDataFile.self, from: data) else {
            return []
        }

        let diets = file.diets.enumerated().map { index, diet -> DietUI in
            // Calculate totals from meals
            var totalCals = 0.0
            var totalProtein = 0.0
            var totalCarbs = 0.0
            var totalFat = 0.0

            for (_, meal) in diet.meals {
                for item in meal.items {
                    if let food = foodLookup[item.food] {
                        // Convert quantity to per-100g basis
                        let multiplier = item.quantity / 100.0
                        totalCals += food.calories * multiplier
                        totalProtein += food.protein * multiplier
                        totalCarbs += food.carbs * multiplier
                        totalFat += food.fat * multiplier
                    }
                }
            }

            return DietUI(
                id: Int64(index + 1),
                name: diet.name,
                description: diet.description,
                calories: Int(totalCals),
                protein: totalProtein,
                carbs: totalCarbs,
                fat: totalFat,
                mealCount: diet.meals.count,
                tags: [diet.meal_type]
            )
        }

        cachedDiets = diets
        return diets
    }

    // MARK: - Load Meals

    func loadMeals() -> [MealUI] {
        if let cached = cachedMeals {
            return cached
        }

        guard let url = Bundle.main.url(forResource: "seed_data", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let file = try? JSONDecoder().decode(SeedDataFile.self, from: data) else {
            return []
        }

        var meals: [MealUI] = []
        var mealId: Int64 = 1
        var seenMeals: Set<String> = []

        // Extract unique meals from all diets
        for diet in file.diets {
            for (slot, meal) in diet.meals {
                // Skip duplicates
                if seenMeals.contains(meal.name) {
                    continue
                }
                seenMeals.insert(meal.name)

                // Calculate meal totals
                var totalCals = 0.0
                var totalProtein = 0.0
                var totalCarbs = 0.0
                var totalFat = 0.0

                for item in meal.items {
                    if let food = foodLookup[item.food] {
                        let multiplier = item.quantity / 100.0
                        totalCals += food.calories * multiplier
                        totalProtein += food.protein * multiplier
                        totalCarbs += food.carbs * multiplier
                        totalFat += food.fat * multiplier
                    }
                }

                let slotDisplay = mapSlotToDisplay(slot)

                meals.append(MealUI(
                    id: mealId,
                    name: meal.name,
                    slot: slotDisplay,
                    calories: Int(totalCals),
                    protein: totalProtein,
                    carbs: totalCarbs,
                    fat: totalFat,
                    foodCount: meal.items.count
                ))

                mealId += 1
            }
        }

        cachedMeals = meals
        return meals
    }

    private func mapSlotToDisplay(_ slot: String) -> String {
        switch slot {
        case "BREAKFAST": return "Breakfast"
        case "LUNCH": return "Lunch"
        case "DINNER": return "Dinner"
        case "NOON": return "Snack"
        case "EVENING": return "Snack"
        case "POST_DINNER": return "Snack"
        default: return slot.capitalized
        }
    }
}
