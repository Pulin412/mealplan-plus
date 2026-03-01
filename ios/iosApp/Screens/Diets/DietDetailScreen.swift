import SwiftUI
import shared

struct DietDetailScreenNew: View {
    let dietId: Int64
    var onUpdate: (() -> Void)? = nil

    @StateObject private var viewModel = DietsViewModel()
    @State private var dietWithMeals: DietWithMeals?
    @State private var tags: [Tag] = []
    @State private var selectedDay = 0
    @State private var showEditSheet = false

    var body: some View {
        ScrollView {
            contentView
        }
        .background(backgroundGradient)
        .navigationTitle("Diet Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarContent }
        .sheet(isPresented: $showEditSheet) { editSheetContent }
        .onAppear { loadDietDetails() }
    }

    @ViewBuilder
    private var contentView: some View {
        if let dwm = dietWithMeals {
            dietContent(dwm)
        } else {
            loadingView
        }
    }

    private var loadingView: some View {
        VStack {
            Spacer()
            ProgressView("Loading diet...")
            Spacer()
        }
    }

    private func dietContent(_ dwm: DietWithMeals) -> some View {
        VStack(spacing: 20) {
            headerCard(dwm)
            nutritionCard(dwm)
            daySelector
            mealSlotsSection(dwm)
            actionButtons
            Spacer().frame(height: 20)
        }
        .padding(.top)
    }

    private func headerCard(_ dwm: DietWithMeals) -> some View {
        VStack(spacing: 12) {
            Text(dwm.diet.name)
                .font(.title2)
                .fontWeight(.bold)

            if let desc = dwm.diet.description_ {
                Text(desc)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            tagsView
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5)
        .padding(.horizontal)
    }

    @ViewBuilder
    private var tagsView: some View {
        if !tags.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(tags, id: \.id) { tag in
                        tagChip(tag)
                    }
                }
            }
        }
    }

    private func tagChip(_ tag: Tag) -> some View {
        let tagColor = Color(hex: tag.color ?? "#4CAF50")
        return Text(tag.name)
            .font(.caption)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(tagColor.opacity(0.2))
            .foregroundColor(tagColor)
            .cornerRadius(12)
    }

    private func nutritionCard(_ dwm: DietWithMeals) -> some View {
        VStack(spacing: 16) {
            Text("Daily Nutrition")
                .font(.headline)

            HStack(spacing: 16) {
                NutritionBox(value: dwm.totalCalories, label: "Calories", unit: "kcal", color: .green)
                NutritionBox(value: dwm.totalProtein, label: "Protein", unit: "g", color: .red)
                NutritionBox(value: dwm.totalCarbs, label: "Carbs", unit: "g", color: .blue)
                NutritionBox(value: dwm.totalFat, label: "Fat", unit: "g", color: .yellow)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5)
        .padding(.horizontal)
    }

    private var daySelector: some View {
        let days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
        return VStack(spacing: 12) {
            Text("Meal Plan")
                .font(.headline)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<7, id: \.self) { index in
                        dayButton(days[index], index: index)
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5)
        .padding(.horizontal)
    }

    private func dayButton(_ day: String, index: Int) -> some View {
        let isSelected = selectedDay == index
        return Button(action: { selectedDay = index }) {
            Text(day)
                .font(.subheadline)
                .fontWeight(isSelected ? .bold : .regular)
                .frame(width: 44, height: 44)
                .background(isSelected ? Color.green : Color.clear)
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(22)
                .overlay(
                    RoundedRectangle(cornerRadius: 22)
                        .stroke(Color.green, lineWidth: isSelected ? 0 : 1)
                )
        }
    }

    private func mealSlotsSection(_ dwm: DietWithMeals) -> some View {
        let slots = ["Breakfast", "Lunch", "Dinner", "Snack"]
        // KMP Map<String, MealWithFoods?> bridges as NSDictionary
        var mealsMap: [String: MealWithFoods?] = [:]
        if let nd = dwm.meals as? NSDictionary {
            for (k, v) in nd {
                if let key = k as? String { mealsMap[key] = v as? MealWithFoods }
            }
        }
        // KMP Map<String, String?> (instructions) also bridges as NSDictionary
        var instructionsMap: [String: String?] = [:]
        if let nd = dwm.instructions as? NSDictionary {
            for (k, v) in nd {
                if let key = k as? String { instructionsMap[key] = v as? String }
            }
        }
        return VStack(alignment: .leading, spacing: 12) {
            ForEach(slots, id: \.self) { slot in
                if let mealWithFoods = mealsMap[slot] ?? nil {
                    MealSlotCardNew(
                        slot: slot,
                        mealWithFoods: mealWithFoods,
                        instructions: instructionsMap[slot] ?? nil
                    )
                } else {
                    EmptyMealSlotCard(slot: slot)
                }
            }
        }
        .padding(.horizontal)
    }

    private var actionButtons: some View {
        HStack(spacing: 12) {
            applyTodayButton
            groceryButton
        }
        .padding(.horizontal)
    }

    private var applyTodayButton: some View {
        Button(action: {}) {
            HStack {
                Image(systemName: "calendar.badge.plus")
                Text("Apply to Today")
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.green)
            .foregroundColor(.white)
            .cornerRadius(10)
        }
    }

    private var groceryButton: some View {
        Button(action: {}) {
            HStack {
                Image(systemName: "cart")
                Text("Grocery List")
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.white)
            .foregroundColor(.green)
            .cornerRadius(10)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color.green, lineWidth: 1)
            )
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

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            Menu {
                Button(action: { showEditSheet = true }) {
                    Label("Edit Diet", systemImage: "pencil")
                }
                Button(action: {}) {
                    Label("Duplicate", systemImage: "doc.on.doc")
                }
                Button(role: .destructive, action: {}) {
                    Label("Delete", systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
        }
    }

    @ViewBuilder
    private var editSheetContent: some View {
        if let dwm = dietWithMeals {
            AddDietScreenNew(userId: dwm.diet.userId, onSave: {
                loadDietDetails()
                onUpdate?()
            }, existingDiet: dwm.diet)
        }
    }

    private func loadDietDetails() {
        Task {
            dietWithMeals = try? await viewModel.getDietWithMeals(dietId: dietId)
            tags = (try? await viewModel.getTagsForDiet(dietId: dietId)) ?? []
        }
    }
}

struct NutritionBox: View {
    let value: Double
    let label: String
    let unit: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text("\(Int(value))")
                .font(.title3)
                .fontWeight(.bold)
            Text(unit)
                .font(.caption2)
                .foregroundColor(.secondary)
            Text(label)
                .font(.caption)
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(color.opacity(0.1))
        .cornerRadius(8)
    }
}

