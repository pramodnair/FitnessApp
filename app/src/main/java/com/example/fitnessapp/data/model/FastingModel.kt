package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class FastingProtocol(
    val fastingHours: Int,
    val eatingHours: Int,
    val label: String,
    val description: String
) {
    LEAN_GAINS(16, 8, "16:8 LeanGains", "Most popular daily fasting window"),
    GENTLE(14, 10, "14:10 Gentle", "Easy beginner-friendly fasting"),
    INTENSE(18, 6, "18:6 Advanced", "Accelerated fat burning & autophagy"),
    WARRIOR(20, 4, "20:4 Warrior", "High metabolic efficiency & longevity"),
    CUSTOM(16, 8, "Custom", "Customized fasting duration")
}

enum class MetabolicStage(
    val startHour: Int,
    val endHour: Int,
    val title: String,
    val subtitle: String,
    val colorHex: Long
) {
    BLOOD_SUGAR(0, 4, "Blood Sugar Settling", "Insulin levels normalize & digestion rests", 0xFF0288D1),
    DIGESTIVE_REST(4, 12, "Digestive Rest", "Glycogen depletion begins", 0xFF00796B),
    FAT_BURNING(12, 16, "Fat Burning Zone 🔥", "Metabolism burns stored fat for energy", 0xFFFF8F00),
    AUTOPHAGY(16, 999, "Autophagy Zone ✨", "Deep cellular recycling and renewal", 0xFF7B1FA2)
}

@Serializable
data class FastingState(
    val isFasting: Boolean = false,
    val startTimeMs: Long = 0L,
    val targetHours: Int = 16,
    val protocol: FastingProtocol = FastingProtocol.LEAN_GAINS
) {
    fun getElapsedMillis(nowMs: Long = System.currentTimeMillis()): Long {
        if (!isFasting || startTimeMs <= 0L) return 0L
        return (nowMs - startTimeMs).coerceAtLeast(0L)
    }

    fun getElapsedHours(nowMs: Long = System.currentTimeMillis()): Float {
        return getElapsedMillis(nowMs) / (1000f * 3600f)
    }

    fun getProgress(nowMs: Long = System.currentTimeMillis()): Float {
        if (targetHours <= 0) return 0f
        return (getElapsedHours(nowMs) / targetHours.toFloat()).coerceIn(0f, 1.5f)
    }

    fun getCurrentStage(nowMs: Long = System.currentTimeMillis()): MetabolicStage {
        val elapsed = getElapsedHours(nowMs)
        return when {
            elapsed < 4f -> MetabolicStage.BLOOD_SUGAR
            elapsed < 12f -> MetabolicStage.DIGESTIVE_REST
            elapsed < 16f -> MetabolicStage.FAT_BURNING
            else -> MetabolicStage.AUTOPHAGY
        }
    }
}
