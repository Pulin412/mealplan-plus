import SwiftUI
import shared

// Wrapper to make MealWithFoods usable with .sheet(item:)
private struct EditMealRequest: Identifiable {
    let id = UUID()
    let mealWithFoods: MealWithFoods
}

struct MealsScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var mealsVM = MealsViewModel()

    @State private var searchText = ""
    @State private var selectedSlot: MealSlotFilter = .all
    @State private var showAddMeal = false
    @State private var editRequest: EditMealRequest? = nil

    private var userId: Int64 { appState.currentUserId ?? 0 }

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

    private func toMealUI(_ meal: Meal) -> MealUI {
        MealUI(id: meal.id, name: meal.name, slot: slotDisplayName(meal.slotType),
               calories: 0, protein: 0, carbs: 0, fat: 0, foodCount: 0)
    }

    var filteredMeals: [Meal] {
        var result = mealsVM.meals
        if !searchText.isEmpty {
            result = result.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
        }
        if selectedSlot != .all {
            let target = selectedSlot.rawValue.uppercased()
            result = result.filter { $0.slotType.uppercased() == target || slotDisplayName($0.slotType) == selectedSlot.rawValue }
        }
        return result
    }

    var body: some View {
        ZStack {
            Color(.systemGroupedBackground).ignoresSafeArea()

            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass").foregroundColor(.gray)
                    TextField("Search meals...", text: $searchText)
                    if !searchText.isEmpty {
                        Button(action: { searchText = "" }) {
                            Image(systemName: "xmark.circle.fill").foregroundColor(.gray)
                        }
                    }
                }
                .padding(12)
                .background(Color(.systemBackground))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top)

                // Slot filter
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(MealSlotFilter.allCases, id: \.self) { slot in
                            FilterChip(title: slot.displayName, isSelected: selectedSlot == slot) {
                                selectedSlot = slot
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 12)
                }

                if mealsVM.isLoading {
                    Spacer(); ProgressView("Loading meals..."); Spacer()
                } else if filteredMeals.isEmpty {
                    EmptyMealsView()
                } else {
                    List {
                        ForEach(filteredMeals, id: \.id) { meal in
                            NavigationLink(destination: MealDetailScreen(meal: toMealUI(meal))) {
                                KMPMealRowView(meal: meal, slotDisplayName: slotDisplayName)
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button {
                                    Task {
                                        if let mwf = try? await mealsVM.getMealWithFoods(mealId: meal.id) {
                                            await MainActor.run { editRequest = EditMealRequest(mealWithFoods: mwf) }
                                        }
                                    }
                                } label: {
                                    Label("Edit", systemImage: "pencil")
                                }
                                .tint(.blue)

                                Button(role: .destructive) {
                                    Task {
                                        try? await mealsVM.deleteMeal(id: meal.id)
                                        await MainActor.run { mealsVM.loadMeals(userId: userId) }
                                    }
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
        }
        .navigationTitle("My Meals")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddMeal = true }) { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $showAddMeal) {
            AddMealScreen(onSave: { mealsVM.loadMeals(userId: userId) })
                .environmentObject(appState)
        }
        .sheet(item: $editRequest) { req in
            AddMealScreen(existingMeal: req.mealWithFoods, onSave: { mealsVM.loadMeals(userId: userId) })
                .environmentObject(appState)
        }
        .onAppear { mealsVM.loadMeals(userId: userId) }
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
                .background(isSelected ? Color.green : Color(.systemBackground))
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

// Row view for KMP Meal objects
struct KMPMealRowView: View {
    let meal: Meal
    let slotDisplayName: (String) -> String

    private func slotIcon(for slotType: String) -> String {
        switch slotType.uppercased() {
        case "BREAKFAST", "EARLY_MORNING": return "sunrise.fill"
        case "LUNCH", "NOON", "MID_MORNING": return "sun.max.fill"
        case "DINNER", "POST_DINNER": return "moon.fill"
        default: return "leaf.fill"
        }
    }

    var body: some View {
        HStack {
            Image(systemName: slotIcon(for: meal.slotType))
                .foregroundColor(.green)
                .frame(width: 30)
            VStack(alignment: .leading, spacing: 4) {
                Text(meal.name).font(.headline)
                Text(slotDisplayName(meal.slotType))
                    .font(.caption)
                    .padding(.horizontal, 8).padding(.vertical, 2)
                    .background(Color.green.opacity(0.2)).cornerRadius(4)
            }
            Spacer()
        }
        .padding(.vertical, 4)
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
                .environmentObject(AppState())
        }
    }
}
