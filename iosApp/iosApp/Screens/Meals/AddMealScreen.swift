import SwiftUI
import shared

struct AddMealScreen: View {
    @Environment(\.dismiss) var dismiss
    var onSave: (MealUI) -> Void

    @State private var name = ""
    @State private var selectedSlot = "Lunch"
    @State private var selectedFoods: [FoodItemUI] = []
    @State private var showFoodPicker = false

    let slots = ["Breakfast", "Lunch", "Dinner", "Snack"]

    var totalCalories: Int {
        selectedFoods.reduce(0) { $0 + $1.calories }
    }

    var totalProtein: Double {
        selectedFoods.reduce(0) { $0 + $1.protein }
    }

    var totalCarbs: Double {
        selectedFoods.reduce(0) { $0 + $1.carbs }
    }

    var totalFat: Double {
        selectedFoods.reduce(0) { $0 + $1.fat }
    }

    var isFormValid: Bool {
        !name.isEmpty && !selectedFoods.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Meal Info") {
                    TextField("Meal Name", text: $name)

                    Picker("Meal Slot", selection: $selectedSlot) {
                        ForEach(slots, id: \.self) { slot in
                            Text(slot).tag(slot)
                        }
                    }
                }

                Section {
                    Button(action: { showFoodPicker = true }) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                                .foregroundColor(.green)
                            Text("Add Foods")
                        }
                    }

                    ForEach(selectedFoods) { food in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(food.name)
                                    .font(.subheadline)
                                Text(food.unit)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(Int(food.calories)) kcal")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .onDelete { indexSet in
                        selectedFoods.remove(atOffsets: indexSet)
                    }
                } header: {
                    Text("Foods (\(selectedFoods.count))")
                }

                if !selectedFoods.isEmpty {
                    Section("Total Nutrition") {
                        HStack {
                            Text("Calories")
                            Spacer()
                            Text("\(Int(totalCalories)) kcal")
                                .fontWeight(.semibold)
                        }
                        HStack {
                            Text("Protein")
                            Spacer()
                            Text("\(Int(totalProtein)) g")
                        }
                        HStack {
                            Text("Carbs")
                            Spacer()
                            Text("\(Int(totalCarbs)) g")
                        }
                        HStack {
                            Text("Fat")
                            Spacer()
                            Text("\(Int(totalFat)) g")
                        }
                    }
                }

                Section {
                    Button(action: saveMeal) {
                        HStack {
                            Spacer()
                            Text("Save Meal")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                    }
                    .disabled(!isFormValid)
                }
            }
            .navigationTitle("Create Meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showFoodPicker) {
                FoodPickerScreen { food in
                    selectedFoods.append(food)
                }
            }
        }
    }

    private func saveMeal() {
        let meal = MealUI(
            id: Int64(Date().timeIntervalSince1970),
            name: name,
            slot: selectedSlot,
            calories: totalCalories,
            protein: totalProtein,
            carbs: totalCarbs,
            fat: totalFat,
            foodCount: selectedFoods.count
        )
        onSave(meal)
        dismiss()
    }
}

struct FoodPickerScreen: View {
    @Environment(\.dismiss) var dismiss
    var onSelect: (FoodItemUI) -> Void

    @State private var searchText = ""
    @State private var availableFoods: [FoodItemUI] = []

