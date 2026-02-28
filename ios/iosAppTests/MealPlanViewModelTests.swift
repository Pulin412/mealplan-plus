import XCTest

/// Pure Swift logic extracted from MealPlanScreen / PlansViewModel.
/// Tests run on-device or in simulator without KMP dependencies.

// MARK: - Helpers under test (mirroring MealPlanScreen logic)

struct MealPlanLogic {

    // MARK: - Date panel state

    enum PanelState {
        case todayWithDiet
        case todayNoDiet
        case futureWithDiet
        case futureNoDiet
        case pastWithDiet
        case pastNoDiet
    }

    static func panelState(isToday: Bool, isPast: Bool, hasDiet: Bool) -> PanelState {
        if isToday {
            return hasDiet ? .todayWithDiet : .todayNoDiet
        } else if isPast {
            return hasDiet ? .pastWithDiet : .pastNoDiet
        } else {
            return hasDiet ? .futureWithDiet : .futureNoDiet
        }
    }

    // MARK: - Short diet name (mirrors Android extractShortDietName)

    static func extractShortDietName(_ name: String) -> String {
        let words = name.split(separator: " ")
        if words.count == 1 {
            return String(name.prefix(6))
        }
        let first = String(words[0].prefix(4))
        let second = String(words[1].prefix(1))
        return "\(first)\(second)"
    }

    // MARK: - Week view toggle

    static func toggleView(isWeekView: Bool) -> Bool {
        return !isWeekView
    }

    // MARK: - Plans map helpers

    static func plansContain(plans: [(date: String, dietName: String?)], date: String) -> Bool {
        return plans.contains { $0.date == date }
    }

    static func removePlan(from plans: [(date: String, dietName: String?)], date: String) -> [(date: String, dietName: String?)] {
        return plans.filter { $0.date != date }
    }

    static func addOrUpdatePlan(
        plans: [(date: String, dietName: String?)],
        date: String,
        dietName: String
    ) -> [(date: String, dietName: String?)] {
        var updated = plans.filter { $0.date != date }
        updated.append((date: date, dietName: dietName))
        return updated
    }
}

// MARK: - Test: Panel State

final class MealPlanPanelStateTests: XCTestCase {

    func test_todayWithDiet_returnsTodayWithDiet() {
        let state = MealPlanLogic.panelState(isToday: true, isPast: false, hasDiet: true)
        XCTAssertEqual(state, .todayWithDiet)
    }

    func test_todayNoDiet_returnsTodayNoDiet() {
        let state = MealPlanLogic.panelState(isToday: true, isPast: false, hasDiet: false)
        XCTAssertEqual(state, .todayNoDiet)
    }

    func test_futureWithDiet_returnsFutureWithDiet() {
        let state = MealPlanLogic.panelState(isToday: false, isPast: false, hasDiet: true)
        XCTAssertEqual(state, .futureWithDiet)
    }

    func test_futureNoDiet_returnsFutureNoDiet() {
        let state = MealPlanLogic.panelState(isToday: false, isPast: false, hasDiet: false)
        XCTAssertEqual(state, .futureNoDiet)
    }

    func test_pastWithDiet_returnsPastWithDiet() {
        let state = MealPlanLogic.panelState(isToday: false, isPast: true, hasDiet: true)
        XCTAssertEqual(state, .pastWithDiet)
    }

    func test_pastNoDiet_returnsPastNoDiet() {
        let state = MealPlanLogic.panelState(isToday: false, isPast: true, hasDiet: false)
        XCTAssertEqual(state, .pastNoDiet)
    }
}

// MARK: - Test: Short Diet Name

final class ShortDietNameTests: XCTestCase {

    func test_singleWord_truncatesTo6() {
        XCTAssertEqual(MealPlanLogic.extractShortDietName("Mediterranean"), "Medite")
    }

    func test_twoWords_firstFourPlusInitial() {
        XCTAssertEqual(MealPlanLogic.extractShortDietName("Low Carb"), "LowC")
    }

    func test_twoWords_diet17Style() {
        // words[0]="Diet" prefix(4)="Diet", words[1]="17" prefix(1)="1" → "Diet1"
        XCTAssertEqual(MealPlanLogic.extractShortDietName("Diet 17"), "Diet1")
    }

    func test_twoWords_ketogenic() {
        XCTAssertEqual(MealPlanLogic.extractShortDietName("Keto Plan"), "KetP")
    }

    func test_shortSingleWord_noTruncation() {
        XCTAssertEqual(MealPlanLogic.extractShortDietName("Keto"), "Keto")
    }
}

// MARK: - Test: Toggle View

final class ToggleViewTests: XCTestCase {

    func test_toggleFromFalse_returnsTrue() {
        XCTAssertTrue(MealPlanLogic.toggleView(isWeekView: false))
    }

    func test_toggleFromTrue_returnsFalse() {
        XCTAssertFalse(MealPlanLogic.toggleView(isWeekView: true))
    }

    func test_doubleToggle_returnsSameValue() {
        let initial = false
        let toggled = MealPlanLogic.toggleView(isWeekView: initial)
        let doubleToggled = MealPlanLogic.toggleView(isWeekView: toggled)
        XCTAssertEqual(initial, doubleToggled)
    }
}

// MARK: - Test: Plans Map (assign / remove)

final class PlansMapTests: XCTestCase {

    private let basePlans: [(date: String, dietName: String?)] = [
        (date: "2025-01-10", dietName: "Low Carb"),
        (date: "2025-01-11", dietName: "Keto")
    ]

    func test_assignDiet_addsToPlans() {
        let updated = MealPlanLogic.addOrUpdatePlan(
            plans: basePlans, date: "2025-01-12", dietName: "Paleo"
        )
        XCTAssertTrue(MealPlanLogic.plansContain(plans: updated, date: "2025-01-12"))
        XCTAssertEqual(updated.count, 3)
    }

    func test_assignDiet_overwritesExisting() {
        let updated = MealPlanLogic.addOrUpdatePlan(
            plans: basePlans, date: "2025-01-10", dietName: "New Diet"
        )
        let entry = updated.first { $0.date == "2025-01-10" }
        XCTAssertEqual(entry?.dietName, "New Diet")
        XCTAssertEqual(updated.count, 2)
    }

    func test_removePlan_removesFromPlans() {
        let updated = MealPlanLogic.removePlan(from: basePlans, date: "2025-01-10")
        XCTAssertFalse(MealPlanLogic.plansContain(plans: updated, date: "2025-01-10"))
        XCTAssertEqual(updated.count, 1)
    }

    func test_removePlan_nonExistentDate_unchanged() {
        let updated = MealPlanLogic.removePlan(from: basePlans, date: "2025-01-99")
        XCTAssertEqual(updated.count, basePlans.count)
    }

    func test_plansContain_existingDate_returnsTrue() {
        XCTAssertTrue(MealPlanLogic.plansContain(plans: basePlans, date: "2025-01-10"))
    }

    func test_plansContain_missingDate_returnsFalse() {
        XCTAssertFalse(MealPlanLogic.plansContain(plans: basePlans, date: "2025-01-15"))
    }
}
