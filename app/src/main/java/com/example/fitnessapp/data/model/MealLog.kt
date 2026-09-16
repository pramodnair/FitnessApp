package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snacks")
}

@Serializable
data class MealLog(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "primary",
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val mealType: MealType = MealType.LUNCH,
    val title: String = "Meal",
    val calories: Int = 0,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val micronutrients: Micronutrients = Micronutrients.ZERO,
    val photoPath: String? = null,
    val aiInsights: String? = null,
    val items: List<FoodItem> = emptyList(),
    val aiSource: String = "LIVE_AI", // "LIVE_AI" or "OFFLINE_MOCK"
    val aiErrorMessage: String? = null
)
