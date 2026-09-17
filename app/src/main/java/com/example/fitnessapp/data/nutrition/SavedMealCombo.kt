package com.example.fitnessapp.data.nutrition

import kotlinx.serialization.Serializable

@Serializable
data class SavedMealCombo(
    val id: String,
    val name: String,
    val items: List<SavedComboItem>,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class SavedComboItem(
    val foodId: String,
    val foodName: String,
    val quantity: Float,
    val servingUnit: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val category: String = "Other"
) {
    fun toFoodItemDefinition(): FoodItemDefinition {
        val baseFactor = if (quantity > 0f) quantity else 1.0f
        return FoodItemDefinition(
            id = foodId,
            name = foodName,
            category = category,
            servingUnit = servingUnit,
            baseQuantity = 1.0f,
            calories = (calories / baseFactor).toInt(),
            proteinG = proteinG / baseFactor,
            carbsG = carbsG / baseFactor,
            fatG = fatG / baseFactor,
            fiberG = fiberG / baseFactor,
            sugarG = sugarG / baseFactor,
            sodiumMg = sodiumMg / baseFactor,
            potassiumMg = potassiumMg / baseFactor,
            isCustomOrAi = true
        )
    }
}
