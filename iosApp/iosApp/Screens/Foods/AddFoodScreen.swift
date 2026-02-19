import SwiftUI
import shared

struct AddFoodScreen: View {
    @Environment(\.dismiss) var dismiss
    var onSave: (FoodItemUI) -> Void

    @State private var name = ""
    @State private var calories = ""
    @State private var protein = ""
    @State private var carbs = ""
    @State private var fat = ""
    @State private var fiber = ""
    @State private var sugar = ""
    @State private var servingSize = "100"
    @State private var selectedUnit = "g"

    let units = ["g", "ml", "oz", "cup", "tbsp", "tsp", "piece"]

    var isFormValid: Bool {
        !name.isEmpty && Double(calories) != nil
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Basic Info") {
                    TextField("Food Name", text: $name)

                    HStack {
                        TextField("Serving Size", text: $servingSize)
                            .keyboardType(.decimalPad)
                        Picker("Unit", selection: $selectedUnit) {
                            ForEach(units, id: \.self) { unit in
                                Text(unit).tag(unit)
                            }
                        }
                        .pickerStyle(MenuPickerStyle())
                    }
                }

                Section("Nutrition (per serving)") {
                    NutritionField(label: "Calories", value: $calories, unit: "kcal")
                    NutritionField(label: "Protein", value: $protein, unit: "g")
                    NutritionField(label: "Carbohydrates", value: $carbs, unit: "g")
                    NutritionField(label: "Fat", value: $fat, unit: "g")
                }

                Section("Additional (optional)") {
                    NutritionField(label: "Fiber", value: $fiber, unit: "g")
                    NutritionField(label: "Sugar", value: $sugar, unit: "g")
                }

                Section {
                    Button(action: saveFood) {
                        HStack {
                            Spacer()
                            Text("Save Food")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                    }
                    .disabled(!isFormValid)
                }
            }
            .navigationTitle("Add Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }

    private func saveFood() {
        let food = FoodItemUI(
            id: Int64(Date().timeIntervalSince1970),
            name: name,
            calories: Double(calories) ?? 0,
            protein: Double(protein) ?? 0,
            carbs: Double(carbs) ?? 0,
            fat: Double(fat) ?? 0,
            unit: "\(servingSize)\(selectedUnit)"
        )
        onSave(food)
        dismiss()
    }
}

struct NutritionField: View {
    let label: String
    @Binding var value: String
    let unit: String

    var body: some View {
        HStack {
            Text(label)
            Spacer()
            TextField("0", text: $value)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .frame(width: 80)
            Text(unit)
                .foregroundColor(.secondary)
                .frame(width: 40, alignment: .leading)
        }
    }
}

struct FoodDetailScreen: View {
    let food: FoodItemUI
    @State private var quantity: Double = 1.0

    var scaledCalories: Double { food.calories * quantity }
    var scaledProtein: Double { food.protein * quantity }
    var scaledCarbs: Double { food.carbs * quantity }
    var scaledFat: Double { food.fat * quantity }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header card
                VStack(spacing: 12) {
                    Image(systemName: "leaf.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.green)

                    Text(food.name)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(food.unit)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

                // Quantity selector
                VStack(spacing: 8) {
                    Text("Quantity")
                        .font(.headline)

                    HStack(spacing: 20) {
                        Button(action: { if quantity > 0.5 { quantity -= 0.5 } }) {
                            Image(systemName: "minus.circle.fill")
                                .font(.title)
                                .foregroundColor(.green)
                        }

                        Text(String(format: "%.1f", quantity))
                            .font(.title)
                            .fontWeight(.bold)
                            .frame(width: 60)

                        Button(action: { quantity += 0.5 }) {
                            Image(systemName: "plus.circle.fill")
                                .font(.title)
                                .foregroundColor(.green)
                        }
                    }
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

                // Nutrition info
                VStack(spacing: 16) {
                    Text("Nutrition Facts")
                        .font(.headline)

                    NutritionRow(label: "Calories", value: "\(Int(scaledCalories))", unit: "kcal", color: .green)
                    Divider()
                    NutritionRow(label: "Protein", value: String(format: "%.1f", scaledProtein), unit: "g", color: .red)
                    Divider()
                    NutritionRow(label: "Carbohydrates", value: String(format: "%.1f", scaledCarbs), unit: "g", color: .blue)
                    Divider()
                    NutritionRow(label: "Fat", value: String(format: "%.1f", scaledFat), unit: "g", color: .yellow)
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

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
        .navigationTitle("Food Details")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct NutritionRow: View {
    let label: String
    let value: String
    let unit: String
    let color: Color

    var body: some View {
        HStack {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)
            Text(label)
            Spacer()
            Text(value)
                .fontWeight(.semibold)
            Text(unit)
                .foregroundColor(.secondary)
        }
    }
}

struct AddFoodScreen_Previews: PreviewProvider {
    static var previews: some View {
        AddFoodScreen { _ in }
    }
}