struct MealSlotCardNew: View {
    let slot: String
    let mealWithFoods: MealWithFoods
    var instructions: String? = nil

    var slotIcon: String {
        switch slot {
        case "Breakfast": return "sunrise.fill"
        case "Lunch": return "sun.max.fill"
        case "Dinner": return "moon.fill"
        default: return "leaf.fill"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: slotIcon)
                    .foregroundColor(.green)
                    .frame(width: 30)

                VStack(alignment: .leading, spacing: 2) {
                    Text(slot)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(mealWithFoods.meal.name)
                        .font(.subheadline)
                        .fontWeight(.medium)
                }

                Spacer()

                Text("\(Int(mealWithFoods.totalCalories)) kcal")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            if let note = instructions, !note.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "note.text")
                        .font(.caption2)
                        .foregroundColor(.green)
                    Text(note)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                .padding(.leading, 36)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: .black.opacity(0.05), radius: 2)
    }
}

struct EmptyMealSlotCard: View {
    let slot: String

    var slotIcon: String {
        switch slot {
        case "Breakfast": return "sunrise.fill"
        case "Lunch": return "sun.max.fill"
        case "Dinner": return "moon.fill"
        default: return "leaf.fill"
        }
    }

    var body: some View {
        HStack {
            Image(systemName: slotIcon)
                .foregroundColor(.gray)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 2) {
                Text(slot)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("No meal assigned")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Image(systemName: "plus.circle")
                .foregroundColor(.green)
        }
        .padding()
        .background(Color.white.opacity(0.7))
        .cornerRadius(10)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.gray.opacity(0.3), style: StrokeStyle(lineWidth: 1, dash: [5]))
        )
    }
}

struct AddDietScreenNew: View {
    @Environment(\.dismiss) var dismiss
    let userId: Int64
    var onSave: () -> Void
    var existingDiet: Diet? = nil

    @StateObject private var viewModel = DietsViewModel()
    @State private var name = ""
    @State private var description = ""
    @State private var tagInput = ""
    @State private var selectedTags: [Tag] = []
    @State private var availableTags: [Tag] = []

