package com.example.fitnessapp.ui.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.FoodItem
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.MealType
import com.example.fitnessapp.data.model.Micronutrients
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.nutrition.FoodDatabase
import com.example.fitnessapp.data.nutrition.FoodItemDefinition
import com.example.fitnessapp.data.nutrition.FoodNutritionSearchService
import com.example.fitnessapp.data.nutrition.SavedMealCombo
import com.example.fitnessapp.data.nutrition.SavedComboItem
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    val plateNutritionTotals: StateFlow<PlateNutritionTotals> = _plateItems.map { items ->
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

    private val _savedCombos = MutableStateFlow(searchService.getSavedCombos())
    val savedCombos: StateFlow<List<SavedMealCombo>> = _savedCombos.asStateFlow()

    private fun getYesterdayDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    val yesterdayMealsForSelectedType: StateFlow<List<MealLog>> = combine(
        repository.allMeals,
        _selectedMealType,
        activeProfile
    ) { meals, mealType, profile ->
        val yesterday = getYesterdayDate()
        meals.filter { it.userId == profile.id && it.date == yesterday && it.mealType == mealType }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentFoods: StateFlow<List<FoodItemDefinition>> = combine(
        repository.allMeals,
        activeProfile
    ) { all, profile ->
        val userMeals = all.filter { it.userId == profile.id }.take(30)
        val seenNames = mutableSetOf<String>()
        val result = mutableListOf<FoodItemDefinition>()

        for (meal in userMeals) {
            for (item in meal.items) {
                val lower = item.name.lowercase().trim()
                if (!seenNames.contains(lower)) {
                    seenNames.add(lower)
                    val found = FoodDatabase.preloadedFoods.find { it.name.equals(item.name, ignoreCase = true) }
                    if (found != null) {
                        result.add(found)
                    } else {
                        val portion = item.portionDescription.ifBlank { "1 serving" }
                        val cleanId = "recent_${item.name.lowercase().trim().replace(Regex("[^a-z0-9]"), "_")}"
                        result.add(
                            FoodItemDefinition(
                                id = cleanId,
                                name = item.name,
                                category = "Recent",
                                servingUnit = portion,
                                servingSizeDescription = portion,
                                baseQuantity = 1.0f,
                                calories = item.calories,
                                proteinG = item.proteinG,
                                carbsG = item.carbsG,
                                fatG = item.fatG,
                                fiberG = item.micronutrients.fiberG,
                                sugarG = item.micronutrients.sugarG,
                                sodiumMg = item.micronutrients.sodiumMg,
                                potassiumMg = item.micronutrients.potassiumMg,
                                isCustomOrAi = true
                            )
                        )
                    }
                }
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
        val category = _selectedCategory.value
        val query = _searchQuery.value
        if (category.contains("Recent", ignoreCase = true)) {
            val recents = recentFoods.value
            _searchResults.value = if (query.isBlank()) recents else recents.filter { it.name.contains(query, ignoreCase = true) }
        } else if (category.contains("Combos", ignoreCase = true)) {
            _searchResults.value = emptyList()
        } else {
            val results = searchService.searchLocal(query, category)
            _searchResults.value = results
        }
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
        val index = currentList.indexOfFirst {
            it.food.id == food.id || it.food.name.equals(food.name, ignoreCase = true)
        }
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
        val index = currentList.indexOfFirst { it.food.id == foodId || it.food.name.equals(foodId, ignoreCase = true) }
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
        val index = currentList.indexOfFirst { it.food.id == foodId || it.food.name.equals(foodId, ignoreCase = true) }
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
        val index = currentList.indexOfFirst { it.food.id == foodId || it.food.name.equals(foodId, ignoreCase = true) }
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
        currentList.removeAll { it.food.id == foodId || it.food.name.equals(foodId, ignoreCase = true) }
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

    fun saveCurrentPlateAsCombo(name: String) {
        val items = _plateItems.value
        if (items.isEmpty()) {
            _statusMessage.value = "Add foods to your plate before saving as a combo."
            return
        }
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _statusMessage.value = "Please enter a name for this combo."
            return
        }
        val totals = plateNutritionTotals.value
        val comboItems = items.map {
            val scaled = it.scaledFoodItem
            SavedComboItem(
                foodId = it.food.id,
                foodName = it.food.name,
                quantity = it.quantity,
                servingUnit = it.food.servingUnit,
                calories = scaled.calories,
                proteinG = scaled.proteinG,
                carbsG = scaled.carbsG,
                fatG = scaled.fatG,
                fiberG = scaled.micronutrients.fiberG,
                sugarG = scaled.micronutrients.sugarG,
                sodiumMg = scaled.micronutrients.sodiumMg,
                potassiumMg = scaled.micronutrients.potassiumMg,
                category = it.food.category
            )
        }
        val combo = SavedMealCombo(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            items = comboItems,
            totalCalories = totals.calories,
            totalProtein = totals.proteinG,
            totalCarbs = totals.carbsG,
            totalFat = totals.fatG
        )
        searchService.saveMealCombo(combo)
        _savedCombos.value = searchService.getSavedCombos()
        _statusMessage.value = "Saved \"$cleanName\" as a meal combo!"
    }

    fun addComboToPlate(combo: SavedMealCombo) {
        for (item in combo.items) {
            val def = item.toFoodItemDefinition()
            addToPlate(def, item.quantity)
        }
        _statusMessage.value = "Added \"${combo.name}\" to your plate!"
    }

    fun deleteCombo(comboId: String) {
        searchService.deleteMealCombo(comboId)
        _savedCombos.value = searchService.getSavedCombos()
        _statusMessage.value = "Meal combo removed."
    }

    fun copyYesterdayMealsToPlate() {
        val yesterdayMeals = yesterdayMealsForSelectedType.value
        if (yesterdayMeals.isEmpty()) {
            _statusMessage.value = "No meals found from yesterday for ${_selectedMealType.value.name.lowercase().replaceFirstChar { it.uppercase() }}."
            return
        }
        var totalAdded = 0
        for (meal in yesterdayMeals) {
            for (foodItem in meal.items) {
                val portion = foodItem.portionDescription.ifBlank { "1 serving" }
                val def = FoodDatabase.preloadedFoods.find { it.name.equals(foodItem.name, ignoreCase = true) }
                    ?: FoodItemDefinition(
                        id = UUID.randomUUID().toString(),
                        name = foodItem.name,
                        category = "Other",
                        servingUnit = portion,
                        servingSizeDescription = portion,
                        baseQuantity = 1.0f,
                        calories = foodItem.calories,
                        proteinG = foodItem.proteinG,
                        carbsG = foodItem.carbsG,
                        fatG = foodItem.fatG,
                        fiberG = foodItem.micronutrients.fiberG,
                        sugarG = foodItem.micronutrients.sugarG,
                        sodiumMg = foodItem.micronutrients.sodiumMg,
                        potassiumMg = foodItem.micronutrients.potassiumMg,
                        isCustomOrAi = true
                    )
                addToPlate(def, 1.0f)
                totalAdded++
            }
        }
        _statusMessage.value = "Copied $totalAdded item(s) from yesterday into your plate!"
    }
}
