package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class BodyPose(val label: String) {
    FRONT("Front View"),
    SIDE("Side View"),
    BACK("Back View")
}

@Serializable
data class BodyPhoto(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "primary",
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val photoPath: String,
    val pose: BodyPose = BodyPose.FRONT,
    val weightKg: Float = 0f,
    val notes: String = ""
)
