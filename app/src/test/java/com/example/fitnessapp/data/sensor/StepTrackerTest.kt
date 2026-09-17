package com.example.fitnessapp.data.sensor

import com.example.fitnessapp.data.model.UserDailyScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTrackerTest {

    @Test
    fun testBurnedCaloriesFormula() {
        val cal70kg = StepTrackerManager.calculateBurnedCalories(10000, 70f)
        assertEquals(400, cal70kg)

        val cal80kg = StepTrackerManager.calculateBurnedCalories(10000, 80f)
        assertTrue(cal80kg > cal70kg)
        assertEquals(457, cal80kg)

        assertEquals(0, StepTrackerManager.calculateBurnedCalories(0, 70f))
    }

    @Test
    fun testUserDailyScoreStepsFields() {
        val score = UserDailyScore(
            userId = "user_123",
            userName = "Pramod",
            calorieBudget = 2000,
            caloriesConsumed = 1800,
            waterIntakeMl = 2500,
            waterTargetMl = 3000,
            currentWeightKg = 72.5f,
            weightLostKg = 2.5f,
            streakDays = 14,
            stepsTaken = 8500,
            stepsTarget = 10000,
            caloriesBurned = 340
        )

        assertEquals(8500, score.stepsTaken)
        assertEquals(10000, score.stepsTarget)
        assertEquals(340, score.caloriesBurned)
        assertEquals(1800 - 340, score.caloriesConsumed - score.caloriesBurned)
        assertEquals(90, score.adherencePercent)
    }
}
