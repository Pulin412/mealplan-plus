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
            updatedAt: timestamp
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

@MainActor
class ProfileViewModel: ObservableObject {
    @Published var user: User?
    @Published var isSaving = false

    private let repo = RepositoryProvider.shared.userRepository

    func load(userId: Int64) {
        Task { user = try? await repo.getUserById(id: userId) }
    }

    func save(_ user: User) async {
        isSaving = true
        try? await repo.updateUser(user: user)
        self.user = user
        isSaving = false
    }
}

struct ProfileScreen: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var vm = ProfileViewModel()

    @State private var displayName = ""
    @State private var weightStr = ""
    @State private var heightStr = ""
    @State private var ageStr = ""
    @State private var selectedGender = "OTHER"
    @State private var selectedActivity = "SEDENTARY"
    @State private var selectedGoal = "MAINTAIN"
    @State private var targetCaloriesStr = ""
    @State private var showDangerZone = false
    @State private var showDeleteConfirm = false

    var weight: Double? { Double(weightStr) }
    var height: Double? { Double(heightStr) }
    var age: Int? { Int(ageStr) }

    var bmr: Int? {
        guard let w = weight, let h = height, let a = age else { return nil }
        let base = 10 * w + 6.25 * h - 5.0 * Double(a)
        switch selectedGender {
        case "MALE":   return Int((base + 5).rounded())
        case "FEMALE": return Int((base - 161).rounded())
        default:       return Int((base - 78).rounded())
        }
    }

    var tdee: Int? {
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

    var bodyFatPct: Double? {
        guard let w = weight, let h = height, let a = age,
              selectedGender == "MALE" || selectedGender == "FEMALE" else { return nil }
        let hM = h / 100.0
        let bmi = w / (hM * hM)
        let offset = selectedGender == "MALE" ? 16.2 : 5.4
        let pct = 1.20 * bmi + 0.23 * Double(a) - offset
        return pct < 0 ? nil : (pct * 10).rounded() / 10.0
    }

    var body: some View {
        Form {
            // Avatar
            Section {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        Image(systemName: "person.circle.fill")
                            .font(.system(size: 70))
                            .foregroundColor(.green)
                        if let email = vm.user?.email {
                            Text(email)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }

            // Identity
            Section("Profile") {
                HStack {
                    Text("Name")
                    Spacer()
                    TextField("Display name", text: $displayName)
                        .multilineTextAlignment(.trailing)
                }
            }

            // Body metrics
            Section("Body Metrics") {
                HStack {
                    Text("Weight")
                    Spacer()
                    TextField("0.0", text: $weightStr)
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 70)
                    Text("kg").foregroundColor(.secondary)
                }
                HStack {
                    Text("Height")
                    Spacer()
                    TextField("0", text: $heightStr)
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 70)
                    Text("cm").foregroundColor(.secondary)
                }
                HStack {
                    Text("Age")
                    Spacer()
                    TextField("0", text: $ageStr)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 70)
                    Text("yrs").foregroundColor(.secondary)
                }
                Picker("Gender", selection: $selectedGender) {
                    Text("Male").tag("MALE")
                    Text("Female").tag("FEMALE")
                    Text("Other").tag("OTHER")
                }
            }

            // Goal & activity
            Section("Goal & Activity") {
                Picker("Goal", selection: $selectedGoal) {
                    Text("Lose weight").tag("LOSE")
                    Text("Maintain weight").tag("MAINTAIN")
                    Text("Gain muscle").tag("GAIN")
                }
                Picker("Activity Level", selection: $selectedActivity) {
                    Text("Sedentary").tag("SEDENTARY")
                    Text("Lightly active").tag("LIGHT")
                    Text("Moderately active").tag("MODERATE")
                    Text("Very active").tag("VERY_ACTIVE")
                    Text("Athlete").tag("EXTRA_ACTIVE")
                }
                HStack {
                    Text("Target Calories")
                    Spacer()
                    TextField("2000", text: $targetCaloriesStr)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 70)
                    Text("kcal").foregroundColor(.secondary)
                }
            }

            // Estimates (BMR / TDEE / BodyFat)
            if bmr != nil || tdee != nil || bodyFatPct != nil {
                Section("Estimates") {
                    if let b = bmr {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("BMR").font(.subheadline)
                                Text("Base metabolic rate").font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(b) kcal").foregroundColor(.secondary)
                        }
                    }
                    if let t = tdee {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("TDEE").font(.subheadline)
                                Text("Daily energy expenditure").font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("\(t) kcal").foregroundColor(.green).fontWeight(.semibold)
                        }
                    }
                    if let bf = bodyFatPct {
                        HStack {
                            Text("Est. Body Fat")
                            Spacer()
                            Text(String(format: "%.1f%%", bf)).foregroundColor(.secondary)
                        }
                    }
                }
            }

            // Save
            Section {
                Button(action: saveProfile) {
                    HStack {
                        Spacer()
                        if vm.isSaving {
                            ProgressView()
                        } else {
                            Text("Save Changes").fontWeight(.semibold)
                        }
                        Spacer()
                    }
                }
                .foregroundColor(.green)
                .disabled(vm.isSaving || vm.user == nil)
            }

            // Danger zone
            Section {
                DisclosureGroup(isExpanded: $showDangerZone) {
                    Button(role: .destructive) { appState.logout() } label: {
                        Label("Log Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                    Button(role: .destructive) { showDeleteConfirm = true } label: {
                        Label("Delete Account", systemImage: "trash")
                    }
                } label: {
                    Text("Danger Zone").foregroundColor(.red)
                }
            }
        }
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Delete Account?", isPresented: $showDeleteConfirm) {
            Button("Delete", role: .destructive) { appState.logout() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will log you out. All data is stored locally on this device.")
        }
        .onAppear {
            if let userId = appState.currentUserId { vm.load(userId: userId) }
        }
        .onChange(of: vm.user) { user in
            guard let user = user else { return }
            displayName          = user.displayName ?? ""
            weightStr            = user.weightKg.map { String(format: "%.1f", $0.doubleValue) } ?? ""
            heightStr            = user.heightCm.map { String(format: "%.0f", $0.doubleValue) } ?? ""
            ageStr               = user.age.map { String($0.intValue) } ?? ""
            selectedGender       = user.gender ?? "OTHER"
            selectedActivity     = user.activityLevel ?? "SEDENTARY"
            selectedGoal         = user.goalType ?? "MAINTAIN"
            targetCaloriesStr    = user.targetCalories.map { String($0.intValue) } ?? ""
        }
    }

    private func saveProfile() {
        guard let user = vm.user else { return }
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
