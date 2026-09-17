package com.example.fitnessapp.data.repository

import android.content.Context
import com.example.fitnessapp.data.model.ActivityLevel
import com.example.fitnessapp.data.model.BodyPhoto
import com.example.fitnessapp.data.model.BodyPose
import com.example.fitnessapp.data.model.DailyNutritionSummary
import com.example.fitnessapp.data.model.DeficitLevel
import com.example.fitnessapp.data.model.FastingProtocol
import com.example.fitnessapp.data.model.FastingState
import com.example.fitnessapp.data.model.Gender
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.Micronutrients
import com.example.fitnessapp.data.model.PartnerDuelSummary
import com.example.fitnessapp.data.model.UserDailyScore
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.model.WeightLog
import com.example.fitnessapp.domain.BmiCalculator
import com.example.fitnessapp.domain.NutritionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface FitnessRepository {
    val activeProfile: StateFlow<UserProfile>
    val partnerProfile: StateFlow<UserProfile>
    val todaySummary: StateFlow<DailyNutritionSummary>
    val todayMeals: StateFlow<List<MealLog>>
    val allMeals: StateFlow<List<MealLog>>
    val weightHistory: StateFlow<List<WeightLog>>
    val bodyPhotos: StateFlow<List<BodyPhoto>>
    val partnerDuel: StateFlow<PartnerDuelSummary>
    val pairCode: StateFlow<String>
    val incomingCheer: StateFlow<String?>
    val hasCompletedOnboarding: StateFlow<Boolean>
    val fastingState: StateFlow<FastingState>

    fun switchActiveProfile(userId: String)
    fun updateProfile(profile: UserProfile)
    fun addMeal(meal: MealLog)
    fun deleteMeal(mealId: String)
    fun getMealsForDate(date: String): List<MealLog>
    fun getSummaryForDate(date: String): DailyNutritionSummary
    fun addWaterForDate(amountMl: Int, date: String)
    fun addWeightLog(weightKg: Float, notes: String = "")
    fun updateStartingWeight(weightKg: Float)
    fun addBodyPhoto(photoPath: String, pose: BodyPose, weightKg: Float, notes: String = "")
    fun addWater(amountMl: Int)
    fun resetWater()
    fun setGeminiApiKey(apiKey: String)
    fun setPinLock(pin: String)
    fun setPairCode(code: String)
    fun updateSyncedPartnerScore(score: UserDailyScore)
    fun getCurrentUserDailyScore(): UserDailyScore
    fun receiveIncomingCheer(senderName: String, message: String)
    fun clearIncomingCheer()
    fun setOnboardingCompleted(completed: Boolean)
    fun setOnDataChangedListener(listener: () -> Unit)
    fun startFast(targetHours: Int = 16, startTimeMs: Long = System.currentTimeMillis())
    fun endFast()
    fun updateFastingTarget(targetHours: Int)
}

class AppFitnessRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : FitnessRepository {

