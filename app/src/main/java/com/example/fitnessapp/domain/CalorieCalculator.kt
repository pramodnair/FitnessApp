package com.example.fitnessapp.domain

import com.example.fitnessapp.data.model.ActivityLevel
import com.example.fitnessapp.data.model.DeficitLevel
import com.example.fitnessapp.data.model.Gender
import kotlin.math.max
import kotlin.math.roundToInt

object CalorieCalculator {

    /**
     * Calculates Basal Metabolic Rate (BMR) using the Mifflin-St Jeor formula:
     * Men:   (10 * weight in kg) + (6.25 * height in cm) - (5 * age) + 5
     * Women: (10 * weight in kg) + (6.25 * height in cm) - (5 * age) - 161
     */
    fun calculateBmr(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int
    ): Float {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) return 1500f
        val base = (10f * weightKg) + (6.25f * heightCm) - (5f * age)
        return when (gender) {
            Gender.MALE -> base + 5f
            Gender.FEMALE -> base - 161f
        }
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE) based on activity level multiplier.
     */
    fun calculateTdee(bmr: Float, activityLevel: ActivityLevel): Float {
        return bmr * activityLevel.multiplier
    }

    /**
     * Minimum recommended daily calories for safe weight loss.
     * Prevents metabolic adaptation, nutritional deficiencies, and muscle wasting.
     */
    fun getMinimumSafeCalories(gender: Gender): Int {
        return when (gender) {
            Gender.MALE -> 1500
            Gender.FEMALE -> 1200
        }
    }

    /**
     * Calculates recommended daily calorie budget for weight loss based on customizable deficit.
     */
    fun calculateDailyBudget(
        tdee: Float,
        deficitLevel: DeficitLevel,
        customDeficitKcal: Int,
        gender: Gender
    ): Int {
        val deficit = when (deficitLevel) {
            DeficitLevel.MILD -> DeficitLevel.MILD.deficitKcal
            DeficitLevel.MODERATE -> DeficitLevel.MODERATE.deficitKcal
            DeficitLevel.AGGRESSIVE -> DeficitLevel.AGGRESSIVE.deficitKcal
            DeficitLevel.CUSTOM -> max(0, customDeficitKcal)
        }

        val calculated = (tdee - deficit).roundToInt()
        val safeMinimum = getMinimumSafeCalories(gender)
        return max(safeMinimum, calculated)
    }

    /**
     * 1 kg of fat roughly equals 7,700 kcal.
     * Estimates days required to lose target weight at the current daily deficit.
     */
    fun estimateDaysToGoal(weightToLoseKg: Float, dailyDeficitKcal: Int): Int {
        if (weightToLoseKg <= 0f || dailyDeficitKcal <= 0) return 0
        val totalCaloriesToBurn = weightToLoseKg * 7700f
        return (totalCaloriesToBurn / dailyDeficitKcal.toFloat()).roundToInt()
    }
}
