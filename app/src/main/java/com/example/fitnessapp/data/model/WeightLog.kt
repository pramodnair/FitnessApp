package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WeightLog(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "primary",
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Float,
    val bmi: Float,
    val notes: String = ""
)