    private val prefs = context.getSharedPreferences("fitness_app_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var activeUserId: String = prefs.getString("active_user_id", "primary") ?: "primary"

    private val _primaryProfile = MutableStateFlow(loadProfile("primary", defaultPrimaryProfile()))
    private val _partnerProfile = MutableStateFlow(loadProfile("partner", defaultPartnerProfile()))

    private val _activeProfile = MutableStateFlow(
        if (activeUserId == "primary") _primaryProfile.value else _partnerProfile.value
    )
    override val activeProfile: StateFlow<UserProfile> = _activeProfile.asStateFlow()
    override val partnerProfile: StateFlow<UserProfile> = _partnerProfile.asStateFlow()

    private val _meals = MutableStateFlow(loadMeals())
    override val allMeals: StateFlow<List<MealLog>> = _meals.asStateFlow()
    private val _todayMeals = MutableStateFlow<List<MealLog>>(emptyList())
    override val todayMeals: StateFlow<List<MealLog>> = _todayMeals.asStateFlow()

    private val _weightLogs = MutableStateFlow(loadWeightLogs())
    override val weightHistory: StateFlow<List<WeightLog>> = _weightLogs.asStateFlow()

    private val _bodyPhotos = MutableStateFlow(loadBodyPhotos())
    override val bodyPhotos: StateFlow<List<BodyPhoto>> = _bodyPhotos.asStateFlow()

    private val _todayWaterMap = MutableStateFlow(loadWater())
    private val _todaySummary = MutableStateFlow(createInitialSummary())
    override val todaySummary: StateFlow<DailyNutritionSummary> = _todaySummary.asStateFlow()

    private var onDataChangedListener: (() -> Unit)? = null
    private val _pairCode = MutableStateFlow(prefs.getString("partner_pair_code", "FIT-8842") ?: "FIT-8842")
    override val pairCode: StateFlow<String> = _pairCode.asStateFlow()

    private fun loadSyncedPartnerScore(): UserDailyScore? {
        val raw = prefs.getString("synced_partner_score", null) ?: return null
        return try {
            json.decodeFromString<UserDailyScore>(raw)
        } catch (e: Exception) {
            null
        }
    }

    private val _syncedPartnerScore = MutableStateFlow<UserDailyScore?>(loadSyncedPartnerScore())
    private val _incomingCheer = MutableStateFlow<String?>(null)
    override val incomingCheer: StateFlow<String?> = _incomingCheer.asStateFlow()

    private val _partnerDuel = MutableStateFlow(createDuelSummary())
    override val partnerDuel: StateFlow<PartnerDuelSummary> = _partnerDuel.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    override val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private fun loadFastingState(): FastingState {
        val raw = prefs.getString("fasting_state_${activeUserId}", null) ?: return FastingState()
        return try {
            json.decodeFromString<FastingState>(raw)
        } catch (e: Exception) {
            FastingState()
        }
    }

    private val _fastingState = MutableStateFlow(loadFastingState())
    override val fastingState: StateFlow<FastingState> = _fastingState.asStateFlow()

    init {
        recalculateToday()
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun switchActiveProfile(userId: String) {
        activeUserId = userId
        prefs.edit().putString("active_user_id", userId).apply()
        val target = if (userId == "primary") _primaryProfile.value else _partnerProfile.value
        val deviceKey = prefs.getString("device_gemini_api_key", "") ?: ""
        _activeProfile.value = if (target.geminiApiKey.isBlank() && deviceKey.isNotBlank()) {
            target.copy(geminiApiKey = deviceKey)
        } else {
            target
        }
        _fastingState.value = loadFastingState()
        recalculateToday()
    }

    override fun updateProfile(profile: UserProfile) {
        val cleanKey = profile.geminiApiKey.trim()
        if (cleanKey.isNotBlank()) {
            prefs.edit().putString("device_gemini_api_key", cleanKey).apply()
        }
        if (profile.id == "primary") {
            _primaryProfile.value = profile
            saveProfile("primary", profile)
            if (_partnerProfile.value.geminiApiKey.isBlank() && cleanKey.isNotBlank()) {
                val updatedPartner = _partnerProfile.value.copy(geminiApiKey = cleanKey)
                _partnerProfile.value = updatedPartner
                saveProfile("partner", updatedPartner)
            }
        } else {
            _partnerProfile.value = profile
            saveProfile("partner", profile)
            if (_primaryProfile.value.geminiApiKey.isBlank() && cleanKey.isNotBlank()) {
                val updatedPrimary = _primaryProfile.value.copy(geminiApiKey = cleanKey)
                _primaryProfile.value = updatedPrimary
                saveProfile("primary", updatedPrimary)
            }
        }
        if (profile.id == activeUserId) {
            _activeProfile.value = profile
        }
        recalculateToday()
    }

    override fun addMeal(meal: MealLog) {
        val current = _meals.value.toMutableList()
        current.add(0, meal)
        _meals.value = current
        saveMeals(current)
        recalculateToday()
        onDataChangedListener?.invoke()
    }

    override fun deleteMeal(mealId: String) {
        val current = _meals.value.toMutableList()
        current.removeAll { it.id == mealId }
        _meals.value = current
        saveMeals(current)
        recalculateToday()
        onDataChangedListener?.invoke()
    }

    override fun getMealsForDate(date: String): List<MealLog> {
        val currentProfile = _activeProfile.value
        return _meals.value.filter { it.userId == currentProfile.id && it.date == date }
    }

    override fun getSummaryForDate(date: String): DailyNutritionSummary {
        val currentProfile = _activeProfile.value
        val rec = NutritionEngine.calculateRecommendations(currentProfile)
        val userMeals = _meals.value.filter { it.userId == currentProfile.id && it.date == date }

        var totalCalories = 0
        var totalProtein = 0f
        var totalCarbs = 0f
        var totalFat = 0f
        var totalMicros = Micronutrients.ZERO

        for (m in userMeals) {
            totalCalories += m.calories
            totalProtein += m.proteinG
            totalCarbs += m.carbsG
            totalFat += m.fatG
            totalMicros = totalMicros + m.micronutrients
        }

        val waterMl = if (date == getTodayDate()) {
            _todayWaterMap.value[activeUserId] ?: 0
        } else {
            val str = prefs.getString("water_$date", null)
            val map: Map<String, Int> = if (str != null) {
                try { json.decodeFromString(str) } catch (e: Exception) { emptyMap() }
            } else emptyMap()
            map[activeUserId] ?: 0
        }

        return DailyNutritionSummary(
            date = date,
            calorieBudget = rec.dailyCalorieBudget,
            caloriesConsumed = totalCalories,
            waterIntakeMl = waterMl,
            waterTargetMl = rec.dailyWaterMl,
            proteinConsumedG = totalProtein,
            proteinTargetG = rec.proteinGrams,
            carbsConsumedG = totalCarbs,
            carbsTargetG = rec.carbsGrams,
            fatConsumedG = totalFat,
            fatTargetG = rec.fatGrams,
            micronutrients = totalMicros
        )
    }

    override fun addWaterForDate(amountMl: Int, date: String) {
        val str = prefs.getString("water_$date", null)
        val map = if (str != null) {
            try { json.decodeFromString<Map<String, Int>>(str).toMutableMap() } catch (e: Exception) { mutableMapOf() }
        } else mutableMapOf()
        val current = map[activeUserId] ?: 0
        map[activeUserId] = (current + amountMl).coerceAtLeast(0)
        prefs.edit().putString("water_$date", json.encodeToString(map)).apply()
        if (date == getTodayDate()) {
            _todayWaterMap.value = map
        }
        recalculateToday()
        onDataChangedListener?.invoke()
    }

    override fun addWeightLog(weightKg: Float, notes: String) {
        val today = getTodayDate()
        val currentProfile = _activeProfile.value
        val bmi = BmiCalculator.calculateBmi(weightKg, currentProfile.heightCm)
        val log = WeightLog(
            userId = currentProfile.id,
            date = today,
            weightKg = weightKg,
            bmi = bmi,
            notes = notes
        )
        val list = _weightLogs.value.toMutableList()
        list.removeAll { it.userId == currentProfile.id && it.date == today }
        list.add(0, log)
        _weightLogs.value = list
        saveWeightLogs(list)

        // Update current weight in profile as well
        updateProfile(currentProfile.copy(currentWeightKg = weightKg))
        onDataChangedListener?.invoke()
    }

    override fun updateStartingWeight(weightKg: Float) {
        val currentProfile = _activeProfile.value
        val updatedProfile = currentProfile.copy(startWeightKg = weightKg)
        updateProfile(updatedProfile)

        // Also update or add the earliest "Starting weight" log
        val list = _weightLogs.value.toMutableList()
        val userLogs = list.filter { it.userId == currentProfile.id }
        val startLog = userLogs.find { it.notes.contains("Starting", ignoreCase = true) } ?: userLogs.lastOrNull()

        val bmi = BmiCalculator.calculateBmi(weightKg, currentProfile.heightCm)
        if (startLog != null) {
            val idx = list.indexOf(startLog)
            if (idx >= 0) {
                list[idx] = startLog.copy(weightKg = weightKg, bmi = bmi, notes = "Starting weight")
            }
        } else {
            list.add(
                WeightLog(
                    userId = currentProfile.id,
                    date = getTodayDate(),
                    weightKg = weightKg,
                    bmi = bmi,
                    notes = "Starting weight"
                )
            )
        }
        _weightLogs.value = list
        saveWeightLogs(list)
        onDataChangedListener?.invoke()
    }

    override fun addBodyPhoto(photoPath: String, pose: BodyPose, weightKg: Float, notes: String) {
        val today = getTodayDate()
        val currentProfile = _activeProfile.value
        val photo = BodyPhoto(
            userId = currentProfile.id,
            date = today,
            photoPath = photoPath,
            pose = pose,
            weightKg = weightKg,
            notes = notes
        )
        val list = _bodyPhotos.value.toMutableList()
        list.add(0, photo)
        _bodyPhotos.value = list
        saveBodyPhotos(list)
    }

    override fun addWater(amountMl: Int) {
        val map = _todayWaterMap.value.toMutableMap()
        val current = map[activeUserId] ?: 0
        map[activeUserId] = (current + amountMl).coerceAtLeast(0)
        _todayWaterMap.value = map
        saveWater(map)
        recalculateToday()
        onDataChangedListener?.invoke()
    }

    override fun resetWater() {
        val map = _todayWaterMap.value.toMutableMap()
        map[activeUserId] = 0
        _todayWaterMap.value = map
        saveWater(map)
        recalculateToday()
        onDataChangedListener?.invoke()
    }

    override fun setGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("device_gemini_api_key", trimmed).apply()
        }
        val updated = _activeProfile.value.copy(geminiApiKey = trimmed)
        updateProfile(updated)
    }

