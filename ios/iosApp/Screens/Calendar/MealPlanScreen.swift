import SwiftUI
import shared

// MARK: - Colors
private let darkGreen = Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0)
private let lightGreenBg = Color(red: 0xE8/255.0, green: 0xF5/255.0, blue: 0xE9/255.0)
private let yellowBg = Color(red: 0xFF/255.0, green: 0xFD/255.0, blue: 0xE7/255.0)
private let tagPurple = Color(red: 0x7B/255.0, green: 0x1F/255.0, blue: 0xA2/255.0)
private let completedGreen = Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0)
private let plannedYellow = Color(red: 0xFF/255.0, green: 0xFD/255.0, blue: 0xE7/255.0)
private let plannedYellowDot = Color(red: 0xFF/255.0, green: 0x99/255.0, blue: 0x00/255.0)

// MARK: - Meal slot display info
private struct SlotInfo {
    let emoji: String
    let color: Color
}

private func slotInfo(for slot: String) -> SlotInfo {
    switch slot {
    case "EARLY_MORNING":   return SlotInfo(emoji: "🌙", color: Color(red: 0x31/255.0, green: 0x3A/255.0, blue: 0x4A/255.0))
    case "BREAKFAST":       return SlotInfo(emoji: "🌅", color: Color(red: 0xFF/255.0, green: 0x98/255.0, blue: 0x00/255.0))
    case "NOON":            return SlotInfo(emoji: "☀️", color: Color(red: 0xFF/255.0, green: 0xC1/255.0, blue: 0x07/255.0))
    case "MID_MORNING":     return SlotInfo(emoji: "🥤", color: Color(red: 0x03/255.0, green: 0xA9/255.0, blue: 0xF4/255.0))
    case "LUNCH":           return SlotInfo(emoji: "☀️", color: Color(red: 0x4C/255.0, green: 0xAF/255.0, blue: 0x50/255.0))
    case "PRE_WORKOUT":     return SlotInfo(emoji: "💪", color: Color(red: 0xF4/255.0, green: 0x43/255.0, blue: 0x36/255.0))
    case "EVENING":         return SlotInfo(emoji: "🌆", color: Color(red: 0xFF/255.0, green: 0x57/255.0, blue: 0x22/255.0))
    case "EVENING_SNACK":   return SlotInfo(emoji: "🍎", color: Color(red: 0xE9/255.0, green: 0x1E/255.0, blue: 0x63/255.0))
    case "POST_WORKOUT":    return SlotInfo(emoji: "🏋️", color: Color(red: 0x9C/255.0, green: 0x27/255.0, blue: 0xB0/255.0))
    case "DINNER":          return SlotInfo(emoji: "🌙", color: Color(red: 0x3F/255.0, green: 0x51/255.0, blue: 0xB5/255.0))
    case "POST_DINNER":     return SlotInfo(emoji: "🍵", color: Color(red: 0x60/255.0, green: 0x7D/255.0, blue: 0x8B/255.0))
    default:                return SlotInfo(emoji: "🍽️", color: Color.gray)
    }
}

private let allSlots = ["EARLY_MORNING", "BREAKFAST", "NOON", "MID_MORNING", "LUNCH",
                        "PRE_WORKOUT", "EVENING", "EVENING_SNACK", "POST_WORKOUT", "DINNER", "POST_DINNER"]

private func slotDisplayName(_ slot: String) -> String {
    switch slot {
    case "EARLY_MORNING":   return "Early Morning"
    case "BREAKFAST":       return "Breakfast"
    case "NOON":            return "Noon"
    case "MID_MORNING":     return "Mid Morning"
    case "LUNCH":           return "Lunch"
    case "PRE_WORKOUT":     return "Pre-Workout"
    case "EVENING":         return "Evening"
    case "EVENING_SNACK":   return "Evening Snack"
    case "POST_WORKOUT":    return "Post-Workout"
    case "DINNER":          return "Dinner"
    case "POST_DINNER":     return "Post Dinner"
    default:                return slot
    }
}

// MARK: - ISO date helpers
private let isoFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    f.locale = Locale(identifier: "en_US_POSIX")
    return f
}()

private func isoString(_ date: Date) -> String {
    isoFormatter.string(from: date)
}

private func isoDate(_ str: String) -> Date? {
    isoFormatter.date(from: str)
}

// MARK: - MealPlanScreen

