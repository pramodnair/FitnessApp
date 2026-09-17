package com.example.fitnessapp

import com.example.fitnessapp.data.model.WeightLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightTrendTest {

    @Test
    fun testExponentialMovingAverageSmoothing() {
        val alpha = 0.25f
        val rawWeights = listOf(80f, 81f, 79.5f, 79f, 78.5f)

        var runningEma = 0f
        val emaList = mutableListOf<Float>()

        rawWeights.forEachIndexed { i, w ->
            runningEma = if (i == 0) w else (w * alpha + runningEma * (1f - alpha))
            emaList.add(runningEma)
        }

        // Day 1 EMA equals starting weight
        assertEquals(80f, emaList[0], 0.001f)
        // Day 2: 81 * 0.25 + 80 * 0.75 = 20.25 + 60 = 80.25
        assertEquals(80.25f, emaList[1], 0.001f)
        // Smoothing should prevent extreme swing: Day 2 raw is 81.0, but smoothed is only 80.25
        assertTrue(emaList[1] < 81.0f)
        // Day 5 should show downward trend smoothly
        assertTrue(emaList.last() < 80f)
    }
}