    override fun setPinLock(pin: String) {
        val updated = _activeProfile.value.copy(pinLock = pin)
        updateProfile(updated)
    }

    private fun recalculateToday() {
        val today = getTodayDate()
        val currentProfile = _activeProfile.value
        val rec = NutritionEngine.calculateRecommendations(currentProfile)

        val userTodayMeals = _meals.value.filter { it.userId == currentProfile.id && it.date == today }
        _todayMeals.value = userTodayMeals

        var totalCalories = 0
        var totalProtein = 0f
        var totalCarbs = 0f
        var totalFat = 0f
        var totalMicros = Micronutrients.ZERO

        for (m in userTodayMeals) {
            totalCalories += m.calories
            totalProtein += m.proteinG
            totalCarbs += m.carbsG
            totalFat += m.fatG
            totalMicros = totalMicros + m.micronutrients
        }

        val waterMl = _todayWaterMap.value[activeUserId] ?: 0

        _todaySummary.value = DailyNutritionSummary(
            date = today,
            calorieBudget = rec.dailyCalorieBudget,
            caloriesConsumed = totalCalories,
            waterIntakeMl = waterMl,
            waterTargetMl = rec.dailyWaterMl,
            proteinConsumedG = totalProtein,
            proteinTargetG = rec.proteinGrams,
            carbsConsumedG = totalCarbs,
            carbsTargetG = rec.carbsGrams,
            fatConsumedG = totalFat,
            fatTargetG = rec.fatGrams,
            micronutrients = totalMicros
        )

        _partnerDuel.value = createDuelSummary()
    }

