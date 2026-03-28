import SwiftUI
import Charts
import shared

// MARK: - Design tokens
private let primaryGreen = Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0)
private let lightGreenBg = Color(red: 0xF0/255.0, green: 0xF9/255.0, blue: 0xF4/255.0)
private let carbsColor   = Color(red: 0xF5/255.0, green: 0xA6/255.0, blue: 0x23/255.0)
private let proteinColor = Color(red: 0x4A/255.0, green: 0x90/255.0, blue: 0xD9/255.0)
private let fatColor     = Color(red: 0xE9/255.0, green: 0x1E/255.0, blue: 0x8C/255.0)
private let weekOrange   = Color(red: 0xF5/255.0, green: 0x7C/255.0, blue: 0x00/255.0)
private let weekRed      = Color(red: 0xD3/255.0, green: 0x2F/255.0, blue: 0x2F/255.0)

// MARK: - HomeScreen

struct HomeScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = HomeViewModel()
    var onNavigateToLogWithDate: ((String) -> Void)?

    @State private var showDietPicker = false
    @State private var mealDetailSlot: TodayPlanSlot? = nil
    @State private var showProfile = false
    @State private var showNavMenu = false
    @State private var showFoodsFromMenu = false
    @State private var showMealsFromMenu = false
    @State private var showSettingsFromMenu = false
    @State private var menuPendingAction: (() -> Void)?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // ── Green header hero ────────────────────────
                HomeHeaderSection(
                    userName: viewModel.userName,
                    userInitial: viewModel.userInitial,
                    caloriesConsumed: viewModel.todayCalories,
                    calorieGoal: viewModel.calorieGoal,
                    onAvatarTap: { showProfile = true },
                    onMenuTap: { showNavMenu = true },
                    isDark: appState.isDarkMode,
                    onThemeToggle: { appState.setDarkMode(!appState.isDarkMode) }
                )

                // ── Content below header ─────────────────────
                VStack(spacing: 12) {
                    MacroRingsCard(
                        calories: viewModel.todayCalories,
                        protein: viewModel.todayProtein,
                        carbs: viewModel.todayCarbs,
                        fat: viewModel.todayFat,
                        calorieGoal: viewModel.calorieGoal
                    )
                    .padding(.horizontal, 16)

                    ThisWeekCard(
                        weekDays: viewModel.weekDays,
                        weekOffset: viewModel.weekOffset,
                        onDayTap: { isoDate in onNavigateToLogWithDate?(isoDate) },
                        onPreviousWeek: { viewModel.previousWeek() },
                        onNextWeek: { viewModel.nextWeek() }
                    )
                    .padding(.horizontal, 16)

                    TodaysPlanCard(
                        slots: mergedSlots,
                        onLogTodayTap: { onNavigateToLogWithDate?(isoToday()) },
                        onSlotToggle: { slot in viewModel.toggleSlotLogged(slot: slot) },
                        onPlanDietTap: { showDietPicker = true },
                        onSlotTap: { slot in
                            if slot.plannedMealId != nil { mealDetailSlot = slot }
                        }
                    )
                    .padding(.horizontal, 16)

                    StatsRow(
                        hba1c: viewModel.latestHba1c,
                        weight: viewModel.latestWeight,
                        dayStreak: viewModel.dayStreak
                    )
                    .padding(.horizontal, 16)

                    Spacer().frame(height: 20)
                }
                .padding(.top, 16)
                .background(Color(.systemGroupedBackground))
            }
        }
        .background(Color(.systemGroupedBackground))
        .ignoresSafeArea(edges: .top)
        .navigationBarHidden(true)
        .sheet(isPresented: $showDietPicker, onDismiss: {
            if let userId = appState.currentUserId { viewModel.load(userId: userId) }
        }) {
            HomeDietPickerSheet { diet in
                if let userId = appState.currentUserId {
                    viewModel.assignDietForToday(userId: userId, diet: diet)
                }
                showDietPicker = false
            }
        }
        .sheet(item: $mealDetailSlot) { slot in
            HomeMealDetailSheet(slot: slot)
        }
        .sheet(isPresented: $showProfile, onDismiss: {
            if let userId = appState.currentUserId { viewModel.load(userId: userId) }
        }) {
            NavigationStack {
                ProfileScreen()
            }
            .environmentObject(appState)
        }
        .sheet(isPresented: $showNavMenu, onDismiss: {
            menuPendingAction?()
            menuPendingAction = nil
        }) {
            HomeNavMenuSheet(onAction: { action in
                menuPendingAction = action
                showNavMenu = false
            })
            .environmentObject(appState)
        }
        .sheet(isPresented: $showFoodsFromMenu) {
            NavigationStack { FoodsScreen() }
                .environmentObject(appState)
        }
        .sheet(isPresented: $showMealsFromMenu) {
            NavigationStack { MealsScreen() }
                .environmentObject(appState)
        }
        .sheet(isPresented: $showSettingsFromMenu) {
            NavigationStack { SettingsScreen() }
                .environmentObject(appState)
        }
        .onAppear {
            if let userId = appState.currentUserId {
                viewModel.load(userId: userId)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .navigateToFoods)) { _ in
            showFoodsFromMenu = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .navigateToMeals)) { _ in
            showMealsFromMenu = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .navigateToSettings)) { _ in
            showSettingsFromMenu = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .navigateToProfile)) { _ in
            showProfile = true
        }
        .onChange(of: appState.customSlotsVersion) { _ in
            if let userId = appState.currentUserId { viewModel.load(userId: userId) }
        }
    }

    private func isoToday() -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    // Slots to display: ViewModel slots + custom slots from AppState (always live, no polling needed)
    private var mergedSlots: [TodayPlanSlot] {
        let vmKeys = Set(viewModel.todayPlanSlots.map { $0.slotType })
        let extra = appState.todayCustomSlots
            .map { def in
                TodayPlanSlot(
                    slotType: "CUSTOM_\(def.id)",
                    slotDisplayName: def.name,
                    emoji: "✦",
                    plannedMealName: nil,
                    plannedMealId: nil,
                    loggedMealId: nil,
                    isLogged: false,
                    loggedFoods: [],
                    isCustom: true
                )
            }
            .filter { !vmKeys.contains($0.slotType) }
        return viewModel.todayPlanSlots + extra
    }
}

