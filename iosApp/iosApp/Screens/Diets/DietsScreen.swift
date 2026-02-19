import SwiftUI
import shared

struct DietsScreen: View {
    @State private var searchText = ""
    @State private var diets: [DietUI] = []
    @State private var showAddDiet = false

    var filteredDiets: [DietUI] {
        if searchText.isEmpty {
            return diets
        }
        return diets.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
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
                    TextField("Search diets...", text: $searchText)
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

                if filteredDiets.isEmpty {
                    EmptyDietsView()
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(filteredDiets) { diet in
                                NavigationLink(destination: DietDetailScreen(diet: diet)) {
                                    DietCardView(diet: diet)
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                        .padding(.horizontal)
                    }
                }
            }
        }
        .navigationTitle("My Diets")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddDiet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddDiet) {
            AddDietScreen { newDiet in
                diets.append(newDiet)
            }
        }
        .onAppear {
            loadSampleDiets()
        }
    }

    private func loadSampleDiets() {
        diets = SeedDataLoader.shared.loadDiets()
    }
}

struct DietCardView: View {
    let diet: DietUI

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(diet.name)
                        .font(.headline)
                        .foregroundColor(.primary)
                    Text(diet.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }

            // Tags
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(diet.tags, id: \.self) { tag in
                        Text(tag)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.green.opacity(0.2))
                            .foregroundColor(.green)
                            .cornerRadius(4)
                    }
                }
            }

            Divider()

            // Stats
            HStack {
                StatItem(value: "\(diet.calories)", label: "kcal")
                Spacer()
                StatItem(value: "\(Int(diet.protein))g", label: "Protein")
                Spacer()
                StatItem(value: "\(Int(diet.carbs))g", label: "Carbs")
                Spacer()
                StatItem(value: "\(Int(diet.fat))g", label: "Fat")
                Spacer()
                StatItem(value: "\(diet.mealCount)", label: "Meals")
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
    }
}

struct StatItem: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.subheadline)
                .fontWeight(.semibold)
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

struct EmptyDietsView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "calendar")
                .font(.system(size: 60))
                .foregroundColor(.green.opacity(0.5))
            Text("No diets yet")
                .font(.headline)
            Text("Create a diet plan by combining meals")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

// UI Model
struct DietUI: Identifiable {
    let id: Int64
    let name: String
    let description: String
    let calories: Int
    let protein: Double
    let carbs: Double
    let fat: Double
    let mealCount: Int
    let tags: [String]
}

struct DietsScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            DietsScreen()
        }
    }
}
