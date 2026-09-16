package com.example.fitnessapp.data.sync

import com.example.fitnessapp.data.model.UserDailyScore
import com.example.fitnessapp.data.sync.wifi.LocalSyncClient
import com.example.fitnessapp.data.sync.wifi.LocalSyncServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalSyncIntegrationTest {

    private lateinit var server: LocalSyncServer
    private lateinit var client: LocalSyncClient
    private var boundPort = 0

    private var lastReceivedScore: UserDailyScore? = null
    private var lastReceivedCheer: String? = null

    private val localScore = UserDailyScore(
        userId = "primary",
        userName = "Pramod",
        calorieBudget = 2273,
        caloriesConsumed = 1800,
        waterIntakeMl = 1750,
        waterTargetMl = 2000,
        currentWeightKg = 84f,
        weightLostKg = 1f,
        streakDays = 5
    )

    @Before
    fun setUp() {
        client = LocalSyncClient()
        server = LocalSyncServer(
            expectedPairCodeProvider = { "FIT-8842" },
            localHandshakeProvider = { port ->
                SyncHandshake(
                    deviceName = "Test Device",
                    userId = "primary",
                    userName = "Pramod",
                    pairCode = "FIT-8842",
                    port = port,
                    installationId = "test-install-uuid-1234"
                )
            },
            localScoreProvider = { localScore },
            onPartnerScoreReceived = { score, cheer ->
                lastReceivedScore = score
                lastReceivedCheer = cheer
            },
            onCheerReceived = { sender, message ->
                lastReceivedCheer = "$sender: $message"
            }
        )
        boundPort = server.start(preferredPort = 0)
        assertTrue(boundPort > 0)
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun testPingHandshake() = runBlocking {
        val result = client.ping("127.0.0.1", boundPort)
        assertTrue(result.isSuccess)
        val handshake = result.getOrNull()
        assertNotNull(handshake)
        assertEquals("FIT-8842", handshake?.pairCode)
        assertEquals("Pramod", handshake?.userName)
        assertEquals("test-install-uuid-1234", handshake?.installationId)
    }

    @Test
    fun testQuickPingHandshake() = runBlocking {
        val result = client.quickPing("127.0.0.1", boundPort, timeoutMs = 500)
        assertTrue(result.isSuccess)
        val handshake = result.getOrNull()
        assertNotNull(handshake)
        assertEquals("FIT-8842", handshake?.pairCode)
        assertEquals("test-install-uuid-1234", handshake?.installationId)
    }

    @Test
    fun testValidSyncExchange() = runBlocking {
        val wifeScore = UserDailyScore(
            userId = "partner",
            userName = "Wife",
            calorieBudget = 1600,
            caloriesConsumed = 1400,
            waterIntakeMl = 1500,
            waterTargetMl = 1800,
            currentWeightKg = 64f,
            weightLostKg = 1f,
            streakDays = 4
        )

        val payload = SyncDataPayload(
            pairCode = "FIT-8842",
            senderScore = wifeScore,
            cheerMessage = "Great job today!"
        )

        val result = client.sendSync("127.0.0.1", boundPort, payload)
        assertTrue(result.isSuccess)

        val response = result.getOrNull()
        assertEquals("ACCEPTED", response?.status)
        assertEquals("Pramod", response?.recipientScore?.userName)

        // Verify callback was invoked on server side
        assertEquals("Wife", lastReceivedScore?.userName)
        assertEquals(1400, lastReceivedScore?.caloriesConsumed)
        assertEquals("Great job today!", lastReceivedCheer)
    }

    @Test
    fun testInvalidPairCodeRejected() = runBlocking {
        val fakeScore = UserDailyScore(
            userId = "stranger",
            userName = "Stranger",
            calorieBudget = 2000,
            caloriesConsumed = 2000,
            waterIntakeMl = 1000,
            waterTargetMl = 2000,
            currentWeightKg = 70f,
            weightLostKg = 0f,
            streakDays = 1
        )

        val payload = SyncDataPayload(
            pairCode = "WRONG-CODE",
            senderScore = fakeScore
        )

        val result = client.sendSync("127.0.0.1", boundPort, payload)
        // Should fail because server returns 403 Forbidden
        assertTrue(result.isFailure)
    }

    @Test
    fun testSendCheer() = runBlocking {
        val cheer = SyncCheerPayload(
            pairCode = "FIT-8842",
            senderName = "Wife",
            message = "Stay hydrated! 💧"
        )

        val result = client.sendCheer("127.0.0.1", boundPort, cheer)
        assertTrue(result.isSuccess)
        assertEquals("Wife: Stay hydrated! 💧", lastReceivedCheer)
    }
}
