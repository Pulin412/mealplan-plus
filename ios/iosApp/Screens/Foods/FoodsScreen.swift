import SwiftUI
import shared

// MARK: - Colour tokens

private let foodsTopBarGreen = Color(red: 0.18, green: 0.49, blue: 0.32)
private let foodsCaloriesColor = Color(red: 0.298, green: 0.686, blue: 0.314)
private let foodsCarbsColor    = Color(red: 1.0,   green: 0.596, blue: 0.0)

// MARK: - FoodsScreen

struct FoodsScreen: View {
    var pickerMode: Bool = false
    var onFoodSelected: ((FoodItem, Double) -> Void)? = nil

    @EnvironmentObject var appState: AppState
    @StateObject private var vm = FoodsViewModel()

    @State private var searchText = ""
    @State private var selectedTab: Int = 0          // 0=All, 1=Favourites, 2=Recent
    @State private var selectedTagNames: Set<String> = []
    @State private var allTags: [Tag] = []
    @State private var expandedFoodId: Int64? = nil

    // sheets
    @State private var showAddFood = false
    @State private var showScanner = false
    @State private var showOnlineSearch = false

    // picker quantity sheet
    @State private var pickerFood: FoodItem? = nil
    @State private var pickerQty: String = "100"

    private var userId: Int64 { appState.currentUserId ?? 0 }

    // ── Derived lists ─────────────────────────────────────────────────────────

    private var tabFoods: [FoodItem] {
        switch selectedTab {
        case 1: return vm.foods.filter { $0.isFavorite }
        case 2: return Array(vm.foods.suffix(20))
        default: return vm.foods
        }
    }

    private var displayedFoods: [FoodItem] {
        var result = tabFoods
        if !selectedTagNames.isEmpty {
            result = result.filter { food in
                selectedTagNames.contains { tagName in
                    food.name.localizedCaseInsensitiveContains(tagName)
                }
            }
        }
        return result
    }

    // ── Body ──────────────────────────────────────────────────────────────────

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Color(red: 0.94, green: 0.976, blue: 0.957).ignoresSafeArea()

            VStack(spacing: 0) {
                topBar
                tabBar
                if !pickerMode && !allTags.isEmpty {
                    tagFilterRow
                }
                contentArea
            }

