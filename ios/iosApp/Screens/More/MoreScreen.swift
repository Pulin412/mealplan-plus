import SwiftUI
import shared

struct MoreScreen: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        List {
            Section {
                NavigationLink(destination: GroceryListsScreen()) {
                    MoreMenuItem(icon: "cart.fill", title: "Grocery Lists", color: .orange)
                }

                NavigationLink(destination: HealthMetricsScreen()) {
                    MoreMenuItem(icon: "heart.fill", title: "Health Metrics", color: .red)
                }

                NavigationLink(destination: ChartsScreen()) {
                    MoreMenuItem(icon: "chart.bar.fill", title: "Analytics", color: .green)
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

// MARK: - Grocery Lists Screen

struct GroceryListsScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = GroceryViewModel()
    @State private var showCreateList = false

    var body: some View {
        ZStack {
            if viewModel.isLoading {
                ProgressView("Loading lists...")
            } else if viewModel.groceryLists.isEmpty {
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
                    ForEach(viewModel.groceryLists, id: \.id) { list in
                        NavigationLink(destination: GroceryDetailScreen(listId: list.id, listName: list.name, onUpdate: { loadLists() })) {
                            GroceryListRow(list: list)
                        }
                    }
                    .onDelete { indexSet in
                        deleteList(at: indexSet)
                    }
                }
            }
        }
        .navigationTitle("Grocery Lists")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0), for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showCreateList = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showCreateList) {
            CreateGroceryListScreen(userId: appState.currentUserId ?? 1) {
                loadLists()
            }
        }
        .onAppear {
            loadLists()
        }
        .refreshable {
            loadLists()
        }
    }

    private func loadLists() {
        if let userId = appState.currentUserId {
            viewModel.loadGroceryLists(userId: userId)
        }
    }

    private func deleteList(at indexSet: IndexSet) {
        Task {
            for index in indexSet {
                try? await viewModel.deleteGroceryList(id: viewModel.groceryLists[index].id)
            }
            loadLists()
        }
    }
}

