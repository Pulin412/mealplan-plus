import SwiftUI
import shared

// ── Colours ───────────────────────────────────────────────────────────────────
private let topBarGreen = Color(red: 0.18, green: 0.49, blue: 0.32)
private let logBg = Color(red: 0.94, green: 0.976, blue: 0.957)
private let caloriesColor = Color(red: 0.298, green: 0.686, blue: 0.314)
private let carbsColor    = Color(red: 1.0, green: 0.596, blue: 0.0)
private let proteinColor  = Color(red: 0.129, green: 0.588, blue: 0.953)
private let fatColor      = Color(red: 0.914, green: 0.118, blue: 0.388)
private let overColor     = Color(red: 1.0, green: 0.596, blue: 0.0)

// ── Slot Order ────────────────────────────────────────────────────────────────
private let slotOrder: [String: Int] = [
    "EARLY_MORNING": 0, "BREAKFAST": 1, "MID_MORNING": 2,
    "NOON": 3, "LUNCH": 4, "PRE_WORKOUT": 5,
    "EVENING": 6, "EVENING_SNACK": 7, "POST_WORKOUT": 8,
    "DINNER": 9, "POST_DINNER": 10
]
private let mainSlots = ["BREAKFAST", "LUNCH", "DINNER", "EVENING_SNACK"]
private func slotDisplayName(_ key: String) -> String {
    key.split(separator: "_").map { $0.capitalized }.joined(separator: " ")
}
private func slotEmoji(_ key: String) -> String {
    switch key {
    case "BREAKFAST": return "🌅"
    case "LUNCH": return "🍽️"
    case "DINNER": return "🌙"
    case "EVENING_SNACK": return "🍎"
    case "EARLY_MORNING": return "🌙"
    case "MID_MORNING": return "☕"
    case "NOON": return "☀️"
    case "PRE_WORKOUT": return "💪"
    case "EVENING": return "🌆"
    case "POST_WORKOUT": return "🥤"
    case "POST_DINNER": return "🍵"
    default: return "🍴"
    }
}
private func slotColor(_ key: String) -> Color {
    switch key {
    case "BREAKFAST": return carbsColor
    case "LUNCH": return proteinColor
    case "DINNER": return Color(red: 0.612, green: 0.153, blue: 0.69)
    case "EVENING_SNACK": return caloriesColor
    default: return Color.gray
    }
}

// ── Custom slot persistence ────────────────────────────────────────────────────
private struct CustomSlotDef: Codable, Equatable {
    let id: Int
    let name: String
}
private func customSlotsKey(userId: Int64, date: String) -> String {
    "custom_slots_\(userId)_\(date)"
}
private func loadCustomSlots(userId: Int64, date: String) -> [CustomSlotDef] {
    guard let data = UserDefaults.standard.data(forKey: customSlotsKey(userId: userId, date: date)),
          let defs = try? JSONDecoder().decode([CustomSlotDef].self, from: data) else { return [] }
    return defs
}
private func saveCustomSlots(_ slots: [CustomSlotDef], userId: Int64, date: String) {
    if let data = try? JSONEncoder().encode(slots) {
        UserDefaults.standard.set(data, forKey: customSlotsKey(userId: userId, date: date))
    }
}
// ── Custom slot done-flag (independent of food presence) ──────────────────────
private func customSlotDoneKey(userId: Int64, date: String) -> String {
    "custom_slot_done_\(userId)_\(date)"
}
private func loadCustomSlotDone(userId: Int64, date: String) -> Set<String> {
    Set(UserDefaults.standard.stringArray(forKey: customSlotDoneKey(userId: userId, date: date)) ?? [])
}
private func toggleCustomSlotDone(_ slotKey: String, userId: Int64, date: String) {
    let key = customSlotDoneKey(userId: userId, date: date)
    var s = Set(UserDefaults.standard.stringArray(forKey: key) ?? [])
    if s.contains(slotKey) { s.remove(slotKey) } else { s.insert(slotKey) }
    UserDefaults.standard.set(Array(s), forKey: key)
}

// ── Full slot order persistence ───────────────────────────────────────────────
private func slotOrderPersistKey(userId: Int64, date: String) -> String { "slot_order_\(userId)_\(date)" }
private func loadSlotOrder(userId: Int64, date: String) -> [String] {
    UserDefaults.standard.stringArray(forKey: slotOrderPersistKey(userId: userId, date: date)) ?? []
}
private func saveSlotOrder(_ keys: [String], userId: Int64, date: String) {
    UserDefaults.standard.set(keys, forKey: slotOrderPersistKey(userId: userId, date: date))
}

