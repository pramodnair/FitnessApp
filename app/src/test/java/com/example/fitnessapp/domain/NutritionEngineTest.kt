package com.example.fitnessapp.domain

import com.example.fitnessapp.data.model.ActivityLevel
import com.example.fitnessapp.data.model.CalorieWarningLevel
import com.example.fitnessapp.data.model.DailyNutritionSummary
import com.example.fitnessapp.data.model.DeficitLevel
import com.example.fitnessapp.data.model.Gender
import com.example.fitnessapp.data.model.PartnerDuelSummary
import com.example.fitnessapp.data.model.UserDailyScore
import com.example.fitnessapp.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionEngineTest {

    @Test
    fun testBmiCalculation() {
        // Height 175cm, Weight 85kg => 85 / (1.75^2) = 27.755 => ~27.8
        val bmi = BmiCalculator.calculateBmi(85f, 175f)
        assertEquals(27.8f, bmi, 0.1f)
        assertEquals(BmiCategory.OVERWEIGHT, BmiCalculator.getCategory(bmi))

        // Normal BMI test
        val normalBmi = BmiCalculator.calculateBmi(70f, 175f)
        assertEquals(22.9f, normalBmi, 0.1f)
        assertEquals(BmiCategory.NORMAL, BmiCalculator.getCategory(normalBmi))

        // Ideal weight range for 175 cm (18.5 to 24.9 BMI)
        val (minW, maxW) = BmiCalculator.getIdealWeightRange(175f)
        assertTrue(minW in 56.5f..57.0f)
        assertTrue(maxW in 76.0f..76.5f)
    }

    @Test
    fun testMifflinStJeorBmrAndTdee() {
        // Male: 32 yrs, 85kg, 175cm
        // BMR = 10*85 + 6.25*175 - 5*32 + 5 = 850 + 1093.75 - 160 + 5 = 1788.75
        val bmrMale = CalorieCalculator.calculateBmr(Gender.MALE, 85f, 175f, 32)
        assertEquals(1788.75f, bmrMale, 0.1f)

        // Moderate activity (1.55x)
        val tdeeMale = CalorieCalculator.calculateTdee(bmrMale, ActivityLevel.MODERATE)
        assertEquals(2772.56f, tdeeMale, 1.0f)

        // Female: 30 yrs, 65kg, 165cm
        // BMR = 10*65 + 6.25*165 - 5*30 - 161 = 650 + 1031.25 - 150 - 161 = 1370.25
        val bmrFemale = CalorieCalculator.calculateBmr(Gender.FEMALE, 65f, 165f, 30)
        assertEquals(1370.25f, bmrFemale, 0.1f)
    }

    @Test
    fun testCustomizableDeficitAndSafeFloor() {
        val tdee = 2000f

        // Mild (-250)
        val mildBudget = CalorieCalculator.calculateDailyBudget(tdee, DeficitLevel.MILD, 0, Gender.MALE)
        assertEquals(1750, mildBudget)

        // Moderate (-500)
        val modBudget = CalorieCalculator.calculateDailyBudget(tdee, DeficitLevel.MODERATE, 0, Gender.MALE)
        assertEquals(1500, modBudget)

        // Aggressive (-750) -> should be capped at safe floor 1500 for males
        val aggBudgetMale = CalorieCalculator.calculateDailyBudget(tdee, DeficitLevel.AGGRESSIVE, 0, Gender.MALE)
        assertEquals(1500, aggBudgetMale)

        // Female safe floor is 1200
        val aggBudgetFemale = CalorieCalculator.calculateDailyBudget(tdee, DeficitLevel.AGGRESSIVE, 0, Gender.FEMALE)
        assertEquals(1250, aggBudgetFemale)

        // Custom deficit (e.g. -400)
        val customBudget = CalorieCalculator.calculateDailyBudget(tdee, DeficitLevel.CUSTOM, 400, Gender.MALE)
        assertEquals(1600, customBudget)
    }

    @Test
    fun testCalorieWarningAlerts() {
        val budget = 2000

        // Safe: 1400 / 2000 (70%)
        val safeSummary = DailyNutritionSummary(
            date = "2026-09-15",
            calorieBudget = budget,
            caloriesConsumed = 1400
        )
        assertEquals(CalorieWarningLevel.SAFE, safeSummary.warningLevel)
        assertNull(safeSummary.warningMessage)

        // Approaching: 1650 / 2000 (82.5%)
        val approachingSummary = DailyNutritionSummary(
            date = "2026-09-15",
            calorieBudget = budget,
            caloriesConsumed = 1650
        )
        assertEquals(CalorieWarningLevel.APPROACHING, approachingSummary.warningLevel)
        assertNotNull(approachingSummary.warningMessage)
        assertTrue(approachingSummary.warningMessage!!.contains("80%+"))

        // Near Limit: 1850 / 2000 (92.5%)
        val nearLimitSummary = DailyNutritionSummary(
            date = "2026-09-15",
            calorieBudget = budget,
            caloriesConsumed = 1850
        )
        assertEquals(CalorieWarningLevel.NEAR_LIMIT, nearLimitSummary.warningLevel)
        assertTrue(nearLimitSummary.warningMessage!!.contains("90%+"))

        // Exceeded: 2150 / 2000 (107.5%)
        val exceededSummary = DailyNutritionSummary(
            date = "2026-09-15",
            calorieBudget = budget,
            caloriesConsumed = 2150
        )
        assertEquals(CalorieWarningLevel.EXCEEDED, exceededSummary.warningLevel)
        assertTrue(exceededSummary.warningMessage!!.contains("exceeded by 150 kcal"))
    }

    @Test
    fun testPartnerCompetitionWinner() {
        val user1 = UserDailyScore(
            userId = "p1",
            userName = "Pramod",
            calorieBudget = 1800,
            caloriesConsumed = 1750, // 97% adherence
            waterIntakeMl = 3000,
            waterTargetMl = 3000,
            currentWeightKg = 84f,
            weightLostKg = 1.0f,
            streakDays = 5
        )

        val user2 = UserDailyScore(
            userId = "p2",
            userName = "Wife",
            calorieBudget = 1500,
            caloriesConsumed = 1700, // over budget (exceeded by 200 kcal, penalised)
            waterIntakeMl = 2000,
            waterTargetMl = 2500,
            currentWeightKg = 64f,
            weightLostKg = 0.8f,
            streakDays = 3
        )

        val duel = PartnerDuelSummary(
            primaryUser = user1,
            partnerUser = user2,
            date = "2026-09-15"
        )

        assertEquals("Pramod", duel.winnerName)
    }
}