struct MealPlanScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var plansVM = PlansViewModel()

    @State private var selectedDate: Date = Date()
    @State private var currentMonth: Date = Date()
    @State private var showDietPicker = false
    @State private var dietPickerDate: Date = Date()

    private var userId: Int64 { appState.currentUserId ?? 0 }

    var body: some View {
        VStack(spacing: 0) {
            mealPlanTopBar
            ScrollView {
                VStack(spacing: 16) {
                    calendarCard
                    legendRow
                    selectedDatePanel
                }
                .padding(.bottom, 24)
            }
            .background(Color(red: 0xF0/255.0, green: 0xF9/255.0, blue: 0xF4/255.0))
        }
        .ignoresSafeArea(edges: .top)
        .sheet(isPresented: $showDietPicker) {
            HomeDietPickerSheet { diet in
                plansVM.assignDiet(userId: userId, date: isoString(dietPickerDate), diet: diet)
                showDietPicker = false
            }
            .environmentObject(appState)
        }
        .onAppear { loadData() }
    }

    // MARK: - Top Bar

    private var mealPlanTopBar: some View {
        ZStack {
            darkGreen.ignoresSafeArea(edges: .top)
            HStack {
                Text("Meal Plan")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                Spacer()
                Button(action: {
                    dietPickerDate = Date()
                    showDietPicker = true
                }) {
                    HStack(spacing: 4) {
                        Text("✨")
                        Text("Select Diet")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(Color.white.opacity(0.7), lineWidth: 1.5)
                    )
                }
            }
            .padding(.horizontal)
            .padding(.top, 56)
            .padding(.bottom, 12)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    // MARK: - Calendar Card

    /// Monday of the week containing selectedDate
    private var weekStart: Date {
        let cal = Calendar.current
        // weekday: 1=Sun, 2=Mon … 7=Sat; offset back to Monday
        let weekday = cal.component(.weekday, from: selectedDate)
        let daysFromMonday = (weekday - 2 + 7) % 7
        return cal.date(byAdding: .day, value: -daysFromMonday, to: selectedDate)!
    }

    /// 7 days of the current week
    private var daysInWeek: [Date] {
        let cal = Calendar.current
        return (0..<7).compactMap { cal.date(byAdding: .day, value: $0, to: weekStart) }
    }

    private var calendarHeaderText: String {
        if plansVM.isWeekView {
            let cal = Calendar.current
            let weekEnd = cal.date(byAdding: .day, value: 6, to: weekStart)!
            let startMonth = cal.component(.month, from: weekStart)
            let endMonth = cal.component(.month, from: weekEnd)
            let year = cal.component(.year, from: weekEnd)
            let monthFmt = DateFormatter()
            if startMonth == endMonth {
                monthFmt.dateFormat = "MMMM yyyy"
                return monthFmt.string(from: weekStart)
            } else {
                monthFmt.dateFormat = "MMM"
                return "\(monthFmt.string(from: weekStart)) – \(monthFmt.string(from: weekEnd)) \(year)"
            }
        } else {
            return monthYearString
        }
    }

    private var calendarCard: some View {
        VStack(spacing: 0) {
            // Header: label + arrows (week view) + toggle pill
            HStack {
                if plansVM.isWeekView {
                    Button(action: {
                        let prev = Calendar.current.date(byAdding: .weekOfYear, value: -1, to: selectedDate)!
                        selectedDate = prev
                        plansVM.selectDate(isoString(prev), userId: userId)
                    }) {
                        Image(systemName: "chevron.left").foregroundColor(darkGreen)
                    }
                } else {
                    Spacer().frame(width: 28)
                }

                Spacer()

                Text(calendarHeaderText)
                    .font(.headline)
                    .foregroundColor(.primary)

                Spacer()

                if plansVM.isWeekView {
                    Button(action: {
                        let next = Calendar.current.date(byAdding: .weekOfYear, value: 1, to: selectedDate)!
                        selectedDate = next
                        plansVM.selectDate(isoString(next), userId: userId)
                    }) {
                        Image(systemName: "chevron.right").foregroundColor(darkGreen)
                    }
                } else {
                    Spacer().frame(width: 28)
                }

                // Toggle pill: shows what you'll switch TO
                Button(action: { plansVM.toggleView() }) {
                    Text(plansVM.isWeekView ? "Month" : "Week")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(darkGreen)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(darkGreen, lineWidth: 1.5)
                        )
                }
            }
            .padding(.horizontal)
            .padding(.top, 12)
            .padding(.bottom, 8)

            // Weekday headers (Mon–Sun)
            HStack(spacing: 0) {
                ForEach(Array(["M", "T", "W", "T", "F", "S", "S"].enumerated()), id: \.offset) { _, d in
                    Text(d)
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 4)

            if plansVM.isWeekView {
                // Single week row — fixed height prevents cells expanding to fill card
                HStack(spacing: 0) {
                    ForEach(daysInWeek, id: \.self) { date in
                        let dateStr = isoString(date)
                        let plan = plansVM.plans.first(where: { $0.date == dateStr })
                        MealPlanDayCell(
                            day: Calendar.current.component(.day, from: date),
                            isSelected: Calendar.current.isDate(date, inSameDayAs: selectedDate),
                            isToday: Calendar.current.isDateInToday(date),
                            hasPlan: plan != nil,
                            isCompleted: plan?.isCompleted ?? false,
                            compact: true,
                            dietName: plan?.dietName.flatMap { extractShortDietName($0) }
                        ) {
                            selectedDate = date
                            plansVM.selectDate(dateStr, userId: userId)
                        }
                        .frame(maxWidth: .infinity, maxHeight: 48)
                    }
                }
                .frame(height: 48)
                .padding(.horizontal, 4)
                .padding(.bottom, 8)
            } else {
                // Full month grid
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 7), spacing: 0) {
                    ForEach(daysInMonth, id: \.self) { date in
                        if let date = date {
                            let dateStr = isoString(date)
                            let plan = plansVM.plans.first(where: { $0.date == dateStr })
                            MealPlanDayCell(
                                day: Calendar.current.component(.day, from: date),
                                isSelected: Calendar.current.isDate(date, inSameDayAs: selectedDate),
                                isToday: Calendar.current.isDateInToday(date),
                                hasPlan: plan != nil,
                                isCompleted: plan?.isCompleted ?? false,
                                compact: true,
                                dietName: plan?.dietName.flatMap { extractShortDietName($0) }
                            ) {
                                selectedDate = date
                                plansVM.selectDate(dateStr, userId: userId)
                            }
                        } else {
                            Color.clear.aspectRatio(1, contentMode: .fit)
                        }
                    }
                }
                .padding(.horizontal, 4)
                .padding(.bottom, 8)
            }
        }
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.06), radius: 6, x: 0, y: 2)
        .padding(.horizontal)
        .padding(.top, 12)
    }

    // MARK: - Legend

    private var legendRow: some View {
        HStack(spacing: 12) {
            MealPlanLegendItem(color: completedGreen, label: "Completed")
            MealPlanLegendItem(color: plannedYellowDot, label: "Planned")
            MealPlanLegendItem(color: darkGreen, label: "Today", outlined: true)
            MealPlanLegendItem(color: Color.gray.opacity(0.5), label: "No plan")
        }
        .padding(.horizontal)
    }

    // MARK: - Selected Date Panel

    private var isSelectedDateToday: Bool { Calendar.current.isDateInToday(selectedDate) }
    private var isSelectedDatePast: Bool {
        selectedDate < Calendar.current.startOfDay(for: Date()) && !isSelectedDateToday
    }

    @ViewBuilder
    private var selectedDatePanel: some View {
        if isSelectedDateToday || isSelectedDatePast {
            if plansVM.selectedDiet != nil {
                todayWithDietPanel(isPast: isSelectedDatePast)
            } else {
                pastNoDietPanel(isPast: isSelectedDatePast)
            }
        } else {
            if plansVM.selectedDiet != nil {
                futureWithDietPanel
            } else {
                futureNoDietPanel
            }
        }
    }

    // State A / D: today or past with diet
    private func todayWithDietPanel(isPast: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(dateLabel(isPast: isPast, isToday: !isPast))
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                Spacer()
                Button("View Log >") {
                    NotificationCenter.default.post(name: .navigateToLog, object: isoString(selectedDate))
                }
                .font(.caption)
                .foregroundColor(darkGreen)
            }
            dietInfoHeader
            if let dwm = plansVM.selectedDietWithMeals {
                dietDetailSection(dwm)
            }
        }
        .padding()
        .background(lightGreenBg)
        .cornerRadius(16)
        .padding(.horizontal)
    }

    // State B: future with diet
    private var futureWithDietPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(dateLabel(isPast: false, isToday: false))
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                Spacer()
                Button("Change") {
                    dietPickerDate = selectedDate
                    showDietPicker = true
                }
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(darkGreen)
                .cornerRadius(12)
            }
            dietInfoHeader
            if let dwm = plansVM.selectedDietWithMeals {
                dietDetailSection(dwm)
            }
            Button(action: {
                plansVM.removeDiet(userId: userId, date: isoString(selectedDate))
            }) {
                HStack {
                    Image(systemName: "xmark.circle")
                    Text("Remove diet from this day")
                }
                .font(.caption)
                .foregroundColor(.red)
                .frame(maxWidth: .infinity)
                .padding(.top, 4)
            }
        }
        .padding()
        .background(yellowBg)
        .cornerRadius(16)
        .padding(.horizontal)
    }

    // State C: future, no diet
    private var futureNoDietPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(dateLabel(isPast: false, isToday: false))
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                Spacer()
                Button("+ Plan") {
                    dietPickerDate = selectedDate
                    showDietPicker = true
                }
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(darkGreen)
                .cornerRadius(12)
            }
            // Empty state
            VStack(spacing: 12) {
                Image(systemName: "fork.knife")
                    .font(.largeTitle)
                    .foregroundColor(.secondary.opacity(0.5))
                Text("No diet planned for this day yet.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                Button(action: {
                    dietPickerDate = selectedDate
                    showDietPicker = true
                }) {
                    HStack {
                        Image(systemName: "plus")
                        Text("Plan a Diet")
                    }
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(darkGreen)
                    .cornerRadius(12)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        }
        .padding()
        .background(yellowBg)
        .cornerRadius(16)
        .padding(.horizontal)
    }

    // Past no-diet: minimal panel
    private func pastNoDietPanel(isPast: Bool) -> some View {
        HStack {
            Text(dateLabel(isPast: isPast, isToday: !isPast))
                .font(.caption)
                .foregroundColor(.secondary)
            Spacer()
            Text("No diet was planned")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .padding(.horizontal)
    }

    // MARK: - Diet Info Header (name + tag)

    private var dietInfoHeader: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let diet = plansVM.selectedDiet {
                Text(diet.name)
                    .font(.headline)
                    .fontWeight(.bold)
            }
            if let firstTag = plansVM.selectedDietTags.first {
                Text(firstTag.name)
                    .font(.caption)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 3)
                    .background(tagPurple.opacity(0.15))
                    .foregroundColor(tagPurple)
                    .cornerRadius(10)
            }
        }
    }

    // MARK: - Diet Detail Section

    private func buildMealsMap(_ dwm: DietWithMeals) -> [String: MealWithFoods?] {
        var map: [String: MealWithFoods?] = [:]
        if let nd = dwm.meals as? NSDictionary {
            for (k, v) in nd {
                if let key = k as? String { map[key] = v as? MealWithFoods }
            }
        }
        return map
    }

    private func dietDetailSection(_ dwm: DietWithMeals) -> some View {
        let mealsMap = buildMealsMap(dwm)
        return VStack(alignment: .leading, spacing: 12) {
            // Macro tiles
            HStack(spacing: 8) {
                MacroTile(value: Int(dwm.totalCalories), label: "Calories", unit: "kcal")
                MacroTile(value: Int(dwm.totalProtein), label: "Protein", unit: "g")
                MacroTile(value: Int(dwm.totalCarbs), label: "Carbs", unit: "g")
                MacroTile(value: Int(dwm.totalFat), label: "Fat", unit: "g")
            }
            // Description
            if let desc = dwm.diet.description_ {
                Text(desc)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            // Meal slots
            VStack(spacing: 0) {
                ForEach(allSlots, id: \.self) { slot in
                    let meal = mealsMap[slot] ?? nil
                    MealPlanSlotRow(slot: slot, meal: meal)
                    if slot != allSlots.last {
                        Divider().padding(.leading, 44)
                    }
                }
            }
            .background(Color(.systemBackground))
            .cornerRadius(10)
        }
    }

    // MARK: - Helpers

    private var monthYearString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: currentMonth)
    }

    private func dateLabel(isPast: Bool, isToday: Bool) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMMM d"
        let dateStr = formatter.string(from: selectedDate)
        if isToday { return "Today · \(dateStr)" }
        if isPast  { return "Past · \(dateStr)" }
        return "Upcoming · \(dateStr)"
    }

    private var daysInMonth: [Date?] {
        let calendar = Calendar.current
        let firstOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: currentMonth))!
        let range = calendar.range(of: .day, in: .month, for: firstOfMonth)!
        let firstWeekday = calendar.component(.weekday, from: firstOfMonth) // 1=Sun … 7=Sat
        // Offset for Mon-first grid: Mon=0, Tue=1, …, Sun=6
        let offset = (firstWeekday - 2 + 7) % 7

        var days: [Date?] = Array(repeating: nil, count: offset)
        for day in range {
            if let d = calendar.date(byAdding: .day, value: day - 1, to: firstOfMonth) {
                days.append(d)
            }
        }
        return days
    }

    private func loadData() {
        let calendar = Calendar.current
        let firstOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: currentMonth))!
        let lastOfMonth = calendar.date(byAdding: DateComponents(month: 1, day: -1), to: firstOfMonth)!

        plansVM.loadPlans(userId: userId, startDate: isoString(firstOfMonth), endDate: isoString(lastOfMonth))
        plansVM.selectDate(isoString(selectedDate), userId: userId)
    }

    // Short diet name helper (mirrors Android extractShortDietName)
    private func extractShortDietName(_ name: String) -> String {
        let words = name.split(separator: " ")
        if words.count == 1 {
            return String(name.prefix(6))
        }
        // First word up to 4 chars + first letter of second word
        let first = String(words[0].prefix(4))
        let second = String((words[1].first ?? Character(" ")))
        return "\(first)\(second)"
    }
}

