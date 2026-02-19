import SwiftUI
import shared

struct FoodsScreen: View {
    @State private var searchText = ""
    @State private var foods: [FoodItemUI] = []
    @State private var showAddFood = false

    var filteredFoods: [FoodItemUI] {
        if searchText.isEmpty {
            return foods
        }
        return foods.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
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
                    TextField("Search foods...", text: $searchText)
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
                .padding()

                if filteredFoods.isEmpty {
                    EmptyFoodsView()
                } else {
                    List {
                        ForEach(filteredFoods) { food in
                            NavigationLink(destination: FoodDetailScreen(food: food)) {
                                FoodRowView(food: food)
                            }
                        }
                        .onDelete(perform: deleteFood)
                    }
                    .listStyle(PlainListStyle())
                }
            }
        }
        .navigationTitle("My Foods")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddFood = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddFood) {
            AddFoodScreen { newFood in
                foods.append(newFood)
            }
        }
        .onAppear {
            loadSampleFoods()
        }
    }

    private func deleteFood(at offsets: IndexSet) {
        foods.remove(atOffsets: offsets)
    }

    private func loadSampleFoods() {
        // Sample data - will be replaced with shared repository
        foods = [
            FoodItemUI(id: 1, name: "Chicken Breast", calories: 165, protein: 31, carbs: 0, fat: 3.6, unit: "100g"),
            FoodItemUI(id: 2, name: "Brown Rice", calories: 112, protein: 2.6, carbs: 24, fat: 0.9, unit: "100g"),
            FoodItemUI(id: 3, name: "Broccoli", calories: 34, protein: 2.8, carbs: 7, fat: 0.4, unit: "100g"),
            FoodItemUI(id: 4, name: "Salmon", calories: 208, protein: 20, carbs: 0, fat: 13, unit: "100g"),
            FoodItemUI(id: 5, name: "Eggs", calories: 155, protein: 13, carbs: 1.1, fat: 11, unit: "100g"),
        ]
    }
}

struct FoodRowView: View {
    let food: FoodItemUI

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(food.name)
                    .font(.headline)
                Text("\(food.unit)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text("\(Int(food.calories)) kcal")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                HStack(spacing: 8) {
                    MacroBadge(value: food.protein, label: "P", color: .red)
                    MacroBadge(value: food.carbs, label: "C", color: .blue)
                    MacroBadge(value: food.fat, label: "F", color: .yellow)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

struct MacroBadge: View {
    let value: Double
    let label: String
    let color: Color

    var body: some View {
        Text("\(label): \(Int(value))")
            .font(.caption2)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color.opacity(0.2))
            .cornerRadius(4)
    }
}

struct EmptyFoodsView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "leaf.fill")
                .font(.system(size: 60))
                .foregroundColor(.green.opacity(0.5))
            Text("No foods yet")
                .font(.headline)
            Text("Add your first food item to get started")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// UI Model
struct FoodItemUI: Identifiable {
    let id: Int64
    let name: String
    let calories: Double
    let protein: Double
    let carbs: Double
    let fat: Double
    let unit: String
}

struct FoodsScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            FoodsScreen()
        }
    }
}
