import SwiftUI

struct AddDietScreen: View {
    let onAdd: (DietUI) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var description = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Diet Info") {
                    TextField("Name", text: $name)
                    TextField("Description", text: $description)
                }
            }
            .navigationTitle("Add Diet")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        guard !name.isEmpty else { return }
                        let newDiet = DietUI(
                            id: Int64(Date().timeIntervalSince1970),
                            name: name,
                            description: description,
                            calories: 0,
                            protein: 0,
                            carbs: 0,
                            fat: 0,
                            mealCount: 0,
                            tags: []
                        )
                        onAdd(newDiet)
                        dismiss()
                    }
                    .disabled(name.isEmpty)
                }
            }
        }
    }
}
