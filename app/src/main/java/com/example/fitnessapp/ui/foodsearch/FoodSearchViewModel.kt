package com.example.fitnessapp.ui.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.FoodItem
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.MealType
import com.example.fitnessapp.data.model.Micronutrients
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.nutrition.FoodItemDefinition
import com.example.fitnessapp.data.nutrition.FoodNutritionSearchService
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PlateItem(
    val food: FoodItemDefinition,
    val quantity: Float = 1.0f
) {
    val scaledFoodItem: FoodItem
        get() = food.scale(quantity)
}

data class PlateNutritionTotals(
    val calories: Int = 0,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val fiberG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val itemCount: Int = 0
)

class FoodSearchViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository,
    private val searchService: FoodNutritionSearchService = FitnessApplication.instance.foodNutritionSearchService
) : ViewModel() {

    val activeProfile: StateFlow<UserProfile> = repository.activeProfile

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FoodItemDefinition>>(emptyList())
    val searchResults: StateFlow<List<FoodItemDefinition>> = _searchResults.asStateFlow()

    private val _plateItems = MutableStateFlow<List<PlateItem>>(emptyList())
    val plateItems: StateFlow<List<PlateItem>> = _plateItems.asStateFlow()

    private val _selectedMealType = MutableStateFlow(determineDefaultMealType())
    val selectedMealType: StateFlow<MealType> = _selectedMealType.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val plateNutritionTotals: StateFlow<PlateNutritionTotals> = _plateItems.combine(_plateItems) { items, _ ->
        var cal = 0
        var pro = 0f
        var carbs = 0f
        var fat = 0f
        var fiber = 0f
        var sugar = 0f
        var sod = 0f
        var pot = 0f

        for (p in items) {
            val s = p.scaledFoodItem
            cal += s.calories
            pro += s.proteinG
            carbs += s.carbsG
            fat += s.fatG
            fiber += s.micronutrients.fiberG
            sugar += s.micronutrients.sugarG
            sod += s.micronutrients.sodiumMg
            pot += s.micronutrients.potassiumMg
        }

        PlateNutritionTotals(
            calories = cal,
            proteinG = ((pro * 10f).toInt() / 10f),
            carbsG = ((carbs * 10f).toInt() / 10f),
            fatG = ((fat * 10f).toInt() / 10f),
            fiberG = ((fiber * 10f).toInt() / 10f),
            sugarG = ((sugar * 10f).toInt() / 10f),
            sodiumMg = ((sod * 10f).toInt() / 10f),
            potassiumMg = ((pot * 10f).toInt() / 10f),
            itemCount = items.size
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlateNutritionTotals())

    init {
        refreshLocalSearch()
    }

    private fun determineDefaultMealType(): MealType {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> MealType.BREAKFAST
            in 11..15 -> MealType.LUNCH
            in 16..18 -> MealType.SNACK
            else -> MealType.DINNER
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        refreshLocalSearch()
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        refreshLocalSearch()
    }

    fun onMealTypeSelected(mealType: MealType) {
        _selectedMealType.value = mealType
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun refreshLocalSearch() {
        val results = searchService.searchLocal(_searchQuery.value, _selectedCategory.value)
        _searchResults.value = results
    }

    fun searchWithGeminiAi() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            _statusMessage.value = "Please type a food name to search online."
            return
        }
        val key = activeProfile.value.geminiApiKey
        if (key.isBlank()) {
            _statusMessage.value = "Gemini API key is required for online AI lookup. Enter it in Settings."
            return
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            _statusMessage.value = "Searching online nutrition database..."
            val result = searchService.searchOnlineWithGemini(query, key)
            _isAiLoading.value = false
            result.onSuccess { items ->
                if (items.isNotEmpty()) {
                    _statusMessage.value = "Found ${items.size} result(s) via Gemini AI."
                    refreshLocalSearch()
                } else {
                    _statusMessage.value = "No online results found for \"$query\"."
                }
            }.onFailure { err ->
                _statusMessage.value = "AI Search failed: ${err.localizedMessage ?: err.message}"
            }
        }
    }

    fun parseNaturalLanguageMeal(sentence: String) {
        val clean = sentence.trim()
        if (clean.isBlank()) return
        val key = activeProfile.value.geminiApiKey

        viewModelScope.launch {
            _isAiLoading.value = true
            _statusMessage.value = "Analyzing meal with AI..."
            val result = searchService.parseMealSentenceWithGemini(clean, key)
            _isAiLoading.value = false
            result.onSuccess { pairs ->
                if (pairs.isNotEmpty()) {
                    for ((food, qty) in pairs) {
                        addToPlate(food, qty)
                    }
                    _statusMessage.value = "Added ${pairs.size} item(s) to your plate!"
                    _searchQuery.value = ""
                    refreshLocalSearch()
                } else {
                    _statusMessage.value = "Could not identify distinct food items. Try searching by name."
                }
            }.onFailure { err ->
                _statusMessage.value = "Parse failed: ${err.message}"
            }
        }
    }

    fun addToPlate(food: FoodItemDefinition, quantity: Float = 1.0f) {
        val currentList = _plateItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.food.name.equals(food.name, ignoreCase = true) }
        if (index >= 0) {
            val existing = currentList[index]
            val newQty = existing.quantity + quantity
            currentList[index] = existing.copy(quantity = newQty)
        } else {
            currentList.add(PlateItem(food = food, quantity = quantity))
        }
        _plateItems.value = currentList
    }

    fun incrementPlateItem(foodId: String) {
        val currentList = _plateItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.food.id == foodId }
        if (index >= 0) {
            val item = currentList[index]
            val step = if (item.food.servingUnit.equals("gram", ignoreCase = true) || item.food.servingUnit.equals("g", ignoreCase = true)) 25f
            else if (item.food.servingUnit.equals("tbsp", ignoreCase = true) || item.food.servingUnit.equals("katori", ignoreCase = true) || item.food.servingUnit.equals("cup", ignoreCase = true)) 0.5f
            else 1.0f
            currentList[index] = item.copy(quantity = item.quantity + step)
            _plateItems.value = currentList
        }
    }

    fun decrementPlateItem(foodId: String) {
        val currentList = _plateItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.food.id == foodId }
        if (index >= 0) {
            val item = currentList[index]
            val step = if (item.food.servingUnit.equals("gram", ignoreCase = true) || item.food.servingUnit.equals("g", ignoreCase = true)) 25f
            else if (item.food.servingUnit.equals("tbsp", ignoreCase = true) || item.food.servingUnit.equals("katori", ignoreCase = true) || item.food.servingUnit.equals("cup", ignoreCase = true)) 0.5f
            else 1.0f
            val newQty = item.quantity - step
            if (newQty <= 0f) {
                currentList.removeAt(index)
            } else {
                currentList[index] = item.copy(quantity = newQty)
            }
            _plateItems.value = currentList
        }
    }

    fun updatePlateItemQuantity(foodId: String, newQuantity: Float) {
        val currentList = _plateItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.food.id == foodId }
        if (index >= 0) {
            if (newQuantity <= 0f) {
                currentList.removeAt(index)
            } else {
                currentList[index] = currentList[index].copy(quantity = newQuantity)
            }
            _plateItems.value = currentList
        }
    }

    fun removeFromPlate(foodId: String) {
        val currentList = _plateItems.value.toMutableList()
        currentList.removeAll { it.food.id == foodId }
        _plateItems.value = currentList
    }

    fun clearPlate() {
        _plateItems.value = emptyList()
    }

    fun logPlateAsMeal(customTitle: String? = null, onCompleted: (itemCount: Int, calories: Int) -> Unit) {
        val items = _plateItems.value
        if (items.isEmpty()) {
            _statusMessage.value = "Your plate is empty! Add foods before logging."
            return
        }

        val count = items.size
        val totals = plateNutritionTotals.value
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val profile = activeProfile.value

        val mealTitle = if (!customTitle.isNullOrBlank()) {
            customTitle.trim()
        } else {
            // E.g. "2 Rotis, 1 Dal Tadka"
            items.take(3).joinToString(", ") {
                val qtyStr = if (it.quantity % 1f == 0f) it.quantity.toInt().toString() else "%.1f".format(it.quantity)
                "$qtyStr ${it.food.name}"
            } + if (items.size > 3) " + ${items.size - 3} more" else ""
        }

        val scaledFoodItems = items.map { it.scaledFoodItem }

        val mealLog = MealLog(
            id = UUID.randomUUID().toString(),
            userId = profile.id,
            date = todayStr,
            timestamp = System.currentTimeMillis(),
            mealType = _selectedMealType.value,
            title = mealTitle,
            calories = totals.calories,
            proteinG = totals.proteinG,
            carbsG = totals.carbsG,
            fatG = totals.fatG,
            micronutrients = Micronutrients(
                fiberG = totals.fiberG,
                sugarG = totals.sugarG,
                sodiumMg = totals.sodiumMg,
                potassiumMg = totals.potassiumMg
            ),
            items = scaledFoodItems,
            aiSource = if (items.any { it.food.isCustomOrAi }) "AI_CUSTOM" else "DATABASE_SEARCH",
            aiInsights = "Logged from Food Search & Plate Builder."
        )

        repository.addMeal(mealLog)
        clearPlate()
        onCompleted(count, totals.calories)
    }
}
