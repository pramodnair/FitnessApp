package com.example.fitnessapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository
) : ViewModel() {

    val activeProfile: StateFlow<UserProfile> = repository.activeProfile

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun completeOnboarding() {
        repository.setOnboardingCompleted(true)
    }
}