// MARK: - Header section

private struct HomeHeaderSection: View {
    let userName: String
    let userInitial: String
    let caloriesConsumed: Double
    let calorieGoal: Double
    var onAvatarTap: (() -> Void)? = nil
    var onMenuTap: (() -> Void)? = nil
    var isDark: Bool = false
    var onThemeToggle: (() -> Void)? = nil

    private var isOver: Bool { caloriesConsumed > calorieGoal }
    private var progress: Double { min(caloriesConsumed / calorieGoal, 1.0) }
    private var diff: Int { abs(Int(caloriesConsumed) - Int(calorieGoal)) }
    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12: return "Good morning"
        case 12..<17: return "Good afternoon"
        default: return "Good evening"
        }
    }

    var body: some View {
        ZStack(alignment: .top) {
            primaryGreen.ignoresSafeArea(edges: .top)

            VStack(spacing: 16) {
                // Safe area spacer
                Spacer().frame(height: 56)

                // Greeting row
                HStack {
                    Button(action: { onMenuTap?() }) {
                        Image(systemName: "line.3.horizontal")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                    }
                    .padding(.trailing, 8)
                    Text("\(greeting), \(userName.isEmpty ? "there" : userName) 👋")
                        .font(.title3)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                    Spacer()
                    // Theme toggle
                    Button(action: { onThemeToggle?() }) {
                        Image(systemName: isDark ? "sun.max.fill" : "moon.fill")
                            .font(.system(size: 18))
                            .foregroundColor(.white)
                    }
                    // Bell
                    Button(action: {}) {
                        Image(systemName: "bell")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                    }
                    // Avatar — tap → profile
                    Button(action: { onAvatarTap?() }) {
                        Circle()
                            .fill(Color.white.opacity(0.25))
                            .frame(width: 36, height: 36)
                            .overlay(
                                Text(userInitial)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(.white)
                            )
                    }
                    .padding(.leading, 8)
                }
                .padding(.horizontal, 20)

                // Calorie target card
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Today's Target")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.8))
                        Spacer()
                        Text("\(Int(caloriesConsumed)) / \(Int(calorieGoal)) kcal")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.8))
                    }

                    // Progress bar
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(Color.white.opacity(0.3))
                                .frame(height: 8)
                            Capsule()
                                .fill(isOver ? Color.orange : Color.white)
                                .frame(width: geo.size.width * CGFloat(progress), height: 8)
                        }
                    }
                    .frame(height: 8)

                    if isOver {
                        Text("🔴 \(diff) kcal over")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.white)
                    } else {
                        Text("🔥 \(diff) kcal remaining")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.white)
                    }
                }
                .padding(16)
                .background(Color.white.opacity(0.15))
                .cornerRadius(16)
                .padding(.horizontal, 16)

                Spacer().frame(height: 8)
            }
        }
        .frame(minHeight: 220)
    }
}