    var isEditMode: Bool { existingDiet != nil }
    var isFormValid: Bool { !name.isEmpty }

    var body: some View {
        NavigationStack {
            Form {
                dietInfoSection
                tagsSection
                noteSection
                saveButtonSection
            }
            .navigationTitle(isEditMode ? "Edit Diet" : "New Diet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
            .onAppear { onAppearSetup() }
        }
    }

    private var dietInfoSection: some View {
        Section("Diet Info") {
            TextField("Diet Name", text: $name)
            TextField("Description", text: $description, axis: .vertical)
                .lineLimit(3...6)
        }
    }

    private var tagsSection: some View {
        Section("Tags") {
            if !availableTags.isEmpty {
                availableTagsView
            }
            newTagField
        }
    }

    private var availableTagsView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(availableTags, id: \.id) { tag in
                    tagToggleButton(tag)
                }
            }
        }
    }

    private func tagToggleButton(_ tag: Tag) -> some View {
        let isSelected = selectedTags.contains(where: { $0.id == tag.id })
        return Button(action: {
            if isSelected {
                selectedTags.removeAll { $0.id == tag.id }
            } else {
                selectedTags.append(tag)
            }
        }) {
            Text(tag.name)
                .font(.caption)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.green : Color.gray.opacity(0.2))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(16)
        }
    }

    private var newTagField: some View {
        HStack {
            TextField("New tag name", text: $tagInput)
            Button(action: createTag) {
                Image(systemName: "plus.circle.fill")
                    .foregroundColor(.green)
            }
            .disabled(tagInput.isEmpty)
        }
    }

    private var noteSection: some View {
        Section {
            Text("After creating, assign meals to each slot")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }

    private var saveButtonSection: some View {
        Section {
            Button(action: saveDiet) {
                HStack {
                    Spacer()
                    Text(isEditMode ? "Update Diet" : "Create Diet")
                        .fontWeight(.semibold)
                    Spacer()
                }
            }
            .disabled(!isFormValid)
        }
    }

    private func onAppearSetup() {
        loadTags()
        if let diet = existingDiet {
            name = diet.name
            description = diet.description_ ?? ""
            loadDietTags(dietId: diet.id)
        }
    }

    private func loadTags() {
        Task {
            availableTags = (try? await viewModel.getAllTags(userId: userId)) ?? []
        }
    }

    private func loadDietTags(dietId: Int64) {
        Task {
            selectedTags = (try? await viewModel.getTagsForDiet(dietId: dietId)) ?? []
        }
    }

    private func createTag() {
        let tagName = tagInput.trimmingCharacters(in: .whitespaces)
        guard !tagName.isEmpty else { return }

        Task {
            let tag = Tag(
                id: 0,
                userId: userId,
                name: tagName,
                color: "#4CAF50",
                createdAt: Int64(Date().timeIntervalSince1970 * 1000)
            )
            if let id = try? await RepositoryProvider.shared.dietRepository.insertTag(tag: tag) {
                let newTag = Tag(id: id.int64Value, userId: userId, name: tagName, color: "#4CAF50", createdAt: tag.createdAt)
                availableTags.append(newTag)
                selectedTags.append(newTag)
                tagInput = ""
            }
        }
    }

    private func saveDiet() {
        Task {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            let diet = Diet(
                id: existingDiet?.id ?? 0,
                userId: userId,
                name: name,
                description: description.isEmpty ? nil : description,
                createdAt: existingDiet?.createdAt ?? now,
                isSystemDiet: false,
                serverId: existingDiet?.serverId,
                updatedAt: now,
                syncedAt: nil
            )

            do {
                let dietId: Int64
                if isEditMode {
                    try await viewModel.updateDiet(diet)
                    dietId = diet.id
                    try await RepositoryProvider.shared.dietRepository.clearDietTags(dietId: dietId)
                } else {
                    dietId = try await viewModel.insertDiet(diet)
                }

                for tag in selectedTags {
                    try await viewModel.addTagToDiet(dietId: dietId, tagId: tag.id)
                }

                onSave()
                dismiss()
            } catch {
                print("Error saving diet: \(error)")
            }
        }
    }
}

// Color extension for hex colors
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

struct DietDetailScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            DietDetailScreenNew(dietId: 1)
        }
    }
}
