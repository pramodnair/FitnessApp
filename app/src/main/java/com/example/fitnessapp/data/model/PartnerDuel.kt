package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDailyScore(
    val userId: String,
    val userName: String,
    val calorieBudget: Int,
    val caloriesConsumed: Int,
    val waterIntakeMl: Int,
    val waterTargetMl: Int,
    val currentWeightKg: Float,
    val weightLostKg: Float,
    val streakDays: Int,
    val proteinConsumedG: Float = 0f,
    val proteinTargetG: Float = 0f,
    val carbsConsumedG: Float = 0f,
    val carbsTargetG: Float = 0f,
    val fatConsumedG: Float = 0f,
    val fatTargetG: Float = 0f,
    val targetWeightKg: Float = 0f,
    val startWeightKg: Float = 0f,
    val stepsTaken: Int = 0,
    val stepsTarget: Int = 10000,
    val caloriesBurned: Int = 0,
    val isLiveSynced: Boolean = false,
    val lastSyncTimestamp: Long = 0L
) {
    // Adherence score: 100 if within budget, penalties if over budget
    val adherencePercent: Int
        get() {
            if (calorieBudget <= 0) return 100
            val ratio = caloriesConsumed.toFloat() / calorieBudget.toFloat()
            return when {
                ratio <= 1.0f -> (ratio * 100).toInt()
                else -> (100 - (ratio - 1.0f) * 100).coerceAtLeast(0f).toInt()
            }
        }
        
    val waterHit: Boolean get() = waterIntakeMl >= waterTargetMl
}

@Serializable
data class PartnerDuelSummary(
    val primaryUser: UserDailyScore,
    val partnerUser: UserDailyScore,
    val date: String,
    val isPartnerSynced: Boolean = false
) {
    val winnerName: String?
        get() = when {
            primaryUser.adherencePercent > partnerUser.adherencePercent -> primaryUser.userName
            partnerUser.adherencePercent > primaryUser.adherencePercent -> partnerUser.userName
            else -> null // Tie!
        }
}
