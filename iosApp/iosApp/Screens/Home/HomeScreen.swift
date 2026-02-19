import SwiftUI
import shared

struct HomeScreen: View {
    @EnvironmentObject var appState: AppState
    @State private var todayCalories: Double = 0
    @State private var todayProtein: Double = 0
    @State private var todayCarbs: Double = 0
    @State private var todayFat: Double = 0
    @State private var calorieGoal: Double = 2000
    @State private var selectedDate = Date()

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMM d"
        return formatter
    }

    var body: some View {
        ZStack {
            // Gradient background
            LinearGradient(
                gradient: Gradient(colors: [Color.green.opacity(0.2), Color.white]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    // Date header
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Today")
                                .font(.title)
                                .fontWeight(.bold)
                            Text(dateFormatter.string(from: selectedDate))
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        NavigationLink(destination: ProfileScreen()) {
                            Image(systemName: "person.circle.fill")
                                .resizable()
                                .frame(width: 40, height: 40)
                                .foregroundColor(.green)
                        }
                    }
                    .padding(.horizontal)

                    // Calories summary card
                    CaloriesSummaryCard(
                        consumed: todayCalories,
                        goal: calorieGoal
                    )
                    .padding(.horizontal)

                    // Macros breakdown
                    MacrosCard(
                        protein: todayProtein,
                        carbs: todayCarbs,
                        fat: todayFat
                    )
                    .padding(.horizontal)

                    // Quick actions
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Quick Actions")
                            .font(.headline)
                            .padding(.horizontal)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                QuickActionButton(
                                    icon: "plus.circle.fill",
                                    title: "Log Meal",
                                    color: .green
                                ) {
                                    // Navigate to daily log
                                }

                                QuickActionButton(
                                    icon: "magnifyingglass",
                                    title: "Search Food",
                                    color: .blue
                                ) {
                                    // Navigate to food search
                                }

                                QuickActionButton(
                                    icon: "barcode.viewfinder",
                                    title: "Scan Barcode",
                                    color: .orange
                                ) {
                                    // Open barcode scanner
                                }

                                QuickActionButton(
                                    icon: "calendar",
                                    title: "View Calendar",
                                    color: .purple
                                ) {
                                    // Navigate to calendar
                                }
                            }
                            .padding(.horizontal)
                        }
                    }

                    // Today's meals
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("Today's Meals")
                                .font(.headline)
                            Spacer()
                            NavigationLink("View All") {
                                DailyLogScreen(date: selectedDate)
                            }
                            .font(.subheadline)
                            .foregroundColor(.green)
                        }
                        .padding(.horizontal)

                        MealSlotRow(slot: "Breakfast", calories: 350, isLogged: true)
                        MealSlotRow(slot: "Lunch", calories: 0, isLogged: false)
                        MealSlotRow(slot: "Dinner", calories: 0, isLogged: false)
                        MealSlotRow(slot: "Snacks", calories: 120, isLogged: true)
                    }
                    .padding(.horizontal)

                    Spacer().frame(height: 20)
                }
                .padding(.top)
            }
        }
        .navigationBarHidden(true)
    }
}

struct CaloriesSummaryCard: View {
    let consumed: Double
    let goal: Double

    var progress: Double {
        min(consumed / goal, 1.0)
    }

    var remaining: Double {
        max(goal - consumed, 0)
    }

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Text("Calories")
                    .font(.headline)
                Spacer()
                Text("\(Int(consumed)) / \(Int(goal)) kcal")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            // Progress bar
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.gray.opacity(0.2))
                        .frame(height: 16)

                    RoundedRectangle(cornerRadius: 8)
                        .fill(progress > 1.0 ? Color.red : Color.green)
                        .frame(width: geometry.size.width * progress, height: 16)
                }
            }
            .frame(height: 16)

            HStack {
                VStack(alignment: .leading) {
                    Text("\(Int(remaining))")
                        .font(.title2)
                        .fontWeight(.bold)
                    Text("Remaining")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text("\(Int(progress * 100))%")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                    Text("of goal")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
    }
}

struct MacrosCard: View {
    let protein: Double
    let carbs: Double
    let fat: Double

    var body: some View {
        HStack(spacing: 20) {
            MacroItem(name: "Protein", value: protein, unit: "g", color: .red)
            MacroItem(name: "Carbs", value: carbs, unit: "g", color: .blue)
            MacroItem(name: "Fat", value: fat, unit: "g", color: .yellow)
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
    }
}

struct MacroItem: View {
    let name: String
    let value: Double
    let unit: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Circle()
                .fill(color.opacity(0.2))
                .frame(width: 50, height: 50)
                .overlay(
                    Text("\(Int(value))")
                        .font(.headline)
                        .fontWeight(.bold)
                )
            Text(name)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct QuickActionButton: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                Text(title)
                    .font(.caption)
                    .foregroundColor(.primary)
            }
            .frame(width: 80, height: 80)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.1), radius: 3, x: 0, y: 1)
        }
    }
}

struct MealSlotRow: View {
    let slot: String
    let calories: Double
    let isLogged: Bool

    var body: some View {
        HStack {
            Image(systemName: slotIcon)
                .foregroundColor(.green)
                .frame(width: 30)

            Text(slot)
                .font(.subheadline)

            Spacer()

            if isLogged {
                Text("\(Int(calories)) kcal")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            } else {
                Text("Not logged")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Image(systemName: "plus.circle")
                    .foregroundColor(.green)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(8)
    }

    var slotIcon: String {
        switch slot {
        case "Breakfast": return "sunrise.fill"
        case "Lunch": return "sun.max.fill"
        case "Dinner": return "moon.fill"
        default: return "leaf.fill"
        }
    }
}

struct HomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            HomeScreen()
        }
        .environmentObject(AppState())
    }
}
