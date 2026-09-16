package com.example.fitnessapp.domain

import com.example.fitnessapp.data.model.Micronutrients
import com.example.fitnessapp.data.model.UserProfile
import kotlin.math.roundToInt

data class RecommendedNutrition(
    val dailyCalorieBudget: Int,
    val bmr: Float,
    val tdee: Float,
    val currentBmi: Float,
    val targetBmi: Float,
    val weightToLoseKg: Float,
    val estimatedDaysToGoal: Int,
    // Macros (in grams)
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    // Daily Micronutrients & Water
    val micronutrients: Micronutrients,
    val dailyWaterMl: Int
)

object NutritionEngine {

    /**
     * Calculates comprehensive nutrition and energy recommendations based on profile.
     */
    fun calculateRecommendations(profile: UserProfile): RecommendedNutrition {
        val bmr = CalorieCalculator.calculateBmr(
            gender = profile.gender,
            weightKg = profile.currentWeightKg,
            heightCm = profile.heightCm,
            age = profile.age
        )

        val tdee = CalorieCalculator.calculateTdee(bmr, profile.activityLevel)

        val dailyBudget = CalorieCalculator.calculateDailyBudget(
            tdee = tdee,
            deficitLevel = profile.deficitLevel,
            customDeficitKcal = profile.customDeficitKcal,
            gender = profile.gender
        )

        val currentBmi = BmiCalculator.calculateBmi(profile.currentWeightKg, profile.heightCm)
        val targetBmi = BmiCalculator.calculateBmi(profile.targetWeightKg, profile.heightCm)
        val weightToLose = (profile.currentWeightKg - profile.targetWeightKg).coerceAtLeast(0f)

        val actualDeficit = (tdee - dailyBudget).roundToInt().coerceAtLeast(1)
        val daysToGoal = CalorieCalculator.estimateDaysToGoal(weightToLose, actualDeficit)

        // Macros Calculation for Weight Loss & Lean Muscle Preservation:
        // High protein during deficit: ~1.8g per kg of bodyweight (4 kcal/g)
        // Healthy fats: 25% of total calorie intake (9 kcal/g)
        // Carbohydrates: Remainder of daily budget (4 kcal/g)
        val proteinGrams = (profile.targetWeightKg * 1.8f).coerceAtLeast(90f)
        val proteinCalories = proteinGrams * 4f

        val fatCalories = dailyBudget * 0.25f
        val fatGrams = fatCalories / 9f

        val remainingCalories = (dailyBudget - (proteinCalories + fatCalories)).coerceAtLeast(200f)
        val carbsGrams = remainingCalories / 4f

        // Hydration recommendation: ~35ml per kg of body weight
        val waterMl = (profile.currentWeightKg * 35f).roundToInt().coerceIn(2200, 4500)

        // Micronutrients baseline for healthy body maintenance
        val micros = Micronutrients.DAILY_RECOMMENDED.copy(
            fiberG = if (profile.gender == com.example.fitnessapp.data.model.Gender.MALE) 34f else 28f
        )

        return RecommendedNutrition(
            dailyCalorieBudget = dailyBudget,
            bmr = bmr,
            tdee = tdee,
            currentBmi = currentBmi,
            targetBmi = targetBmi,
            weightToLoseKg = weightToLose,
            estimatedDaysToGoal = daysToGoal,
            proteinGrams = (proteinGrams * 10f).roundToInt() / 10f,
            carbsGrams = (carbsGrams * 10f).roundToInt() / 10f,
            fatGrams = (fatGrams * 10f).roundToInt() / 10f,
            micronutrients = micros,
            dailyWaterMl = waterMl
        )
    }
}
