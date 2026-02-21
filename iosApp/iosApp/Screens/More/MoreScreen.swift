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

struct GroceryDetailScreen: View {
    let listId: Int64
    let listName: String
    var onUpdate: () -> Void

    @StateObject private var viewModel = GroceryViewModel()
    @State private var showAddItem = false
    @State private var newItemName = ""

    var checkedCount: Int {
        viewModel.currentList?.items.filter { $0.item.isChecked }.count ?? 0
    }

    var totalCount: Int {
        viewModel.currentList?.items.count ?? 0
    }

    var items: [GroceryItemWithFood] {
        viewModel.currentList?.items ?? []
    }

    var body: some View {
        VStack(spacing: 0) {
            // Progress header
            if totalCount > 0 {
                HStack {
                    Text("\(checkedCount) of \(totalCount) items")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                    CircularProgress(progress: Double(checkedCount) / Double(max(totalCount, 1)))
                }
                .padding()
                .background(Color.white)
            }

            if viewModel.currentList == nil {
                Spacer()
                ProgressView("Loading items...")
                Spacer()
            } else if viewModel.currentList?.items.isEmpty == true {
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
            } else {
                List {
                    ForEach(items, id: \.item.id) { itemWithFood in
                        GroceryItemRow(itemWithFood: itemWithFood) { isChecked in
                            toggleItem(itemWithFood: itemWithFood, isChecked: isChecked)
                        }
                    }
                    .onDelete { indexSet in
                        deleteItems(at: indexSet)
                    }
                }
                .listStyle(PlainListStyle())
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
        .onAppear {
            loadItems()
        }
    }

    private func loadItems() {
        Task {
            try? await viewModel.loadGroceryListWithItems(listId: listId)
        }
    }

    private func toggleItem(itemWithFood: GroceryItemWithFood, isChecked: Bool) {
        Task {
            try? await viewModel.updateItemChecked(id: itemWithFood.item.id, isChecked: isChecked)
            loadItems()
        }
    }

    private func deleteItems(at indexSet: IndexSet) {
        Task {
            for index in indexSet {
                try? await viewModel.deleteGroceryItem(id: items[index].item.id)
            }
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
            sortOrder: 0
        )
        Task {
            try? await viewModel.insertGroceryItem(item)
            await MainActor.run {
                newItemName = ""
            }
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
