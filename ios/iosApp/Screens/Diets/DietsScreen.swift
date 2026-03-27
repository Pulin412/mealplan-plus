import SwiftUI
import shared

// Identifiable+Hashable wrapper for Int64 diet IDs
private struct DietIDWrapper: Identifiable, Hashable {
    let id: Int64
}

// MARK: - Diets Screen

struct DietsScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var vm = DietsViewModel()
    @State private var searchText = ""
    @State private var foodFilter = ""
    @State private var showFoodFilter = false
    @State private var selectedTagId: Int64? = nil
    @State private var expandedDietId: Int64? = nil
    @State private var showAddDiet = false
    @State private var editDietForId: DietIDWrapper? = nil
    @State private var deleteDietId: Int64? = nil
    @State private var showDeleteAlert = false

    /// Set to receive a DietSummary when in picker mode (diet selection flow).
    var onSelect: ((DietSummary) -> Void)? = nil
    private var isPickerMode: Bool { onSelect != nil }

    private var userId: Int64 { Int64(appState.currentUserId ?? 1) }

    // All unique tags derived from loaded dietTagsMap
    private var allUniqueTags: [Tag] {
        var seen = Set<Int64>()
        var tags: [Tag] = []
        for dietTags in vm.dietTagsMap.values {
            for tag in dietTags {
                if !seen.contains(tag.id) { seen.insert(tag.id); tags.append(tag) }
            }
        }
        return tags.sorted { $0.name < $1.name }
    }

    // Count of diets per tag
    private var tagCounts: [Int64: Int] {
        var counts: [Int64: Int] = [:]
        for (_, tags) in vm.dietTagsMap {
            for tag in tags { counts[tag.id, default: 0] += 1 }
        }
        return counts
    }

    private var filtered: [DietSummary] {
        var result = vm.diets
        if !searchText.isEmpty {
            result = result.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
        }
        if let tagId = selectedTagId {
            result = result.filter { vm.dietTagsMap[$0.id]?.contains { $0.id == tagId } ?? false }
        }
        if !foodFilter.isEmpty {
            result = result.filter { diet in
                vm.dietFoodNamesMap[diet.id]?.contains { $0.localizedCaseInsensitiveContains(foodFilter) } ?? false
            }
        }
        return result
    }

    private var hasActiveFilter: Bool { selectedTagId != nil || !foodFilter.isEmpty }
    private let green = Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0)

    var body: some View {
        ZStack {
            Color(red: 0xF0/255.0, green: 0xF9/255.0, blue: 0xF4/255.0).ignoresSafeArea()

            VStack(spacing: 0) {
                // Search bar + filter toggle
                HStack(spacing: 8) {
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass").foregroundColor(.secondary)
                        TextField("Search diets…", text: $searchText)
                        if !searchText.isEmpty {
                            Button(action: { searchText = "" }) {
                                Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding(10)
                    .background(Color(.systemBackground))
                    .cornerRadius(10)
                    .shadow(color: .black.opacity(0.05), radius: 3)

                    Button(action: { withAnimation(.easeInOut(duration: 0.2)) { showFoodFilter.toggle() } }) {
                        Image(systemName: showFoodFilter || !foodFilter.isEmpty
                              ? "line.3.horizontal.decrease.circle.fill"
                              : "line.3.horizontal.decrease.circle")
                            .font(.system(size: 22))
                            .foregroundColor(green)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 8)

                // Ingredient filter (collapsible)
                if showFoodFilter {
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 8) {
                            Image(systemName: "leaf").foregroundColor(.secondary)
                            TextField("Filter by ingredient…", text: $foodFilter)
                            if !foodFilter.isEmpty {
                                Button(action: { foodFilter = "" }) {
                                    Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                                }
                            }
                        }
                        .padding(10)
                        .background(Color(.systemBackground))
                        .cornerRadius(10)
                        .shadow(color: .black.opacity(0.05), radius: 3)
                        .padding(.horizontal, 16)

                        if !foodFilter.isEmpty {
                            HStack {
                                Text("Diets containing '\(foodFilter)'")
                                    .font(.caption).foregroundColor(.secondary)
                                Spacer()
                                Button("Clear") { foodFilter = "" }
                                    .font(.caption).foregroundColor(.red)
                            }
                            .padding(.horizontal, 16)
                        }
                    }
                    .padding(.bottom, 8)
                    .transition(.move(edge: .top).combined(with: .opacity))
                }

                // Tag filter chips (only shown once tags are loaded)
                if !allUniqueTags.isEmpty {
                    let tc = tagCounts
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            TagFilterChip(label: "All \(vm.diets.count)", isSelected: selectedTagId == nil) {
                                selectedTagId = nil
                            }
                            ForEach(allUniqueTags, id: \.id) { tag in
                                let count = tc[tag.id] ?? 0
                                if count > 0 {
                                    TagFilterChip(
                                        label: "\(tag.name) (\(count))",
                                        isSelected: selectedTagId == tag.id,
                                        color: Color(hex: tag.color ?? "#2E7D52")
                                    ) {
                                        selectedTagId = selectedTagId == tag.id ? nil : tag.id
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 8)
                }

                // Clear all filters button
                if hasActiveFilter {
                    HStack {
                        Spacer()
                        Button(action: { selectedTagId = nil; foodFilter = "" }) {
                            Label("Clear all filters", systemImage: "xmark.circle")
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                        .padding(.trailing, 16)
                    }
                    .padding(.bottom, 6)
                }

                if vm.isLoading {
                    Spacer(); ProgressView(); Spacer()
                } else if filtered.isEmpty {
                    VStack(spacing: 16) {
                        Spacer()
                        Image(systemName: "fork.knife")
                            .font(.system(size: 48)).foregroundColor(.secondary)
                        Text(searchText.isEmpty && !hasActiveFilter
                             ? "No diets yet"
                             : "No diets match filters")
                            .font(.headline).foregroundColor(.secondary)
                        if searchText.isEmpty && !hasActiveFilter {
                            Text("Tap + to create your first diet plan")
                                .font(.subheadline).foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        Spacer()
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 10) {
                            ForEach(filtered, id: \.id) { summary in
                                ExpandableDietCard(
                                    summary: summary,
                                    tags: vm.dietTagsMap[summary.id] ?? [],
                                    isExpanded: expandedDietId == summary.id,
                                    isPickerMode: isPickerMode,
                                    onToggleExpand: {
                                        expandedDietId = expandedDietId == summary.id ? nil : summary.id
                                    },
                                    onSelect: { onSelect?(summary) },
                                    viewLink: DietIDWrapper(id: summary.id),
                                    onEdit: { editDietForId = DietIDWrapper(id: summary.id) },
                                    onDuplicate: { duplicateDiet(summary) },
                                    onDelete: {
                                        deleteDietId = summary.id
                                        showDeleteAlert = true
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                    }
                }
            }
        }
        .navigationTitle(isPickerMode ? "Select a Diet" : "My Diets")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0), for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar {
            if !isPickerMode {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showAddDiet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .sheet(isPresented: $showAddDiet, onDismiss: { vm.loadDiets(userId: userId) }) {
            AddDietScreenNew(userId: userId, onSave: { vm.loadDiets(userId: userId); showAddDiet = false })
        }
        .sheet(item: $editDietForId, onDismiss: { vm.loadDiets(userId: userId) }) { wrapper in
            NavigationStack {
                DietDetailScreenNew(dietId: wrapper.id, isReadOnly: false, onUpdate: { vm.loadDiets(userId: userId) })
            }
        }
        .navigationDestination(for: DietIDWrapper.self) { wrapper in
            DietDetailScreenNew(dietId: wrapper.id, isReadOnly: true, onUpdate: { vm.loadDiets(userId: userId) })
        }
        .alert("Delete Diet?", isPresented: $showDeleteAlert) {
            Button("Delete", role: .destructive) {
                if let id = deleteDietId {
                    Task {
                        try? await vm.deleteDiet(id: id)
                        vm.loadDiets(userId: userId)
                    }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: { Text("This diet plan will be permanently removed.") }
        .onAppear { vm.loadDiets(userId: userId) }
    }

    private func duplicateDiet(_ summary: DietSummary) {
        Task {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            let copy = Diet(
                id: 0,
                userId: summary.userId,
                name: summary.name + " (Copy)",
                description: summary.description_,
                createdAt: now,
                isSystemDiet: false,
                serverId: nil,
                updatedAt: now,
                syncedAt: nil
            )
            _ = try? await vm.insertDiet(copy)
            vm.loadDiets(userId: userId)
        }
    }
}

// MARK: - Expandable Diet Card

private struct ExpandableDietCard: View {
    let summary: DietSummary
    let tags: [Tag]
    let isExpanded: Bool
    var isPickerMode: Bool = false
    let onToggleExpand: () -> Void
    let onSelect: () -> Void
    let viewLink: DietIDWrapper
    let onEdit: () -> Void
    let onDuplicate: () -> Void
    let onDelete: () -> Void

    private let green = Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0)

    var body: some View {
        VStack(spacing: 0) {
            Button(action: isPickerMode ? onSelect : onToggleExpand) {
                HStack(alignment: .top, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(summary.name)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.primary)
                            .lineLimit(1)
                        if let desc = summary.description_, !desc.isEmpty {
                            Text(desc)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                        }
                        HStack(spacing: 10) {
                            Label("\(summary.totalCalories) kcal", systemImage: "flame.fill")
                                .font(.caption2).foregroundColor(green)
                            Label("\(summary.mealCount) meals", systemImage: "fork.knife")
                                .font(.caption2).foregroundColor(.secondary)
                        }
                        .padding(.top, 2)
                        // Tag chips
                        if !tags.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 4) {
                                    ForEach(tags.prefix(3), id: \.id) { tag in
                                        Text(tag.name)
                                            .font(.system(size: 10, weight: .medium))
                                            .padding(.horizontal, 7)
                                            .padding(.vertical, 3)
                                            .background(Color(hex: tag.color ?? "#2E7D52").opacity(0.15))
                                            .foregroundColor(Color(hex: tag.color ?? "#2E7D52"))
                                            .clipShape(Capsule())
                                    }
                                }
                            }
                            .padding(.top, 2)
                        }
                    }
                    Spacer()
                    Image(systemName: isPickerMode ? "chevron.right"
                          : (isExpanded ? "chevron.up" : "chevron.down"))
                        .foregroundColor(.secondary).font(.system(size: 12))
                }
                .padding(14)
            }
            .buttonStyle(.plain)

            if isExpanded && !isPickerMode {
                Divider()
                HStack(spacing: 0) {
                    NavigationLink(value: viewLink) {
                        VStack(spacing: 3) {
                            Image(systemName: "eye").font(.system(size: 14))
                            Text("View").font(.caption2)
                        }
                        .foregroundColor(green)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                    }
                    .buttonStyle(.plain)
                    Divider().frame(height: 36)
                    dietAction("Edit", "pencil", green) { onEdit() }
                    Divider().frame(height: 36)
                    dietAction("Duplicate", "doc.on.doc", .blue) { onDuplicate() }
                    Divider().frame(height: 36)
                    dietAction("Delete", "trash", .red) { onDelete() }
                }
                .padding(.vertical, 4)
            }
        }
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }

    private func dietAction(_ label: String, _ icon: String, _ color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 3) {
                Image(systemName: icon).font(.system(size: 14))
                Text(label).font(.caption2)
            }
            .foregroundColor(color)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Tag Filter Chip

private struct TagFilterChip: View {
    let label: String
    let isSelected: Bool
    var color: Color = Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0)
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? color : Color(.systemGray6))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}

// MARK: - Legacy models kept for backward compat

struct DietUI: Identifiable {
    let id: Int64
    let name: String
    let description: String
    let calories: Int
    let protein: Double
    let carbs: Double
    let fat: Double
    let mealCount: Int
    let tags: [String]
}

struct EmptyDietsView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "calendar")
                .font(.system(size: 60))
                .foregroundColor(.green.opacity(0.5))
            Text("No diets yet").font(.headline)
            Text("Create a diet plan by combining meals")
                .font(.subheadline).foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

struct StatItem: View {
    let value: String
    let label: String
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.subheadline).fontWeight(.semibold)
            Text(label).font(.caption2).foregroundColor(.secondary)
        }
    }
}

struct DietsScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            DietsScreen().environmentObject(AppState())
        }
    }
}
