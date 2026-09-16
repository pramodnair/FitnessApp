package com.example.fitnessapp.ui.weight

import androidx.lifecycle.ViewModel
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.model.WeightLog
import com.example.fitnessapp.data.repository.FitnessRepository
import com.example.fitnessapp.domain.NutritionEngine
import com.example.fitnessapp.domain.RecommendedNutrition
import kotlinx.coroutines.flow.StateFlow

class WeightTrackerViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository
) : ViewModel() {

    val activeProfile: StateFlow<UserProfile> = repository.activeProfile
    val weightHistory: StateFlow<List<WeightLog>> = repository.weightHistory

    fun getNutritionRecommendations(): RecommendedNutrition {
        return NutritionEngine.calculateRecommendations(activeProfile.value)
    }

    fun addWeightEntry(weightKg: Float, notes: String = "") {
        repository.addWeightLog(weightKg, notes)
    }

    fun updateStartingWeight(weightKg: Float) {
        repository.updateStartingWeight(weightKg)
    }
}
