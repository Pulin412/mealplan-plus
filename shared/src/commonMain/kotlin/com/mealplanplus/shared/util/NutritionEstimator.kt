package com.mealplanplus.shared.util

import com.mealplanplus.shared.model.ActivityLevel
import com.mealplanplus.shared.model.Gender
import kotlin.math.roundToInt

/**
 * BMR / TDEE / body-fat estimations (shared across Android + iOS).
 *
 * BMR formula  : Mifflin-St Jeor
 * Body fat     : Deurenberg BMI-based
 */
object NutritionEstimator {

    /**
     * Basal Metabolic Rate via Mifflin-St Jeor.
     * Returns null if any required param is missing.
     */
    fun computeBmr(
        weightKg: Double?,
        heightCm: Double?,
        ageYears: Int?,
        gender: Gender?
    ): Int? {
        if (weightKg == null || heightCm == null || ageYears == null || gender == null) return null
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (gender) {
            Gender.MALE   -> (base + 5).roundToInt()
            Gender.FEMALE -> (base - 161).roundToInt()
            Gender.OTHER  -> (base - 78).roundToInt()  // avg of male/female offsets
        }
    }

    /**
     * Total Daily Energy Expenditure = BMR × activity multiplier.
     */
    fun computeTdee(bmr: Int?, activityLevel: ActivityLevel?): Int? {
        if (bmr == null || activityLevel == null) return null
        return (bmr * activityLevel.multiplier).roundToInt()
    }

    /**
     * Deurenberg BMI-based body fat percentage.
     * Returns null for Gender.OTHER (formula is sex-specific).
     */
    fun computeBodyFatPercent(
        weightKg: Double?,
        heightCm: Double?,
        ageYears: Int?,
        gender: Gender?
    ): Double? {
        if (weightKg == null || heightCm == null || ageYears == null ||
            gender == null || gender == Gender.OTHER
        ) return null
        val hM = heightCm / 100.0
        val bmi = weightKg / (hM * hM)
        val offset = if (gender == Gender.MALE) 16.2 else 5.4
        val pct = 1.20 * bmi + 0.23 * ageYears - offset
        return if (pct < 0) null else (pct * 10).roundToInt() / 10.0
    }

    /**
     * Convenience: compute all three estimates from a user's profile.
     * Returns a [NutritionEstimates] data object.
     */
    fun estimate(
        weightKg: Double?,
        heightCm: Double?,
        ageYears: Int?,
        gender: Gender?,
        activityLevel: ActivityLevel?
    ): NutritionEstimates {
        val bmr = computeBmr(weightKg, heightCm, ageYears, gender)
        return NutritionEstimates(
            bmr = bmr,
            tdee = computeTdee(bmr, activityLevel),
            bodyFatPercent = computeBodyFatPercent(weightKg, heightCm, ageYears, gender)
        )
    }
}

data class NutritionEstimates(
    val bmr: Int?,
    val tdee: Int?,
    val bodyFatPercent: Double?
)
