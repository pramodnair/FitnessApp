package com.example.fitnessapp.ui.partner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.PartnerDuelSummary
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.repository.FitnessRepository
import com.example.fitnessapp.data.sync.SyncConnectionStatus
import com.example.fitnessapp.data.sync.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PartnerViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository,
    private val syncCoordinator: SyncCoordinator = FitnessApplication.instance.syncCoordinator
) : ViewModel() {

    val primaryProfile: StateFlow<UserProfile> = repository.activeProfile
    val partnerProfile: StateFlow<UserProfile> = repository.partnerProfile
    val partnerDuel: StateFlow<PartnerDuelSummary> = repository.partnerDuel

    val connectionStatus: StateFlow<SyncConnectionStatus> = syncCoordinator.connectionStatus
    val isSyncing: StateFlow<Boolean> = syncCoordinator.isSyncing
    val localDeviceIp: StateFlow<String?> = syncCoordinator.localDeviceIp
    val localPort: StateFlow<Int> = syncCoordinator.localPort
    val isScanningSubnet: StateFlow<Boolean> = syncCoordinator.isScanningSubnet
    val pairCode: StateFlow<String> = repository.pairCode
    val incomingCheer: StateFlow<String?> = repository.incomingCheer

    private val _cheerMessage = MutableStateFlow<String?>(null)
    val cheerMessage: StateFlow<String?> = _cheerMessage.asStateFlow()

    private val _syncNotice = MutableStateFlow<String?>(null)
    val syncNotice: StateFlow<String?> = _syncNotice.asStateFlow()

    fun sendCheer(message: String) {
        _cheerMessage.value = "Sent: $message"
        syncCoordinator.sendCheer(message) { success ->
            if (success) {
                _syncNotice.value = "Cheer sent to partner over Wi-Fi! 🎉"
            }
        }
    }

    fun dismissCheer() {
        _cheerMessage.value = null
        repository.clearIncomingCheer()
    }

    fun dismissSyncNotice() {
        _syncNotice.value = null
    }

    fun triggerManualSync() {
        syncCoordinator.triggerManualSync { success, message ->
            _syncNotice.value = message
        }
    }

    fun scanSubnet() {
        viewModelScope.launch {
            _syncNotice.value = "Scanning local Wi-Fi for partner..."
            syncCoordinator.scanSubnetForPartner { success, message ->
                _syncNotice.value = message
            }
        }
    }

    fun connectDirectIp(ip: String) {
        viewModelScope.launch {
            _syncNotice.value = "Connecting directly to $ip..."
            syncCoordinator.connectToPartnerDirectly(ip) { success, message ->
                _syncNotice.value = message
            }
        }
    }

    fun updatePairCode(newCode: String) {
        syncCoordinator.setPairCode(newCode)
        _syncNotice.value = "Pair code set to $newCode"
    }

    fun switchProfile(userId: String) {
        repository.switchActiveProfile(userId)
    }
}
