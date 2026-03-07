import SwiftUI
import shared

struct HealthMetricsScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = HealthMetricsViewModel()
    @State private var showAddMetric = false
    @State private var selectedMetricType: String? = nil
    @State private var periodDays = 30

    let metricTypes = [
        ("weight", "Weight", "scalemass.fill", Color.blue),
        ("blood_sugar", "Blood Sugar", "drop.fill", Color.red),
        ("blood_pressure", "Blood Pressure", "heart.fill", Color.pink),
        ("sleep", "Sleep", "bed.double.fill", Color.purple)
    ]

    var cutoffDate: String {
        let cal = Calendar.current
        let date = cal.date(byAdding: .day, value: -periodDays, to: Date()) ?? Date()
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    func metricsInPeriod(type: String) -> [HealthMetric] {
        viewModel.metrics
            .filter { $0.metricType == type && $0.date >= cutoffDate }
            .sorted { $0.date < $1.date }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Period selector
                Picker("Period", selection: $periodDays) {
                    Text("7D").tag(7)
                    Text("30D").tag(30)
                    Text("90D").tag(90)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                // Quick metrics overview
                quickMetricsGrid

                // Metric categories
                ForEach(metricTypes, id: \.0) { metric in
                    metricCard(type: metric.0, name: metric.1, icon: metric.2, color: metric.3)
                }

                // Custom metrics section
                if !viewModel.customMetricTypes.isEmpty {
                    customMetricsSection
                }

                Spacer().frame(height: 20)
            }
            .padding(.top)
        }
        .background(Color(red: 0xF0/255.0, green: 0xF9/255.0, blue: 0xF4/255.0).ignoresSafeArea())
        .navigationTitle("Health Metrics")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0), for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddMetric = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddMetric) {
            AddMetricScreen(userId: appState.currentUserId ?? 1, preselectedType: selectedMetricType) {
                loadMetrics()
            }
        }
        .onAppear {
            loadMetrics()
        }
        .refreshable {
            loadMetrics()
        }
    }

    private var quickMetricsGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            ForEach(metricTypes.prefix(4), id: \.0) { metric in
                quickMetricCard(type: metric.0, name: metric.1, icon: metric.2, color: metric.3)
            }
        }
        .padding(.horizontal)
    }

    private func quickMetricCard(type: String, name: String, icon: String, color: Color) -> some View {
        let periodMetrics = metricsInPeriod(type: type)
        let latestValue = periodMetrics.last.map { getFormattedValue($0, type: type) } ?? "--"
        let changeStr = periodChange(metrics: periodMetrics, type: type)
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Spacer()
                Button(action: {
                    selectedMetricType = type
                    showAddMetric = true
                }) {
                    Image(systemName: "plus.circle")
                        .foregroundColor(.green)
                }
            }

            Text(name)
                .font(.caption)
                .foregroundColor(.secondary)

            Text(latestValue)
                .font(.title2)
                .fontWeight(.bold)

            if let change = changeStr {
                Text(change)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
    }

    private func metricCard(type: String, name: String, icon: String, color: Color) -> some View {
        let periodMetrics = metricsInPeriod(type: type)
        let latestValue = periodMetrics.last?.value

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                    .frame(width: 40)

                VStack(alignment: .leading, spacing: 2) {
                    Text(name)
                        .font(.headline)
                    if let value = latestValue {
                        Text("Latest: \(String(format: "%.1f", value)) \(unitFor(type))")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    } else {
                        Text("No data in period")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                NavigationLink(destination: MetricHistoryScreen(userId: appState.currentUserId ?? 1, metricType: type, metricName: name)) {
                    Text("History")
                        .font(.caption)
                        .foregroundColor(.green)
                }
            }

            // Mini chart — always visible (shows single bar if only 1 entry)
            miniChart(metrics: Array(periodMetrics.suffix(14)), color: color)

            // Range distribution
            if periodMetrics.count >= 2 {
                let values = periodMetrics.map { $0.value }
                let lo = values.min()!; let hi = values.max()!
                HStack {
                    Text("Range: \(String(format: "%.1f", lo)) – \(String(format: "%.1f", hi)) \(unitFor(type))")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("\(periodMetrics.count) readings")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
        .padding(.horizontal)
    }

    private func getFormattedValue(_ metric: HealthMetric, type: String) -> String {
        "\(String(format: "%.1f", metric.value)) \(unitFor(type))"
    }

    private func periodChange(metrics: [HealthMetric], type: String) -> String? {
        guard metrics.count >= 2, let first = metrics.first, let last = metrics.last else { return nil }
        let diff = last.value - first.value
        let sign = diff >= 0 ? "+" : ""
        return "\(sign)\(String(format: "%.1f", diff)) \(unitFor(type)) vs \(periodDays)d ago"
    }

    private func miniChart(metrics: [HealthMetric], color: Color) -> some View {
        let values = metrics.map { $0.value }
        let maxVal = values.max() ?? 1
        let minVal = values.min() ?? 0
        let range = max(maxVal - minVal, 1)

        return Group {
            if values.isEmpty {
                // No data placeholder
                HStack {
                    Text("No data for period")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .frame(height: 44)
            } else {
                HStack(alignment: .bottom, spacing: 4) {
                    ForEach(Array(values.enumerated()), id: \.offset) { _, value in
                        let normalizedHeight = (value - minVal) / range
                        RoundedRectangle(cornerRadius: 2)
                            .fill(color.opacity(0.6))
                            .frame(width: 8, height: max(4, CGFloat(normalizedHeight) * 40))
                    }
                    Spacer()
                }
                .frame(height: 44)
            }
        }
    }

    private var customMetricsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Custom Metrics")
                .font(.headline)
                .padding(.horizontal)

            ForEach(viewModel.customMetricTypes, id: \.id) { customType in
                let metricsOfType = viewModel.metrics.filter { $0.customTypeId?.int64Value == customType.id }
                let latestValue = metricsOfType.first?.value

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(customType.name)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        if let value = latestValue {
                            Text("\(String(format: "%.1f", value)) \(customType.unit)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    Spacer()

                    Button(action: {
                        // TODO: Navigate to custom metric history
                    }) {
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                .background(Color.white)
                .cornerRadius(10)
                .padding(.horizontal)
            }
        }
    }

    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: [Color.green.opacity(0.2), Color.white]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()
    }

    private func loadMetrics() {
        if let userId = appState.currentUserId {
            viewModel.loadMetrics(userId: userId)
        }
    }

    private func getLatestValue(for type: String) -> String? {
        guard let metric = viewModel.metrics.first(where: { $0.metricType == type }) else {
            return nil
        }
        return "\(String(format: "%.1f", metric.value)) \(unitFor(type))"
    }

    private func unitFor(_ type: String) -> String {
        switch type {
        case "weight": return "kg"
        case "blood_sugar": return "mg/dL"
        case "blood_pressure": return "mmHg"
        case "sleep": return "hrs"
        default: return ""
        }
    }
}

struct MetricHistoryScreen: View {
    let userId: Int64
    let metricType: String
    let metricName: String

    @StateObject private var viewModel = HealthMetricsViewModel()
    @State private var metrics: [HealthMetric] = []

    var body: some View {
        List {
            if metrics.isEmpty {
                Text("No \(metricName.lowercased()) data recorded")
                    .foregroundColor(.secondary)
            } else {
                ForEach(metrics, id: \.id) { metric in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(formatDate(metric.date))
                                .font(.subheadline)
                            if let notes = metric.notes {
                                Text(notes)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        Spacer()
                        Text("\(String(format: "%.1f", metric.value))")
                            .font(.headline)
                    }
                }
                .onDelete { indexSet in
                    deleteMetrics(at: indexSet)
                }
            }
        }
        .navigationTitle("\(metricName) History")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            loadHistory()
        }
    }

    private func loadHistory() {
        Task {
            metrics = (try? await viewModel.loadMetricsByType(userId: userId, metricType: metricType)) ?? []
        }
    }

    private func deleteMetrics(at offsets: IndexSet) {
        Task {
            for index in offsets {
                try? await viewModel.deleteMetric(id: metrics[index].id)
            }
            loadHistory()
        }
    }

    private func formatDate(_ dateString: String) -> String {
        let inputFormatter = DateFormatter()
        inputFormatter.dateFormat = "yyyy-MM-dd"

        let outputFormatter = DateFormatter()
        outputFormatter.dateStyle = .medium

        if let date = inputFormatter.date(from: dateString) {
            return outputFormatter.string(from: date)
        }
        return dateString
    }
}

struct HealthMetricsScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            HealthMetricsScreen()
                .environmentObject(AppState())
        }
    }
}