    var filteredFoods: [FoodItemUI] {
        if searchText.isEmpty {
            return availableFoods
        }
        return availableFoods.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        NavigationStack {
            VStack {
                // Search
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Search foods...", text: $searchText)
                }
                .padding(12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()

                List(filteredFoods) { food in
                    Button(action: {
                        onSelect(food)
                        dismiss()
                    }) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(food.name)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Text(food.unit)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(Int(food.calories)) kcal")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
            .navigationTitle("Select Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                loadFoods()
            }
        }
    }

    private func loadFoods() {
        availableFoods = [
            FoodItemUI(id: 1, name: "Chicken Breast", calories: 165, protein: 31, carbs: 0, fat: 3.6, unit: "100g"),
            FoodItemUI(id: 2, name: "Brown Rice", calories: 112, protein: 2.6, carbs: 24, fat: 0.9, unit: "100g"),
            FoodItemUI(id: 3, name: "Broccoli", calories: 34, protein: 2.8, carbs: 7, fat: 0.4, unit: "100g"),
            FoodItemUI(id: 4, name: "Salmon", calories: 208, protein: 20, carbs: 0, fat: 13, unit: "100g"),
            FoodItemUI(id: 5, name: "Eggs", calories: 155, protein: 13, carbs: 1.1, fat: 11, unit: "100g"),
            FoodItemUI(id: 6, name: "Oatmeal", calories: 68, protein: 2.4, carbs: 12, fat: 1.4, unit: "100g"),
            FoodItemUI(id: 7, name: "Greek Yogurt", calories: 59, protein: 10, carbs: 3.6, fat: 0.7, unit: "100g"),
            FoodItemUI(id: 8, name: "Banana", calories: 89, protein: 1.1, carbs: 23, fat: 0.3, unit: "1 medium"),
        ]
    }
}

struct MealDetailScreen: View {
    let meal: MealUI

    @StateObject private var mealsVM = MealsViewModel()
    @State private var mealWithFoods: MealWithFoods?
    @State private var isLoadingFoods = true

    var foodItems: [MealFoodItemWithDetails] {
        guard let mwf = mealWithFoods else { return [] }
        return (mwf.items as? NSArray)?.compactMap { $0 as? MealFoodItemWithDetails } ?? []
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                mealHeaderCard
                mealNutritionCard
                ingredientsCard
                Spacer()
            }
            .padding(.top)
        }
        .background(
            LinearGradient(
                gradient: Gradient(colors: [Color.green.opacity(0.2), Color.white]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
        .navigationTitle("Meal Details")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { loadMealDetails() }
    }

    private var mealHeaderCard: some View {
        VStack(spacing: 12) {
            Image(systemName: "fork.knife.circle.fill")
                .font(.system(size: 50))
                .foregroundColor(.green)
            Text(meal.name).font(.title2).fontWeight(.bold)
            Text(meal.slot)
                .font(.subheadline)
                .padding(.horizontal, 12).padding(.vertical, 4)
                .background(Color.green.opacity(0.2))
                .cornerRadius(8)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5)
        .padding(.horizontal)
    }

    private var mealNutritionCard: some View {
        VStack(spacing: 16) {
            Text("Nutrition Summary").font(.headline)
            HStack(spacing: 20) {
                NutritionCircle(value: Double(meal.calories), label: "kcal", color: .green)
                NutritionCircle(value: meal.protein, label: "Protein", color: .red)
                NutritionCircle(value: meal.carbs, label: "Carbs", color: .blue)
                NutritionCircle(value: meal.fat, label: "Fat", color: .yellow)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5)
        .padding(.horizontal)
    }

    private var ingredientsCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Ingredients")
                .font(.headline)
                .padding(.horizontal)
                .padding(.bottom, 8)
            ingredientsContent
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5)
        .padding(.horizontal)
    }

    @ViewBuilder
    private var ingredientsContent: some View {
        if isLoadingFoods {
            HStack {
                ProgressView()
                Text("Loading ingredients...").font(.caption).foregroundColor(.secondary)
            }
            .padding()
        } else if foodItems.isEmpty {
            Text("No ingredients listed")
                .font(.caption).foregroundColor(.secondary).padding()
        } else {
            ForEach(foodItems, id: \.food.id) { item in
                IngredientRow(item: item, unitDisplay: unitDisplay)
                if item.food.id != foodItems.last?.food.id {
                    Divider().padding(.horizontal)
                }
            }
        }
    }

    private func loadMealDetails() {
        Task {
            mealWithFoods = try? await mealsVM.getMealWithFoods(mealId: meal.id)
            isLoadingFoods = false
        }
    }

    private func unitDisplay(_ unit: FoodUnit) -> String {
        switch unit.name {
        case "GRAM":  return "g"
        case "ML":    return "ml"
        case "PIECE": return "pc"
        case "CUP":   return "cup"
        case "TBSP":  return "tbsp"
        case "TSP":   return "tsp"
        case "SLICE": return "slice"
        case "SCOOP": return "scoop"
        default:      return unit.name.lowercased()
        }
    }
}

struct NutritionCircle: View {
    let value: Double
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .stroke(color.opacity(0.3), lineWidth: 4)
                    .frame(width: 60, height: 60)
                Circle()
                    .fill(color.opacity(0.1))
                    .frame(width: 56, height: 56)
                Text("\(Int(value))")
                    .font(.headline)
                    .fontWeight(.bold)
            }
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

struct IngredientRow: View {
    let item: MealFoodItemWithDetails
    let unitDisplay: (FoodUnit) -> String

    private var quantityText: String {
        let qty = item.mealFoodItem.quantity
        let formatted = qty.truncatingRemainder(dividingBy: 1) == 0
            ? String(format: "%.0f", qty)
            : String(format: "%.1f", qty)
        return "\(formatted) \(unitDisplay(item.mealFoodItem.unit))"
    }

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.food.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(quantityText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("\(Int(item.calculatedCalories)) kcal")
                    .font(.caption)
                    .fontWeight(.semibold)
                HStack(spacing: 6) {
                    Text("P:\(Int(item.calculatedProtein))g")
                    Text("C:\(Int(item.calculatedCarbs))g")
                    Text("F:\(Int(item.calculatedFat))g")
                }
                .font(.caption2)
                .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}

struct AddMealScreen_Previews: PreviewProvider {
    static var previews: some View {
        AddMealScreen { _ in }
    }
}