struct GroceryListRow: View {
    let list: GroceryList

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(list.name)
                    .font(.headline)
                Text(formatDate(list.createdAt))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
        }
    }

    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: date)
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
    let userId: Int64
    var onSave: () -> Void

    @StateObject private var viewModel = GroceryViewModel()
    @State private var name = ""
    @State private var isLoading = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("List Name", text: $name)
                }

                Section {
                    Text("Add items manually after creating the list")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Section {
                    Button(action: createList) {
                        HStack {
                            Spacer()
                            if isLoading {
                                ProgressView()
                            } else {
                                Text("Create List")
                                    .fontWeight(.semibold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(name.isEmpty || isLoading)
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
        isLoading = true
        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)
        let list = GroceryList(
            id: 0,
            userId: userId,
            name: name,
            startDate: nil,
            endDate: nil,
            createdAt: timestamp,
            updatedAt: timestamp,
            serverId: nil,
            syncedAt: nil
        )

        Task {
            do {
                _ = try await viewModel.insertGroceryList(list)
                await MainActor.run {
                    isLoading = false
                    onSave()
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                }
            }
        }
    }
}

// MARK: - Grocery Detail Screen

private let groceryCategoryOrder = [
    "Dairy", "Proteins", "Grains", "Vegetables", "Fruits",
    "Fats & Oils", "Beverages", "Spices & Condiments", "Other"
]
private let groceryCategoryEmojis: [String: String] = [
    "Dairy": "🥛", "Proteins": "🥩", "Grains": "🌾",
    "Vegetables": "🥦", "Fruits": "🍎", "Fats & Oils": "🫙",
    "Beverages": "🧃", "Spices & Condiments": "🧂", "Other": "🛒"
]

struct GroceryDetailScreen: View {
    let listId: Int64
    let listName: String
    var onUpdate: () -> Void

    @StateObject private var viewModel = GroceryViewModel()
    @State private var showAddItem = false
    @State private var newItemName = ""
    @State private var selectedTab = 0  // 0 = All, 1 = To Buy

    var checkedCount: Int { viewModel.currentList?.items.filter { $0.item.isChecked }.count ?? 0 }
    var totalCount: Int   { viewModel.currentList?.items.count ?? 0 }
    var items: [GroceryItemWithFood] { viewModel.currentList?.items ?? [] }

    var toBuyItems: [GroceryItemWithFood] { items.filter { !$0.item.isChecked } }

    var groupedItems: [(String, [GroceryItemWithFood])] {
        var dict: [String: [GroceryItemWithFood]] = [:]
        for item in items {
            let cat = item.item.category ?? "Other"
            dict[cat, default: []].append(item)
        }
        return groceryCategoryOrder.compactMap { cat -> (String, [GroceryItemWithFood])? in
            guard let catItems = dict[cat], !catItems.isEmpty else { return nil }
            let emoji = groceryCategoryEmojis[cat] ?? "🛒"
            return (emoji + " " + cat, catItems)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Progress header
            if totalCount > 0 {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(checkedCount) of \(totalCount) items")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        if totalCount > 0 {
                            Text("\(Int(Double(checkedCount) / Double(totalCount) * 100))% complete")
                                .font(.caption2)
                                .foregroundColor(.green)
                        }
                    }
                    Spacer()
                    CircularProgress(progress: Double(checkedCount) / Double(max(totalCount, 1)))
                }
                .padding()
                .background(Color.white)
            }

            // Tab selector
            Picker("View", selection: $selectedTab) {
                Text("All (\(totalCount))").tag(0)
                Text("To Buy (\(toBuyItems.count))").tag(1)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            .padding(.vertical, 8)
            .background(Color.white)

            if viewModel.currentList == nil {
                Spacer()
                ProgressView("Loading items...")
                Spacer()
            } else if items.isEmpty {
                VStack(spacing: 16) {
                    Spacer()
                    Image(systemName: "cart")
                        .font(.system(size: 50))
                        .foregroundColor(.gray.opacity(0.5))
                    Text("No items yet")
                        .font(.headline)
                    Text("Tap + to add items")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            } else if selectedTab == 0 {
                // All — grouped by category
                List {
                    ForEach(groupedItems, id: \.0) { (header, catItems) in
                        Section(header: Text(header)) {
                            ForEach(catItems, id: \.item.id) { itemWithFood in
                                GroceryItemRow(itemWithFood: itemWithFood) { isChecked in
                                    toggleItem(itemWithFood: itemWithFood, isChecked: isChecked)
                                }
                                .swipeActions(edge: .trailing) {
                                    Button(role: .destructive) {
                                        Task {
                                            try? await viewModel.deleteGroceryItem(id: itemWithFood.item.id)
                                            loadItems()
                                        }
                                    } label: { Label("Delete", systemImage: "trash") }
                                }
                            }
                        }
                    }
                }
            } else {
                // To Buy — flat unchecked list
                if toBuyItems.isEmpty {
                    VStack(spacing: 16) {
                        Spacer()
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 50))
                            .foregroundColor(.green.opacity(0.5))
                        Text("All items checked!")
                            .font(.headline)
                        Spacer()
                    }
                } else {
                    List {
                        ForEach(toBuyItems, id: \.item.id) { itemWithFood in
                            GroceryItemRow(itemWithFood: itemWithFood) { isChecked in
                                toggleItem(itemWithFood: itemWithFood, isChecked: isChecked)
                            }
                        }
                        .onDelete { indexSet in
                            Task {
                                for index in indexSet {
                                    try? await viewModel.deleteGroceryItem(id: toBuyItems[index].item.id)
                                }
                                loadItems()
                            }
                        }
                    }
                    .listStyle(PlainListStyle())
                }
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
        .navigationTitle(listName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddItem = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .alert("Add Item", isPresented: $showAddItem) {
            TextField("Item name", text: $newItemName)
            Button("Cancel", role: .cancel) { newItemName = "" }
            Button("Add") { addItem() }
        }
        .onAppear { loadItems() }
    }

    private func loadItems() {
        Task { try? await viewModel.loadGroceryListWithItems(listId: listId) }
    }

    private func toggleItem(itemWithFood: GroceryItemWithFood, isChecked: Bool) {
        Task {
            try? await viewModel.updateItemChecked(id: itemWithFood.item.id, isChecked: isChecked)
            loadItems()
        }
    }

    private func addItem() {
        guard !newItemName.isEmpty else { return }
        let item = GroceryItem(
            id: 0,
            listId: listId,
            foodId: nil,
            customName: newItemName,
            quantity: 1.0,
            unit: .piece,
            isChecked: false,
            sortOrder: 0,
            category: nil
        )
        Task {
            try? await viewModel.insertGroceryItem(item)
            await MainActor.run { newItemName = "" }
            loadItems()
        }
    }
}

struct GroceryItemRow: View {
    let itemWithFood: GroceryItemWithFood
    var onToggle: (Bool) -> Void

    var body: some View {
        HStack {
            Button(action: { onToggle(!itemWithFood.item.isChecked) }) {
                Image(systemName: itemWithFood.item.isChecked ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(itemWithFood.item.isChecked ? .green : .gray)
                    .font(.title2)
            }
            .buttonStyle(PlainButtonStyle())

            VStack(alignment: .leading, spacing: 2) {
                Text(itemWithFood.displayName)
                    .font(.body)
                    .strikethrough(itemWithFood.item.isChecked)
                    .foregroundColor(itemWithFood.item.isChecked ? .secondary : .primary)
                Text(itemWithFood.displayQuantity)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Settings Screen

struct SettingsScreen: View {
    @EnvironmentObject var appState: AppState
    @State private var calorieGoal = "2000"
    @State private var darkMode = false
    @State private var notifications = true
    @State private var isImporting = false
    @State private var importMessage: String?

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

                Button(action: importSampleData) {
                    HStack {
                        if isImporting {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle())
                        }
                        Text(isImporting ? "Importing..." : "Import Sample Data")
                    }
                }
                .disabled(isImporting)

                if let message = importMessage {
                    Text(message)
                        .font(.caption)
                        .foregroundColor(message.contains("✓") ? .green : .red)
                }
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

    private func importSampleData() {
        guard let userId = appState.currentUserId else {
            importMessage = "Error: Not logged in"
            return
        }

        isImporting = true
        importMessage = nil

        Task {
            let result = await DataImporter.shared.importSampleData(userId: userId)

            await MainActor.run {
                isImporting = false
                importMessage = result
            }
        }
    }
}

// MARK: - Profile Screen

private let profileGreen = Color(red: 0x2E/255.0, green: 0x7D/255.0, blue: 0x52/255.0)
private let profileBg    = Color(red: 0xF0/255.0, green: 0xF9/255.0, blue: 0xF4/255.0)

@MainActor
class ProfileViewModel: ObservableObject {
    @Published var user: User?
    @Published var isLoading = false
    @Published var isSaving  = false
    @Published var saveSuccess = false
    @Published var isClearingData = false
    @Published var clearSuccess   = false
    @Published var error: String?

    private let repo        = RepositoryProvider.shared.userRepository
    private let dietRepo    = RepositoryProvider.shared.dietRepository
    private let groceryRepo = RepositoryProvider.shared.groceryRepository
    private let healthRepo  = RepositoryProvider.shared.healthMetricRepository

    func load(userId: Int64) {
        isLoading = true
        Task {
            user = try? await repo.getUserById(id: userId)
            isLoading = false
        }
    }

    func save(_ user: User) async {
        isSaving = true
        print("ProfileVM.save: id=\(user.id) email=\(user.email) weight=\(String(describing: user.weightKg)) height=\(String(describing: user.heightCm)) calories=\(String(describing: user.targetCalories))")
        do {
            try await repo.updateUser(user: user)
            self.user = user
            saveSuccess = true
            print("ProfileVM.save: SUCCESS")
        } catch {
            self.error = error.localizedDescription
            print("ProfileVM.save: ERROR \(error)")
        }
        isSaving = false
    }

    func clearAllData(userId: Int64) async {
        isClearingData = true
        // Delete all diets
        let diets = (try? await dietRepo.getDietSummariesSnapshot(userId: userId)) ?? []
        for d in diets { try? await dietRepo.deleteDiet(id: d.id) }
        // Delete all grocery lists
        let lists = (try? await groceryRepo.getAllGroceryListsSnapshot(userId: userId)) ?? []
        for l in lists { try? await groceryRepo.deleteGroceryList(id: l.id) }
        // Delete all health metrics
        let metrics = (try? await healthRepo.getAllHealthMetricsSnapshot(userId: userId)) ?? []
        for m in metrics { try? await healthRepo.deleteHealthMetric(id: m.id) }
        isClearingData = false
        clearSuccess = true
    }
}

// ── Profile Section Card ──────────────────────────────────────────────────────
private struct ProfileSectionCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.primary)
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

// ── Chip row helper ───────────────────────────────────────────────────────────
private struct ChipRow<T: Hashable>: View {
    let options: [(label: String, value: T)]
    @Binding var selection: T

    var body: some View {
        HStack(spacing: 8) {
            ForEach(options, id: \.value) { opt in
                Button(action: { selection = opt.value }) {
                    Text(opt.label)
                        .font(.system(size: 13, weight: selection == opt.value ? .semibold : .regular))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(selection == opt.value ? profileGreen : Color(.systemGray6))
                        .foregroundColor(selection == opt.value ? .white : .primary)
                        .cornerRadius(20)
                }
            }
            Spacer()
        }
    }
}

// ── Estimate mini-card ────────────────────────────────────────────────────────
private struct EstimateTile: View {
    let label: String; let value: String; let sub: String
    var body: some View {
        VStack(spacing: 3) {
            Text(label).font(.caption2).foregroundColor(profileGreen.opacity(0.8))
            Text(value).font(.system(size: 14, weight: .bold)).foregroundColor(profileGreen)
            Text(sub).font(.caption2).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(profileGreen.opacity(0.08))
        .cornerRadius(10)
    }
}

// ── Profile Field ─────────────────────────────────────────────────────────────
private struct ProfileField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default
    var unit: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            HStack {
                TextField(placeholder, text: $text)
                    .keyboardType(keyboardType)
                if let u = unit {
                    Text(u).font(.caption).foregroundColor(.secondary)
                }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────
struct ProfileScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var vm = ProfileViewModel()

    @State private var displayName       = ""
    @State private var weightStr         = ""
    @State private var heightStr         = ""
    @State private var ageStr            = ""
    @State private var selectedGender    = "OTHER"
    @State private var selectedActivity  = "SEDENTARY"
    @State private var selectedGoal      = "MAINTAIN"
    @State private var targetCaloriesStr = ""

    @State private var showClearDataAlert   = false
    @State private var showDeleteAlert      = false
    @State private var showSaveSuccessAlert = false

    private var userId: Int64 { Int64(appState.currentUserId ?? 0) }
    private var weight: Double? { Double(weightStr) }
    private var height: Double? { Double(heightStr) }
    private var age: Int?       { Int(ageStr) }

    private var bmr: Int? {
        guard let w = weight, let h = height, let a = age else { return nil }
        let base = 10 * w + 6.25 * h - 5.0 * Double(a)
        switch selectedGender {
        case "MALE":   return Int((base + 5).rounded())
        case "FEMALE": return Int((base - 161).rounded())
        default:       return Int((base - 78).rounded())
        }
    }
    private var tdee: Int? {
        guard let b = bmr else { return nil }
        let m: Double
        switch selectedActivity {
        case "LIGHT":        m = 1.375
        case "MODERATE":     m = 1.55
        case "VERY_ACTIVE":  m = 1.725
        case "EXTRA_ACTIVE": m = 1.9
        default:             m = 1.2
        }
        return Int((Double(b) * m).rounded())
    }
    private var bodyFatPct: Double? {
        guard let w = weight, let h = height, let a = age,
              selectedGender == "MALE" || selectedGender == "FEMALE" else { return nil }
        let hM = h / 100.0
        let bmi = w / (hM * hM)
        let offset = selectedGender == "MALE" ? 16.2 : 5.4
        let pct = 1.20 * bmi + 0.23 * Double(a) - offset
        return pct < 0 ? nil : (pct * 10).rounded() / 10.0
    }

    var body: some View {
        ZStack {
            profileBg.ignoresSafeArea()
            if vm.isLoading {
                ProgressView().tint(profileGreen)
            } else {
                ScrollView {
                    VStack(spacing: 12) {
                        avatarHeader
                        personalInfoSection
                        bodyMetricsSection
                        lifestyleSection
                        if bmr != nil { estimatesSection }
                        nutritionGoalSection
                        saveButton
                        logoutButton
                        dangerZone
                        Spacer().frame(height: 32)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                }
            }
        }
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(profileGreen, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .onAppear { vm.load(userId: userId) }
        .onChange(of: vm.isLoading) { loading in if !loading { populateFields(from: vm.user) } }
        .onChange(of: vm.saveSuccess) { if $0 { showSaveSuccessAlert = true; vm.saveSuccess = false } }
        .onChange(of: vm.clearSuccess) { if $0 { vm.clearSuccess = false } }
        .alert("Save Failed", isPresented: Binding(get: { vm.error != nil }, set: { if !$0 { vm.error = nil } })) {
            Button("OK", role: .cancel) { vm.error = nil }
        } message: { Text(vm.error ?? "") }
        .alert("Saved!", isPresented: $showSaveSuccessAlert) {
            Button("OK", role: .cancel) {}
        } message: { Text("Profile saved successfully.") }
        .alert("Clear All Data?", isPresented: $showClearDataAlert) {
            Button("Clear All", role: .destructive) {
                Task { await vm.clearAllData(userId: userId) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Deletes all diets, grocery lists, and health readings. Your account stays active.")
        }
        .alert("Delete Account?", isPresented: $showDeleteAlert) {
            Button("Delete", role: .destructive) { appState.logout() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Permanently removes your account and logs you out. Local data stays on device.")
        }
    }

    // MARK: - Sections

    private var avatarHeader: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(profileGreen.opacity(0.15))
                    .frame(width: 80, height: 80)
                Image(systemName: "person.fill")
                    .font(.system(size: 36))
                    .foregroundColor(profileGreen)
            }
            if let email = vm.user?.email {
                Text(email)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }

    private var personalInfoSection: some View {
        ProfileSectionCard(title: "Personal Info") {
            ProfileField(label: "Name", placeholder: "Display name", text: $displayName)
            ProfileField(label: "Age", placeholder: "e.g. 30", text: $ageStr, keyboardType: .numberPad, unit: "yrs")
            VStack(alignment: .leading, spacing: 6) {
                Text("Gender").font(.caption).foregroundColor(.secondary)
                ChipRow(options: [
                    ("Male", "MALE"), ("Female", "FEMALE"), ("Other", "OTHER")
                ], selection: $selectedGender)
            }
        }
    }

    private var bodyMetricsSection: some View {
        ProfileSectionCard(title: "Body Metrics") {
            HStack(spacing: 10) {
                ProfileField(label: "Weight", placeholder: "0.0", text: $weightStr, keyboardType: .decimalPad, unit: "kg")
                ProfileField(label: "Height", placeholder: "0", text: $heightStr, keyboardType: .decimalPad, unit: "cm")
            }
        }
    }

    private var lifestyleSection: some View {
        ProfileSectionCard(title: "Lifestyle") {
            VStack(alignment: .leading, spacing: 6) {
                Text("Activity Level").font(.caption).foregroundColor(.secondary)
                Picker("Activity Level", selection: $selectedActivity) {
                    Text("Sedentary").tag("SEDENTARY")
                    Text("Lightly active").tag("LIGHT")
                    Text("Moderately active").tag("MODERATE")
                    Text("Very active").tag("VERY_ACTIVE")
                    Text("Athlete").tag("EXTRA_ACTIVE")
                }
                .pickerStyle(.menu)
                .tint(profileGreen)
                .padding(.vertical, 2)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text("Goal").font(.caption).foregroundColor(.secondary)
                ChipRow(options: [
                    ("Lose", "LOSE"), ("Maintain", "MAINTAIN"), ("Gain", "GAIN")
                ], selection: $selectedGoal)
            }
        }
    }

    private var estimatesSection: some View {
        ProfileSectionCard(title: "Health Estimates") {
            HStack(spacing: 8) {
                EstimateTile(label: "BMR",
                             value: bmr.map { "\($0) kcal" } ?? "—",
                             sub: "basal")
                EstimateTile(label: "TDEE",
                             value: tdee.map { "\($0) kcal" } ?? "—",
                             sub: "maintenance")
                EstimateTile(label: "Body Fat",
                             value: bodyFatPct.map { String(format: "%.1f%%", $0) } ?? "—",
                             sub: "estimate")
            }
            Text("* Estimates only. BMR via Mifflin-St Jeor; body fat via Deurenberg.")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }

    private var nutritionGoalSection: some View {
        ProfileSectionCard(title: "Nutrition Goal") {
            ProfileField(label: "Daily Target Calories", placeholder: "e.g. 2000",
                         text: $targetCaloriesStr, keyboardType: .numberPad, unit: "kcal")
            if let t = tdee {
                Text("Blank = use TDEE (~\(t) kcal)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var saveButton: some View {
        Button(action: saveProfile) {
            HStack {
                if vm.isSaving {
                    ProgressView().tint(.white)
                } else {
                    Image(systemName: "checkmark")
                    Text("Save Profile").fontWeight(.semibold)
                }
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(vm.isSaving || vm.user == nil ? profileGreen.opacity(0.5) : profileGreen)
            .cornerRadius(12)
        }
        .disabled(vm.isSaving || vm.user == nil)
    }

    private var logoutButton: some View {
        Button(action: { appState.logout() }) {
            HStack {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                Text("Logout").fontWeight(.semibold)
            }
            .foregroundColor(.red)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.red, lineWidth: 1.5))
        }
    }

    private var dangerZone: some View {
        VStack(alignment: .leading, spacing: 10) {
            Divider()
            Text("Danger Zone")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.red)

            Button(action: { showClearDataAlert = true }) {
                HStack {
                    if vm.isClearingData {
                        ProgressView().tint(.red)
                    } else {
                        Image(systemName: "trash.slash").font(.system(size: 14))
                        Text("Clear All Data").fontWeight(.medium)
                    }
                }
                .foregroundColor(.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.red, lineWidth: 1.5))
            }
            .disabled(vm.isClearingData)

            Button(action: { showDeleteAlert = true }) {
                HStack {
                    Image(systemName: "person.fill.xmark").font(.system(size: 14))
                    Text("Delete Account").fontWeight(.medium)
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.red)
                .cornerRadius(12)
            }
            .disabled(vm.isClearingData)
        }
    }

    // MARK: - Helpers

    private static let validGenders    = Set(["MALE", "FEMALE", "OTHER"])
    private static let validActivities = Set(["SEDENTARY", "LIGHT", "MODERATE", "VERY_ACTIVE", "EXTRA_ACTIVE"])
    private static let validGoals      = Set(["LOSE", "MAINTAIN", "GAIN"])

    private func populateFields(from user: User?) {
        guard let user = user else { return }
        displayName       = user.displayName ?? ""
        weightStr         = user.weightKg.map { String(format: "%.1f", $0.doubleValue) } ?? ""
        heightStr         = user.heightCm.map { String(format: "%.0f", $0.doubleValue) } ?? ""
        ageStr            = user.age.map { String($0.intValue) } ?? ""
        let g = user.gender ?? ""
        selectedGender    = Self.validGenders.contains(g)    ? g : "OTHER"
        let a = user.activityLevel ?? ""
        selectedActivity  = Self.validActivities.contains(a) ? a : "SEDENTARY"
        let gl = user.goalType ?? ""
        selectedGoal      = Self.validGoals.contains(gl)     ? gl : "MAINTAIN"
        targetCaloriesStr = user.targetCalories.map { String($0.intValue) } ?? ""
    }

    private func saveProfile() {
        guard let user = vm.user else { print("saveProfile: vm.user is nil!"); return }
        print("saveProfile: userId=\(user.id) weightStr='\(weightStr)' heightStr='\(heightStr)' targetCalories='\(targetCaloriesStr)'")
        let updated = User(
            id: user.id,
            email: user.email,
            passwordHash: user.passwordHash,
            displayName: displayName.isEmpty ? nil : displayName,
            photoUrl: user.photoUrl,
            age: ageStr.isEmpty ? nil : KotlinInt(value: Int32(Int(ageStr) ?? 0)),
            contact: user.contact,
            weightKg: weightStr.isEmpty ? nil : KotlinDouble(value: Double(weightStr) ?? 0),
            heightCm: heightStr.isEmpty ? nil : KotlinDouble(value: Double(heightStr) ?? 0),
            gender: selectedGender,
            activityLevel: selectedActivity,
            targetCalories: targetCaloriesStr.isEmpty ? nil : KotlinInt(value: Int32(Int(targetCaloriesStr) ?? 0)),
            goalType: selectedGoal,
            createdAt: user.createdAt,
            updatedAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        Task { await vm.save(updated) }
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
