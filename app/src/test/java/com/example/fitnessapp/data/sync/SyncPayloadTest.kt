package com.example.fitnessapp.data.sync

import com.example.fitnessapp.data.model.UserDailyScore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPayloadTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSyncHandshakeSerialization() {
        val handshake = SyncHandshake(
            deviceName = "Pixel 8",
            userId = "primary",
            userName = "Pramod",
            pairCode = "FIT-8842",
            port = 8988
        )

        val encoded = json.encodeToString(handshake)
        assertTrue(encoded.contains("FIT-8842"))
        assertTrue(encoded.contains("Pramod"))

        val decoded = json.decodeFromString<SyncHandshake>(encoded)
        assertEquals("Pixel 8", decoded.deviceName)
        assertEquals("FIT-8842", decoded.pairCode)
        assertEquals(8988, decoded.port)
    }

    @Test
    fun testSyncDataPayloadSerialization() {
        val score = UserDailyScore(
            userId = "primary",
            userName = "Pramod",
            calorieBudget = 2273,
            caloriesConsumed = 1850,
            waterIntakeMl = 1500,
            waterTargetMl = 2000,
            currentWeightKg = 83.5f,
            weightLostKg = 1.5f,
            streakDays = 6
        )

        val payload = SyncDataPayload(
            pairCode = "FIT-8842",
            senderScore = score,
            cheerMessage = "Crushing it!"
        )

        val encoded = json.encodeToString(payload)
        val decoded = json.decodeFromString<SyncDataPayload>(encoded)

        assertEquals("FIT-8842", decoded.pairCode)
        assertEquals(1850, decoded.senderScore.caloriesConsumed)
        assertEquals(81, decoded.senderScore.adherencePercent)
        assertEquals("Crushing it!", decoded.cheerMessage)
    }

    @Test
    fun testSyncResponseAccepted() {
        val partnerScore = UserDailyScore(
            userId = "partner",
            userName = "Wife",
            calorieBudget = 1600,
            caloriesConsumed = 1520,
            waterIntakeMl = 2000,
            waterTargetMl = 2000,
            currentWeightKg = 63.0f,
            weightLostKg = 2.0f,
            streakDays = 5
        )

        val response = SyncResponse(
            status = "ACCEPTED",
            message = "Success",
            recipientScore = partnerScore
        )

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<SyncResponse>(encoded)

        assertEquals("ACCEPTED", decoded.status)
        assertNotNull(decoded.recipientScore)
        assertEquals("Wife", decoded.recipientScore?.userName)
        assertTrue(decoded.recipientScore!!.waterHit)
    }
}
