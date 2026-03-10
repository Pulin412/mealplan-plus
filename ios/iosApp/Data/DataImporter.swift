import Foundation
import shared

// MARK: - JSON Models

// For common_foods.json (detailed foods)
private struct BundledFood: Codable {
    let name: String
    let category: String
    let caloriesPer100: Double
    let proteinPer100: Double
    let carbsPer100: Double
    let fatPer100: Double
    let gramsPerPiece: Double?
    let gramsPerCup: Double?
    let gramsPerTbsp: Double?
    let gramsPerTsp: Double?
    let glycemicIndex: Int?
}

// For ingredients.json (simple foods)
private struct SeedFood: Codable {
    let name: String
    let calories: Double
    let protein: Double
    let carbs: Double
    let fat: Double
}

private struct SeedFoodWrapper: Codable {
    let foods: [SeedFood]
}

// For seed_data.json (diets/meals)
private struct SeedFoodItem: Codable {
    let food: String
    let quantity: Double
    let unit: String
}

private struct SeedMeal: Codable {
    let name: String
    let items: [SeedFoodItem]
}

private struct SeedDiet: Codable {
    let name: String
    let meal_type: String
    let description: String
    let meals: [String: SeedMeal]
}

private struct SeedData: Codable {
    let diets: [SeedDiet]
}

/// Simple data importer - mirrors Android's DatabaseSeeder approach
@MainActor
final class DataImporter {
    static let shared = DataImporter()
    private init() {}

    /// Import sample data from bundled JSON files
    /// Returns status message
    func importSampleData(userId: Int64) async -> String {
        var foodsImported = 0
        var dietsImported = 0
        var mealsImported = 0

        // 1. Import foods from ingredients.json
        let foodMap = await importFoods()
        foodsImported = foodMap.count

        if foodMap.isEmpty {
            return "Error: No foods imported"
        }

        // 2. Import diets/meals from seed_data.json
        let (diets, meals) = await importDietsAndMeals(userId: userId, foodMap: foodMap)
        dietsImported = diets
        mealsImported = meals

        return "✓ Imported \(foodsImported) foods, \(dietsImported) diets, \(mealsImported) meals"
    }

    // MARK: - Import Foods

    private func importFoods() async -> [String: Int64] {
        var foodMap: [String: Int64] = [:]
        let foodRepo = RepositoryProvider.shared.foodRepository

        // First get existing foods
        do {
            let existingFoods = try await foodRepo.getAllFoodsSnapshot()
            for food in existingFoods {
                foodMap[food.name] = food.id
            }
            print("DataImporter: Found \(existingFoods.count) existing foods")
        } catch {
            print("DataImporter: Failed to get existing foods: \(error)")
        }

        // Import from ingredients.json
        guard let url = Bundle.main.url(forResource: "ingredients", withExtension: "json") else {
            print("DataImporter: ingredients.json not found")
            return foodMap
        }

        do {
            let data = try Data(contentsOf: url)
            let wrapper = try JSONDecoder().decode(SeedFoodWrapper.self, from: data)
            var newCount = 0

            for sf in wrapper.foods {
                // Skip if already exists
                if foodMap[sf.name] != nil {
                    continue
                }

                let food = FoodItem(
                    id: 0,
                    name: sf.name,
                    brand: nil,
                    barcode: nil,
                    caloriesPer100: sf.calories,
                    proteinPer100: sf.protein,
                    carbsPer100: sf.carbs,
                    fatPer100: sf.fat,
                    gramsPerPiece: nil,
                    gramsPerCup: nil,
                    gramsPerTbsp: nil,
                    gramsPerTsp: nil,
                    glycemicIndex: nil,
                    preferredUnit: nil,
                    isFavorite: false,
                    lastUsed: nil,
                    createdAt: Int64(Date().timeIntervalSince1970 * 1000),
                    isSystemFood: true
                )

                do {
                    let id = try await foodRepo.insertFood(food: food)
                    foodMap[sf.name] = id.int64Value
                    newCount += 1
                } catch {
                    print("DataImporter: Failed to insert \(sf.name): \(error)")
                }
            }

            print("DataImporter: Added \(newCount) new foods, total \(foodMap.count)")
        } catch {
            print("DataImporter: Failed to load ingredients.json: \(error)")
        }

        return foodMap
    }

