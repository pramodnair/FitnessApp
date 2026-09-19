package com.example.fitnessapp

import com.example.fitnessapp.data.model.DayDeficitStatus
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.MealType
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.model.WeeklyNutritionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FeaturesDailyUsabilityTest {

    @Test
    fun testUserProfileLifestyleTargetsDefaultAndCustomization() {
        val defaultProfile = UserProfile()
        assertEquals(10000, defaultProfile.dailyStepTarget)
        assertEquals(3000, defaultProfile.dailyWaterTargetMl)

        val customProfile = defaultProfile.copy(
            dailyStepTarget = 8000,
            dailyWaterTargetMl = 2500
        )
        assertEquals(8000, customProfile.dailyStepTarget)
        assertEquals(2500, customProfile.dailyWaterTargetMl)
    }

    @Test
    fun testWeeklyNutritionSummaryCalculations() {
        val days = listOf(
            DayDeficitStatus(
                dateStr = "2026-09-13",
                dayLabel = "Sun",
                caloriesConsumed = 1600,
                calorieBudget = 2000,
                isWithinBudget = true,
                hasLogs = true,
                isFuture = false
            ),
            DayDeficitStatus(
                dateStr = "2026-09-14",
                dayLabel = "Mon",
                caloriesConsumed = 1800,
                calorieBudget = 2000,
                isWithinBudget = true,
                hasLogs = true,
                isFuture = false
            ),
            DayDeficitStatus(
                dateStr = "2026-09-15",
                dayLabel = "Tue",
                caloriesConsumed = 2200,
                calorieBudget = 2000,
                isWithinBudget = false,
                hasLogs = true,
                isFuture = false
            )
        )

        val summary = WeeklyNutritionSummary(
            startDateStr = "2026-09-13",
            endDateStr = "2026-09-19",
            totalCaloriesConsumed = 5600,
            totalCalorieBudget = 6000,
            averageDailyCalories = 1867,
            dailyCalorieBudget = 2000,
            totalNetDeficitKcal = 400,
            estimatedKgLost = 0.05f,
            daysOnTarget = 2,
            totalDaysWithLogs = 3,
            dayStatuses = days
        )

        assertEquals(400, summary.totalNetDeficit)
        assertEquals(3, summary.days.size)
        assertEquals(2, summary.daysOnTrack)
        assertTrue(summary.days[0].isUnderBudget)
        assertFalse(summary.days[2].isUnderBudget)
    }

    @Test
    fun testRepeatMealCloningLogic() {
        val original = MealLog(
            id = "meal_123",
            userId = "user_1",
            date = "2026-09-18",
            timestamp = 1726650000000L,
            mealType = MealType.LUNCH,
            title = "Grilled Salmon Bowl",
            calories = 550,
            proteinG = 42f,
            carbsG = 35f,
            fatG = 18f
        )

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val duplicated = original.copy(
            id = UUID.randomUUID().toString(),
            date = todayDate,
            timestamp = System.currentTimeMillis()
        )

        assertNotEquals(original.id, duplicated.id)
        assertEquals(todayDate, duplicated.date)
        assertEquals(original.title, duplicated.title)
        assertEquals(original.calories, duplicated.calories)
        assertEquals(original.proteinG, duplicated.proteinG, 0.01f)
        assertEquals(original.carbsG, duplicated.carbsG, 0.01f)
        assertEquals(original.fatG, duplicated.fatG, 0.01f)
    }
}