// MARK: - MealPlanDayCell

struct MealPlanDayCell: View {
    let day: Int
    let isSelected: Bool
    let isToday: Bool
    let hasPlan: Bool
    let isCompleted: Bool
    let compact: Bool
    let dietName: String?
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                // Background
                if isSelected {
                    Circle().fill(darkGreen)
                } else if hasPlan && isCompleted {
                    Circle().fill(completedGreen)
                } else if hasPlan && !isCompleted {
                    Circle().fill(plannedYellow)
                } else if isToday {
                    Circle().stroke(darkGreen, lineWidth: 2)
                }

                VStack(spacing: 1) {
                    Text("\(day)")
                        .font(compact ? .caption2 : .subheadline)
                        .fontWeight(isToday ? .bold : .regular)
                        .foregroundColor(dayTextColor)

                    if hasPlan, let name = dietName {
                        Text(name)
                            .font(.system(size: compact ? 6 : 8))
                            .fontWeight(.bold)
                            .foregroundColor(dayTextColor.opacity(0.8))
                            .lineLimit(1)
                    } else if hasPlan {
                        Circle()
                            .fill(dayTextColor.opacity(0.6))
                            .frame(width: compact ? 3 : 4, height: compact ? 3 : 4)
                    } else {
                        Circle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(width: compact ? 3 : 4, height: compact ? 3 : 4)
                    }
                }
            }
            .aspectRatio(1, contentMode: .fit)
            .padding(compact ? 1 : 2)
        }
        .buttonStyle(PlainButtonStyle())
    }

    private var dayTextColor: Color {
        if isSelected { return .white }
        if hasPlan && isCompleted { return .white }
        if hasPlan && !isCompleted { return .black }
        if isToday { return darkGreen }
        return .primary
    }
}

