import SwiftUI
import shared

struct OnlineSearchScreen: View {
    @Environment(\.dismiss) var dismiss
    @State private var searchText = ""
    @State private var searchResults: [OnlineFoodResult] = []
    @State private var isSearching = false
    @State private var errorMessage: String?
    @State private var selectedSource: FoodSource = .usda

    var onFoodSelected: (OnlineFoodResult) -> Void

    enum FoodSource: String, CaseIterable {
        case usda = "USDA"
        case openFoodFacts = "Open Food Facts"
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Source picker
                Picker("Source", selection: $selectedSource) {
                    ForEach(FoodSource.allCases, id: \.self) { source in
                        Text(source.rawValue).tag(source)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding()

                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Search foods online...", text: $searchText)
                        .onSubmit {
                            performSearch()
                        }
                    if !searchText.isEmpty {
                        Button(action: { searchText = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.gray)
                        }
                    }
                    Button(action: performSearch) {
                        Text("Search")
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.green)
                            .cornerRadius(8)
                    }
                    .disabled(searchText.isEmpty || isSearching)
                }
                .padding(12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding(.horizontal)

                // Content
                if isSearching {
                    Spacer()
                    ProgressView("Searching...")
                    Spacer()
                } else if let error = errorMessage {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 40))
                            .foregroundColor(.orange)
                        Text(error)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                    Spacer()
                } else if searchResults.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 40))
                            .foregroundColor(.gray)
                        Text("Search for foods online")
                            .font(.headline)
                        Text("Results from \(selectedSource.rawValue) database")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                } else {
                    List(searchResults) { result in
                        Button(action: {
                            onFoodSelected(result)
                            dismiss()
                        }) {
                            OnlineFoodRow(result: result)
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("Online Search")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func performSearch() {
        guard !searchText.isEmpty else { return }

        isSearching = true
        errorMessage = nil
        searchResults = []

        // Simulate API call - in real app would use shared Ktor client
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            isSearching = false

            // Sample results
            searchResults = [
                OnlineFoodResult(
                    id: "1",
                    name: "\(searchText.capitalized) (Generic)",
                    brand: nil,
                    calories: Double.random(in: 50...300),
                    protein: Double.random(in: 2...30),
                    carbs: Double.random(in: 5...50),
                    fat: Double.random(in: 1...20),
                    servingSize: "100g",
                    source: selectedSource.rawValue
                ),
                OnlineFoodResult(
                    id: "2",
                    name: "Organic \(searchText.capitalized)",
                    brand: "Nature's Best",
                    calories: Double.random(in: 50...300),
                    protein: Double.random(in: 2...30),
                    carbs: Double.random(in: 5...50),
                    fat: Double.random(in: 1...20),
                    servingSize: "100g",
                    source: selectedSource.rawValue
                ),
                OnlineFoodResult(
                    id: "3",
                    name: "\(searchText.capitalized) - Low Fat",
                    brand: "Healthy Choice",
                    calories: Double.random(in: 50...200),
                    protein: Double.random(in: 5...25),
                    carbs: Double.random(in: 10...40),
                    fat: Double.random(in: 0...5),
                    servingSize: "1 serving",
                    source: selectedSource.rawValue
                ),
            ]
        }
    }
}

struct OnlineFoodRow: View {
    let result: OnlineFoodResult

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(result.name)
                        .font(.headline)
                        .foregroundColor(.primary)
                    if let brand = result.brand {
                        Text(brand)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                Spacer()
                Text("\(Int(result.calories)) kcal")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.green)
            }

            HStack(spacing: 12) {
                MacroLabel(label: "P", value: result.protein, color: .red)
                MacroLabel(label: "C", value: result.carbs, color: .blue)
                MacroLabel(label: "F", value: result.fat, color: .yellow)
                Spacer()
                Text(result.servingSize)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            HStack {
                Image(systemName: "globe")
                    .font(.caption2)
                Text(result.source)
                    .font(.caption2)
            }
            .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}

struct MacroLabel: View {
    let label: String
    let value: Double
    let color: Color

    var body: some View {
        HStack(spacing: 2) {
            Text(label)
                .font(.caption2)
                .fontWeight(.bold)
            Text("\(Int(value))g")
                .font(.caption)
        }
        .foregroundColor(color)
    }
}

struct OnlineFoodResult: Identifiable {
    let id: String
    let name: String
    let brand: String?
    let calories: Double
    let protein: Double
    let carbs: Double
    let fat: Double
    let servingSize: String
    let source: String
}

struct OnlineSearchScreen_Previews: PreviewProvider {
    static var previews: some View {
        OnlineSearchScreen { _ in }
    }
}
