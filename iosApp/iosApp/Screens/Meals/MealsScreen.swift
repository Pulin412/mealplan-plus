import SwiftUI
import shared

struct MealsScreen: View {
    @State private var searchText = ""
    @State private var meals: [MealUI] = []
    @State private var selectedSlot: MealSlotFilter = .all
    @State private var showAddMeal = false

    var filteredMeals: [MealUI] {
        var result = meals

        if !searchText.isEmpty {
            result = result.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
        }

        if selectedSlot != .all {
            result = result.filter { $0.slot == selectedSlot.rawValue }
        }

        return result
    }

    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [Color.green.opacity(0.2), Color.white]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Search meals...", text: $searchText)
                    if !searchText.isEmpty {
                        Button(action: { searchText = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.gray)
                        }
                    }
                }
                .padding(12)
                .background(Color.white)
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top)

                // Slot filter
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(MealSlotFilter.allCases, id: \.self) { slot in
                            FilterChip(
                                title: slot.displayName,
                                isSelected: selectedSlot == slot
                            ) {
                                selectedSlot = slot
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 12)
                }

                if filteredMeals.isEmpty {
                    EmptyMealsView()
                } else {
                    List {
                        ForEach(filteredMeals) { meal in
                            NavigationLink(destination: MealDetailScreen(meal: meal)) {
                                MealRowView(meal: meal)
                            }
                        }
                        .onDelete(perform: deleteMeal)
                    }
                    .listStyle(PlainListStyle())
                }
            }
        }
        .navigationTitle("My Meals")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddMeal = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddMeal) {
            AddMealScreen { newMeal in
                meals.append(newMeal)
            }
        }
        .onAppear {
            loadSampleMeals()
        }
    }

    private func deleteMeal(at offsets: IndexSet) {
        meals.remove(atOffsets: offsets)
    }

    private func loadSampleMeals() {
        meals = SeedDataLoader.shared.loadMeals()
    }
}

enum MealSlotFilter: String, CaseIterable {
    case all = "All"
    case breakfast = "Breakfast"
    case lunch = "Lunch"
    case dinner = "Dinner"
    case snack = "Snack"

    var displayName: String { rawValue }
}

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? Color.green : Color.white)
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(Color.green, lineWidth: isSelected ? 0 : 1)
                )
        }
    }
}

struct MealRowView: View {
    let meal: MealUI

    var body: some View {
        HStack {
            // Slot icon
            Image(systemName: slotIcon(for: meal.slot))
                .foregroundColor(.green)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 4) {
                Text(meal.name)
                    .font(.headline)
                HStack {
                    Text(meal.slot)
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Color.green.opacity(0.2))
                        .cornerRadius(4)
                    Text("\(meal.foodCount) foods")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text("\(meal.calories) kcal")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                HStack(spacing: 4) {
                    Text("P:\(Int(meal.protein))")
                    Text("C:\(Int(meal.carbs))")
                    Text("F:\(Int(meal.fat))")
                }
                .font(.caption2)
                .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private func slotIcon(for slot: String) -> String {
        switch slot {
        case "Breakfast": return "sunrise.fill"
        case "Lunch": return "sun.max.fill"
        case "Dinner": return "moon.fill"
        default: return "leaf.fill"
        }
    }
}

struct EmptyMealsView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "fork.knife")
                .font(.system(size: 60))
                .foregroundColor(.green.opacity(0.5))
            Text("No meals yet")
                .font(.headline)
            Text("Create your first meal by combining foods")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

// UI Model
struct MealUI: Identifiable {
    let id: Int64
    let name: String
    let slot: String
    let calories: Int
    let protein: Double
    let carbs: Double
    let fat: Double
    let foodCount: Int
}

struct MealsScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            MealsScreen()
        }
    }
}
