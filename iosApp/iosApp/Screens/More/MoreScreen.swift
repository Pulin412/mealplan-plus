import SwiftUI
import shared

struct MoreScreen: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        List {
            Section {
                NavigationLink(destination: CalendarScreen()) {
                    MoreMenuItem(icon: "calendar", title: "Calendar", color: .purple)
                }

                NavigationLink(destination: GroceryListsScreen()) {
                    MoreMenuItem(icon: "cart.fill", title: "Grocery Lists", color: .orange)
                }

                NavigationLink(destination: HealthScreen()) {
                    MoreMenuItem(icon: "heart.fill", title: "Health Metrics", color: .red)
                }
            }

            Section {
                NavigationLink(destination: SettingsScreen()) {
                    MoreMenuItem(icon: "gearshape.fill", title: "Settings", color: .gray)
                }

                NavigationLink(destination: ProfileScreen()) {
                    MoreMenuItem(icon: "person.fill", title: "Profile", color: .blue)
                }
            }

            Section {
                Button(action: { appState.logout() }) {
                    HStack {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                            .foregroundColor(.red)
                            .frame(width: 30)
                        Text("Logout")
                            .foregroundColor(.red)
                    }
                }
            }

            Section {
                HStack {
                    Spacer()
                    VStack(spacing: 4) {
                        Text("MealPlan+")
                            .font(.caption)
                            .fontWeight(.semibold)
                        Text("Version 1.0.0")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("Powered by Kotlin Multiplatform")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }
        }
        .navigationTitle("More")
    }
}

struct MoreMenuItem: View {
    let icon: String
    let title: String
    let color: Color

    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(color)
                .frame(width: 30)
            Text(title)
        }
    }
}

// MARK: - Calendar Screen

struct CalendarScreen: View {
    @State private var selectedDate = Date()
    @State private var currentMonth = Date()

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Month navigation
                HStack {
                    Button(action: previousMonth) {
                        Image(systemName: "chevron.left")
                    }
                    Spacer()
                    Text(monthYearString)
                        .font(.headline)
                    Spacer()
                    Button(action: nextMonth) {
                        Image(systemName: "chevron.right")
                    }
                }
                .padding()

                // Calendar grid
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: 8) {
                    // Weekday headers
                    ForEach(["S", "M", "T", "W", "T", "F", "S"], id: \.self) { day in
                        Text(day)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.secondary)
                    }

                    // Days
                    ForEach(daysInMonth, id: \.self) { date in
                        if let date = date {
                            CalendarDayCell(
                                date: date,
                                isSelected: Calendar.current.isDate(date, inSameDayAs: selectedDate),
                                hasLog: Int.random(in: 0...1) == 1
                            ) {
                                selectedDate = date
                            }
                        } else {
                            Text("")
                                .frame(height: 40)
                        }
                    }
                }
                .padding(.horizontal)

                // Selected date info
                if Calendar.current.isDateInToday(selectedDate) || selectedDate < Date() {
                    NavigationLink(destination: DailyLogScreen(date: selectedDate)) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(selectedDateString)
                                    .font(.headline)
                                Text("Tap to view daily log")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.white)
                        .cornerRadius(12)
                        .shadow(color: .black.opacity(0.1), radius: 3)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .padding(.horizontal)
                }

                Spacer()
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
        .navigationTitle("Calendar")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var monthYearString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: currentMonth)
    }

    private var selectedDateString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMM d"
        return formatter.string(from: selectedDate)
    }

    private var daysInMonth: [Date?] {
        let calendar = Calendar.current
        let range = calendar.range(of: .day, in: .month, for: currentMonth)!
        let firstDay = calendar.date(from: calendar.dateComponents([.year, .month], from: currentMonth))!
        let firstWeekday = calendar.component(.weekday, from: firstDay)

        var days: [Date?] = Array(repeating: nil, count: firstWeekday - 1)

        for day in range {
            if let date = calendar.date(byAdding: .day, value: day - 1, to: firstDay) {
                days.append(date)
            }
        }

        return days
    }

    private func previousMonth() {
        currentMonth = Calendar.current.date(byAdding: .month, value: -1, to: currentMonth)!
    }

    private func nextMonth() {
        currentMonth = Calendar.current.date(byAdding: .month, value: 1, to: currentMonth)!
    }
}

struct CalendarDayCell: View {
    let date: Date
    let isSelected: Bool
    let hasLog: Bool
    let action: () -> Void

    private var dayNumber: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d"
        return formatter.string(from: date)
    }

    private var isToday: Bool {
        Calendar.current.isDateInToday(date)
    }

    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Text(dayNumber)
                    .font(.subheadline)
                    .fontWeight(isToday ? .bold : .regular)

                if hasLog {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 6, height: 6)
                } else {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: 6, height: 6)
                }
            }
            .frame(width: 40, height: 40)
            .background(isSelected ? Color.green : (isToday ? Color.green.opacity(0.2) : Color.clear))
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(8)
        }
    }
}

