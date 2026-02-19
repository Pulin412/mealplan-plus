import SwiftUI
import shared

struct ContentView: View {
    @State private var greetingText = "Loading..."

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Image(systemName: "fork.knife.circle.fill")
                    .resizable()
                    .frame(width: 100, height: 100)
                    .foregroundColor(.green)

                Text("MealPlan+")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text(greetingText)
                    .font(.headline)
                    .foregroundColor(.secondary)

                Divider()
                    .padding(.horizontal, 40)

                VStack(alignment: .leading, spacing: 10) {
                    FeatureRow(icon: "leaf.fill", text: "Track your meals")
                    FeatureRow(icon: "chart.bar.fill", text: "Monitor nutrition")
                    FeatureRow(icon: "calendar", text: "Plan your diet")
                    FeatureRow(icon: "cart.fill", text: "Grocery lists")
                }
                .padding()

                Spacer()

                Text("Shared code from Kotlin!")
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            .padding()
            .navigationBarHidden(true)
        }
        .onAppear {
            // Test shared Kotlin code
            let greeting = Greeting().greet()
            greetingText = greeting
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(.green)
                .frame(width: 30)
            Text(text)
                .font(.body)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