// MARK: - MealPlanLegendItem

struct MealPlanLegendItem: View {
    let color: Color
    let label: String
    var outlined: Bool = false

    var body: some View {
        HStack(spacing: 4) {
            if outlined {
                Circle()
                    .stroke(color, lineWidth: 1.5)
                    .frame(width: 10, height: 10)
            } else {
                Circle()
                    .fill(color)
                    .frame(width: 10, height: 10)
            }
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - MacroTile

struct MacroTile: View {
    let value: Int
    let label: String
    let unit: String

    var body: some View {
        VStack(spacing: 2) {
            Text("\(value)")
                .font(.subheadline)
                .fontWeight(.bold)
            Text(unit)
                .font(.caption2)
                .foregroundColor(.secondary)
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(Color(.systemBackground))
        .cornerRadius(8)
        .shadow(color: .black.opacity(0.04), radius: 2, x: 0, y: 1)
    }
}

// MARK: - MealPlanSlotRow

struct MealPlanSlotRow: View {
    let slot: String
    let meal: MealWithFoods?

    var body: some View {
        let info = slotInfo(for: slot)
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(info.color.opacity(0.15))
                    .frame(width: 32, height: 32)
                Text(info.emoji)
                    .font(.system(size: 16))
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(slotDisplayName(slot))
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(meal?.meal.name ?? "—")
                    .font(.subheadline)
                    .fontWeight(meal != nil ? .medium : .regular)
                    .foregroundColor(meal != nil ? .primary : .secondary)
            }
            Spacer()
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 12)
    }
}