    private fun createDuelSummary(): PartnerDuelSummary {
        val today = getTodayDate()
        val user1Score = getCurrentUserDailyScore()

        val syncedScore = _syncedPartnerScore.value
        val isSynced = syncedScore != null

        val partnerScore = syncedScore ?: run {
            val p2 = _partnerProfile.value
            val p2Rec = NutritionEngine.calculateRecommendations(p2)
            val p2InitialWeight = if (p2.startWeightKg > 0f) p2.startWeightKg else p2.currentWeightKg
            UserDailyScore(
                userId = p2.id,
                userName = p2.name.ifBlank { "Partner" },
                calorieBudget = p2Rec.dailyCalorieBudget,
                caloriesConsumed = 0,
                waterIntakeMl = 0,
                waterTargetMl = p2Rec.dailyWaterMl,
                currentWeightKg = p2.currentWeightKg,
                weightLostKg = (p2InitialWeight - p2.currentWeightKg).coerceAtLeast(0f),
                streakDays = 0,
                proteinConsumedG = 0f,
                proteinTargetG = p2Rec.proteinGrams,
                carbsConsumedG = 0f,
                carbsTargetG = p2Rec.carbsGrams,
                fatConsumedG = 0f,
                fatTargetG = p2Rec.fatGrams,
                targetWeightKg = p2.targetWeightKg,
                startWeightKg = p2InitialWeight,
                isLiveSynced = false,
                lastSyncTimestamp = 0L
            )
        }

        return PartnerDuelSummary(
            primaryUser = user1Score,
            partnerUser = partnerScore,
            date = today,
            isPartnerSynced = isSynced
        )
    }

    override fun setPairCode(code: String) {
        val sanitized = code.trim().uppercase()
        _pairCode.value = sanitized
        prefs.edit().putString("partner_pair_code", sanitized).apply()
    }

