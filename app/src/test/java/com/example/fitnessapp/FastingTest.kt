package com.example.fitnessapp

import com.example.fitnessapp.data.model.FastingProtocol
import com.example.fitnessapp.data.model.FastingState
import com.example.fitnessapp.data.model.MetabolicStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FastingTest {

    @Test
    fun testFastingProgressionAndStages() {
        val startMs = 1000000L
        val targetHours = 16
        val state = FastingState(
            isFasting = true,
            startTimeMs = startMs,
            targetHours = targetHours,
            protocol = FastingProtocol.LEAN_GAINS
        )

        // At start (0 hours): Blood sugar settling stage
        assertEquals(MetabolicStage.BLOOD_SUGAR, state.getCurrentStage(startMs))
        assertEquals(0f, state.getProgress(startMs), 0.01f)

        // After 6 hours: Digestive rest
        val sixHoursLater = startMs + (6L * 3600 * 1000)
        assertEquals(MetabolicStage.DIGESTIVE_REST, state.getCurrentStage(sixHoursLater))

        // After 13 hours: Fat burning stage
        val thirteenHoursLater = startMs + (13L * 3600 * 1000)
        assertEquals(MetabolicStage.FAT_BURNING, state.getCurrentStage(thirteenHoursLater))

        // After 16 hours: Target achieved, Autophagy stage
        val sixteenHoursLater = startMs + (16L * 3600 * 1000)
        assertEquals(MetabolicStage.AUTOPHAGY, state.getCurrentStage(sixteenHoursLater))
        assertEquals(1.0f, state.getProgress(sixteenHoursLater), 0.01f)
    }

    @Test
    fun testFastingInactiveState() {
        val state = FastingState(isFasting = false)
        assertEquals(0L, state.getElapsedMillis())
        assertEquals(0f, state.getElapsedHours(), 0.01f)
        assertEquals(0f, state.getProgress(), 0.01f)
    }
}
