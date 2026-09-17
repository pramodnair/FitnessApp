package com.example.fitnessapp.data.nutrition

import android.content.Context
import android.util.Log
import com.example.fitnessapp.data.network.GeminiVisionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class FoodNutritionSearchService(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val prefs = context.getSharedPreferences("custom_foods_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val cachedCustomFoods = mutableListOf<FoodItemDefinition>()
    private val savedCombos = mutableListOf<SavedMealCombo>()

    init {
        loadCachedFoods()
        loadSavedCombos()
    }

    private fun loadSavedCombos() {
        try {
            val jsonStr = prefs.getString("saved_meal_combos", null)
            if (!jsonStr.isNullOrBlank()) {
                val list = json.decodeFromString<List<SavedMealCombo>>(jsonStr)
                savedCombos.clear()
                savedCombos.addAll(list)
            }
        } catch (e: Exception) {
            Log.e("FoodSearchService", "Error loading saved combos: ${e.message}")
        }
    }

    fun getSavedCombos(): List<SavedMealCombo> = synchronized(savedCombos) { savedCombos.toList() }

    @Synchronized
    fun saveMealCombo(combo: SavedMealCombo) {
        savedCombos.removeAll { it.id == combo.id || it.name.equals(combo.name, ignoreCase = true) }
        savedCombos.add(0, combo)
        try {
            val serialized = json.encodeToString(savedCombos)
            prefs.edit().putString("saved_meal_combos", serialized).apply()
        } catch (e: Exception) {
            Log.e("FoodSearchService", "Error saving meal combo: ${e.message}")
        }
    }

    @Synchronized
    fun deleteMealCombo(comboId: String) {
        savedCombos.removeAll { it.id == comboId }
        try {
            val serialized = json.encodeToString(savedCombos)
            prefs.edit().putString("saved_meal_combos", serialized).apply()
        } catch (e: Exception) {
            Log.e("FoodSearchService", "Error deleting meal combo: ${e.message}")
        }
    }

    private fun loadCachedFoods() {
        try {
            val jsonStr = prefs.getString("cached_food_items", null)
            if (!jsonStr.isNullOrBlank()) {
                val list = json.decodeFromString<List<FoodItemDefinition>>(jsonStr)
                cachedCustomFoods.clear()
                cachedCustomFoods.addAll(list)
            }
        } catch (e: Exception) {
            Log.e("FoodSearchService", "Error loading cached foods: ${e.message}")
        }
    }

    @Synchronized
    fun saveCustomFood(food: FoodItemDefinition) {
        // Avoid duplicate ids or duplicate exact names
        cachedCustomFoods.removeAll { it.name.equals(food.name, ignoreCase = true) }
        cachedCustomFoods.add(0, food.copy(isCustomOrAi = true))
        // Cap custom foods at 200 items
        if (cachedCustomFoods.size > 200) {
            cachedCustomFoods.removeAt(cachedCustomFoods.size - 1)
        }
        try {
            val serialized = json.encodeToString(cachedCustomFoods)
            prefs.edit().putString("cached_food_items", serialized).apply()
        } catch (e: Exception) {
            Log.e("FoodSearchService", "Error saving custom food: ${e.message}")
        }
    }

    /**
     * Instant local search against preloaded database + cached AI/custom items.
     * Offline, 0-latency.
     */
    fun searchLocal(query: String, category: String = "All"): List<FoodItemDefinition> {
        val allFoods = cachedCustomFoods + FoodDatabase.preloadedFoods
        val cleanQuery = query.trim().lowercase()

        val categoryFiltered = if (category.equals("All", ignoreCase = true)) {
            allFoods
        } else {
            allFoods.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (cleanQuery.isEmpty()) {
            return categoryFiltered
        }

        // Rank search results: exact match first, starts-with next, then contains
        return categoryFiltered.filter { food ->
            food.name.lowercase().contains(cleanQuery) ||
                    food.category.lowercase().contains(cleanQuery) ||
                    food.servingUnit.lowercase().contains(cleanQuery)
        }.sortedWith(
            compareBy<FoodItemDefinition> { food ->
                val nameLower = food.name.lowercase()
                when {
                    nameLower == cleanQuery -> 0
                    nameLower.startsWith(cleanQuery) -> 1
                    nameLower.split(" ", "-", "/").any { it.startsWith(cleanQuery) } -> 2
                    else -> 3
                }
            }.thenBy { it.name }
        )
    }

    /**
     * Online AI search using Gemini for unlisted or exotic food items.
     * Caches newly fetched foods locally so next time they are found instantly.
     */
    suspend fun searchOnlineWithGemini(query: String, apiKey: String): Result<List<FoodItemDefinition>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanQuery = query.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Gemini API Key provided. Please add your API key in Settings."))
        }
        if (cleanQuery.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        val prompt = """
            You are an expert nutritional database engine.
            The user is searching for food items matching: "$cleanQuery".
            Return a JSON array containing up to 4 food item definitions representing realistic standard servings of this item or its common variants.
            Use standard Indian household units where appropriate (e.g., katori, piece, bowl, cup, tbsp, roti, glass, 100g).
            Ensure nutritional macros are accurate per 1 serving.

            Return ONLY valid raw JSON array matching this exact schema, without markdown formatting:
            [
              {
                "name": "Food Name",
                "category": "Curries & Dals",
                "servingUnit": "katori",
                "servingSizeDescription": "1 katori (~150g)",
                "baseQuantity": 1.0,
                "calories": 180,
                "proteinG": 6.5,
                "carbsG": 14.0,
                "fatG": 8.0,
                "fiberG": 3.0,
                "sugarG": 2.0,
                "sodiumMg": 380.0,
                "potassiumMg": 220.0
              }
            ]
        """.trimIndent()

        val modelsToTry = GeminiVisionService.CANDIDATE_MODELS

        for (model in modelsToTry) {
            try {
                val contentObj = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                val contents = JSONArray().put(contentObj)

                val genConfig = JSONObject()
                    .put("temperature", 0.2)
                    .put("responseMimeType", "application/json")

                if (model.contains("3.5") || model.contains("3.8") || model.contains("3.1")) {
                    genConfig.put("thinkingConfig", JSONObject().put("thinkingLevel", "MINIMAL"))
                } else if (model.contains("2.5")) {
                    genConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                }

                val requestJson = JSONObject()
                    .put("contents", contents)
                    .put("generationConfig", genConfig)
                    .toString()

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.w("FoodSearchService", "Model $model returned error ${response.code}: $body")
                    continue
                }

                val rawText = extractTextFromGeminiResponse(body)
                val items = parseFoodItemsJson(rawText)
                if (items.isNotEmpty()) {
                    // Cache each item
                    items.forEach { saveCustomFood(it) }
                    return@withContext Result.success(items)
                }
            } catch (e: Exception) {
                Log.w("FoodSearchService", "Failed with model $model: ${e.message}")
            }
        }

        Result.failure(Exception("Could not retrieve nutrition information online. Please check network or API key."))
    }

    /**
     * AI Natural Language Meal Parser.
     * Converts phrases like "2 rotis, 1 bowl dal and half cup curd"
     * into a structured list of (FoodItemDefinition, Quantity).
     */
    suspend fun parseMealSentenceWithGemini(
        sentence: String,
        apiKey: String
    ): Result<List<Pair<FoodItemDefinition, Float>>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanSentence = sentence.trim()
        if (cleanSentence.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        if (cleanKey.isBlank()) {
            // Local fallback attempt using heuristic matching
            val localExtracted = extractMealItemsLocally(cleanSentence)
            return@withContext Result.success(localExtracted)
        }

        val prompt = """
            You are an expert nutritional meal logger.
            Parse the following user meal description into individual food items with quantities:
            "$cleanSentence"

            For each item, identify:
            - quantity (Float, e.g. 2.0 for "2 rotis", 0.5 for "half cup")
            - food definition with realistic nutritional values per 1 base unit.

            Return ONLY valid JSON array matching this schema:
            [
              {
                "quantity": 2.0,
                "food": {
                  "name": "Roti / Chapati (Whole Wheat)",
                  "category": "Breads",
                  "servingUnit": "piece",
                  "servingSizeDescription": "1 medium roti (~40g)",
                  "baseQuantity": 1.0,
                  "calories": 104,
                  "proteinG": 3.1,
                  "carbsG": 18.0,
                  "fatG": 1.7,
                  "fiberG": 2.8,
                  "sugarG": 0.3,
                  "sodiumMg": 110.0,
                  "potassiumMg": 95.0
                }
              }
            ]
        """.trimIndent()

        val modelsToTry = GeminiVisionService.CANDIDATE_MODELS

        for (model in modelsToTry) {
            try {
                val contentObj = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                val contents = JSONArray().put(contentObj)

                val genConfig = JSONObject()
                    .put("temperature", 0.2)
                    .put("responseMimeType", "application/json")

                if (model.contains("3.5") || model.contains("3.8") || model.contains("3.1")) {
                    genConfig.put("thinkingConfig", JSONObject().put("thinkingLevel", "MINIMAL"))
                } else if (model.contains("2.5")) {
                    genConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                }

                val requestJson = JSONObject()
                    .put("contents", contents)
                    .put("generationConfig", genConfig)
                    .toString()

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) continue

                val rawText = extractTextFromGeminiResponse(body)
                val pairs = parseMealSentenceJson(rawText)
                if (pairs.isNotEmpty()) {
                    // Cache the foods
                    pairs.forEach { saveCustomFood(it.first) }
                    return@withContext Result.success(pairs)
                }
            } catch (e: Exception) {
                Log.w("FoodSearchService", "Meal parse error with model $model: ${e.message}")
            }
        }

        // Fallback to local heuristic extraction if AI call failed
        val localFallback = extractMealItemsLocally(cleanSentence)
        if (localFallback.isNotEmpty()) {
            return@withContext Result.success(localFallback)
        }

        Result.failure(Exception("Could not parse meal items. Try adding items individually using search."))
    }

    private fun extractMealItemsLocally(sentence: String): List<Pair<FoodItemDefinition, Float>> {
        val results = mutableListOf<Pair<FoodItemDefinition, Float>>()
        val tokens = sentence.split(",", ";", " and ", " with ", "+")
        for (token in tokens) {
            val trimmed = token.trim()
            if (trimmed.isBlank()) continue

            // Check for leading number or fractions
            var qty = 1.0f
            var itemText = trimmed
            val numberRegex = Regex("""^(\d+(\.\d+)?|\d+/\d+|half|one|two|three|four)\s+""", RegexOption.IGNORE_CASE)
            val match = numberRegex.find(trimmed)
            if (match != null) {
                val numStr = match.groupValues[1].lowercase()
                qty = when (numStr) {
                    "half" -> 0.5f
                    "one" -> 1.0f
                    "two" -> 2.0f
                    "three" -> 3.0f
                    "four" -> 4.0f
                    "1/2" -> 0.5f
                    "1/4" -> 0.25f
                    "3/4" -> 0.75f
                    else -> numStr.toFloatOrNull() ?: 1.0f
                }
                itemText = trimmed.substring(match.range.last + 1).trim()
            }

            // Search local database for match
            val matched = searchLocal(itemText).firstOrNull()
            if (matched != null) {
                results.add(Pair(matched, qty))
            }
        }
        return results
    }

    private fun extractTextFromGeminiResponse(responseJson: String): String {
        return try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseFoodItemsJson(rawJson: String): List<FoodItemDefinition> {
        val clean = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val list = mutableListOf<FoodItemDefinition>()
        try {
            val jsonArray = JSONArray(clean)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FoodItemDefinition(
                        id = UUID.randomUUID().toString(),
                        name = obj.optString("name", "Unknown Food"),
                        category = obj.optString("category", "General"),
                        servingUnit = obj.optString("servingUnit", "serving"),
                        servingSizeDescription = obj.optString("servingSizeDescription", "1 serving"),
                        baseQuantity = obj.optDouble("baseQuantity", 1.0).toFloat(),
                        calories = obj.optInt("calories", 0),
                        proteinG = obj.optDouble("proteinG", 0.0).toFloat(),
                        carbsG = obj.optDouble("carbsG", 0.0).toFloat(),
                        fatG = obj.optDouble("fatG", 0.0).toFloat(),
                        fiberG = obj.optDouble("fiberG", 0.0).toFloat(),
                        sugarG = obj.optDouble("sugarG", 0.0).toFloat(),
                        sodiumMg = obj.optDouble("sodiumMg", 0.0).toFloat(),
                        potassiumMg = obj.optDouble("potassiumMg", 0.0).toFloat(),
                        isCustomOrAi = true
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("FoodSearchService", "Error parsing food JSON: ${e.message}")
        }
        return list
    }

    private fun parseMealSentenceJson(rawJson: String): List<Pair<FoodItemDefinition, Float>> {
        val clean = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val list = mutableListOf<Pair<FoodItemDefinition, Float>>()
        try {
            val jsonArray = JSONArray(clean)
            for (i in 0 until jsonArray.length()) {
                val entry = jsonArray.getJSONObject(i)
                val qty = entry.optDouble("quantity", 1.0).toFloat()
                val foodObj = entry.getJSONObject("food")
                val item = FoodItemDefinition(
                    id = UUID.randomUUID().toString(),
                    name = foodObj.optString("name", "Food Item"),
                    category = foodObj.optString("category", "General"),
                    servingUnit = foodObj.optString("servingUnit", "serving"),
                    servingSizeDescription = foodObj.optString("servingSizeDescription", "1 serving"),
                    baseQuantity = foodObj.optDouble("baseQuantity", 1.0).toFloat(),
                    calories = foodObj.optInt("calories", 0),
                    proteinG = foodObj.optDouble("proteinG", 0.0).toFloat(),
                    carbsG = foodObj.optDouble("carbsG", 0.0).toFloat(),
                    fatG = foodObj.optDouble("fatG", 0.0).toFloat(),
                    fiberG = foodObj.optDouble("fiberG", 0.0).toFloat(),
                    sugarG = foodObj.optDouble("sugarG", 0.0).toFloat(),
                    sodiumMg = foodObj.optDouble("sodiumMg", 0.0).toFloat(),
                    potassiumMg = foodObj.optDouble("potassiumMg", 0.0).toFloat(),
                    isCustomOrAi = true
                )
                list.add(Pair(item, qty))
            }
        } catch (e: Exception) {
            Log.e("FoodSearchService", "Error parsing meal sentence JSON: ${e.message}")
        }
        return list
    }
}
