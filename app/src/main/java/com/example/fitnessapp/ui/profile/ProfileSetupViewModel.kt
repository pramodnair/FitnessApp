package com.example.fitnessapp.ui.profile

import androidx.lifecycle.ViewModel
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.StateFlow

import com.example.fitnessapp.data.network.ApiKeyValidationResult
import com.example.fitnessapp.data.network.GeminiVisionService

class ProfileSetupViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository,
    private val visionService: GeminiVisionService = FitnessApplication.instance.geminiVisionService
) : ViewModel() {

    val activeProfile: StateFlow<UserProfile> = repository.activeProfile

    fun saveProfile(profile: UserProfile) {
        repository.updateProfile(profile)
    }

    fun setGeminiApiKey(key: String) {
        repository.setGeminiApiKey(key)
    }

    suspend fun testApiKey(key: String): ApiKeyValidationResult {
        return visionService.validateApiKey(key)
    }
}
