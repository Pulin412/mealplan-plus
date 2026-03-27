import SwiftUI
import shared

struct AddMealScreen: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var appState: AppState
    var existingMeal: MealWithFoods? = nil   // nil = create, non-nil = edit
    var onSave: () -> Void

    @State private var name = ""
    @State private var selectedSlot = "Lunch"
    @State private var selectedFoods: [FoodItemUI] = []
    @State private var showFoodPicker = false
    @StateObject private var mealsVM = MealsViewModel()
    @State private var isSaving = false

    let slots = ["Breakfast", "Lunch", "Dinner", "Snack",
                 "Pre-Workout", "Post-Workout", "Evening Snack", "Early Morning"]

    private var isEditing: Bool { existingMeal != nil }
    private var userId: Int64 { appState.currentUserId ?? 0 }

    var totalCalories: Int   { selectedFoods.reduce(0) { $0 + $1.calories } }
    var totalProtein: Double { selectedFoods.reduce(0) { $0 + $1.protein } }
    var totalCarbs: Double   { selectedFoods.reduce(0) { $0 + $1.carbs } }
    var totalFat: Double     { selectedFoods.reduce(0) { $0 + $1.fat } }
    var isFormValid: Bool    { !name.isEmpty && !selectedFoods.isEmpty }

    var body: some View {
        NavigationStack {
            Form {
                Section("Meal Info") {
                    TextField("Meal Name", text: $name)
                    Picker("Meal Slot", selection: $selectedSlot) {
                        ForEach(slots, id: \.self) { slot in Text(slot).tag(slot) }
                    }
                }

                Section {
                    Button(action: { showFoodPicker = true }) {
                        HStack {
                            Image(systemName: "plus.circle.fill").foregroundColor(.green)
                            Text("Add Foods")
                        }
                    }
                    ForEach(selectedFoods) { food in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(food.name).font(.subheadline)
                                Text(food.unit).font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(Int(food.calories)) kcal").font(.caption).foregroundColor(.secondary)
                        }
                    }
                    .onDelete { indexSet in selectedFoods.remove(atOffsets: indexSet) }
                } header: {
                    Text("Foods (\(selectedFoods.count))")
                }

                if !selectedFoods.isEmpty {
                    Section("Total Nutrition") {
                        HStack { Text("Calories"); Spacer(); Text("\(totalCalories) kcal").fontWeight(.semibold) }
                        HStack { Text("Protein");  Spacer(); Text("\(Int(totalProtein)) g") }
                        HStack { Text("Carbs");    Spacer(); Text("\(Int(totalCarbs)) g") }
                        HStack { Text("Fat");      Spacer(); Text("\(Int(totalFat)) g") }
                    }
                }

                Section {
                    Button(action: saveMeal) {
                        HStack {
                            Spacer()
                            if isSaving {
                                ProgressView().progressViewStyle(CircularProgressViewStyle()).padding(.trailing, 4)
                            }
                            Text(isEditing ? "Save Changes" : "Save Meal").fontWeight(.semibold)
                            Spacer()
                        }
                    }
                    .disabled(!isFormValid || isSaving)
                }
            }
            .navigationTitle(isEditing ? "Edit Meal" : "Create Meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { Button("Cancel") { dismiss() } }
            }
            .sheet(isPresented: $showFoodPicker) {
                FoodPickerScreen { food in selectedFoods.append(food) }
            }
            .onAppear { populateIfEditing() }
        }
    }

    private func populateIfEditing() {
        guard let mwf = existingMeal else { return }
        name = mwf.meal.name
        selectedSlot = slotDisplayName(mwf.meal.slotType)
        let items = (mwf.items as? NSArray)?.compactMap { $0 as? MealFoodItemWithDetails } ?? []
        selectedFoods = items.map { item in
            let qty = item.mealFoodItem.quantity
            let unitName = item.mealFoodItem.unit.name
            let formatted = qty == floor(qty) ? "\(Int(qty))" : String(format: "%.1f", qty)
            let unitDisplay: String
            switch unitName {
            case "GRAM":    unitDisplay = "\(formatted)g"
            case "ML":      unitDisplay = "\(formatted)ml"
            case "SERVING": unitDisplay = "\(formatted) srv"
            case "PIECE":   unitDisplay = "\(formatted) pcs"
            default:        unitDisplay = "\(formatted) \(unitName.lowercased())"
            }
            return FoodItemUI(
                id: item.food.id,
                name: item.food.name,
                calories: Int(item.calculatedCalories),
                protein: item.calculatedProtein,
                carbs: item.calculatedCarbs,
                fat: item.calculatedFat,
                unit: unitDisplay,
                quantity: qty,
                unitKmpName: unitName
            )
        }
    }

    private func foodUnitFromKmpName(_ name: String) -> FoodUnit {
        switch name {
        case "ML":      return .ml
        case "SERVING": return .serving
        case "PIECE":   return .piece
        case "CUP":     return .cup
        case "TBSP":    return .tbsp
        case "TSP":     return .tsp
        case "SLICE":   return .slice
        case "SCOOP":   return .scoop
        default:        return .gram
        }
    }

    private func slotDisplayName(_ slotType: String) -> String {
        switch slotType.uppercased() {
        case "BREAKFAST":     return "Breakfast"
        case "LUNCH":         return "Lunch"
        case "DINNER":        return "Dinner"
        case "PRE_WORKOUT":   return "Pre-Workout"
        case "POST_WORKOUT":  return "Post-Workout"
        case "EVENING_SNACK": return "Evening Snack"
        case "EARLY_MORNING": return "Early Morning"
        default:              return "Snack"
        }
    }

    private func kmpSlotType(from displayName: String) -> String {
        switch displayName {
        case "Breakfast":     return "BREAKFAST"
        case "Lunch":         return "LUNCH"
        case "Dinner":        return "DINNER"
        case "Pre-Workout":   return "PRE_WORKOUT"
        case "Post-Workout":  return "POST_WORKOUT"
        case "Evening Snack": return "EVENING_SNACK"
        case "Early Morning": return "EARLY_MORNING"
        default:              return "SNACK"
        }
    }

    private func saveMeal() {
        guard !isSaving else { return }
        isSaving = true
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let slot = kmpSlotType(from: selectedSlot)
        Task {
            if let existing = existingMeal {
                let updated = Meal(
                    id: existing.meal.id, userId: userId, name: name,
                    description: existing.meal.description_,
                    slotType: slot, customSlotId: existing.meal.customSlotId,
                    createdAt: existing.meal.createdAt, serverId: existing.meal.serverId,
                    updatedAt: now, syncedAt: existing.meal.syncedAt
                )
                try? await mealsVM.updateMeal(updated)
                let mealId = existing.meal.id
                let oldItems = (existing.items as? NSArray)?.compactMap { $0 as? MealFoodItemWithDetails } ?? []
                for item in oldItems {
                    try? await mealsVM.removeFoodFromMeal(mealId: mealId, foodId: item.food.id)
                }
                for food in selectedFoods {
                    try? await mealsVM.addFoodToMeal(mealId: mealId, foodId: food.id, quantity: food.quantity, unit: foodUnitFromKmpName(food.unitKmpName), notes: nil)
                }
            } else {
                let meal = Meal(
                    id: 0, userId: userId, name: name, description: nil,
                    slotType: slot, customSlotId: nil,
                    createdAt: now, serverId: nil, updatedAt: now, syncedAt: nil
                )
                let mealId = try? await mealsVM.insertMeal(meal)
                if let id = mealId {
                    for food in selectedFoods {
                        try? await mealsVM.addFoodToMeal(mealId: id, foodId: food.id, quantity: food.quantity, unit: foodUnitFromKmpName(food.unitKmpName), notes: nil)
                    }
                }
            }
            await MainActor.run {
                isSaving = false
                onSave()
                dismiss()
            }
        }
    }
}