// ── isoDate helpers ───────────────────────────────────────────────────────────
private func isoDate(from date: Date) -> String {
    let fmt = DateFormatter(); fmt.dateFormat = "yyyy-MM-dd"; return fmt.string(from: date)
}
private func date(from iso: String) -> Date? {
    let fmt = DateFormatter(); fmt.dateFormat = "yyyy-MM-dd"; return fmt.date(from: iso)
}
private func displayDate(_ date: Date) -> String {
    let fmt = DateFormatter(); fmt.dateFormat = "MMMM d, yyyy"; return fmt.string(from: date)
}

// ── Sheet enum ────────────────────────────────────────────────────────────────
private enum LogSheet: Identifiable {
    case addFood(slotKey: String)
    case customFoodPicker(slotKey: String)
    case dietPicker
    var id: Int {
        switch self {
        case .addFood:            return 1
        case .customFoodPicker:   return 2
        case .dietPicker:         return 3
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────
struct DailyLogScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var vm = DailyLogViewModel()
    @State private var selectedDate: Date = Date()
    @State private var selectedTab: Int = 0
    @State private var expandedSlots: Set<String> = ["BREAKFAST"]
    @State private var activeSheet: LogSheet? = nil
    @State private var showClearPlanAlert: Bool = false
    @State private var customSlots: [CustomSlotDef] = []
    @State private var customSlotDoneKeys: Set<String> = []
    @State private var savedSlotOrder: [String] = []
    @State private var showAddSlotAlert: Bool = false
    @State private var newSlotName: String = ""

    private var userId: Int64 { Int64(appState.currentUserId ?? 0) }
    private var isToday: Bool { Calendar.current.isDateInToday(selectedDate) }
    private var isPastOrToday: Bool { selectedDate <= Date() }

    var body: some View {
        NavigationStack {
            ZStack(alignment: .top) {
                logBg.ignoresSafeArea()
                VStack(spacing: 0) {
                    dateNavigatorPill
                    macroSummaryCard
                    if vm.isLoading {
                        Spacer()
                        ProgressView().tint(caloriesColor)
                        Spacer()
                    } else if selectedTab == 0 {
                        dailyLogTab
                    } else {
                        planVsActualTab
                    }
                }
            }
            .navigationTitle("Food Log")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(topBarGreen, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar { logToolbar }
            .sheet(item: $activeSheet) { sheet in
                switch sheet {
                case .addFood(let slotKey):
                    FoodPickerSheet(
                        slotKey: slotKey,
                        userId: userId,
                        date: isoDate(from: selectedDate),
                        onLogged: { activeSheet = nil; reload() }
                    )
                case .customFoodPicker(let slotKey):
                    FoodsScreen(pickerMode: true, onFoodSelected: { food, qty in
                        vm.logFood(userId: userId, date: isoDate(from: selectedDate),
                                   foodId: food.id, quantity: qty, slotType: slotKey)
                        activeSheet = nil
                        reload()
                    })
                    .environmentObject(appState)
                case .dietPicker:
                    HomeDietPickerSheet { diet in
                        vm.assignDiet(userId: userId, date: isoDate(from: selectedDate), diet: diet)
                        activeSheet = nil
                        reload()
                    }
                    .environmentObject(appState)
                }
            }
            .alert("Clear Plan?", isPresented: $showClearPlanAlert) {
                Button("Clear", role: .destructive) {
                    vm.clearDiet(userId: userId, date: isoDate(from: selectedDate))
                }
                Button("Cancel", role: .cancel) {}
            } message: { Text("Remove the diet plan for this day?") }
        }
        .onAppear { reload() }
        .onChange(of: selectedDate) { _ in reload() }
        .onReceive(NotificationCenter.default.publisher(for: .navigateToDate)) { notification in
            if let isoString = notification.object as? String {
                let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
                if let date = f.date(from: isoString) { selectedDate = date }
            }
        }
    }

    @ToolbarContentBuilder
    private var logToolbar: some ToolbarContent {
        // Today button (when not on today)
        if !isToday {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Today") { selectedDate = Date() }
                    .foregroundColor(.white)
            }
        }
        ToolbarItemGroup(placement: .navigationBarTrailing) {
            // Select Diet
            Button(action: { activeSheet = .dietPicker }) {
                Image(systemName: "fork.knife.circle")
            }
            // Clear Plan (only when plan exists and not completed)
            if vm.currentPlanDietId != nil && !vm.isPlanCompleted {
                Button(action: { showClearPlanAlert = true }) {
                    Image(systemName: "trash")
                }
            }
            // Complete Day (plan exists, past or today, not yet completed)
            if vm.currentPlanDietId != nil && isPastOrToday && !vm.isPlanCompleted {
                Button(action: { vm.completeDay(userId: userId, date: isoDate(from: selectedDate)) }) {
                    Image(systemName: "checkmark.circle")
                }
            }
            // Reopen Day
            if vm.isPlanCompleted {
                Button(action: { vm.reopenDay(userId: userId, date: isoDate(from: selectedDate)) }) {
                    Image(systemName: "arrow.counterclockwise.circle")
                }
            }
        }
    }

    private func reload() {
        let dateStr = isoDate(from: selectedDate)
        vm.loadLog(userId: userId, date: dateStr)
        customSlots = loadCustomSlots(userId: userId, date: dateStr)
        customSlotDoneKeys = loadCustomSlotDone(userId: userId, date: dateStr)
        savedSlotOrder = loadSlotOrder(userId: userId, date: dateStr)
    }

    private func addCustomSlot() {
        let name = newSlotName.trimmingCharacters(in: .whitespaces)
        guard !name.isEmpty else { newSlotName = ""; return }
        let nextId = (customSlots.map { $0.id }.max() ?? 0) + 1
        let def = CustomSlotDef(id: nextId, name: name)
        customSlots.append(def)
        saveCustomSlots(customSlots, userId: userId, date: isoDate(from: selectedDate))
        expandedSlots.insert("CUSTOM_\(nextId)")
        newSlotName = ""
        appState.customSlotsVersion += 1
    }

    private func deleteCustomSlot(key: String) {
        customSlots.removeAll { "CUSTOM_\($0.id)" == key }
        saveCustomSlots(customSlots, userId: userId, date: isoDate(from: selectedDate))
        expandedSlots.remove(key)
        appState.customSlotsVersion += 1
    }

    // ── Date Navigator Pill ──────────────────────────────────────────────────
    private var dateNavigatorPill: some View {
        HStack {
            Button(action: { selectedDate = Calendar.current.date(byAdding: .day, value: -1, to: selectedDate) ?? selectedDate }) {
                Image(systemName: "chevron.left")
                    .foregroundColor(topBarGreen)
                    .frame(width: 40, height: 40)
            }
            Spacer()
            VStack(spacing: 0) {
                if isToday {
                    Text("Today · \(displayDate(selectedDate))")
                        .font(.subheadline).fontWeight(.medium)
                        .foregroundColor(Color(red: 0.106, green: 0.369, blue: 0.125))
                } else {
                    Text(displayDate(selectedDate))
                        .font(.subheadline).fontWeight(.medium)
                        .foregroundColor(Color(red: 0.106, green: 0.369, blue: 0.125))
                }
            }
            Spacer()
            Button(action: { selectedDate = Calendar.current.date(byAdding: .day, value: 1, to: selectedDate) ?? selectedDate }) {
                Image(systemName: "chevron.right")
                    .foregroundColor(isToday ? Color.gray : topBarGreen)
                    .frame(width: 40, height: 40)
            }
            .disabled(isToday)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
        .padding(.horizontal, 16)
        .padding(.top, 10)
        .padding(.bottom, 4)
    }

    // ── Macro Summary Card ───────────────────────────────────────────────────
    private var macroSummaryCard: some View {
        // Actual = individually logged foods + planned diet food items (shown in slot cards)
        let allPlanned = vm.plannedMealsBySlot.values.flatMap { $0 }
        let totalCal = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateCalories(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateCalories(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }
        let totalPro = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateProtein(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateProtein(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }
        let totalCarb = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateCarbs(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateCarbs(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }
        let totalFat_ = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateFat(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateFat(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }

        return VStack(spacing: 12) {
            HStack(spacing: 0) {
                MacroTileView(label: "Calories", value: Int(totalCal), unit: "kcal", planned: Int(vm.plannedCalories), color: caloriesColor)
                MacroTileView(label: "Carbs",    value: Int(totalCarb), unit: "g",    planned: Int(vm.plannedCarbs),    color: carbsColor)
                MacroTileView(label: "Protein",  value: Int(totalPro),  unit: "g",    planned: Int(vm.plannedProtein),  color: proteinColor)
                MacroTileView(label: "Fat",      value: Int(totalFat_), unit: "g",    planned: Int(vm.plannedFat),      color: fatColor)
            }
            // Tab toggle
            HStack(spacing: 0) {
                tabButton(label: "Daily Log", index: 0)
                tabButton(label: "Plan vs Actual", index: 1)
            }
            .background(Color(white: 0.96))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
        .padding(.horizontal, 16)
        .padding(.bottom, 4)
    }

    @ViewBuilder
    private func tabButton(label: String, index: Int) -> some View {
        let selected = selectedTab == index
        Text(label)
            .font(.system(size: 13, weight: selected ? .semibold : .regular))
            .foregroundColor(selected ? .white : .gray)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(selected ? topBarGreen : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 17))
            .padding(3)
            .onTapGesture { withAnimation(.easeInOut(duration: 0.15)) { selectedTab = index } }
    }

    // ── Slot card builder (extracted to help type-checker) ───────────────────
    @ViewBuilder
    private func slotCardView(key: String, customSlotNames: [String: String]) -> some View {
        let foods = vm.loggedFoodsBySlot[key] ?? []
        let plannedItems = vm.plannedMealsBySlot[key] ?? []
        let loggedFoodIds = Set(foods.map { $0.loggedFood.foodId })
        let isCustom = key.hasPrefix("CUSTOM_")
        let dateStr = isoDate(from: selectedDate)
        SlotCard(
            slotKey: key,
            titleOverride: customSlotNames[key],
            foods: foods,
            plannedItems: plannedItems,
            loggedFoodIds: loggedFoodIds,
            isExpanded: expandedSlots.contains(key),
            onToggle: {
                if expandedSlots.contains(key) { expandedSlots.remove(key) }
                else { expandedSlots.insert(key) }
            },
            onAddFood: { activeSheet = .addFood(slotKey: key) },
            onDeleteFood: { id in vm.removeLoggedFood(userId: userId, id: id) },
            onDeleteSlot: isCustom ? { deleteCustomSlot(key: key) } : nil,
            isCustomDone: isCustom && customSlotDoneKeys.contains(key),
            onToggleDone: isCustom ? {
                toggleCustomSlotDone(key, userId: userId, date: dateStr)
                customSlotDoneKeys = loadCustomSlotDone(userId: userId, date: dateStr)
            } : nil,
            isDraggable: true,
            onTogglePlannedFood: { item in
                if loggedFoodIds.contains(item.food.id) {
                    if let lf = foods.first(where: { $0.loggedFood.foodId == item.food.id }) {
                        vm.removeLoggedFood(userId: userId, id: lf.loggedFood.id)
                    }
                } else {
                    vm.logFood(userId: userId, date: dateStr,
                               foodId: item.food.id, quantity: item.mealFoodItem.quantity, slotType: key)
                }
            },
            onToggleAllPlannedFoods: {
                let allTicked = plannedItems.allSatisfy { loggedFoodIds.contains($0.food.id) }
                if allTicked {
                    let ids = plannedItems.compactMap { item in
                        foods.first { $0.loggedFood.foodId == item.food.id }?.loggedFood.id
                    }
                    vm.batchRemoveLoggedFoods(userId: userId, ids: ids)
                } else {
                    let toLog = plannedItems.filter { !loggedFoodIds.contains($0.food.id) }
                        .map { (foodId: $0.food.id, qty: $0.mealFoodItem.quantity, slotType: key) }
                    vm.batchLogFoods(userId: userId, date: dateStr, items: toLog)
                }
            }
        )
    }

    // ── Daily Log Tab ────────────────────────────────────────────────────────
    private var dailyLogTab: some View {
        let foodSlotKeys = Set(vm.loggedFoods.map { $0.loggedFood.slotType.uppercased() })
        let plannedSlotKeys = Set(vm.plannedMealsBySlot.keys)
        let customSlotNames = Dictionary(uniqueKeysWithValues: customSlots.map { ("CUSTOM_\($0.id)", $0.name) })
        let dateStr = isoDate(from: selectedDate)

        // All available keys
        let available = Set(mainSlots).union(foodSlotKeys).union(plannedSlotKeys)
            .union(Set(customSlots.map { "CUSTOM_\($0.id)" }))
        // Merge with saved order: keep saved order for known keys, append new keys in default order
        var ordered = savedSlotOrder.filter { available.contains($0) }
        let inOrder = Set(ordered)
        let defaultSorted = available.subtracting(inOrder).sorted { a, b in
            let oa = slotOrder[a] ?? 99, ob = slotOrder[b] ?? 99
            if oa != ob { return oa < ob }
            let ia = customSlots.firstIndex(where: { "CUSTOM_\($0.id)" == a }) ?? Int.max
            let ib = customSlots.firstIndex(where: { "CUSTOM_\($0.id)" == b }) ?? Int.max
            return ia < ib
        }
        ordered.append(contentsOf: defaultSorted)

        return List {
            ForEach(ordered, id: \.self) { key in
                slotCardView(key: key, customSlotNames: customSlotNames)
                    .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                    .listRowBackground(logBg)
                    .listRowSeparator(.hidden)
            }
            .onMove { from, to in
                var newOrder = ordered
                newOrder.move(fromOffsets: from, toOffset: to)
                savedSlotOrder = newOrder
                saveSlotOrder(newOrder, userId: userId, date: dateStr)
                // Sync customSlots array order to match
                let newCustom = newOrder.compactMap { key in customSlots.first(where: { "CUSTOM_\($0.id)" == key }) }
                if newCustom.count == customSlots.count {
                    customSlots = newCustom
                    saveCustomSlots(customSlots, userId: userId, date: dateStr)
                }
            }
            Button(action: { showAddSlotAlert = true }) {
                HStack(spacing: 6) {
                    Image(systemName: "plus.circle.fill").font(.system(size: 14))
                    Text("Add Meal Slot").font(.system(size: 14, weight: .medium))
                }
                .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.32))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }
            .listRowBackground(logBg)
            .listRowSeparator(.hidden)
            .moveDisabled(true)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .environment(\.editMode, .constant(.active))
        .alert("New Meal Slot", isPresented: $showAddSlotAlert) {
            TextField("Slot name (e.g. Pre-Bed Snack)", text: $newSlotName)
            Button("Add") { addCustomSlot() }
            Button("Cancel", role: .cancel) { newSlotName = "" }
        } message: {
            Text("Enter a name for the new meal slot")
        }
    }

    // ── Plan vs Actual Tab ───────────────────────────────────────────────────
    private var planVsActualTab: some View {
        let allPlanned = vm.plannedMealsBySlot.values.flatMap { $0 }
        let totalCal = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateCalories(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateCalories(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }
        let totalPro = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateProtein(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateProtein(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }
        let totalCarb = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateCarbs(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateCarbs(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }
        let totalFat_ = vm.loggedFoods.reduce(0.0) { $0 + $1.food.calculateFat(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) }
            + allPlanned.reduce(0.0) { $0 + $1.food.calculateFat(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) }

        return ScrollView {
            VStack(spacing: 0) {
                // Legend
                HStack(spacing: 16) {
                    legendDot(color: Color.gray.opacity(0.4), label: "Planned")
                    legendDot(color: caloriesColor, label: "Actual")
                    legendDot(color: overColor, label: "Over target")
                    Spacer()
                }
                .padding(.bottom, 12)
                MacroCompRowView(label: "Calories", actual: Int(totalCal),  planned: Int(vm.plannedCalories), color: caloriesColor, unit: "kcal")
                MacroCompRowView(label: "Carbs",    actual: Int(totalCarb), planned: Int(vm.plannedCarbs),    color: carbsColor,    unit: "g")
                MacroCompRowView(label: "Protein",  actual: Int(totalPro),  planned: Int(vm.plannedProtein),  color: proteinColor,  unit: "g")
                MacroCompRowView(label: "Fat",      actual: Int(totalFat_), planned: Int(vm.plannedFat),      color: fatColor,      unit: "g")
            }
            .padding(16)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .shadow(color: .black.opacity(0.05), radius: 3)
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
    }

    private func legendDot(color: Color, label: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 10, height: 10)
            Text(label).font(.caption).foregroundColor(.secondary)
        }
    }
}

// ── Macro Tile ────────────────────────────────────────────────────────────────
private struct MacroTileView: View {
    let label: String
    let value: Int
    let unit: String
    let planned: Int
    let color: Color

    private var fraction: CGFloat {
        planned > 0 ? min(1.0, CGFloat(value) / CGFloat(planned)) : 0
    }

    var body: some View {
        VStack(spacing: 2) {
            Text("\(value)")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(color)
            Text(unit)
                .font(.system(size: 10))
                .foregroundColor(color.opacity(0.7))
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(color.opacity(0.15))
                        .frame(height: 4)
                    RoundedRectangle(cornerRadius: 2)
                        .fill(color)
                        .frame(width: geo.size.width * fraction, height: 4)
                }
            }
            .frame(height: 4)
            .padding(.vertical, 3)
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// ── Slot Card ─────────────────────────────────────────────────────────────────
private struct SlotCard: View {
    let slotKey: String
    var titleOverride: String? = nil
    let foods: [LoggedFoodWithDetails]
    let plannedItems: [MealFoodItemWithDetails]
    let loggedFoodIds: Set<Int64>
    let isExpanded: Bool
    let onToggle: () -> Void
    let onAddFood: () -> Void
    let onDeleteFood: (Int64) -> Void
    var onDeleteSlot: (() -> Void)? = nil
    var isCustomDone: Bool = false
    var onToggleDone: (() -> Void)? = nil
    var isDraggable: Bool = false
    var onTogglePlannedFood: ((MealFoodItemWithDetails) -> Void)? = nil
    var onToggleAllPlannedFoods: (() -> Void)? = nil

    private var allPlannedTicked: Bool {
        !plannedItems.isEmpty && plannedItems.allSatisfy { loggedFoodIds.contains($0.food.id) }
    }
    private var loggedKcal: Int {
        Int(foods.reduce(0.0) { $0 + $1.food.calculateCalories(quantity: $1.loggedFood.quantity, unit: $1.loggedFood.unit) })
    }
    private var plannedKcal: Int {
        Int(plannedItems.reduce(0.0) { $0 + $1.food.calculateCalories(quantity: $1.mealFoodItem.quantity, unit: $1.mealFoodItem.unit) })
    }
    private var subtitle: String {
        switch (foods.isEmpty, plannedItems.isEmpty) {
        case (false, false): return "\(foods.count) logged · \(plannedItems.count) planned · \(loggedKcal) kcal"
        case (false, true):  return "\(foods.count) logged · \(loggedKcal) kcal"
        case (true, false):  return "\(plannedItems.count) planned · \(plannedKcal) kcal"
        case (true, true):   return "Nothing logged"
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header — emoji/name on left, tick-all + chevron on right
            HStack(spacing: 10) {
                // Left drag handle — visual affordance for the native List reorder gesture
                if isDraggable {
                    Image(systemName: "line.3.horizontal")
                        .font(.system(size: 13))
                        .foregroundColor(Color.gray.opacity(0.45))
                        .frame(width: 18)
                }
                ZStack {
                    Circle()
                        .fill(slotColor(slotKey).opacity(0.15))
                        .frame(width: 36, height: 36)
                    Text(slotEmoji(slotKey)).font(.system(size: 16))
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(titleOverride ?? slotDisplayName(slotKey))
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.primary)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                // Slot-level tick-all (planned slots)
                if !plannedItems.isEmpty {
                    ZStack {
                        Circle()
                            .fill(allPlannedTicked ? caloriesColor : Color.gray.opacity(0.15))
                            .frame(width: 24, height: 24)
                        Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(allPlannedTicked ? .white : Color.gray.opacity(0.4))
                    }
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
                    .highPriorityGesture(TapGesture().onEnded { onToggleAllPlannedFoods?() })
                }
                // Custom slot: independent done tick (not food-presence based)
                if onToggleDone != nil {
                    ZStack {
                        Circle()
                            .fill(isCustomDone ? caloriesColor : Color.gray.opacity(0.15))
                            .frame(width: 24, height: 24)
                        Image(systemName: "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(isCustomDone ? .white : Color.gray.opacity(0.4))
                    }
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
                    .highPriorityGesture(TapGesture().onEnded { onToggleDone?() })
                }
                Image(systemName: isExpanded ? "chevron.up" : "chevron.right")
                    .foregroundColor(.secondary)
                    .font(.system(size: 13))
            }
            .padding(12)
            .contentShape(Rectangle())
            .onTapGesture { onToggle() }

            if isExpanded {
                Divider().padding(.horizontal, 0)
                if foods.isEmpty && plannedItems.isEmpty {
                    Text("No foods logged")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                // Planned items — tappable to tick/untick (log/unlog)
                ForEach(Array(plannedItems.enumerated()), id: \.offset) { _, item in
                    let isTicked = loggedFoodIds.contains(item.food.id)
                    PlannedLogFoodRowView(
                        item: item,
                        isTicked: isTicked,
                        onTap: { onTogglePlannedFood?(item) }
                    )
                    Divider().padding(.horizontal, 12).opacity(0.3)
                }
                // Individually logged foods (green ticks) not already in planned
                let plannedFoodIds = Set(plannedItems.map { $0.food.id })
                ForEach(foods.filter { !plannedFoodIds.contains($0.loggedFood.foodId) }, id: \.loggedFood.id) { lf in
                    LogFoodRowView(food: lf, onDelete: { onDeleteFood(lf.loggedFood.id) })
                    Divider().padding(.horizontal, 12).opacity(0.4)
                }
                // Add Food + Delete Slot buttons
                HStack(spacing: 16) {
                    Button(action: onAddFood) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus").font(.system(size: 13))
                            Text("Add Food").font(.system(size: 13, weight: .medium))
                        }
                        .foregroundColor(Color(red: 0.18, green: 0.49, blue: 0.32))
                    }
                    Spacer()
                    if let onDeleteSlot = onDeleteSlot {
                        Button(action: onDeleteSlot) {
                            HStack(spacing: 4) {
                                Image(systemName: "trash").font(.system(size: 12))
                                Text("Delete Slot").font(.system(size: 12))
                            }
                            .foregroundColor(.red)
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
            }
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.05), radius: 3, x: 0, y: 1)
    }
}

// ── Food Row ──────────────────────────────────────────────────────────────────
private struct LogFoodRowView: View {
    let food: LoggedFoodWithDetails
    let onDelete: () -> Void

    private var kcal: Int {
        Int(food.food.calculateCalories(quantity: food.loggedFood.quantity, unit: food.loggedFood.unit))
    }
    private var carbs: Int {
        Int(food.food.calculateCarbs(quantity: food.loggedFood.quantity, unit: food.loggedFood.unit))
    }
    private var gi: Int32? { food.food.glycemicIndex?.int32Value }

    var body: some View {
        HStack(spacing: 10) {
            // Green logged circle
            ZStack {
                Circle().fill(caloriesColor).frame(width: 20, height: 20)
                Image(systemName: "checkmark").font(.system(size: 10, weight: .bold)).foregroundColor(.white)
            }
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(food.food.name)
                        .font(.system(size: 14))
                        .lineLimit(1)
                    if let gi = gi {
                        GiBadgeView(gi: Int(gi))
                    }
                }
                Text("\(Int(food.loggedFood.quantity))g · \(carbs)g carbs")
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
            }
            Spacer()
            Text("\(kcal) kcal")
                .font(.system(size: 12, weight: .medium))
            Button(action: onDelete) {
                Image(systemName: "xmark")
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
                    .frame(width: 28, height: 28)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }
}

// ── Planned Food Row ──────────────────────────────────────────────────────────
private struct PlannedLogFoodRowView: View {
    let item: MealFoodItemWithDetails
    var isTicked: Bool = false
    var onTap: (() -> Void)? = nil

    private var kcal: Int {
        Int(item.food.calculateCalories(quantity: item.mealFoodItem.quantity, unit: item.mealFoodItem.unit))
    }
    private var carbs: Int {
        Int(item.food.calculateCarbs(quantity: item.mealFoodItem.quantity, unit: item.mealFoodItem.unit))
    }
    private var gi: Int32? { item.food.glycemicIndex?.int32Value }

    var body: some View {
        Button(action: { onTap?() }) {
            HStack(spacing: 10) {
                // Tick indicator: green check if logged, grey circle if not
                if isTicked {
                    ZStack {
                        Circle().fill(caloriesColor).frame(width: 20, height: 20)
                        Image(systemName: "checkmark")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.white)
                    }
                } else {
                    Circle()
                        .fill(Color.gray.opacity(0.15))
                        .frame(width: 20, height: 20)
                }
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(item.food.name)
                            .font(.system(size: 14))
                            .foregroundColor(isTicked ? .primary : .secondary)
                            .lineLimit(1)
                        if let gi = gi {
                            GiBadgeView(gi: Int(gi))
                        }
                    }
                    Text("\(Int(item.mealFoodItem.quantity))g · \(carbs)g carbs")
                        .font(.system(size: 11))
                        .foregroundColor(Color.secondary.opacity(0.7))
                }
                Spacer()
                Text("\(kcal) kcal")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                Spacer().frame(width: 28)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }
}

// ── GI Badge ──────────────────────────────────────────────────────────────────
private struct GiBadgeView: View {
    let gi: Int
    private var bg: Color {
        gi <= 55 ? caloriesColor : gi <= 69 ? carbsColor : Color.red
    }
    var body: some View {
        Text("GI \(gi)")
            .font(.system(size: 9, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 5)
            .padding(.vertical, 2)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

// ── Macro Comparison Row ──────────────────────────────────────────────────────
private struct MacroCompRowView: View {
    let label: String
    let actual: Int
    let planned: Int
    let color: Color
    let unit: String

    private var diff: Int { actual - planned }
    private var isOver: Bool { diff > 0 && planned > 0 }
    private var barColor: Color { isOver ? overColor : color }
    private var fraction: CGFloat {
        planned > 0 ? min(1.0, CGFloat(actual) / CGFloat(planned)) : 0
    }

    var body: some View {
        VStack(spacing: 4) {
            HStack {
                Text(label)
                    .font(.system(size: 13, weight: .medium))
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("Plan: \(planned)\(unit)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("\(actual)\(unit)")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(barColor)
                if planned > 0 {
                    Text(diff >= 0 ? "+\(diff)" : "\(diff)")
                        .font(.caption)
                        .foregroundColor(isOver ? overColor : caloriesColor)
                }
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color.gray.opacity(0.15))
                        .frame(height: 6)
                    RoundedRectangle(cornerRadius: 3)
                        .fill(barColor)
                        .frame(width: geo.size.width * fraction, height: 6)
                }
            }
            .frame(height: 6)
        }
        .padding(.bottom, 10)
    }
}

// ── Food Picker Sheet ─────────────────────────────────────────────────────────
struct FoodPickerSheet: View {
    @Environment(\.dismiss) private var dismiss
    let slotKey: String
    let userId: Int64
    let date: String
    let onLogged: () -> Void

    @StateObject private var foodsVM = FoodsViewModel()
    @State private var searchText = ""
    @State private var selectedFood: FoodItem?   // KMP FoodItem
    @State private var quantityText = "100"

    private var filtered: [FoodItem] {
        searchText.isEmpty
            ? foodsVM.foods
            : foodsVM.foods.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }
    private var logRepo: DailyLogRepository { RepositoryProvider.shared.dailyLogRepository }

    var body: some View {
        NavigationStack {
            Group {
                if selectedFood == nil {
                    foodList
                } else {
                    quantityPicker
                }
            }
            .navigationTitle("Add Food to \(slotDisplayName(slotKey))")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if selectedFood != nil {
                        Button("Back") { selectedFood = nil; quantityText = "100" }
                    } else {
                        Button("Cancel") { dismiss() }
                    }
                }
            }
        }
        .onAppear { foodsVM.loadFoods() }
    }

    private var foodList: some View {
        VStack(spacing: 0) {
            HStack {
                Image(systemName: "magnifyingglass").foregroundColor(.gray)
                TextField("Search foods…", text: $searchText)
                    .onChange(of: searchText) { q in
                        if q.isEmpty { foodsVM.loadFoods() } else { foodsVM.searchFoods(query: q) }
                    }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            List(filtered, id: \.id) { food in
                Button(action: { selectedFood = food; quantityText = "100" }) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(food.name).font(.system(size: 14)).foregroundColor(.primary)
                            Text("\(Int(food.caloriesPer100)) kcal/100g · P:\(Int(food.proteinPer100))g C:\(Int(food.carbsPer100))g")
                                .font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        if let gi = food.glycemicIndex {
                            GiBadgeView(gi: Int(gi.int32Value))
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }

    private var quantityPicker: some View {
        let food = selectedFood!
        let qty = Double(quantityText) ?? 100.0
        let kcal    = Int(food.caloriesPer100 * qty / 100)
        let protein = Int(food.proteinPer100  * qty / 100)
        let carbs   = Int(food.carbsPer100    * qty / 100)
        let fat     = Int(food.fatPer100      * qty / 100)

        return VStack(spacing: 16) {
            Text(food.name)
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
            Text("\(kcal) kcal · P:\(protein)g C:\(carbs)g F:\(fat)g")
                .font(.subheadline).foregroundColor(.secondary)
            TextField("Quantity (grams)", text: $quantityText)
                .keyboardType(.decimalPad)
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal, 16)
            Button(action: { logFood(food: food, qty: qty) }) {
                Text("Log Food")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color(red: 0.18, green: 0.49, blue: 0.32))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.horizontal, 16)
            Spacer()
        }
    }

    private func logFood(food: FoodItem, qty: Double) {
        Task {
            let lf = LoggedFood(
                id: 0, userId: userId, logDate: date, foodId: food.id,
                quantity: qty, unit: FoodUnit.gram,
                slotType: slotKey, timestamp: nil, notes: nil
            )
            _ = try? await logRepo.insertLoggedFood(loggedFood: lf)
            await MainActor.run {
                onLogged()
                dismiss()
            }
        }
    }
}

