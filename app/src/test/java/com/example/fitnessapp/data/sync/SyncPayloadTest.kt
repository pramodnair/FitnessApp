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

    @Test
    fun testEnrichedUserDailyScoreAndDuelSummarySerialization() {
        val score = UserDailyScore(
            userId = "primary",
            userName = "Pramod",
            calorieBudget = 2200,
            caloriesConsumed = 1900,
            waterIntakeMl = 2500,
            waterTargetMl = 3000,
            currentWeightKg = 83.2f,
            weightLostKg = 1.8f,
            streakDays = 7,
            proteinConsumedG = 135f,
            proteinTargetG = 150f,
            carbsConsumedG = 190f,
            carbsTargetG = 220f,
            fatConsumedG = 55f,
            fatTargetG = 65f,
            targetWeightKg = 72f,
            startWeightKg = 85f,
            isLiveSynced = true,
            lastSyncTimestamp = 1710000000000L
        )

        val partnerScore = UserDailyScore(
            userId = "partner",
            userName = "Wife",
            calorieBudget = 1600,
            caloriesConsumed = 1450,
            waterIntakeMl = 2200,
            waterTargetMl = 2000,
            currentWeightKg = 62.5f,
            weightLostKg = 2.5f,
            streakDays = 6,
            proteinConsumedG = 95f,
            proteinTargetG = 100f,
            carbsConsumedG = 140f,
            carbsTargetG = 160f,
            fatConsumedG = 42f,
            fatTargetG = 48f,
            targetWeightKg = 55f,
            startWeightKg = 65f,
            isLiveSynced = true,
            lastSyncTimestamp = 1710000000000L
        )

        val duel = com.example.fitnessapp.data.model.PartnerDuelSummary(
            primaryUser = score,
            partnerUser = partnerScore,
            date = "2026-09-16",
            isPartnerSynced = true
        )

        val encoded = json.encodeToString(duel)
        val decoded = json.decodeFromString<com.example.fitnessapp.data.model.PartnerDuelSummary>(encoded)

        assertTrue(decoded.isPartnerSynced)
        assertEquals(135f, decoded.primaryUser.proteinConsumedG)
        assertEquals(150f, decoded.primaryUser.proteinTargetG)
        assertEquals(85f, decoded.primaryUser.startWeightKg)
        assertTrue(decoded.primaryUser.isLiveSynced)
        assertEquals(1710000000000L, decoded.primaryUser.lastSyncTimestamp)

        assertEquals("Wife", decoded.partnerUser.userName)
        assertEquals(95f, decoded.partnerUser.proteinConsumedG)
        assertTrue(decoded.partnerUser.isLiveSynced)
    }
}
