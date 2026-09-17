package com.example.fitnessapp.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fitnessapp.MainActivity
import com.example.fitnessapp.R

object FitnessNotificationHelper {

    const val CHANNEL_HYDRATION = "channel_hydration"
    const val CHANNEL_MEALS = "channel_meals"
    const val CHANNEL_FASTING = "channel_fasting"

    const val NOTIF_ID_HYDRATION = 1001
    const val NOTIF_ID_LUNCH = 1002
    const val NOTIF_ID_DINNER = 1003
    const val NOTIF_ID_FASTING = 1004

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val hydrationChannel = NotificationChannel(
                CHANNEL_HYDRATION,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle water intake reminders throughout the day"
            }

            val mealsChannel = NotificationChannel(
                CHANNEL_MEALS,
                "Meal Logging Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to log Lunch and Dinner meals"
            }

            val fastingChannel = NotificationChannel(
                CHANNEL_FASTING,
                "Fasting Goal Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when intermittent fasting targets are reached"
            }

            notificationManager.createNotificationChannels(
                listOf(hydrationChannel, mealsChannel, fastingChannel)
            )
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showHydrationNotification(context: Context, intakeMl: Int = 0, targetMl: Int = 2500) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_HYDRATION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hydration Check-in 💧")
            .setContentText("Time for a glass of water! Progress: $intakeMl / $targetMl ml")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_HYDRATION, notification)
    }

    fun showMealNotification(context: Context, mealLabel: String) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_MEALS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$mealLabel Time! 🍽️")
            .setContentText("Don't forget to track your meal to keep your calories and macros on target.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .build()

        val id = if (mealLabel.equals("Lunch", ignoreCase = true)) NOTIF_ID_LUNCH else NOTIF_ID_DINNER
        notificationManager.notify(id, notification)
    }

    fun showFastingGoalNotification(context: Context, hours: Int) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_FASTING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Fasting Goal Achieved! 🏆")
            .setContentText("You completed your $hours-hour fast! Deep autophagy and fat burning achieved.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_FASTING, notification)
    }
}