    // MARK: - Import Diets and Meals

    private func importDietsAndMeals(userId: Int64, foodMap: [String: Int64]) async -> (diets: Int, meals: Int) {
        guard let url = Bundle.main.url(forResource: "seed_data", withExtension: "json") else {
            print("DataImporter: seed_data.json not found")
            return (0, 0)
        }

        do {
            let data = try Data(contentsOf: url)
            let seedData = try JSONDecoder().decode(SeedData.self, from: data)

            let dietRepo = RepositoryProvider.shared.dietRepository
            let mealRepo = RepositoryProvider.shared.mealRepository

            // Create tags first
            let tagMap = await createTags(userId: userId)

            var dietsCreated = 0
            var mealsCreated = 0

            for seedDiet in seedData.diets {
                // Create diet
                let now = Int64(Date().timeIntervalSince1970 * 1000)
                let diet = Diet(
                    id: 0,
                    userId: userId,
                    name: seedDiet.name,
                    description: seedDiet.description,
                    createdAt: now,
                    isSystemDiet: false,
                    serverId: nil,
                    updatedAt: now,
                    syncedAt: nil
                )

                do {
                    let dietId = try await dietRepo.insertDiet(diet: diet)

                    // Assign tag
                    if let tagId = tagMap[seedDiet.meal_type] {
                        try? await dietRepo.addTagToDiet(dietId: dietId.int64Value, tagId: tagId)
                    }

                    // Create meals for each slot
                    for (slotType, seedMeal) in seedDiet.meals {
                        let meal = Meal(
                            id: 0,
                            userId: userId,
                            name: seedMeal.name,
                            description: nil,
                            slotType: slotType,
                            customSlotId: nil,
                            createdAt: now,
                            serverId: nil,
                            updatedAt: now,
                            syncedAt: nil
                        )

                        let mealId = try await mealRepo.insertMeal(meal: meal)

                        // Add food items
                        for item in seedMeal.items {
                            if let foodId = foodMap[item.food] {
                                try? await mealRepo.addFoodToMeal(
                                    mealId: mealId.int64Value,
                                    foodId: foodId,
                                    quantity: item.quantity,
                                    unit: parseUnit(item.unit),
                                    notes: nil
                                )
                            }
                        }

                        // Link meal to diet slot
                        try? await dietRepo.setDietMeal(
                            dietId: dietId.int64Value,
                            slotType: slotType,
                            mealId: KotlinLong(value: mealId.int64Value),
                            instructions: nil
                        )

                        mealsCreated += 1
                    }

                    dietsCreated += 1
                } catch {
                    print("DataImporter: Failed to create diet \(seedDiet.name): \(error)")
                }
            }

            print("DataImporter: Imported \(dietsCreated) diets, \(mealsCreated) meals")
            return (dietsCreated, mealsCreated)

        } catch {
            print("DataImporter: Failed to parse seed_data.json: \(error)")
            return (0, 0)
        }
    }

    private func createTags(userId: Int64) async -> [String: Int64] {
        let dietRepo = RepositoryProvider.shared.dietRepository
        var tagMap: [String: Int64] = [:]

        for tagName in ["REMISSION", "MAINTENANCE", "SOS"] {
            let tag = Tag(
                id: 0,
                userId: userId,
                name: tagName.capitalized,
                color: nil,
                createdAt: Int64(Date().timeIntervalSince1970 * 1000)
            )

            do {
                let tagId = try await dietRepo.insertTag(tag: tag)
                tagMap[tagName] = tagId.int64Value
            } catch {
                print("DataImporter: Failed to create tag \(tagName): \(error)")
            }
        }

        return tagMap
    }

    private func parseUnit(_ unitStr: String) -> FoodUnit {
        switch unitStr.lowercased() {
        case "g", "gram", "grams": return .gram
        case "piece", "pieces": return .piece
        case "cup", "cups": return .cup
        case "tbsp": return .tbsp
        case "tsp": return .tsp
        case "ml": return .gram
        default: return .gram
        }
    }
}
