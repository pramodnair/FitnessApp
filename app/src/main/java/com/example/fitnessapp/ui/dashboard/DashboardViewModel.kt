package com.example.fitnessapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.DailyNutritionSummary
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.MealType
import com.example.fitnessapp.data.model.PartnerDuelSummary
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.model.WeeklyNutritionSummary
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val fastingState: StateFlow<com.example.fitnessapp.data.model.FastingState> = repository.fastingState

    fun startFast(targetHours: Int = 16, startTimeMs: Long = System.currentTimeMillis()) {
        repository.startFast(targetHours, startTimeMs)
    }

    fun endFast() {
        repository.endFast()
    }

    fun updateFastingTarget(targetHours: Int) {
        repository.updateFastingTarget(targetHours)
    }

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

    val weeklySummary: StateFlow<WeeklyNutritionSummary> = combine(
        repository.todaySummary,
        _selectedDate
    ) { _, date ->
        repository.getWeeklyNutritionSummary(date)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.getWeeklyNutritionSummary()
    )

    val loggingStreak: StateFlow<Int> = repository.todaySummary.map {
        repository.calculateLoggingStreak()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.calculateLoggingStreak()
    )

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

    fun repeatMealToToday(meal: MealLog) {
        repository.repeatMealToday(meal)
    }

    fun logQuickCalories(calories: Int, mealType: MealType, note: String = "Quick Add") {
        val date = _selectedDate.value
        val title = if (note.isNotBlank() && note != "Quick Add") note else "Quick Add (${calories} kcal)"
        val meal = MealLog(
            id = UUID.randomUUID().toString(),
            userId = repository.activeProfile.value.id,
            title = title,
            calories = calories,
            proteinG = 0f,
            carbsG = 0f,
            fatG = 0f,
            mealType = mealType,
            date = date,
            timestamp = System.currentTimeMillis()
        )
        repository.addMeal(meal)
    }
}
