package com.example.fitnessapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReminderPreferences(
    val hydrationEnabled: Boolean = true,
    val hydrationIntervalHours: Int = 2,
    val mealRemindersEnabled: Boolean = true,
    val lunchTimeStr: String = "13:30",
    val dinnerTimeStr: String = "20:30",
    val fastingAlertEnabled: Boolean = true
)