// MARK: - Meal Food Unit (iOS-side enum, maps to KMP FoodUnit)

enum MealFoodUnit: String, CaseIterable {
    case gram    = "GRAM"
    case ml      = "ML"
    case serving = "SERVING"
    case piece   = "PIECE"

    var displayName: String {
        switch self {
        case .gram:    return "Grams"
        case .ml:      return "ml"
        case .serving: return "Servings"
        case .piece:   return "Pieces"
        }
    }

    var shortLabel: String {
        switch self {
        case .gram:    return "g"
        case .ml:      return "ml"
        case .serving: return "srv"
        case .piece:   return "pcs"
        }
    }

    var kmpName: String { rawValue }
}

// MARK: - Food Picker (uses real KMP database)
struct FoodPickerScreen: View {
    @Environment(\.dismiss) var dismiss
    var onSelect: (FoodItemUI) -> Void

    @StateObject private var foodsVM = FoodsViewModel()
    @State private var searchText = ""
    @State private var pendingFood: FoodItem? = nil

    var filteredFoods: [FoodItem] {
        if searchText.isEmpty { return foodsVM.foods }
        return foodsVM.foods.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "magnifyingglass").foregroundColor(.gray)
                    TextField("Search foods...", text: $searchText)
                        .onChange(of: searchText) { q in
                            if q.isEmpty { foodsVM.loadFoods() } else { foodsVM.searchFoods(query: q) }
                        }
                }
                .padding(12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()

                if foodsVM.isLoading {
                    Spacer(); ProgressView("Loading foods..."); Spacer()
                } else if filteredFoods.isEmpty {
                    Spacer()
                    Text(searchText.isEmpty ? "No foods available" : "No results for \"\(searchText)\"")
                        .foregroundColor(.secondary)
                    Spacer()
                } else {
                    List(filteredFoods, id: \.id) { food in
                        Button(action: { pendingFood = food }) {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(food.name).font(.headline).foregroundColor(.primary)
                                    Text("\(Int(food.caloriesPer100)) kcal/100g · P:\(Int(food.proteinPer100))g C:\(Int(food.carbsPer100))g")
                                        .font(.caption).foregroundColor(.secondary)
                                }
                                Spacer()
                                Text("\(Int(food.caloriesPer100)) kcal").foregroundColor(.secondary)
                            }
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("Select Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { Button("Cancel") { dismiss() } }
            }
            .sheet(isPresented: Binding(
                get: { pendingFood != nil },
                set: { if !$0 { pendingFood = nil } }
            )) {
                if let food = pendingFood {
                    FoodQuantitySheetView(food: food) { foodUI in
                        onSelect(foodUI)
                        pendingFood = nil
                        dismiss()
                    }
                }
            }
            .onAppear { foodsVM.loadFoods() }
        }
    }
}