// MARK: - Macro rings card

private struct MacroRingsCard: View {
    let calories: Double
    let protein: Double
    let carbs: Double
    let fat: Double
    let calorieGoal: Double

    private let carbGoal: Double = 250
    private let protGoal: Double = 60
    private let fatGoal: Double = 65

    var body: some View {
        HStack(spacing: 0) {
            MacroRingItem(label: "Carbs", value: Int(carbs), goal: Int(carbGoal), unit: "g", color: carbsColor)
            MacroRingItem(label: "Protein", value: Int(protein), goal: Int(protGoal), unit: "g", color: proteinColor)
            MacroRingItem(label: "Fat", value: Int(fat), goal: Int(fatGoal), unit: "g", color: fatColor)
            MacroRingItem(label: "Calories", value: Int(calories), goal: Int(calorieGoal), unit: "kcal", color: primaryGreen)
        }
        .padding(.vertical, 16)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
    }
}

private struct MacroRingItem: View {
    let label: String
    let value: Int
    let goal: Int
    let unit: String
    let color: Color

    private var progress: Double {
        guard goal > 0 else { return 0 }
        return min(Double(value) / Double(goal), 1.0)
    }
    private var pct: Int { Int(progress * 100) }

    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .stroke(color.opacity(0.2), lineWidth: 8)
                    .frame(width: 60, height: 60)
                Circle()
                    .trim(from: 0, to: CGFloat(progress))
                    .stroke(color, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: 60, height: 60)
                Text("\(value)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.primary)
            }
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
            Text("\(pct)%")
                .font(.caption2)
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Quick Log Food button

private struct QuickLogFoodButton: View {
    var onTap: (() -> Void)?
    var body: some View {
        Button(action: { onTap?() }) {
            HStack(spacing: 8) {
                Image(systemName: "book.closed.fill")
                    .font(.system(size: 16))
                Text("Quick Log Food")
                    .font(.system(size: 16, weight: .semibold))
                Spacer()
                Image(systemName: "plus.circle.fill")
                    .font(.system(size: 20))
            }
            .foregroundColor(.white)
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(primaryGreen)
            .cornerRadius(14)
        }
    }
}

// MARK: - This Week card

private struct ThisWeekCard: View {
    let weekDays: [WeekDayInfo]
    var weekOffset: Int = 0
    var onDayTap: ((String) -> Void)?
    var onPreviousWeek: (() -> Void)?
    var onNextWeek: (() -> Void)?

    private var weekLabel: String {
        if weekOffset == 0 { return "This Week" }
        if weekOffset == -1 { return "Last Week" }
        return "\(-weekOffset) weeks ago"
    }

