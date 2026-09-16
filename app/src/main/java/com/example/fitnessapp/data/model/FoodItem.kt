package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodItem(
    val name: String,
    val portionDescription: String = "",
    val calories: Int = 0,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val micronutrients: Micronutrients = Micronutrients.ZERO
)
