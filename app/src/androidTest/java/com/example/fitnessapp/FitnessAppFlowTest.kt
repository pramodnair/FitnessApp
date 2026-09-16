package com.example.fitnessapp

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fitnessapp.ui.bodyprogress.BodyProgressScreen
import com.example.fitnessapp.ui.dashboard.DashboardScreen
import com.example.fitnessapp.ui.partner.PartnerScreen
import com.example.fitnessapp.ui.weight.WeightTrackerScreen
import org.junit.Rule
import org.junit.Test

class FitnessAppFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testDashboardDisplaysCalorieMeterAndNutrients() {
        composeTestRule.setContent {
            MaterialTheme {
                DashboardScreen(
                    onNavigateToScanner = {},
                    onNavigateToManualEntry = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Verify Daily Calorie Budget gauge & nutrients exist
        composeTestRule.onNodeWithText("DAILY CALORIE BUDGET").assertIsDisplayed()
        composeTestRule.onNodeWithText("ESSENTIAL MICRONUTRIENTS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hydration Tracker").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Food Scan").assertIsDisplayed()
    }

    @Test
    fun testWeightTrackerDisplaysBmiAndTarget() {
        composeTestRule.setContent {
            MaterialTheme {
                WeightTrackerScreen()
            }
        }

        // Verify BMI and Goal Journey
        composeTestRule.onNodeWithText("Weight Loss & BMI Journey").assertIsDisplayed()
        composeTestRule.onNodeWithText("CURRENT BMI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Healthy Weight Range").assertIsDisplayed()
    }

    @Test
    fun testBodyProgressDisplaysComparison() {
        composeTestRule.setContent {
            MaterialTheme {
                BodyProgressScreen()
            }
        }

        // Verify transformation screen elements
        composeTestRule.onNodeWithText("Body Transformation & Comparison").assertIsDisplayed()
    }

    @Test
    fun testPartnerDuelDisplaysCompetition() {
        composeTestRule.setContent {
            MaterialTheme {
                PartnerScreen()
            }
        }

        // Verify Couples Duel & Leaderboard
        composeTestRule.onNodeWithText("Couples Fitness Duel & Accountability").assertIsDisplayed()
        composeTestRule.onNodeWithText("DAILY LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pramod").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wife").assertIsDisplayed()
    }
}
