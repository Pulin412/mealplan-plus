import SwiftUI
import shared

struct DietDetailScreen: View {
    let diet: DietUI
    @State private var selectedDay = 0

    let days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header card
                VStack(spacing: 12) {
                    Text(diet.name)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(diet.description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)

                    // Tags
                    HStack(spacing: 8) {
                        ForEach(diet.tags, id: \.self) { tag in
                            Text(tag)
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.green.opacity(0.2))
                                .foregroundColor(.green)
                                .cornerRadius(12)
                        }
                    }
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

                // Daily nutrition
                VStack(spacing: 16) {
                    Text("Daily Nutrition")
                        .font(.headline)

                    HStack(spacing: 16) {
                        NutritionBox(value: diet.calories, label: "Calories", unit: "kcal", color: .green)
                        NutritionBox(value: diet.protein, label: "Protein", unit: "g", color: .red)
                        NutritionBox(value: diet.carbs, label: "Carbs", unit: "g", color: .blue)
                        NutritionBox(value: diet.fat, label: "Fat", unit: "g", color: .yellow)
                    }
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

                // Day selector
                VStack(spacing: 12) {
                    Text("Meal Plan")
                        .font(.headline)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(0..<7) { index in
                                Button(action: { selectedDay = index }) {
                                    Text(days[index])
                                        .font(.subheadline)
                                        .fontWeight(selectedDay == index ? .bold : .regular)
                                        .frame(width: 44, height: 44)
                                        .background(selectedDay == index ? Color.green : Color.clear)
                                        .foregroundColor(selectedDay == index ? .white : .primary)
                                        .cornerRadius(22)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 22)
                                                .stroke(Color.green, lineWidth: selectedDay == index ? 0 : 1)
                                        )
                                }
                            }
                        }
                    }
                }
                .padding()
                .background(Color.white)
                .cornerRadius(12)
                .shadow(color: .black.opacity(0.1), radius: 5)
                .padding(.horizontal)

                // Meal slots
                VStack(alignment: .leading, spacing: 12) {
                    MealSlotCard(slot: "Breakfast", mealName: "Oatmeal with Berries", calories: 350)
                    MealSlotCard(slot: "Lunch", mealName: "Chicken & Rice Bowl", calories: 550)
                    MealSlotCard(slot: "Dinner", mealName: "Grilled Salmon Dinner", calories: 620)
                    MealSlotCard(slot: "Snacks", mealName: "Protein Smoothie", calories: 280)
                }
                .padding(.horizontal)

                // Actions
                HStack(spacing: 12) {
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
                .padding(.horizontal)

                Spacer().frame(height: 20)
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
        .navigationTitle("Diet Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: {}) {
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

struct MealSlotCard: View {
    let slot: String
    let mealName: String
    let calories: Double

    var body: some View {
        HStack {
            Image(systemName: slotIcon)
                .foregroundColor(.green)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 2) {
                Text(slot)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(mealName)
                    .font(.subheadline)
                    .fontWeight(.medium)
            }

            Spacer()

            Text("\(Int(calories)) kcal")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color.white)
        .cornerRadius(10)
        .shadow(color: .black.opacity(0.05), radius: 2)
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

struct AddDietScreen: View {
    @Environment(\.dismiss) var dismiss
    var onSave: (DietUI) -> Void

    @State private var name = ""
    @State private var description = ""
    @State private var tagInput = ""
    @State private var tags: [String] = []

    var isFormValid: Bool {
        !name.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Diet Info") {
                    TextField("Diet Name", text: $name)
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(3...6)
                }

                Section("Tags") {
                    HStack {
                        TextField("Add tag", text: $tagInput)
                        Button(action: addTag) {
                            Image(systemName: "plus.circle.fill")
                                .foregroundColor(.green)
                        }
                        .disabled(tagInput.isEmpty)
                    }

                    if !tags.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(tags, id: \.self) { tag in
                                    HStack(spacing: 4) {
                                        Text(tag)
                                            .font(.caption)
                                        Button(action: { tags.removeAll { $0 == tag } }) {
                                            Image(systemName: "xmark.circle.fill")
                                                .font(.caption)
                                        }
                                    }
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.green.opacity(0.2))
                                    .foregroundColor(.green)
                                    .cornerRadius(12)
                                }
                            }
                        }
                    }
                }

                Section {
                    Text("After creating, add meals to each slot")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Section {
                    Button(action: saveDiet) {
                        HStack {
                            Spacer()
                            Text("Create Diet")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                    }
                    .disabled(!isFormValid)
                }
            }
            .navigationTitle("New Diet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }

    private func addTag() {
        let tag = tagInput.trimmingCharacters(in: .whitespaces)
        if !tag.isEmpty && !tags.contains(tag) {
            tags.append(tag)
            tagInput = ""
        }
    }

    private func saveDiet() {
        let diet = DietUI(
            id: Int64(Date().timeIntervalSince1970),
            name: name,
            description: description,
            calories: 0,
            protein: 0,
            carbs: 0,
            fat: 0,
            mealCount: 0,
            tags: tags
        )
        onSave(diet)
        dismiss()
    }
}

struct DietDetailScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            DietDetailScreen(diet: DietUI(
                id: 1,
                name: "High Protein",
                description: "Focus on protein-rich meals",
                calories: 2200,
                protein: 180,
                carbs: 200,
                fat: 70,
                mealCount: 4,
                tags: ["Muscle", "High Protein"]
            ))
        }
    }
}