    private var monthYear: String {
        guard let firstDay = weekDays.first?.date else {
            let f = DateFormatter(); f.dateFormat = "MMMM yyyy"; return f.string(from: Date())
        }
        let f = DateFormatter(); f.dateFormat = "MMMM yyyy"; return f.string(from: firstDay)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                // Prev week arrow
                Button(action: { onPreviousWeek?() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(primaryGreen)
                        .frame(width: 28, height: 28)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(weekLabel)
                        .font(.system(size: 15, weight: .semibold))
                    Text(monthYear)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                // Next week arrow (disabled at current week)
                Button(action: { onNextWeek?() }) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(weekOffset >= 0 ? Color.gray.opacity(0.3) : primaryGreen)
                        .frame(width: 28, height: 28)
                }
                .disabled(weekOffset >= 0)
            }

            HStack(spacing: 0) {
                ForEach(weekDays, id: \.isoDate) { info in
                    WeekDayCell(
                        info: info,
                        onTap: { onDayTap?(info.isoDate) }
                    )
                    .frame(maxWidth: .infinity)
                }
            }

            // Legend
            HStack(spacing: 12) {
                Spacer()
                legendDot(color: primaryGreen, label: "Done")
                legendDot(color: weekOrange, label: "Planned")
                legendDot(color: weekRed, label: "Missed")
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
    }

    private func legendDot(color: Color, label: String) -> some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(label).font(.caption2).foregroundColor(.secondary)
        }
    }
}

private struct WeekDayCell: View {
    let info: WeekDayInfo
    var onTap: (() -> Void)?

    private var dayLetter: String {
        // Show first letter of day name (locale-aware)
        let cal = Calendar.current
        let weekday = cal.component(.weekday, from: info.date) // 1=Sun…7=Sat
        return cal.shortWeekdaySymbols[weekday - 1].prefix(1).uppercased()
    }

    private var dayNumber: String {
        let cal = Calendar.current
        return "\(cal.component(.day, from: info.date))"
    }

    private var isToday: Bool {
        Calendar.current.isDateInToday(info.date)
    }

    private var fillColor: Color {
        switch info.state {
        case .completed:
            return primaryGreen
        case .plannedFuture:
            return weekOrange.opacity(0.6)
        case .missed:
            return weekRed.opacity(0.6)
        case .noData:
            return Color.gray.opacity(0.15)
        }
    }

    private var textColor: Color {
        switch info.state {
        case .completed: return .white
        case .plannedFuture: return .white
        case .missed: return .white
        case .noData: return .gray
        }
    }

    var body: some View {
        VStack(spacing: 4) {
            Text(dayLetter)
                .font(.caption2)
                .foregroundColor(.secondary)
            ZStack {
                Circle()
                    .fill(fillColor)
                    .frame(width: 30, height: 30)
                Text(dayNumber)
                    .font(.caption)
                    .fontWeight(isToday ? .bold : .regular)
                    .foregroundColor(textColor)
            }
            .onTapGesture { onTap?() }
            // Diet label below circle
            if let label = info.dietLabel {
                Text(label)
                    .font(.system(size: 8))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            } else {
                Text(" ")
                    .font(.system(size: 8))
            }
        }
    }
}

// MARK: - Blood Glucose card

private struct BloodGlucoseCard: View {
    let latestSugar: HealthMetric?
    let glucoseHistory: [HealthMetric]
    let onDetailsTap: () -> Void

    private var latestValue: Double { latestSugar?.value ?? 0 }
    private var isInRange: Bool { latestValue >= 80 && latestValue <= 130 }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header row
            HStack {
                Image(systemName: "drop.fill")
                    .foregroundColor(.red)
                Text("Blood Glucose")
                    .font(.system(size: 15, weight: .semibold))
                Spacer()
                Button("Details >") { onDetailsTap() }
                    .font(.caption)
                    .foregroundColor(primaryGreen)
            }

            if latestSugar != nil {
                // Value row
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("\(Int(latestValue))")
                        .font(.system(size: 40, weight: .bold))
                    Text("mg/dL")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    // In Range badge
                    Text(isInRange ? "In Range" : "Out of Range")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(isInRange ? primaryGreen : Color.red)
                        .cornerRadius(20)
                }

                // Target range label
                Text("Target Range: 80–130 mg/dL")
                    .font(.caption2)
                    .foregroundColor(.secondary)

