package com.example.fitnessapp.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.DailyNutritionSummary
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.MealType
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.network.GeminiVisionService
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data object Analyzing : ScannerUiState
    data class Review(val mealLog: MealLog) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class FoodScannerViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository,
    private val visionService: GeminiVisionService = FitnessApplication.instance.geminiVisionService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _lastScannedMeal = MutableStateFlow<MealLog?>(null)
    val lastScannedMeal: StateFlow<MealLog?> = _lastScannedMeal.asStateFlow()

    val activeProfile: StateFlow<UserProfile> = repository.activeProfile
    val todaySummary: StateFlow<DailyNutritionSummary> = repository.todaySummary

    fun processCapturedBitmap(bitmap: Bitmap, mealType: MealType = MealType.LUNCH) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Analyzing
            try {
                // Save photo to internal storage
                val profile = activeProfile.value
                val photoFile = File(
                    FitnessApplication.instance.cacheDir,
                    "meal_${System.currentTimeMillis()}.jpg"
                )
                FileOutputStream(photoFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                val analyzedMeal = visionService.analyzeFoodImage(
                    bitmap = bitmap,
                    apiKey = profile.geminiApiKey,
                    mealType = mealType,
                    userId = profile.id
                ).copy(photoPath = photoFile.absolutePath)

                _lastScannedMeal.value = analyzedMeal
                _uiState.value = ScannerUiState.Review(analyzedMeal)
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error("Analysis failed: ${e.message}")
            }
        }
    }

    fun restoreLastScan() {
        _lastScannedMeal.value?.let {
            _uiState.value = ScannerUiState.Review(it)
        }
    }

    fun clearLastScan() {
        _lastScannedMeal.value = null
    }

    fun confirmMeal(meal: MealLog) {
        repository.addMeal(meal)
        _lastScannedMeal.value = null
        _uiState.value = ScannerUiState.Idle
    }

    fun dismissReview() {
        _uiState.value = ScannerUiState.Idle
    }
}