// MARK: - Food Quantity + Unit Picker Sheet

private struct FoodQuantitySheetView: View {
    @Environment(\.dismiss) var dismiss
    let food: FoodItem
    var onConfirm: (FoodItemUI) -> Void

    @State private var quantity: Double = 100.0
    @State private var quantityText: String = "100"
    @State private var selectedUnit: MealFoodUnit = .gram

    private var gramsPerPiece: Double { food.gramsPerPiece ?? 100.0 }

    private var multiplier: Double {
        switch selectedUnit {
        case .gram:    return quantity / 100.0
        case .ml:      return quantity / 100.0
        case .serving: return quantity
        case .piece:   return quantity * gramsPerPiece / 100.0
        }
    }

    private var calcCalories: Double { multiplier * food.caloriesPer100 }
    private var calcProtein:  Double { multiplier * food.proteinPer100 }
    private var calcCarbs:    Double { multiplier * food.carbsPer100 }
    private var calcFat:      Double { multiplier * food.fatPer100 }

    private var quickValues: [Double] {
        switch selectedUnit {
        case .gram:    return [50, 100, 150, 200]
        case .ml:      return [50, 100, 200, 250]
        case .serving: return [0.5, 1, 1.5, 2]
        case .piece:   return [1, 2, 3, 4]
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(food.name).font(.headline)
                    Text("\(Int(food.caloriesPer100)) kcal / 100g · P:\(Int(food.proteinPer100))g C:\(Int(food.carbsPer100))g F:\(Int(food.fatPer100))g")
                        .font(.caption).foregroundColor(.secondary)
                }

                Section("Unit") {
                    Picker("Unit", selection: $selectedUnit) {
                        ForEach(MealFoodUnit.allCases, id: \.self) { u in
                            Text(u.displayName).tag(u)
                        }
                    }
                    .pickerStyle(.segmented)
                    .onChange(of: selectedUnit) { _ in
                        switch selectedUnit {
                        case .gram:    setQty(100)
                        case .ml:      setQty(100)
                        case .serving: setQty(1)
                        case .piece:   setQty(1)
                        }
                    }
                }

                Section("Quantity") {
                    HStack {
                        TextField("Amount", text: $quantityText)
                            .keyboardType(.decimalPad)
                            .onChange(of: quantityText) { val in
                                if let d = Double(val), d > 0 { quantity = d }
                            }
                        Text(selectedUnit.shortLabel).foregroundColor(.secondary)
                    }
                    if selectedUnit == .piece {
                        Text("1 piece ≈ \(Int(gramsPerPiece))g")
                            .font(.caption).foregroundColor(.secondary)
                    }
                    // Quick-select chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(quickValues, id: \.self) { val in
                                Button(action: { setQty(val) }) {
                                    let lbl = val == floor(val) ? "\(Int(val))" : String(format: "%.1f", val)
                                    Text(lbl)
                                        .font(.caption)
                                        .padding(.horizontal, 12).padding(.vertical, 6)
                                        .background(quantity == val ? Color.green : Color(.systemGray5))
                                        .foregroundColor(quantity == val ? .white : .primary)
                                        .cornerRadius(14)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                }

                Section("Nutrition Preview") {
                    HStack {
                        FoodQtyMacroTile(label: "Calories", value: "\(Int(calcCalories))", unit: "kcal", color: .green)
                        FoodQtyMacroTile(label: "Protein",  value: "\(Int(calcProtein))",  unit: "g",    color: .blue)
                        FoodQtyMacroTile(label: "Carbs",    value: "\(Int(calcCarbs))",    unit: "g",    color: .orange)
                        FoodQtyMacroTile(label: "Fat",      value: "\(Int(calcFat))",      unit: "g",    color: .pink)
                    }
                }

                Section {
                    Button(action: confirm) {
                        HStack {
                            Spacer()
                            Text("Add to Meal").fontWeight(.semibold)
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Add Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { Button("Cancel") { dismiss() } }
            }
        }
    }

    private func setQty(_ val: Double) {
        quantity = val
        quantityText = val == floor(val) ? "\(Int(val))" : String(format: "%.1f", val)
    }

    private func confirm() {
        let qStr = quantity == floor(quantity) ? "\(Int(quantity))" : String(format: "%.1f", quantity)
        let displayUnit: String
        switch selectedUnit {
        case .gram:    displayUnit = "\(qStr)g"
        case .ml:      displayUnit = "\(qStr)ml"
        case .serving: displayUnit = "\(qStr) srv"
        case .piece:   displayUnit = "\(qStr) pcs"
        }
        let foodUI = FoodItemUI(
            id: food.id, name: food.name,
            calories: Int(calcCalories), protein: calcProtein, carbs: calcCarbs, fat: calcFat,
            unit: displayUnit, quantity: quantity, unitKmpName: selectedUnit.kmpName
        )
        onConfirm(foodUI)
        dismiss()
    }
}

private struct FoodQtyMacroTile: View {
    let label: String; let value: String; let unit: String; let color: Color
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.subheadline).fontWeight(.bold).foregroundColor(color)
            Text(unit).font(.caption2).foregroundColor(.secondary)
            Text(label).font(.caption2).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
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
            Color(.systemGroupedBackground).ignoresSafeArea()
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
        .background(Color(.systemBackground))
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
        .background(Color(.systemBackground))
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
        .background(Color(.systemBackground))
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
        case "GRAM":    return "g"
        case "ML":      return "ml"
        case "SERVING": return "srv"
        case "PIECE":   return "pcs"
        case "CUP":     return "cup"
        case "TBSP":    return "tbsp"
        case "TSP":     return "tsp"
        case "SLICE":   return "slice"
        case "SCOOP":   return "scoop"
        default:        return unit.name.lowercased()
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
        AddMealScreen(onSave: {})
            .environmentObject(AppState())
    }
}
