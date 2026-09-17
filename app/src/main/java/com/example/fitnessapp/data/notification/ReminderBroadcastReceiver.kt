package com.example.fitnessapp.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.ReminderPreferences
import java.util.Calendar

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HYDRATION -> {
                val repo = FitnessApplication.instance.repository
                val summary = repo.todaySummary.value
                FitnessNotificationHelper.showHydrationNotification(
                    context,
                    summary.waterIntakeMl,
                    summary.waterTargetMl
                )
                // Schedule next hydration reminder
                ReminderScheduler.scheduleNextHydration(context)
            }
            ACTION_LUNCH -> {
                FitnessNotificationHelper.showMealNotification(context, "Lunch")
                ReminderScheduler.scheduleNextMeal(context, isLunch = true)
            }
            ACTION_DINNER -> {
                FitnessNotificationHelper.showMealNotification(context, "Dinner")
                ReminderScheduler.scheduleNextMeal(context, isLunch = false)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                ReminderScheduler.rescheduleAll(context)
            }
        }
    }

    companion object {
        const val ACTION_HYDRATION = "com.example.fitnessapp.ACTION_HYDRATION"
        const val ACTION_LUNCH = "com.example.fitnessapp.ACTION_LUNCH"
        const val ACTION_DINNER = "com.example.fitnessapp.ACTION_DINNER"
    }
}

object ReminderScheduler {

    fun rescheduleAll(context: Context) {
        scheduleNextHydration(context)
        scheduleNextMeal(context, isLunch = true)
        scheduleNextMeal(context, isLunch = false)
    }

    fun scheduleNextHydration(context: Context, intervalHours: Int = 2) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_HYDRATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Only remind between 9 AM and 9 PM
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val targetHour = if (currentHour in 9..20) currentHour + intervalHours else 9
        if (currentHour >= 21) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        cal.set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(9, 21))
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.HOUR_OF_DAY, intervalHours)
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback for devices restricting exact alarms
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }

    fun scheduleNextMeal(context: Context, isLunch: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val action = if (isLunch) ReminderBroadcastReceiver.ACTION_LUNCH else ReminderBroadcastReceiver.ACTION_DINNER
        val reqCode = if (isLunch) 102 else 103
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetHour = if (isLunch) 13 else 20
        val targetMinute = 30

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        listOf(
            ReminderBroadcastReceiver.ACTION_HYDRATION to 101,
            ReminderBroadcastReceiver.ACTION_LUNCH to 102,
            ReminderBroadcastReceiver.ACTION_DINNER to 103
        ).forEach { (action, code) ->
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply { this.action = action }
            val pi = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pi)
        }
    }
}
