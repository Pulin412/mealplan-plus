import SwiftUI
import Charts
import shared

struct ChartsScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = DailyLogViewModel()
    @StateObject private var healthViewModel = HealthMetricsViewModel()
    @State private var selectedTab = 0
    @State private var dateRange: DateRange = .week

    enum DateRange: String, CaseIterable {
        case week = "7D"
        case month = "30D"
        case quarter = "90D"

        var days: Int {
            switch self {
            case .week: return 7
            case .month: return 30
            case .quarter: return 90
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Tab selector
            Picker("Chart Type", selection: $selectedTab) {
                Text("Calories").tag(0)
                Text("Macros").tag(1)
                Text("Weight").tag(2)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding()

            // Date range selector
            HStack {
                ForEach(DateRange.allCases, id: \.self) { range in
                    Button(action: { dateRange = range }) {
                        Text(range.rawValue)
                            .font(.subheadline)
                            .fontWeight(dateRange == range ? .semibold : .regular)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(dateRange == range ? Color.green : Color.clear)
                            .foregroundColor(dateRange == range ? .white : .primary)
                            .cornerRadius(8)
                    }
                }
            }
            .padding(.horizontal)

            // Chart content
            ScrollView {
                VStack(spacing: 20) {
                    switch selectedTab {
                    case 0:
                        calorieChart
                    case 1:
                        macroChart
                    case 2:
                        weightChart
                    default:
                        EmptyView()
                    }
                }
                .padding()
            }
        }
        .background(
            Color(.systemGroupedBackground).ignoresSafeArea()
        )
        .navigationTitle("Analytics")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            loadData()
        }
        .onChange(of: dateRange) { _ in
            loadData()
        }
    }

    private var calorieChart: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Calorie Trend")
                .font(.headline)

            if viewModel.macroSummaries.isEmpty {
                emptyChartPlaceholder(message: "No calorie data for this period")
            } else {
                Chart {
                    ForEach(viewModel.macroSummaries, id: \.date) { summary in
                        BarMark(
                            x: .value("Date", formatChartDate(summary.date)),
                            y: .value("Calories", summary.calories)
                        )
                        .foregroundStyle(Color.green.gradient)
                    }
                }
                .frame(height: 200)
                .chartXAxis {
                    AxisMarks(values: .automatic) { _ in
                        AxisValueLabel()
                    }
                }

                // Stats summary
                HStack(spacing: 20) {
                    statCard(title: "Average", value: "\(Int(averageCalories))", unit: "kcal")
                    statCard(title: "Max", value: "\(Int(maxCalories))", unit: "kcal")
                    statCard(title: "Min", value: "\(Int(minCalories))", unit: "kcal")
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
    }

    private var macroChart: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Macro Breakdown")
                .font(.headline)

            if viewModel.macroSummaries.isEmpty {
                emptyChartPlaceholder(message: "No macro data for this period")
            } else {
                let totals = calculateMacroTotals()

                // Macro distribution bar chart (iOS 16 compatible)
                Chart {
                    BarMark(
                        x: .value("Macro", "Protein"),
                        y: .value("Grams", totals.protein)
                    )
                    .foregroundStyle(Color.red)

                    BarMark(
                        x: .value("Macro", "Carbs"),
                        y: .value("Grams", totals.carbs)
                    )
                    .foregroundStyle(Color.blue)

                    BarMark(
                        x: .value("Macro", "Fat"),
                        y: .value("Grams", totals.fat)
                    )
                    .foregroundStyle(Color.yellow)
                }
                .frame(height: 200)

                // Custom donut-style visual (iOS 16 compatible)
                macroPieView(totals: totals)

                // Legend
                HStack(spacing: 20) {
                    macroLegendItem(color: .red, label: "Protein", value: totals.protein)
                    macroLegendItem(color: .blue, label: "Carbs", value: totals.carbs)
                    macroLegendItem(color: .yellow, label: "Fat", value: totals.fat)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
    }

    private func macroPieView(totals: (protein: Double, carbs: Double, fat: Double)) -> some View {
        let total = totals.protein + totals.carbs + totals.fat
        let proteinPct = total > 0 ? totals.protein / total : 0.33
        let carbsPct = total > 0 ? totals.carbs / total : 0.33
        let fatPct = total > 0 ? totals.fat / total : 0.33

        return GeometryReader { geometry in
            let size = min(geometry.size.width, 150)
            ZStack {
                // Fat (background)
                Circle()
                    .trim(from: 0, to: CGFloat(fatPct + carbsPct + proteinPct))
                    .stroke(Color.yellow, lineWidth: 20)
                    .rotationEffect(.degrees(-90))

                // Carbs
                Circle()
                    .trim(from: 0, to: CGFloat(carbsPct + proteinPct))
                    .stroke(Color.blue, lineWidth: 20)
                    .rotationEffect(.degrees(-90))

                // Protein
                Circle()
                    .trim(from: 0, to: CGFloat(proteinPct))
                    .stroke(Color.red, lineWidth: 20)
                    .rotationEffect(.degrees(-90))

                // Center label
                VStack {
                    Text("\(Int(total))g")
                        .font(.title3)
                        .fontWeight(.bold)
                    Text("Total")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .frame(width: size, height: size)
            .frame(maxWidth: .infinity)
        }
        .frame(height: 150)
    }

    private var weightChart: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Weight Trend")
                .font(.headline)

            let weightMetrics = healthViewModel.metrics.filter { $0.metricType == "weight" }

            if weightMetrics.isEmpty {
                emptyChartPlaceholder(message: "No weight data recorded")
            } else {
                Chart {
                    ForEach(weightMetrics.prefix(dateRange.days).reversed(), id: \.id) { metric in
                        LineMark(
                            x: .value("Date", formatChartDate(metric.date)),
                            y: .value("Weight", metric.value)
                        )
                        .foregroundStyle(Color.blue)

                        PointMark(
                            x: .value("Date", formatChartDate(metric.date)),
                            y: .value("Weight", metric.value)
                        )
                        .foregroundStyle(Color.blue)
                    }
                }
                .frame(height: 200)

                // Stats
                let weights = weightMetrics.map { $0.value }
                HStack(spacing: 20) {
                    statCard(title: "Current", value: String(format: "%.1f", weights.first ?? 0), unit: "kg")
                    statCard(title: "Avg", value: String(format: "%.1f", weights.reduce(0, +) / Double(max(weights.count, 1))), unit: "kg")
                    if let first = weights.last, let last = weights.first {
                        let change = last - first
                        statCard(title: "Change", value: String(format: "%+.1f", change), unit: "kg")
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
    }

    private func emptyChartPlaceholder(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "chart.bar.xaxis")
                .font(.system(size: 40))
                .foregroundColor(.gray.opacity(0.5))
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(height: 200)
        .frame(maxWidth: .infinity)
    }

    private func statCard(title: String, value: String, unit: String) -> some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
            Text(unit)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(Color.gray.opacity(0.1))
        .cornerRadius(8)
    }

    private func macroLegendItem(color: Color, label: String, value: Double) -> some View {
        HStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)
            VStack(alignment: .leading) {
                Text(label)
                    .font(.caption)
                Text("\(Int(value))g")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var averageCalories: Double {
        guard !viewModel.macroSummaries.isEmpty else { return 0 }
        return viewModel.macroSummaries.map { $0.calories }.reduce(0, +) / Double(viewModel.macroSummaries.count)
    }

    private var maxCalories: Double {
        viewModel.macroSummaries.map { $0.calories }.max() ?? 0
    }

    private var minCalories: Double {
        viewModel.macroSummaries.map { $0.calories }.min() ?? 0
    }

    private func calculateMacroTotals() -> (protein: Double, carbs: Double, fat: Double) {
        let protein = viewModel.macroSummaries.map { $0.protein }.reduce(0, +)
        let carbs = viewModel.macroSummaries.map { $0.carbs }.reduce(0, +)
        let fat = viewModel.macroSummaries.map { $0.fat }.reduce(0, +)
        return (protein, carbs, fat)
    }

    private func formatChartDate(_ dateString: String) -> String {
        let inputFormatter = DateFormatter()
        inputFormatter.dateFormat = "yyyy-MM-dd"

        let outputFormatter = DateFormatter()
        outputFormatter.dateFormat = "M/d"

        if let date = inputFormatter.date(from: dateString) {
            return outputFormatter.string(from: date)
        }
        return dateString
    }

    private func loadData() {
        guard let userId = appState.currentUserId else { return }

        let endDate = Date()
        let startDate = Calendar.current.date(byAdding: .day, value: -dateRange.days, to: endDate)!

        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"

        viewModel.loadMacroSummaries(
            userId: userId,
            startDate: formatter.string(from: startDate),
            endDate: formatter.string(from: endDate)
        )
        healthViewModel.loadMetrics(userId: userId)
    }
}

struct ChartsScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            ChartsScreen()
                .environmentObject(AppState())
        }
    }
}
