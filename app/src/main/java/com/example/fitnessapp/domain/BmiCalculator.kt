package com.example.fitnessapp.domain

import com.example.fitnessapp.data.model.DeficitLevel
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

enum class BmiCategory(val label: String, val colorHex: Long) {
    UNDERWEIGHT("Underweight (< 18.5)", 0xFF2196F3),
    NORMAL("Healthy / Normal (18.5 - 24.9)", 0xFF4CAF50),
    OVERWEIGHT("Overweight (25.0 - 29.9)", 0xFFFF9800),
    OBESE("Obese (>= 30.0)", 0xFFF44336)
}

object BmiCalculator {

    /**
     * Calculates Body Mass Index: weight (kg) / [height (m)]^2
     */
    fun calculateBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0f || weightKg <= 0f) return 0f
        val heightM = heightCm / 100f
        val bmi = weightKg / (heightM.pow(2))
        return (bmi * 10f).roundToInt() / 10f
    }

    /**
     * Returns the clinical BMI category.
     */
    fun getCategory(bmi: Float): BmiCategory {
        return when {
            bmi < 18.5f -> BmiCategory.UNDERWEIGHT
            bmi < 25.0f -> BmiCategory.NORMAL
            bmi < 30.0f -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
    }

    /**
     * Returns the ideal weight range (kg) for a normal BMI (18.5 to 24.9).
     */
    fun getIdealWeightRange(heightCm: Float): Pair<Float, Float> {
        if (heightCm <= 0f) return Pair(0f, 0f)
        val heightM = heightCm / 100f
        val minIdealWeight = 18.5f * (heightM.pow(2))
        val maxIdealWeight = 24.9f * (heightM.pow(2))
        return Pair(
            (minIdealWeight * 10f).roundToInt() / 10f,
            (maxIdealWeight * 10f).roundToInt() / 10f
        )
    }

    /**
     * Calculates the weight in kg needed to achieve a target BMI.
     * Default healthy target BMI is 22.0.
     */
    fun getWeightForBmi(targetBmi: Float = 22.0f, heightCm: Float): Float {
        if (heightCm <= 0f) return 0f
        val heightM = heightCm / 100f
        val targetWeight = targetBmi * (heightM.pow(2))
        return (targetWeight * 10f).roundToInt() / 10f
    }

    fun calculateTargetWeight(heightCm: Float, targetBmi: Float = 22.0f): Float {
        return getWeightForBmi(targetBmi, heightCm)
    }

    /**
     * Calculates recommended target weight based on height, starting weight, target BMI, and deficit pace.
     */
    fun calculateTargetWeight(
        heightCm: Float,
        startWeightKg: Float,
        targetBmi: Float = 22.0f,
        deficitLevel: DeficitLevel = DeficitLevel.MODERATE
    ): Float {
        if (heightCm <= 50f) return 0f

        val effectiveBmi = when (deficitLevel) {
            DeficitLevel.AGGRESSIVE -> if (targetBmi >= 22f) 21.0f else targetBmi
            DeficitLevel.MODERATE -> if (targetBmi != 22f && targetBmi != 21f && targetBmi != 23.5f) targetBmi else 22.0f
            DeficitLevel.MILD -> if (targetBmi <= 22f) 23.5f else targetBmi
            DeficitLevel.CUSTOM -> targetBmi
        }

        var idealWeight = getWeightForBmi(effectiveBmi, heightCm)

        // If starting weight is entered and is already at or below calculated ideal weight:
        if (startWeightKg > 0f && startWeightKg <= idealWeight) {
            val reduction = when (deficitLevel) {
                DeficitLevel.MILD -> 2f
                DeficitLevel.MODERATE -> 4f
                DeficitLevel.AGGRESSIVE -> 6f
                DeficitLevel.CUSTOM -> 3f
            }
            val minSafeWeight = getWeightForBmi(19.0f, heightCm)
            idealWeight = (startWeightKg - reduction).coerceAtLeast(minSafeWeight)
            idealWeight = (idealWeight * 10f).roundToInt() / 10f
        }

        return idealWeight
    }
}
