package com.example.fitnessapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.DailyNutritionSummary
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.PartnerDuelSummary
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val activeProfile: StateFlow<UserProfile> = repository.activeProfile
    val partnerProfile: StateFlow<UserProfile> = repository.partnerProfile
    val partnerDuel: StateFlow<PartnerDuelSummary> = repository.partnerDuel

    private val _selectedDate = MutableStateFlow(getTodayDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val todaySteps: StateFlow<Int> = FitnessApplication.instance.stepTrackerManager.todaySteps
    val caloriesBurned: StateFlow<Int> = FitnessApplication.instance.stepTrackerManager.caloriesBurned

    init {
        FitnessApplication.instance.stepTrackerManager.updateUserWeight(activeProfile.value.currentWeightKg)
    }

    fun addManualSteps(count: Int) {
        FitnessApplication.instance.stepTrackerManager.addManualSteps(count)
    }

    fun getTodayDate(): String = dateFormat.format(Date())

    // Meals for the selected date
    val displayMeals: StateFlow<List<MealLog>> = combine(
        repository.allMeals,
        _selectedDate,
        activeProfile
    ) { allMeals, date, profile ->
        allMeals.filter { it.userId == profile.id && it.date == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Nutrition summary for the selected date
    val displaySummary: StateFlow<DailyNutritionSummary> = combine(
        repository.allMeals,
        _selectedDate,
        activeProfile,
        repository.todaySummary // triggers on water/profile updates
    ) { _, date, _, todaySum ->
        if (date == getTodayDate()) {
            todaySum
        } else {
            repository.getSummaryForDate(date)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.todaySummary.value)

    fun selectPreviousDay() {
        val cal = Calendar.getInstance()
        val current = try { dateFormat.parse(_selectedDate.value) } catch (e: Exception) { null }
        if (current != null) cal.time = current
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDate.value = dateFormat.format(cal.time)
    }

    fun selectNextDay() {
        val cal = Calendar.getInstance()
        val current = try { dateFormat.parse(_selectedDate.value) } catch (e: Exception) { null }
        if (current != null) cal.time = current
        cal.add(Calendar.DAY_OF_YEAR, 1)
        _selectedDate.value = dateFormat.format(cal.time)
    }

    fun selectToday() {
        _selectedDate.value = getTodayDate()
    }

    fun switchProfile(userId: String) {
        repository.switchActiveProfile(userId)
    }

    fun addWater(amountMl: Int) {
        val date = _selectedDate.value
        repository.addWaterForDate(amountMl, date)
    }

    fun deleteMeal(mealId: String) {
        repository.deleteMeal(mealId)
    }
}
