package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class CalorieWarningLevel {
    SAFE,
    APPROACHING, // 80% - 89%
    NEAR_LIMIT,  // 90% - 99%
    EXCEEDED     // >= 100%
}

@Serializable
data class DailyNutritionSummary(
    val date: String,
    val calorieBudget: Int,
    val caloriesConsumed: Int,
    val caloriesBurned: Int = 0,
    val waterIntakeMl: Int = 0,
    val waterTargetMl: Int = 3000,
    val proteinConsumedG: Float = 0f,
    val proteinTargetG: Float = 140f,
    val carbsConsumedG: Float = 0f,
    val carbsTargetG: Float = 180f,
    val fatConsumedG: Float = 0f,
    val fatTargetG: Float = 60f,
    val micronutrients: Micronutrients = Micronutrients.ZERO
) {
    val netCalories: Int get() = caloriesConsumed - caloriesBurned
    val remainingCalories: Int get() = calorieBudget - netCalories
    val percentageConsumed: Float get() = if (calorieBudget > 0) (netCalories.toFloat() / calorieBudget.toFloat()) * 100f else 0f

    val warningLevel: CalorieWarningLevel
        get() = when {
            netCalories >= calorieBudget -> CalorieWarningLevel.EXCEEDED
            percentageConsumed >= 90f -> CalorieWarningLevel.NEAR_LIMIT
            percentageConsumed >= 80f -> CalorieWarningLevel.APPROACHING
            else -> CalorieWarningLevel.SAFE
        }

    val warningMessage: String?
        get() = when (warningLevel) {
            CalorieWarningLevel.EXCEEDED -> 
                "Daily calorie limit exceeded by ${netCalories - calorieBudget} kcal! Stop or choose low-calorie snacks."
            CalorieWarningLevel.NEAR_LIMIT -> 
                "Warning: 90%+ of daily limit consumed! Only $remainingCalories kcal remaining today."
            CalorieWarningLevel.APPROACHING -> 
                "Heads up: You've consumed 80%+ of your daily allowance. $remainingCalories kcal left."
            CalorieWarningLevel.SAFE -> null
        }
}
