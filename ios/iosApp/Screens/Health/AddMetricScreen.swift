import SwiftUI
import shared

struct AddMetricScreen: View {
    @Environment(\.dismiss) var dismiss
    let userId: Int64
    var preselectedType: String? = nil
    var onSave: () -> Void

    @StateObject private var viewModel = HealthMetricsViewModel()

    @State private var selectedType = "weight"
    @State private var value = ""
    @State private var date = Date()
    @State private var notes = ""
    @State private var isLoading = false
    @State private var showError = false
    @State private var errorMessage = ""

    let metricTypes = [
        ("weight", "Weight", "kg"),
        ("blood_sugar", "Blood Sugar", "mg/dL"),
        ("blood_pressure", "Blood Pressure", "mmHg"),
        ("sleep", "Sleep", "hrs")
    ]

    var body: some View {
        NavigationStack {
            Form {
                Section("Metric Type") {
                    Picker("Type", selection: $selectedType) {
                        ForEach(metricTypes, id: \.0) { type in
                            Text(type.1).tag(type.0)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                }

                Section("Value") {
                    HStack {
                        TextField("Enter value", text: $value)
                            .keyboardType(.decimalPad)
                        Text(unitForSelectedType)
                            .foregroundColor(.secondary)
                    }
                }

                Section("Date") {
                    DatePicker("Date", selection: $date, displayedComponents: .date)
                }

                Section("Notes (Optional)") {
                    TextField("Add notes...", text: $notes)
                }

                Section {
                    Button(action: saveMetric) {
                        HStack {
                            Spacer()
                            if isLoading {
                                ProgressView()
                            } else {
                                Text("Save Metric")
                                    .fontWeight(.semibold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(!isFormValid || isLoading)
                }
            }
            .navigationTitle("Log Metric")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                if let type = preselectedType {
                    selectedType = type
                }
            }
            .alert("Error", isPresented: $showError) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(errorMessage)
            }
        }
    }

    private var unitForSelectedType: String {
        metricTypes.first { $0.0 == selectedType }?.2 ?? ""
    }

    private var isFormValid: Bool {
        guard let _ = Double(value) else { return false }
        return !value.isEmpty
    }

    private func saveMetric() {
        guard let doubleValue = Double(value) else { return }

        isLoading = true

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        let dateString = dateFormatter.string(from: date)

        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let metric = HealthMetric(
            id: 0,
            userId: userId,
            date: dateString,
            timestamp: now,
            metricType: selectedType,
            customTypeId: nil,
            value: doubleValue,
            secondaryValue: nil,
            subType: nil,
            notes: notes.isEmpty ? nil : notes,
            serverId: nil,
            updatedAt: now,
            syncedAt: nil
        )

        Task {
            do {
                try await viewModel.insertMetric(metric)
                await MainActor.run {
                    isLoading = false
                    onSave()
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = error.localizedDescription
                    showError = true
                }
            }
        }
    }
}

struct AddMetricScreen_Previews: PreviewProvider {
    static var previews: some View {
        AddMetricScreen(userId: 1, onSave: {})
    }
}
