package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Micronutrients(
    val fiberG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val calciumMg: Float = 0f,
    val ironMg: Float = 0f,
    val vitaminCMg: Float = 0f
) {
    operator fun plus(other: Micronutrients): Micronutrients {
        return Micronutrients(
            fiberG = this.fiberG + other.fiberG,
            sugarG = this.sugarG + other.sugarG,
            sodiumMg = this.sodiumMg + other.sodiumMg,
            potassiumMg = this.potassiumMg + other.potassiumMg,
            calciumMg = this.calciumMg + other.calciumMg,
            ironMg = this.ironMg + other.ironMg,
            vitaminCMg = this.vitaminCMg + other.vitaminCMg
        )
    }

    companion object {
        val ZERO = Micronutrients()
        
        // Recommended daily targets for optimal health
        val DAILY_RECOMMENDED = Micronutrients(
            fiberG = 30f,        // 28-34g recommended
            sugarG = 35f,        // Max recommended added sugars
            sodiumMg = 2300f,    // Max recommended intake
            potassiumMg = 3500f, // Adequate intake
            calciumMg = 1000f,   // Recommended daily allowance
            ironMg = 14f,        // Recommended average
            vitaminCMg = 90f     // Recommended intake
        )
    }
}
