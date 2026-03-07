import SwiftUI
import shared

struct DietPickerSheet: View {
    let diets: [DietSummary]
    let onSelect: (Diet) -> Void
    let onDismiss: () -> Void

    @State private var searchText = ""
    @StateObject private var dietsVM = DietsViewModel()
    @EnvironmentObject var appState: AppState

    private var userId: Int64 { appState.currentUserId ?? 0 }

    private var filtered: [DietSummary] {
        if searchText.isEmpty { return diets }
        return diets.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Search diets...", text: $searchText)
                        .autocorrectionDisabled()
                }
                .padding(10)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 4)

                if filtered.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "fork.knife")
                            .font(.largeTitle)
                            .foregroundColor(.secondary.opacity(0.4))
                        Text(diets.isEmpty ? "No diets available" : "No results for \"\(searchText)\"")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                } else {
                    List(filtered, id: \.id) { summary in
                        DietPickerRow(summary: summary, dietsVM: dietsVM) { diet in
                            onSelect(diet)
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Pick a Diet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel", action: onDismiss)
                }
            }
        }
    }
}

// MARK: - DietPickerRow

private struct DietPickerRow: View {
    let summary: DietSummary
    let dietsVM: DietsViewModel
    let onSelect: (Diet) -> Void

    @State private var tags: [Tag] = []
    @State private var loaded = false

    var body: some View {
        Button(action: {
            let diet = Diet(
                id: summary.id,
                userId: summary.userId,
                name: summary.name,
                description: summary.description_,
                createdAt: summary.createdAt,
                isSystemDiet: false,
                serverId: nil,
                updatedAt: summary.createdAt,
                syncedAt: nil
            )
            onSelect(diet)
        }) {
            HStack(spacing: 12) {
                // Diet name + tag
                VStack(alignment: .leading, spacing: 4) {
                    Text(summary.name)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    if let desc = summary.description_, !desc.isEmpty {
                        Text(desc)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    if let firstTag = tags.first {
                        Text(firstTag.name)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color(red: 0x7B/255.0, green: 0x1F/255.0, blue: 0xA2/255.0).opacity(0.12))
                            .foregroundColor(Color(red: 0x7B/255.0, green: 0x1F/255.0, blue: 0xA2/255.0))
                            .cornerRadius(8)
                    }
                }
                Spacer()
                // Calories only (DietSummary does not expose protein/carbs/fat)
                Text("\(summary.totalCalories) kcal")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
        .onAppear {
            guard !loaded else { return }
            loaded = true
            Task {
                tags = (try? await dietsVM.getTagsForDiet(dietId: summary.id)) ?? []
            }
        }
    }
}