            if !pickerMode {
                fabStack
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            vm.loadFoods()
            if !pickerMode {
                Task {
                    if let tags = try? await DietsViewModel().getAllTags(userId: userId) {
                        allTags = tags
                    }
                }
            }
        }
        .sheet(isPresented: $showAddFood) {
            AddFoodScreen { _ in vm.loadFoods() }
        }
        .fullScreenCover(isPresented: $showScanner) {
            BarcodeScannerScreen { barcode in
                print("Scanned: \(barcode)")
            }
        }
        .sheet(isPresented: $showOnlineSearch) {
            OnlineSearchScreen { result in
                Task {
                    let food = FoodItem(
                        id: 0,
                        name: result.name,
                        brand: result.brand,
                        barcode: nil,
                        caloriesPer100: result.calories,
                        proteinPer100: result.protein,
                        carbsPer100: result.carbs,
                        fatPer100: result.fat,
                        gramsPerPiece: nil,
                        gramsPerCup: nil,
                        gramsPerTbsp: nil,
                        gramsPerTsp: nil,
                        glycemicIndex: nil,
                        preferredUnit: nil,
                        isFavorite: false,
                        lastUsed: nil,
                        createdAt: Int64(Date().timeIntervalSince1970 * 1000),
                        isSystemFood: false
                    )
                    _ = try? await vm.insertFood(food)
                }
            }
        }
        .sheet(item: $pickerFood) { food in
            pickerQuantitySheet(food: food)
        }
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private var topBar: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Text(pickerMode ? "Select Food" : "My Foods")
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)

                Spacer()

                // Search field
                HStack(spacing: 6) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.white.opacity(0.8))
                        .font(.system(size: 14))
                    TextField("", text: $searchText, prompt: Text("Search…").foregroundColor(.white.opacity(0.7)))
                        .foregroundColor(.white)
                        .font(.system(size: 14))
                        .autocorrectionDisabled()
                        .onChange(of: searchText) { _ in
                            if searchText.isEmpty { vm.loadFoods() } else { vm.searchFoods(query: searchText) }
                        }
                    if !searchText.isEmpty {
                        Button { searchText = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.white.opacity(0.8))
                                .font(.system(size: 14))
                        }
                    }
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color.white.opacity(0.2))
                .cornerRadius(8)
                .frame(maxWidth: 180)

                if !pickerMode {
                    Button { showAddFood = true } label: {
                        Image(systemName: "plus")
                            .foregroundColor(.white)
                            .font(.system(size: 18, weight: .medium))
                    }
                    Button { showScanner = true } label: {
                        Image(systemName: "barcode.viewfinder")
                            .foregroundColor(.white)
                            .font(.system(size: 18, weight: .medium))
                    }
                    Button { showOnlineSearch = true } label: {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.white)
                            .font(.system(size: 18, weight: .medium))
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(foodsTopBarGreen)
        }
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private var tabBar: some View {
        HStack(spacing: 0) {
            ForEach(["All", "Favourites", "Recent"].indices, id: \.self) { idx in
                let label = ["All", "Favourites", "Recent"][idx]
                Button {
                    withAnimation(.easeInOut(duration: 0.15)) { selectedTab = idx }
                } label: {
                    VStack(spacing: 4) {
                        Text(label)
                            .font(.system(size: 13, weight: selectedTab == idx ? .semibold : .regular))
                            .foregroundColor(selectedTab == idx ? foodsTopBarGreen : .secondary)
                        Rectangle()
                            .frame(height: 2)
                            .foregroundColor(selectedTab == idx ? foodsTopBarGreen : .clear)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                }
            }
        }
        .background(Color(.systemBackground))
        .shadow(color: .black.opacity(0.06), radius: 2, y: 1)
    }

    // ── Tag filter row ────────────────────────────────────────────────────────

    private var tagFilterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FoodsTagChip(label: "All", isSelected: selectedTagNames.isEmpty) {
                    selectedTagNames.removeAll()
                }
                ForEach(allTags, id: \.id) { tag in
                    FoodsTagChip(label: tag.name, isSelected: selectedTagNames.contains(tag.name)) {
                        if selectedTagNames.contains(tag.name) {
                            selectedTagNames.remove(tag.name)
                        } else {
                            selectedTagNames.insert(tag.name)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .background(Color(.systemBackground).opacity(0.95))
    }

    // ── Content area ──────────────────────────────────────────────────────────

    @ViewBuilder
    private var contentArea: some View {
        if vm.isLoading {
            Spacer()
            ProgressView()
            Spacer()
        } else if displayedFoods.isEmpty {
            EmptyFoodsView()
        } else {
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(displayedFoods, id: \.id) { food in
                        FoodExpandableCard(
                            food: food,
                            isExpanded: expandedFoodId == food.id,
                            pickerMode: pickerMode,
                            onToggle: {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    expandedFoodId = expandedFoodId == food.id ? nil : food.id
                                }
                            },
                            onFavourite: {
                                Task { try? await vm.setFavorite(id: food.id, isFavorite: !food.isFavorite) }
                            },
                            onDelete: {
                                Task { try? await vm.deleteFood(id: food.id) }
                            },
                            onSelect: {
                                pickerFood = food
                                pickerQty = "100"
                            }
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, pickerMode ? 16 : 100)
            }
        }
    }

    // ── FABs ──────────────────────────────────────────────────────────────────

    private var fabStack: some View {
        VStack(spacing: 12) {
            FoodsFAB(icon: "magnifyingglass") { showOnlineSearch = true }
            FoodsFAB(icon: "barcode.viewfinder") { showScanner = true }
            FoodsFAB(icon: "plus") { showAddFood = true }
        }
        .padding(.trailing, 20)
        .padding(.bottom, 24)
    }

    // ── Picker quantity sheet ─────────────────────────────────────────────────

    private func pickerQuantitySheet(food: FoodItem) -> some View {
        NavigationStack {
            VStack(spacing: 24) {
                Text(food.name)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .padding(.top)

                VStack(alignment: .leading, spacing: 6) {
                    Text("Quantity (g)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    TextField("100", text: $pickerQty)
                        .keyboardType(.decimalPad)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .font(.title3)
                }
                .padding(.horizontal, 32)

                Button {
                    let qty = Double(pickerQty) ?? 100
                    onFoodSelected?(food, qty)
                    pickerFood = nil
                } label: {
                    Text("Confirm")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(foodsTopBarGreen)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 32)

                Spacer()
            }
            .navigationTitle("Select Quantity")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { pickerFood = nil }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

// MARK: - FoodExpandableCard

private struct FoodExpandableCard: View {
    let food: FoodItem
    let isExpanded: Bool
    let pickerMode: Bool
    let onToggle: () -> Void
    let onFavourite: () -> Void
    let onDelete: () -> Void
    let onSelect: () -> Void

    private var gi: Int? {
        guard let gi = food.glycemicIndex else { return nil }
        return Int(gi.int32Value)
    }

    var body: some View {
        VStack(spacing: 0) {
            // ── Header row ────────────────────────────────────────────────────
            Button(action: onToggle) {
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(food.name)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.leading)
                        Text("\(Int(food.caloriesPer100)) kcal/100g")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        HStack(spacing: 6) {
                            MacroBadge(value: food.proteinPer100, label: "P", color: .red)
                            MacroBadge(value: food.carbsPer100, label: "C", color: .blue)
                            MacroBadge(value: food.fatPer100, label: "F", color: .yellow)
                        }
                    }

                    Spacer()

                    if pickerMode {
                        Button(action: onSelect) {
                            Text("Select")
                                .font(.system(size: 13, weight: .semibold))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 7)
                                .background(foodsTopBarGreen)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                        .buttonStyle(PlainButtonStyle())
                    } else {
                        Button(action: onFavourite) {
                            Image(systemName: food.isFavorite ? "star.fill" : "star")
                                .foregroundColor(food.isFavorite ? .yellow : .gray)
                                .font(.system(size: 18))
                        }
                        .buttonStyle(PlainButtonStyle())
                    }

                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                }
                .padding(12)
            }
            .buttonStyle(PlainButtonStyle())

            // ── Expanded content (non-picker only) ───────────────────────────
            if isExpanded && !pickerMode {
                Divider().padding(.horizontal, 12)

                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 16) {
                        FoodMacroDetail(label: "Protein", value: food.proteinPer100, color: .red)
                        FoodMacroDetail(label: "Carbs",   value: food.carbsPer100,   color: .blue)
                        FoodMacroDetail(label: "Fat",     value: food.fatPer100,     color: .yellow)
                    }

                    if let giVal = gi {
                        FoodsGiBadge(gi: giVal)
                    }

                    HStack {
                        Spacer()
                        Button(role: .destructive, action: onDelete) {
                            Label("Delete", systemImage: "trash")
                                .font(.system(size: 13))
                                .foregroundColor(.red)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(12)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
    }
}

// MARK: - Helper sub-views

private struct FoodMacroDetail: View {
    let label: String
    let value: Double
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
            Text("\(String(format: "%.1f", value))g")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct FoodsGiBadge: View {
    let gi: Int
    private var bg: Color {
        gi <= 55 ? foodsCaloriesColor : gi <= 69 ? foodsCarbsColor : Color.red
    }
    var body: some View {
        Text("GI \(gi)")
            .font(.system(size: 10, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

private struct FoodsTagChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? foodsTopBarGreen : Color(.systemGray6))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}

private struct FoodsFAB: View {
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 50, height: 50)
                .background(foodsTopBarGreen)
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
        }
    }
}

// MARK: - FoodItem + Identifiable for sheet

extension FoodItem: Identifiable {}

// MARK: - Legacy models / views kept for backward compat

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
                Text("\(food.calories) kcal")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                HStack(spacing: 8) {
                    MacroBadge(value: food.protein, label: "P", color: .red)
                    MacroBadge(value: food.carbs,   label: "C", color: .blue)
                    MacroBadge(value: food.fat,     label: "F", color: .yellow)
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
            Spacer()
            Image(systemName: "leaf.fill")
                .font(.system(size: 60))
                .foregroundColor(.green.opacity(0.5))
            Text("No foods yet")
                .font(.headline)
            Text("Add your first food item to get started")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// UI Model
struct FoodItemUI: Identifiable {
    let id: Int64
    let name: String
    let calories: Int
    let protein: Double
    let carbs: Double
    let fat: Double
    let unit: String
}

struct FoodsScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            FoodsScreen()
                .environmentObject(AppState())
        }
    }
}