                // Line chart
                if !glucoseHistory.isEmpty {
                    GlucoseChart(history: glucoseHistory)
                        .frame(height: 80)
                }
            } else {
                Text("No data logged today")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
    }
}

private struct GlucoseChart: View {
    let history: [HealthMetric]

    private var sortedHistory: [HealthMetric] {
        history.sorted { $0.date < $1.date }
    }

    var body: some View {
        Chart {
            ForEach(Array(sortedHistory.enumerated()), id: \.offset) { index, metric in
                LineMark(
                    x: .value("Day", index),
                    y: .value("Glucose", metric.value)
                )
                .foregroundStyle(primaryGreen)
                .interpolationMethod(.catmullRom)

                AreaMark(
                    x: .value("Day", index),
                    yStart: .value("Min", 80.0),
                    yEnd: .value("Glucose", metric.value)
                )
                .foregroundStyle(primaryGreen.opacity(0.1))
            }

            RuleMark(y: .value("Upper", 130.0))
                .foregroundStyle(Color.orange.opacity(0.5))
                .lineStyle(StrokeStyle(dash: [4]))
            RuleMark(y: .value("Lower", 80.0))
                .foregroundStyle(Color.orange.opacity(0.5))
                .lineStyle(StrokeStyle(dash: [4]))
        }
        .chartXAxis(.hidden)
        .chartYAxis {
            AxisMarks(values: [80.0, 130.0]) { _ in
                AxisValueLabel()
                    .font(.caption2)
            }
        }
    }
}

// MARK: - Stats row

private struct StatsRow: View {
    let hba1c: HealthMetric?
    let weight: HealthMetric?
    let dayStreak: Int

    var body: some View {
        HStack(spacing: 8) {
            StatCard(
                icon: "waveform.path.ecg",
                iconColor: Color.purple,
                label: "A1C",
                value: hba1c.map { String(format: "%.1f%%", $0.value) } ?? "--"
            )
            StatCard(
                icon: "scalemass.fill",
                iconColor: Color.blue,
                label: "Weight",
                value: weight.map { String(format: "%.1f lbs", $0.value) } ?? "--"
            )
            StatCard(
                icon: "flame.fill",
                iconColor: Color.orange,
                label: "Streak",
                value: "\(dayStreak) days"
            )
        }
    }
}

private struct StatCard: View {
    let icon: String
    let iconColor: Color
    let label: String
    let value: String
    var onClick: (() -> Void)? = nil

    var body: some View {
        Button(action: { onClick?() }) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(iconColor)
                Text(value)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.primary)
                Text(label)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Today's Plan card

private struct TodaysPlanCard: View {
    let slots: [TodayPlanSlot]
    let onLogTodayTap: () -> Void
    var onSlotToggle: ((TodayPlanSlot) -> Void)?
    var onPlanDietTap: (() -> Void)?
    var onSlotTap: ((TodayPlanSlot) -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Today's Plan")
                    .font(.system(size: 15, weight: .semibold))
                Spacer()
                Button(slots.isEmpty ? "Plan a Diet" : "Change Diet") {
                    onPlanDietTap?()
                }
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(primaryGreen)
            }

            if slots.isEmpty {
                VStack(spacing: 6) {
                    Image(systemName: "calendar.badge.plus")
                        .font(.system(size: 28))
                        .foregroundColor(.secondary)
                    Text("No diet planned for today — tap Plan a Diet to add meals")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
            } else {
                ForEach(slots) { slot in
                    TodayPlanSlotRow(
                        slot: slot,
                        onToggle: { onSlotToggle?(slot) },
                        onTap: slot.plannedMealId != nil ? { onSlotTap?(slot) } : nil
                    )
                    if slot.id != slots.last?.id {
                        Divider()
                    }
                }
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
    }
}

private struct TodayPlanSlotRow: View {
    let slot: TodayPlanSlot
    var onToggle: (() -> Void)?
    var onTap: (() -> Void)?
    @State private var isExpanded: Bool = false

    private var canToggle: Bool { slot.plannedMealId != nil || slot.isLogged || slot.isCustom }
    private var hasLoggedFoods: Bool { !slot.loggedFoods.isEmpty }

