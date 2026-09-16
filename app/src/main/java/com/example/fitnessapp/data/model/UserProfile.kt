package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Gender {
    MALE,
    FEMALE
}

@Serializable
enum class ActivityLevel(val multiplier: Float, val label: String) {
    SEDENTARY(1.2f, "Sedentary (Little or no exercise)"),
    LIGHT(1.375f, "Light (1-3 days/week)"),
    MODERATE(1.55f, "Moderate (3-5 days/week)"),
    VERY_ACTIVE(1.725f, "Very Active (6-7 days/week)")
}

@Serializable
enum class DeficitLevel(val deficitKcal: Int, val label: String, val paceKgPerWeek: Float) {
    MILD(250, "Mild (-250 kcal/day ~ 0.25 kg/week)", 0.25f),
    MODERATE(500, "Moderate (-500 kcal/day ~ 0.5 kg/week)", 0.5f),
    AGGRESSIVE(750, "Aggressive (-750 kcal/day ~ 0.75 kg/week)", 0.75f),
    CUSTOM(0, "Custom Deficit", 0.0f)
}

@Serializable
data class UserProfile(
    val id: String = "primary",
    val name: String = "Pramod",
    val gender: Gender = Gender.MALE,
    val age: Int = 32,
    val heightCm: Float = 175f,
    val startWeightKg: Float = 85f,
    val currentWeightKg: Float = 85f,
    val targetWeightKg: Float = 72f,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val deficitLevel: DeficitLevel = DeficitLevel.MODERATE,
    val customDeficitKcal: Int = 500,
    val dailyWaterTargetMl: Int = 3000,
    val targetBmi: Float = 22.0f,
    val autoCalculateTargetFromBmi: Boolean = true,
    val geminiApiKey: String = "",
    val pinLock: String = ""
)