    override fun updateSyncedPartnerScore(score: UserDailyScore) {
        val updated = score.copy(
            isLiveSynced = true,
            lastSyncTimestamp = if (score.lastSyncTimestamp > 0L) score.lastSyncTimestamp else System.currentTimeMillis()
        )
        _syncedPartnerScore.value = updated
        try {
            prefs.edit().putString("synced_partner_score", json.encodeToString(updated)).apply()
        } catch (e: Exception) {
            // ignore
        }
        recalculateToday()
    }

    override fun getCurrentUserDailyScore(): UserDailyScore {
        val currentProfile = _activeProfile.value
        val today = getTodayDate()
        val rec = NutritionEngine.calculateRecommendations(currentProfile)
        val userMeals = _meals.value.filter { it.userId == currentProfile.id && it.date == today }
        val userWater = _todayWaterMap.value[currentProfile.id] ?: 0
        val initialWeight = if (currentProfile.startWeightKg > 0f) currentProfile.startWeightKg else currentProfile.currentWeightKg

        val totalProtein = userMeals.sumOf { it.proteinG.toDouble() }.toFloat()
        val totalCarbs = userMeals.sumOf { it.carbsG.toDouble() }.toFloat()
        val totalFat = userMeals.sumOf { it.fatG.toDouble() }.toFloat()

        val steps = try {
            com.example.fitnessapp.FitnessApplication.instance.stepTrackerManager.todaySteps.value
        } catch (e: Exception) {
            0
        }
        val burned = try {
            com.example.fitnessapp.FitnessApplication.instance.stepTrackerManager.caloriesBurned.value
        } catch (e: Exception) {
            0
        }

        return UserDailyScore(
            userId = currentProfile.id,
            userName = currentProfile.name,
            calorieBudget = rec.dailyCalorieBudget,
            caloriesConsumed = userMeals.sumOf { it.calories },
            waterIntakeMl = userWater,
            waterTargetMl = rec.dailyWaterMl,
            currentWeightKg = currentProfile.currentWeightKg,
            weightLostKg = (initialWeight - currentProfile.currentWeightKg).coerceAtLeast(0f),
            streakDays = 5,
            proteinConsumedG = totalProtein,
            proteinTargetG = rec.proteinGrams,
            carbsConsumedG = totalCarbs,
            carbsTargetG = rec.carbsGrams,
            fatConsumedG = totalFat,
            fatTargetG = rec.fatGrams,
            targetWeightKg = currentProfile.targetWeightKg,
            startWeightKg = initialWeight,
            stepsTaken = steps,
            stepsTarget = 10000,
            caloriesBurned = burned,
            isLiveSynced = true,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    override fun receiveIncomingCheer(senderName: String, message: String) {
        _incomingCheer.value = "$senderName: $message"
    }

    override fun clearIncomingCheer() {
        _incomingCheer.value = null
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        _hasCompletedOnboarding.value = completed
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }

    override fun setOnDataChangedListener(listener: () -> Unit) {
        this.onDataChangedListener = listener
    }

    override fun startFast(targetHours: Int, startTimeMs: Long) {
        val protocol = when (targetHours) {
            14 -> FastingProtocol.GENTLE
            16 -> FastingProtocol.LEAN_GAINS
            18 -> FastingProtocol.INTENSE
            20 -> FastingProtocol.WARRIOR
            else -> FastingProtocol.CUSTOM
        }
        val newState = FastingState(
            isFasting = true,
            startTimeMs = startTimeMs,
            targetHours = targetHours,
            protocol = protocol
        )
        _fastingState.value = newState
        prefs.edit().putString("fasting_state_${activeUserId}", json.encodeToString(newState)).apply()
        onDataChangedListener?.invoke()
    }

    override fun endFast() {
        val current = _fastingState.value
        val newState = current.copy(isFasting = false)
        _fastingState.value = newState
        prefs.edit().putString("fasting_state_${activeUserId}", json.encodeToString(newState)).apply()
        onDataChangedListener?.invoke()
    }

    override fun updateFastingTarget(targetHours: Int) {
        val current = _fastingState.value
        val newState = current.copy(targetHours = targetHours)
        _fastingState.value = newState
        prefs.edit().putString("fasting_state_${activeUserId}", json.encodeToString(newState)).apply()
        onDataChangedListener?.invoke()
    }

    private fun createInitialSummary(): DailyNutritionSummary {
        val current = _activeProfile.value
        val rec = NutritionEngine.calculateRecommendations(current)
        return DailyNutritionSummary(
            date = getTodayDate(),
            calorieBudget = rec.dailyCalorieBudget,
            caloriesConsumed = 0,
            waterIntakeMl = 0,
            waterTargetMl = rec.dailyWaterMl,
            proteinTargetG = rec.proteinGrams,
            carbsTargetG = rec.carbsGrams,
            fatTargetG = rec.fatGrams
        )
    }

    private fun defaultPrimaryProfile(): UserProfile {
        return UserProfile(
            id = "primary",
            name = "Pramod",
            gender = Gender.MALE,
            age = 32,
            heightCm = 175f,
            startWeightKg = 85f,
            currentWeightKg = 85f,
            targetWeightKg = 72f,
            activityLevel = ActivityLevel.MODERATE,
            deficitLevel = DeficitLevel.MODERATE,
            customDeficitKcal = 500,
            dailyWaterTargetMl = 3000
        )
    }

    private fun defaultPartnerProfile(): UserProfile {
        return UserProfile(
            id = "partner",
            name = "Wife",
            gender = Gender.FEMALE,
            age = 30,
            heightCm = 165f,
            startWeightKg = 65f,
            currentWeightKg = 65f,
            targetWeightKg = 57f,
            activityLevel = ActivityLevel.LIGHT,
            deficitLevel = DeficitLevel.MODERATE,
            customDeficitKcal = 400,
            dailyWaterTargetMl = 2500
        )
    }

    private fun saveProfile(key: String, profile: UserProfile) {
        val cleanKey = profile.geminiApiKey.trim()
        if (cleanKey.isNotBlank()) {
            prefs.edit().putString("device_gemini_api_key", cleanKey).apply()
        }
        prefs.edit().putString("profile_$key", json.encodeToString(profile)).apply()
    }

    private fun loadProfile(key: String, default: UserProfile): UserProfile {
        val deviceKey = prefs.getString("device_gemini_api_key", "") ?: ""
        val str = prefs.getString("profile_$key", null)
        val loaded = if (str != null) {
            try {
                val p: UserProfile = json.decodeFromString(str)
                if (p.startWeightKg <= 0f) p.copy(startWeightKg = p.currentWeightKg) else p
            } catch (e: Exception) {
                default
            }
        } else {
            default
        }
        return if (loaded.geminiApiKey.isBlank() && deviceKey.isNotBlank()) {
            loaded.copy(geminiApiKey = deviceKey)
        } else {
            loaded
        }
    }

    private fun saveMeals(meals: List<MealLog>) {
        prefs.edit().putString("saved_meals", json.encodeToString(meals)).apply()
    }

    private fun loadMeals(): List<MealLog> {
        val str = prefs.getString("saved_meals", null) ?: return emptyList()
        return try {
            json.decodeFromString(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveWeightLogs(logs: List<WeightLog>) {
        prefs.edit().putString("saved_weight_logs", json.encodeToString(logs)).apply()
    }

    private fun loadWeightLogs(): List<WeightLog> {
        val str = prefs.getString("saved_weight_logs", null)
        if (str.isNullOrBlank()) {
            val today = getTodayDate()
            return listOf(
                WeightLog(userId = "primary", date = today, weightKg = 85f, bmi = 27.8f, notes = "Starting weight"),
                WeightLog(userId = "partner", date = today, weightKg = 65f, bmi = 23.9f, notes = "Starting weight")
            )
        }
        return try {
            json.decodeFromString(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveBodyPhotos(photos: List<BodyPhoto>) {
        prefs.edit().putString("saved_body_photos", json.encodeToString(photos)).apply()
    }

    private fun loadBodyPhotos(): List<BodyPhoto> {
        val str = prefs.getString("saved_body_photos", null) ?: return emptyList()
        return try {
            json.decodeFromString(str)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveWater(map: Map<String, Int>) {
        prefs.edit().putString("water_${getTodayDate()}", json.encodeToString(map)).apply()
    }

    private fun loadWater(): Map<String, Int> {
        val str = prefs.getString("water_${getTodayDate()}", null) ?: return emptyMap()
        return try {
            json.decodeFromString(str)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