// MARK: - Grocery Lists Screen

struct GroceryListsScreen: View {
    @State private var groceryLists: [GroceryListUI] = []
    @State private var showCreateList = false

    var body: some View {
        ZStack {
            if groceryLists.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "cart")
                        .font(.system(size: 60))
                        .foregroundColor(.green.opacity(0.5))
                    Text("No grocery lists")
                        .font(.headline)
                    Text("Create a list from your diet plans")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            } else {
                List {
                    ForEach(groceryLists) { list in
                        GroceryListRow(list: list)
                    }
                    .onDelete { indexSet in
                        groceryLists.remove(atOffsets: indexSet)
                    }
                }
            }
        }
        .navigationTitle("Grocery Lists")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showCreateList = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showCreateList) {
            CreateGroceryListScreen { list in
                groceryLists.append(list)
            }
        }
        .onAppear {
            loadSampleLists()
        }
    }

    private func loadSampleLists() {
        groceryLists = [
            GroceryListUI(id: 1, name: "Weekly Shopping", itemCount: 12, checkedCount: 5, createdDate: Date()),
            GroceryListUI(id: 2, name: "High Protein Diet", itemCount: 8, checkedCount: 0, createdDate: Date()),
        ]
    }
}

struct GroceryListRow: View {
    let list: GroceryListUI

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(list.name)
                    .font(.headline)
                Text("\(list.checkedCount)/\(list.itemCount) items")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            CircularProgress(progress: Double(list.checkedCount) / Double(max(list.itemCount, 1)))
        }
    }
}

struct CircularProgress: View {
    let progress: Double

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.gray.opacity(0.3), lineWidth: 3)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(Color.green, lineWidth: 3)
                .rotationEffect(.degrees(-90))
        }
        .frame(width: 30, height: 30)
    }
}

struct CreateGroceryListScreen: View {
    @Environment(\.dismiss) var dismiss
    var onCreate: (GroceryListUI) -> Void

    @State private var name = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("List Name", text: $name)
                }

                Section {
                    Text("Items will be generated from your selected diet")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Section {
                    Button(action: createList) {
                        HStack {
                            Spacer()
                            Text("Create List")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                    }
                    .disabled(name.isEmpty)
                }
            }
            .navigationTitle("New Grocery List")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func createList() {
        let list = GroceryListUI(
            id: Int64(Date().timeIntervalSince1970),
            name: name,
            itemCount: 0,
            checkedCount: 0,
            createdDate: Date()
        )
        onCreate(list)
        dismiss()
    }
}

struct GroceryListUI: Identifiable {
    let id: Int64
    let name: String
    let itemCount: Int
    let checkedCount: Int
    let createdDate: Date
}

// MARK: - Health Screen

struct HealthScreen: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text("Health metrics coming soon")
                    .foregroundColor(.secondary)
            }
            .padding()
        }
        .navigationTitle("Health Metrics")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Settings Screen

struct SettingsScreen: View {
    @State private var calorieGoal = "2000"
    @State private var darkMode = false
    @State private var notifications = true

    var body: some View {
        Form {
            Section("Goals") {
                HStack {
                    Text("Daily Calorie Goal")
                    Spacer()
                    TextField("2000", text: $calorieGoal)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                    Text("kcal")
                        .foregroundColor(.secondary)
                }
            }

            Section("Appearance") {
                Toggle("Dark Mode", isOn: $darkMode)
            }

            Section("Notifications") {
                Toggle("Meal Reminders", isOn: $notifications)
            }

            Section("Data") {
                Button("Export Data") {}
                Button("Import Data") {}
            }

            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0.0")
                        .foregroundColor(.secondary)
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Profile Screen

struct ProfileScreen: View {
    @EnvironmentObject var appState: AppState
    @State private var name = "User"
    @State private var email = "user@example.com"

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Profile header
                VStack(spacing: 12) {
                    Image(systemName: "person.circle.fill")
                        .resizable()
                        .frame(width: 100, height: 100)
                        .foregroundColor(.green)

                    Text(name)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(email)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding()

                // Stats
                HStack(spacing: 20) {
                    ProfileStat(value: "45", label: "Days Active")
                    ProfileStat(value: "156", label: "Meals Logged")
                    ProfileStat(value: "12", label: "Diets Created")
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

                // Edit profile button
                Button(action: {}) {
                    Text("Edit Profile")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                .padding(.horizontal)

                // Logout button
                Button(action: { appState.logout() }) {
                    Text("Logout")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.red.opacity(0.1))
                        .foregroundColor(.red)
                        .cornerRadius(10)
                }
                .padding(.horizontal)

                Spacer()
            }
            .padding(.top)
        }
        .background(
            LinearGradient(
                gradient: Gradient(colors: [Color.green.opacity(0.2), Color.white]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct ProfileStat: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.green)
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct MoreScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            MoreScreen()
        }
        .environmentObject(AppState())
    }
}