    private var emojiBgColor: Color {
        if slot.isCustom { return primaryGreen.opacity(0.10) }
        switch slot.slotType.uppercased() {
        case "BREAKFAST":    return Color.orange.opacity(0.15)
        case "LUNCH":        return Color.yellow.opacity(0.15)
        case "DINNER":       return Color.blue.opacity(0.15)
        case "EARLY_MORNING": return Color.pink.opacity(0.12)
        case "PRE_WORKOUT":  return Color.purple.opacity(0.12)
        case "POST_WORKOUT": return Color.teal.opacity(0.12)
        default:             return primaryGreen.opacity(0.12)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Row header
            HStack(spacing: 12) {
                // Emoji circle
                ZStack {
                    Circle()
                        .fill(emojiBgColor)
                        .frame(width: 40, height: 40)
                    Text(slot.emoji)
                        .font(.system(size: 18))
                }

                // Slot name + subtitle
                VStack(alignment: .leading, spacing: 2) {
                    Text(slot.slotDisplayName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.primary)
                    if let mealName = slot.plannedMealName {
                        Text(mealName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    } else if hasLoggedFoods && !isExpanded {
                        Text("\(slot.loggedFoods.count) logged")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                // Expand/collapse chevron for slots with logged foods
                if hasLoggedFoods {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundColor(.secondary)
                } else if onTap != nil {
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                // Logged indicator
                Group {
                    if slot.isLogged {
                        ZStack {
                            Circle().fill(primaryGreen).frame(width: 26, height: 26)
                            Image(systemName: "checkmark")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white)
                        }
                    } else {
                        Circle()
                            .stroke(Color.gray.opacity(0.4), lineWidth: 2)
                            .frame(width: 26, height: 26)
                    }
                }
                .onTapGesture { if canToggle { onToggle?() } }
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
            .onTapGesture {
                if hasLoggedFoods {
                    withAnimation(.easeInOut(duration: 0.15)) { isExpanded.toggle() }
                } else {
                    onTap?()
                }
            }

            // Expanded logged foods list
            if isExpanded && hasLoggedFoods {
                Divider().padding(.leading, 52)
                ForEach(slot.loggedFoods, id: \.loggedFood.id) { lf in
                    HStack(spacing: 8) {
                        Circle().fill(primaryGreen).frame(width: 6, height: 6)
                            .padding(.leading, 52)
                        Text(lf.food.name)
                            .font(.caption)
                            .foregroundColor(.primary)
                            .lineLimit(1)
                        Spacer()
                        Text("\(Int(lf.food.calculateCalories(quantity: lf.loggedFood.quantity, unit: lf.loggedFood.unit))) kcal")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 3)
                    .padding(.trailing, 4)
                }
            }
        }
    }
}

// MARK: - Home Diet Picker Sheet
// Now wraps the full DietsScreen in picker mode so users can search, filter by tag/ingredient, and select.

struct HomeDietPickerSheet: View {
    @EnvironmentObject var appState: AppState
    let onSelect: (Diet) -> Void

    var body: some View {
        NavigationStack {
            DietsScreen(onSelect: { summary in
                let now = Int64(Date().timeIntervalSince1970 * 1000)
                let diet = Diet(
                    id: summary.id,
                    userId: summary.userId,
                    name: summary.name,
                    description: summary.description_,
                    createdAt: now,
                    isSystemDiet: false,
                    serverId: nil,
                    updatedAt: now,
                    syncedAt: nil
                )
                onSelect(diet)
            })
        }
        .environmentObject(appState)
    }
}

// MARK: - Home Meal Detail Sheet (ingredient checklist view)

struct HomeMealDetailSheet: View {
    let slot: TodayPlanSlot
    @StateObject private var mealsVM = MealsViewModel()
    @State private var mealWithFoods: MealWithFoods? = nil
    @State private var isLoading = true
    @State private var checkedIds: Set<Int64> = []

    enum SortOrderHMD { case none, az, qty }
    @State private var sortOrder: SortOrderHMD = .none

    private func sortedItems(_ items: [MealFoodItemWithDetails]) -> [MealFoodItemWithDetails] {
        switch sortOrder {
        case .none: return items
        case .az:   return items.sorted { $0.food.name < $1.food.name }
        case .qty:  return items.sorted { $0.mealFoodItem.quantity > $1.mealFoodItem.quantity }
        }
    }

    private func quantityText(_ item: MealFoodItemWithDetails) -> String {
        let qty = item.mealFoodItem.quantity
        let unitName = item.mealFoodItem.unit.name
        let formatted = qty == floor(qty) ? "\(Int(qty))" : String(format: "%.1f", qty)
        switch unitName {
        case "GRAM":    return "\(formatted)g"
        case "ML":      return "\(formatted)ml"
        case "SERVING": return "\(formatted) srv"
        case "PIECE":   return "\(formatted) pcs"
        default:        return "\(formatted) \(unitName.lowercased())"
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let mwf = mealWithFoods {
                    let allItems = (mwf.items as? NSArray)?.compactMap { $0 as? MealFoodItemWithDetails } ?? []
                    let displayItems = sortedItems(allItems)
                    let checkedCount = allItems.filter { checkedIds.contains($0.food.id) }.count

                    List {
                        // Macro header
                        Section {
                            HStack {
                                MacroMiniTile(label: "Calories", value: "\(Int(mwf.totalCalories))", unit: "kcal", color: primaryGreen)
                                MacroMiniTile(label: "Protein",  value: "\(Int(mwf.totalProtein))g",  unit: "", color: .blue)
                                MacroMiniTile(label: "Carbs",    value: "\(Int(mwf.totalCarbs))g",    unit: "", color: .orange)
                                MacroMiniTile(label: "Fat",      value: "\(Int(mwf.totalFat))g",      unit: "", color: .pink)
                            }
                        }

                        // Sort chips + progress bar
                        Section {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    hmdSortChip("A–Z", active: sortOrder == .az) {
                                        sortOrder = sortOrder == .az ? .none : .az
                                    }
                                    hmdSortChip("Qty ↓", active: sortOrder == .qty) {
                                        sortOrder = sortOrder == .qty ? .none : .qty
                                    }
                                }
                                .padding(.vertical, 2)
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text("\(checkedCount) of \(allItems.count) prepared")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Spacer()
                                    if checkedCount > 0 {
                                        Button("Clear all") { checkedIds.removeAll() }
                                            .font(.caption)
                                            .foregroundColor(.red)
                                    }
                                }
                                ProgressView(value: Double(checkedCount), total: Double(max(allItems.count, 1)))
                                    .tint(.green)
                            }
                        }

                        // Ingredients
                        Section("Ingredients (\(allItems.count))") {
                            ForEach(Array(displayItems.enumerated()), id: \.offset) { _, item in
                                let isChecked = checkedIds.contains(item.food.id)
                                Button(action: {
                                    if isChecked {
                                        checkedIds.remove(item.food.id)
                                    } else {
                                        checkedIds.insert(item.food.id)
                                    }
                                }) {
                                    HStack(spacing: 10) {
                                        Image(systemName: isChecked ? "checkmark.circle.fill" : "circle")
                                            .foregroundColor(isChecked ? .green : .secondary)
                                            .font(.title3)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(item.food.name)
                                                .font(.system(size: 15, weight: .medium))
                                                .strikethrough(isChecked, color: .secondary)
                                                .foregroundColor(isChecked ? .secondary : .primary)
                                            Text("\(quantityText(item)) · \(Int(item.calculatedCalories)) kcal")
                                                .font(.caption).foregroundColor(.secondary)
                                        }
                                        Spacer()
                                        VStack(alignment: .trailing, spacing: 2) {
                                            Text("P \(Int(item.calculatedProtein))g").font(.caption2).foregroundColor(.blue)
                                            Text("C \(Int(item.calculatedCarbs))g").font(.caption2).foregroundColor(.orange)
                                            Text("F \(Int(item.calculatedFat))g").font(.caption2).foregroundColor(.pink)
                                        }
                                    }
                                    .padding(.vertical, 2)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                } else {
                    Text("No meal details available").foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .navigationTitle(slot.plannedMealName ?? slot.slotDisplayName)
            .navigationBarTitleDisplayMode(.inline)
        }
        .onAppear {
            guard let mealId = slot.plannedMealId else { isLoading = false; return }
            Task {
                mealWithFoods = try? await mealsVM.getMealWithFoods(mealId: mealId)
                isLoading = false
            }
        }
    }

    @ViewBuilder
    private func hmdSortChip(_ label: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.caption)
                .padding(.horizontal, 12).padding(.vertical, 5)
                .background(active ? Color.green : Color(.systemGray5))
                .foregroundColor(active ? .white : .primary)
                .cornerRadius(14)
        }
        .buttonStyle(.plain)
    }
}

private struct MacroMiniTile: View {
    let label: String; let value: String; let unit: String; let color: Color
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.system(size: 13, weight: .bold)).foregroundColor(color)
            if !unit.isEmpty { Text(unit).font(.caption2).foregroundColor(.secondary) }
            Text(label).font(.caption2).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Sync Status Banner (kept for profile use)

private struct SyncStatusBanner: View {
    @ObservedObject var syncVM: SyncViewModel
    let onSyncTap: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            if syncVM.isSyncing {
                ProgressView().scaleEffect(0.8)
                Text("Syncing…")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else {
                Image(systemName: syncVM.syncError != nil ? "exclamationmark.icloud" : "checkmark.icloud")
                    .foregroundColor(syncVM.syncError != nil ? .orange : .green)
                    .font(.caption)
                VStack(alignment: .leading, spacing: 1) {
                    if let err = syncVM.syncError {
                        Text(err)
                            .font(.caption2)
                            .foregroundColor(.orange)
                            .lineLimit(1)
                    } else {
                        Text("Last sync: \(syncVM.lastSyncDisplay)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
            Spacer()
            if !syncVM.isSyncing {
                Button(action: onSyncTap) {
                    Text("Sync")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.green)
                        .cornerRadius(8)
                }
            }
        }
        .padding(10)
        .background(Color(.systemBackground))
        .cornerRadius(10)
        .shadow(color: .black.opacity(0.06), radius: 3)
    }
}

// MARK: - Nav Menu Sheet

struct HomeNavMenuSheet: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) private var dismiss
    var onAction: ((@escaping () -> Void) -> Void)?

    private func go(_ action: @escaping () -> Void) {
        if let onAction = onAction {
            onAction(action)
        } else {
            dismiss()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { action() }
        }
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Label("Home", systemImage: "house.fill")
                        .onTapGesture { dismiss() }
                    Label("Foods", systemImage: "leaf.fill")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToFoods, object: nil) }
                        }
                    Label("Meals", systemImage: "fork.knife")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToMeals, object: nil) }
                        }
                    Label("Diets", systemImage: "list.clipboard")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToTab, object: 3) }
                        }
                    Label("Log Today", systemImage: "square.and.pencil")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToLog, object: nil) }
                        }
                    Label("Health", systemImage: "heart.fill")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToTab, object: 4) }
                        }
                    Label("Grocery Lists", systemImage: "cart.fill")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToTab, object: 5) }
                        }
                    Label("Meal Plan", systemImage: "calendar")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToTab, object: 1) }
                        }
                }
                Section {
                    Label("Settings", systemImage: "gearshape.fill")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToSettings, object: nil) }
                        }
                    Label("Profile", systemImage: "person.circle.fill")
                        .onTapGesture {
                            go { NotificationCenter.default.post(name: .navigateToProfile, object: nil) }
                        }
                    Label("Logout", systemImage: "rectangle.portrait.and.arrow.right")
                        .foregroundColor(.red)
                        .onTapGesture {
                            dismiss()
                            appState.logout()
                        }
                }
            }
            .navigationTitle("MealPlan+")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Preview

struct HomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        HomeScreen()
            .environmentObject(AppState())
    }
}
