import SwiftUI
import shared

struct DailyLogScreen: View {
    let date: Date
    @State private var loggedMeals: [LoggedMealUI] = []
    @State private var showMealPicker = false
    @State private var selectedSlot: String = "Breakfast"

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMM d, yyyy"
        return formatter
    }

    var totalCalories: Double {
        loggedMeals.reduce(0) { $0 + $1.calories }
    }

    var totalProtein: Double {
        loggedMeals.reduce(0) { $0 + $1.protein }
    }

    var totalCarbs: Double {
        loggedMeals.reduce(0) { $0 + $1.carbs }
    }

    var totalFat: Double {
        loggedMeals.reduce(0) { $0 + $1.fat }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Date header
                Text(dateFormatter.string(from: date))
                    .font(.headline)
                    .foregroundColor(.secondary)
                    .padding(.top)

                // Daily summary
                VStack(spacing: 16) {
                    Text("Daily Summary")
                        .font(.headline)

                    HStack(spacing: 20) {
                        DailyStat(value: totalCalories, label: "Calories", color: .green)
                        DailyStat(value: totalProtein, label: "Protein", color: .red)
                        DailyStat(value: totalCarbs, label: "Carbs", color: .blue)
                        DailyStat(value: totalFat, label: "Fat", color: .yellow)
                    }
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

                // Meal slots
                ForEach(["Breakfast", "Lunch", "Dinner", "Snacks"], id: \.self) { slot in
                    MealSlotSection(
                        slot: slot,
                        meals: loggedMeals.filter { $0.slot == slot },
                        onAddTapped: {
                            selectedSlot = slot
                            showMealPicker = true
                        },
                        onRemove: { meal in
                            loggedMeals.removeAll { $0.id == meal.id }
                        }
                    )
                }

                Spacer().frame(height: 20)
            }
        }
        .background(
            LinearGradient(
                gradient: Gradient(colors: [Color.green.opacity(0.2), Color.white]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
        .navigationTitle("Daily Log")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showMealPicker) {
            LogMealPickerScreen(slot: selectedSlot) { meal in
                loggedMeals.append(meal)
            }
        }
        .onAppear {
            loadSampleData()
        }
    }

    private func loadSampleData() {
        loggedMeals = [
            LoggedMealUI(id: 1, name: "Oatmeal with Berries", slot: "Breakfast", calories: 350, protein: 12, carbs: 58, fat: 8, quantity: 1.0),
            LoggedMealUI(id: 2, name: "Protein Smoothie", slot: "Snacks", calories: 280, protein: 25, carbs: 30, fat: 6, quantity: 1.0),
        ]
    }
}

struct DailyStat: View {
    let value: Double
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text("\(Int(value))")
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(color)
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct MealSlotSection: View {
    let slot: String
    let meals: [LoggedMealUI]
    let onAddTapped: () -> Void
    let onRemove: (LoggedMealUI) -> Void

    var slotCalories: Double {
        meals.reduce(0) { $0 + $1.calories }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: slotIcon)
                    .foregroundColor(.green)
                Text(slot)
                    .font(.headline)
                Spacer()
                Text("\(Int(slotCalories)) kcal")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)

            if meals.isEmpty {
                Button(action: onAddTapped) {
                    HStack {
                        Image(systemName: "plus.circle")
                        Text("Add \(slot)")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.white)
                    .foregroundColor(.green)
                    .cornerRadius(10)
                }
                .padding(.horizontal)
            } else {
                ForEach(meals) { meal in
                    LoggedMealRow(meal: meal, onRemove: { onRemove(meal) })
                }
                .padding(.horizontal)

                Button(action: onAddTapped) {
                    HStack {
                        Image(systemName: "plus")
                        Text("Add more")
                    }
                    .font(.subheadline)
                    .foregroundColor(.green)
                }
                .padding(.horizontal)
            }
        }
        .padding(.vertical, 8)
    }

    var slotIcon: String {
        switch slot {
        case "Breakfast": return "sunrise.fill"
        case "Lunch": return "sun.max.fill"
        case "Dinner": return "moon.fill"
        default: return "leaf.fill"
        }
    }
}

struct LoggedMealRow: View {
    let meal: LoggedMealUI
    let onRemove: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(meal.name)
                    .font(.subheadline)
                HStack(spacing: 8) {
                    Text("P: \(Int(meal.protein))g")
                    Text("C: \(Int(meal.carbs))g")
                    Text("F: \(Int(meal.fat))g")
                }
                .font(.caption2)
                .foregroundColor(.secondary)
            }

            Spacer()

            Text("\(Int(meal.calories)) kcal")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.red.opacity(0.7))
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(10)
    }
}

struct LogMealPickerScreen: View {
    @Environment(\.dismiss) var dismiss
    let slot: String
    var onSelect: (LoggedMealUI) -> Void

    @State private var meals: [MealUI] = []
    @State private var searchText = ""

    var filteredMeals: [MealUI] {
        if searchText.isEmpty {
            return meals
        }
        return meals.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        NavigationStack {
            VStack {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Search meals...", text: $searchText)
                }
                .padding(12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()

                List(filteredMeals) { meal in
                    Button(action: { selectMeal(meal) }) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(meal.name)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Text("\(meal.slot) • \(meal.foodCount) foods")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(Int(meal.calories)) kcal")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }
            .navigationTitle("Log \(slot)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                loadMeals()
            }
        }
    }

    private func selectMeal(_ meal: MealUI) {
        let logged = LoggedMealUI(
            id: Int64(Date().timeIntervalSince1970),
            name: meal.name,
            slot: slot,
            calories: meal.calories,
            protein: meal.protein,
            carbs: meal.carbs,
            fat: meal.fat,
            quantity: 1.0
        )
        onSelect(logged)
        dismiss()
    }

    private func loadMeals() {
        meals = [
            MealUI(id: 1, name: "Chicken & Rice Bowl", slot: "Lunch", calories: 550, protein: 45, carbs: 55, fat: 12, foodCount: 3),
            MealUI(id: 2, name: "Oatmeal with Berries", slot: "Breakfast", calories: 350, protein: 12, carbs: 58, fat: 8, foodCount: 4),
            MealUI(id: 3, name: "Grilled Salmon Dinner", slot: "Dinner", calories: 620, protein: 42, carbs: 35, fat: 28, foodCount: 4),
            MealUI(id: 4, name: "Protein Smoothie", slot: "Snack", calories: 280, protein: 25, carbs: 30, fat: 6, foodCount: 5),
        ]
    }
}

// UI Model
struct LoggedMealUI: Identifiable {
    let id: Int64
    let name: String
    let slot: String
    let calories: Double
    let protein: Double
    let carbs: Double
    let fat: Double
    let quantity: Double
}

struct DailyLogScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            DailyLogScreen(date: Date())
        }
    }
}
