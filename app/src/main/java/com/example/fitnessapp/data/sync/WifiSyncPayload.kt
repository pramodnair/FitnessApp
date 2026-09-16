package com.example.fitnessapp.data.sync

import com.example.fitnessapp.data.model.UserDailyScore
import kotlinx.serialization.Serializable

@Serializable
data class SyncHandshake(
    val deviceName: String,
    val userId: String,
    val userName: String,
    val pairCode: String,
    val port: Int,
    val installationId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SyncDataPayload(
    val pairCode: String,
    val senderScore: UserDailyScore,
    val cheerMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SyncResponse(
    val status: String, // "ACCEPTED", "REJECTED_PAIR_CODE", "ERROR"
    val message: String,
    val recipientScore: UserDailyScore? = null
)

@Serializable
data class SyncCheerPayload(
    val pairCode: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface SyncConnectionStatus {
    data object Disconnected : SyncConnectionStatus
    data object SearchingWifi : SyncConnectionStatus
    data class ConnectedWifi(
        val partnerIp: String,
        val partnerName: String,
        val lastSyncedTime: Long = System.currentTimeMillis()
    ) : SyncConnectionStatus
    data class CloudFallback(
        val isConfigured: Boolean,
        val statusMessage: String
    ) : SyncConnectionStatus
}
