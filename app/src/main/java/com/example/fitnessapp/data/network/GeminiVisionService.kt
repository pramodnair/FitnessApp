package com.example.fitnessapp.data.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.fitnessapp.data.model.FoodItem
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.MealType
import com.example.fitnessapp.data.model.Micronutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

@Serializable
data class GeminiAnalysisResult(
    val title: String,
    val portionDescription: String = "",
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val healthInsights: String = ""
)

data class ApiKeyValidationResult(
    val isValid: Boolean,
    val message: String
)

class GeminiVisionService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        val CANDIDATE_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.5-flash",
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b"
        )
    }

    @Volatile
    private var activeModel: String? = null

    suspend fun validateApiKey(apiKey: String): ApiKeyValidationResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext ApiKeyValidationResult(false, "API key cannot be empty.")
        }

        // 1. Dynamic Model Discovery: Query models endpoint enabled for this API key
        try {
            val listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$trimmed"
            val listReq = Request.Builder().url(listUrl).get().build()
            val listResp = client.newCall(listReq).execute()
            val listBody = listResp.body?.string() ?: ""

            if (listResp.isSuccessful && listBody.isNotBlank()) {
                val modelNames = try {
                    val root = jsonParser.parseToJsonElement(listBody).jsonObject
                    root["models"]?.jsonArray?.mapNotNull { elem ->
                        val name = elem.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                        name.removePrefix("models/")
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                // Match against candidate models in priority order
                val matched = CANDIDATE_MODELS.firstOrNull { it in modelNames }
                    ?: modelNames.firstOrNull { it.contains("3.8-flash") }
                    ?: modelNames.firstOrNull { it.contains("flash") }
                    ?: "gemini-3.8-flash"

                activeModel = matched
                val readable = formatModelDisplayName(matched)
                return@withContext ApiKeyValidationResult(true, "Connected successfully! Active model: $readable")
            } else if (listResp.code == 400 || listResp.code == 403) {
                val errorDetails = extractErrorMessage(listBody, listResp.code)
                return@withContext ApiKeyValidationResult(false, errorDetails)
            }
        } catch (e: Exception) {
            Log.w("GeminiVision", "Dynamic model query failed, falling back to direct probe: ${e.message}")
        }

        // 2. Direct Probe Fallback: Test candidate models sequentially
        var lastError = "Unable to connect to Google Gemini API."
        for (model in CANDIDATE_MODELS) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model?key=$trimmed"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    activeModel = model
                    val readable = formatModelDisplayName(model)
                    return@withContext ApiKeyValidationResult(true, "Connected successfully! Active model: $readable")
                } else if (response.code != 404) {
                    val errorDetails = extractErrorMessage(body, response.code)
                    return@withContext ApiKeyValidationResult(false, errorDetails)
                } else {
                    lastError = extractErrorMessage(body, response.code)
                }
            } catch (e: Exception) {
                return@withContext ApiKeyValidationResult(false, "Network error: ${e.localizedMessage ?: e.message}")
            }
        }
        ApiKeyValidationResult(false, lastError)
    }

    suspend fun analyzeFoodImage(
        bitmap: Bitmap,
        apiKey: String,
        mealType: MealType = MealType.LUNCH,
        userId: String = "primary"
    ): MealLog = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cleanKey = apiKey.trim()

        if (cleanKey.isBlank()) {
            return@withContext getMockAnalysis(today, mealType, userId, "No API key configured (Using offline test mode)")
        }

        try {
            val resized = scaleBitmapIfNeeded(bitmap, maxDimension = 1024)
            val base64Image = bitmapToBase64(resized)
            val prompt = """
                You are a certified clinical nutritionist and dietary vision AI specializing in global and Indian cuisine.
                Analyze this food photograph carefully.
                
                Special instructions for Indian cuisine:
                - Accurately count staple items: Number of Rotis / Phulkas / Parathas, pieces of Modak / sweets, katoris of Dal, katoris of Sabzi / Curry, and cups of Rice.
                - Identify specific dishes: Modak, Toor Dal, Moong Dal, Chana Dal, Rajma, Chole, Paneer, Biryani, Idli, Dosa, etc.
                - Estimate hidden cooking fats: Indian dishes (curries, dals with tadka, dry sabzis) typically use 1-2 teaspoons (5-10g) of ghee or cooking oil per serving. Factor this into fat and calorie calculations.
                - For South Indian dishes: Recognise Idli (steamed, low fat), Dosa/Uttapam (tawa oil), Medu Vada (deep fried), Sambar (lentil base), and Coconut Chutney (coconut fats).
                - Calculate total caloric energy (in kcal), macronutrients (Protein in grams, Carbohydrates in grams, Fat in grams), and micronutrients (Dietary Fiber in grams, Sugar in grams, Sodium in milligrams, Potassium in milligrams).
                - Provide a concise 1-sentence health insight relevant to the dish.

                Respond ONLY with a valid JSON object matching this schema:
                {
                  "title": "Dish Name",
                  "portionDescription": "e.g. 2 pieces (approx 80g)",
                  "calories": 230,
                  "proteinG": 3.2,
                  "carbsG": 42.0,
                  "fatG": 6.0,
                  "fiberG": 2.5,
                  "sugarG": 18.0,
                  "sodiumMg": 35.0,
                  "potassiumMg": 110.0,
                  "healthInsights": "Nutritional advice sentence."
                }
            """.trimIndent()

            val textPart = JSONObject().put("text", prompt)
            val inlineData = JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", base64Image)
            val imagePart = JSONObject().put("inlineData", inlineData)

            val parts = JSONArray().put(textPart).put(imagePart)
            val contentObj = JSONObject().put("parts", parts)
            val contents = JSONArray().put(contentObj)

            val genConfig = JSONObject()
                .put("temperature", 0.2)
                .put("responseMimeType", "application/json")

            val safetySettings = JSONArray().apply {
                val categories = listOf(
                    "HARM_CATEGORY_HARASSMENT",
                    "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    "HARM_CATEGORY_DANGEROUS_CONTENT"
                )
                for (category in categories) {
                    put(JSONObject().apply {
                        put("category", category)
                        put("threshold", "BLOCK_ONLY_HIGH")
                    })
                }
            }

            val requestJsonObj = JSONObject()
                .put("contents", contents)
                .put("generationConfig", genConfig)
                .put("safetySettings", safetySettings)

            val requestJson = requestJsonObj.toString()

            val modelsToTry = (listOfNotNull(activeModel) + CANDIDATE_MODELS).distinct()
            var lastResponseBody = ""
            var lastStatusCode = 0

            for (model in modelsToTry) {
                var attempts = 0
                while (attempts < 2) {
                    attempts++
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestJson.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""
                    lastStatusCode = response.code
                    lastResponseBody = responseBody

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        activeModel = model
                        val parsed = parseAnalysisResult(responseBody)
                        return@withContext MealLog(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            date = today,
                            timestamp = System.currentTimeMillis(),
                            mealType = mealType,
                            title = parsed.title,
                            calories = parsed.calories,
                            proteinG = parsed.proteinG,
                            carbsG = parsed.carbsG,
                            fatG = parsed.fatG,
                            micronutrients = Micronutrients(
                                fiberG = parsed.fiberG,
                                sugarG = parsed.sugarG,
                                sodiumMg = parsed.sodiumMg,
                                potassiumMg = parsed.potassiumMg
                            ),
                            aiInsights = parsed.healthInsights,
                            aiSource = "LIVE_AI",
                            aiErrorMessage = null,
                            items = listOf(
                                FoodItem(
                                    name = parsed.title,
                                    portionDescription = parsed.portionDescription,
                                    calories = parsed.calories,
                                    proteinG = parsed.proteinG,
                                    carbsG = parsed.carbsG,
                                    fatG = parsed.fatG,
                                    micronutrients = Micronutrients(
                                        fiberG = parsed.fiberG,
                                        sugarG = parsed.sugarG,
                                        sodiumMg = parsed.sodiumMg,
                                        potassiumMg = parsed.potassiumMg
                                    )
                                )
                            )
                        )
                    } else if (response.code == 429) {
                        Log.w("GeminiVision", "Model $model returned HTTP 429 (rate limit). Attempt $attempts of 2.")
                        if (attempts < 2) {
                            kotlinx.coroutines.delay(1500)
                        }
                    } else if (response.code == 503 || response.code == 500) {
                        Log.w("GeminiVision", "Model $model returned HTTP ${response.code}. Attempt $attempts of 2.")
                        if (attempts < 2) {
                            kotlinx.coroutines.delay(1000)
                        }
                    } else if (response.code == 404) {
                        // Model not available on this API key or region; try next candidate model immediately
                        break
                    } else {
                        // Unrecoverable errors like 400 (Bad Request) or 403 (Invalid Key / Blocked)
                        val errorMsg = extractErrorMessage(responseBody, response.code)
                        Log.e("GeminiVision", "Gemini API unrecoverable error $lastStatusCode: $responseBody")
                        return@withContext getMockAnalysis(today, mealType, userId, errorMsg)
                    }
                }
            }

            val errorMsg = extractErrorMessage(lastResponseBody, lastStatusCode)
            Log.e("GeminiVision", "All candidate models failed. Last error $lastStatusCode: $lastResponseBody")
            return@withContext getMockAnalysis(today, mealType, userId, errorMsg)
        } catch (e: Exception) {
            Log.e("GeminiVision", "Exception in analyzeFoodImage", e)
            return@withContext getMockAnalysis(today, mealType, userId, "Connection error: ${e.localizedMessage ?: e.message}")
        }
    }

    internal fun parseAnalysisResult(rawResponse: String): GeminiAnalysisResult {
        val extractedJson = extractJsonText(rawResponse)
        val root = try {
            jsonParser.parseToJsonElement(extractedJson).jsonObject
        } catch (e: Exception) {
            Log.w("GeminiVision", "Could not parse JSON element from extracted text: $extractedJson", e)
            null
        }

        if (root != null && (root.containsKey("title") || root.containsKey("calories"))) {
            val title = root["title"]?.jsonPrimitive?.content ?: "Scanned Dish"
            val portion = root["portionDescription"]?.jsonPrimitive?.content ?: ""
            val calories = root["calories"]?.jsonPrimitive?.content?.toIntOrNull() ?: 250
            val protein = root["proteinG"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 10f
            val carbs = root["carbsG"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 30f
            val fat = root["fatG"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 8f
            val fiber = root["fiberG"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 2f
            val sugar = root["sugarG"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 2f
            val sodium = root["sodiumMg"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 300f
            val potassium = root["potassiumMg"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 200f
            val insights = when (val elem = root["healthInsights"]) {
                is kotlinx.serialization.json.JsonArray -> elem.joinToString(" ") { it.jsonPrimitive.content }
                is kotlinx.serialization.json.JsonPrimitive -> elem.content
                else -> "Nutrient-balanced meal."
            }
            return GeminiAnalysisResult(
                title = title,
                portionDescription = portion,
                calories = calories,
                proteinG = protein,
                carbsG = carbs,
                fatG = fat,
                fiberG = fiber,
                sugarG = sugar,
                sodiumMg = sodium,
                potassiumMg = potassium,
                healthInsights = insights
            )
        }

        return jsonParser.decodeFromString<GeminiAnalysisResult>(extractedJson)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun formatModelDisplayName(model: String): String {
        return when (model) {
            "gemini-3.8-flash" -> "Gemini 3.8 Flash (Latest Flagship)"
            "gemini-3.7-flash" -> "Gemini 3.7 Flash"
            "gemini-3.6-flash" -> "Gemini 3.6 Flash"
            "gemini-3.5-flash" -> "Gemini 3.5 Flash"
            "gemini-3.1-flash-lite" -> "Gemini 3.1 Flash-Lite"
            "gemini-2.5-flash" -> "Gemini 2.5 Flash"
            "gemini-2.0-flash" -> "Gemini 2.0 Flash"
            "gemini-1.5-flash" -> "Gemini 1.5 Flash"
            "gemini-1.5-flash-8b" -> "Gemini 1.5 Flash-8B"
            else -> model.replace("-", " ").replaceFirstChar { it.uppercase() }
        }
    }

    private fun extractErrorMessage(body: String, statusCode: Int): String {
        return try {
            val root = jsonParser.parseToJsonElement(body).jsonObject
            val errorObj = root["error"]?.jsonObject
            val message = errorObj?.get("message")?.jsonPrimitive?.content
            val status = errorObj?.get("status")?.jsonPrimitive?.content
            if (statusCode == 429 || status == "RESOURCE_EXHAUSTED") {
                "Google Gemini Free Tier rate limit reached (HTTP 429). Free quota allows 15 scans/min. Wait 15s or configure separate API keys for each device."
            } else if (!message.isNullOrBlank()) {
                message
            } else {
                "HTTP $statusCode error from Gemini API"
            }
        } catch (e: Exception) {
            if (statusCode == 429) {
                "Google Gemini Free Tier rate limit reached (HTTP 429). Free quota allows 15 scans/min. Wait 15s or configure separate API keys for each device."
            } else {
                "HTTP $statusCode: ${body.take(120)}"
            }
        }
    }

    internal fun extractJsonText(rawResponse: String): String {
        return try {
            val root = jsonParser.parseToJsonElement(rawResponse).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val finishReason = firstCandidate?.get("finishReason")?.jsonPrimitive?.content
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val rawText = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

            if (rawText.isNullOrBlank()) {
                if (finishReason == "SAFETY") {
                    throw IllegalStateException("Image analysis was blocked by Gemini safety filters.")
                }
                throw IllegalStateException("Gemini returned empty response (finishReason: $finishReason)")
            }

            // Extract innermost JSON object from candidate's text
            val startIdx = rawText.indexOf('{')
            val lastIdx = rawText.lastIndexOf('}')
            if (startIdx != -1 && lastIdx > startIdx) {
                rawText.substring(startIdx, lastIdx + 1).trim()
            } else {
                rawText.trim()
            }
        } catch (e: Exception) {
            Log.w("GeminiVision", "extractJsonText fallback: ${e.message}")
            // Fallback: search rawResponse directly for any JSON object
            val startIdx = rawResponse.indexOf('{')
            val lastIdx = rawResponse.lastIndexOf('}')
            if (startIdx != -1 && lastIdx > startIdx) {
                rawResponse.substring(startIdx, lastIdx + 1).trim()
            } else {
                rawResponse
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun getMockAnalysis(
        date: String,
        mealType: MealType,
        userId: String,
        errorMessage: String? = null
    ): MealLog {
        val sampleMeals = listOf(
            GeminiAnalysisResult(
                title = "Modak (Steamed Sweet Dumplings)",
                portionDescription = "2 pieces steamed modak (approx 80g)",
                calories = 230,
                proteinG = 3.2f,
                carbsG = 42.0f,
                fatG = 6.0f,
                fiberG = 2.5f,
                sugarG = 18.0f,
                sodiumMg = 35f,
                potassiumMg = 110f,
                healthInsights = "Festive Indian sweet with coconut & jaggery filling. Consume in moderation when in deficit."
            ),
            GeminiAnalysisResult(
                title = "2 Phulkas with Dal Tadka & Bhindi Sabzi",
                portionDescription = "2 whole wheat rotis, 1 katori toor dal, 1 katori bhindi masala (approx 1 tsp oil)",
                calories = 440,
                proteinG = 14.5f,
                carbsG = 66.0f,
                fatG = 13.0f,
                fiberG = 9.8f,
                sugarG = 3.2f,
                sodiumMg = 490f,
                potassiumMg = 640f,
                healthInsights = "Balanced Indian meal rich in prebiotic fiber and complex carbs for sustained fullness."
            ),
            GeminiAnalysisResult(
                title = "Paneer Bhurji with 2 Multigrain Rotis",
                portionDescription = "150g paneer bhurji cooked with onions, tomatoes, spices + 2 rotis",
                calories = 510,
                proteinG = 25.0f,
                carbsG = 44.0f,
                fatG = 26.0f,
                fiberG = 7.5f,
                sugarG = 3.5f,
                sodiumMg = 520f,
                potassiumMg = 580f,
                healthInsights = "High-protein vegetarian option. Excellent for muscle preservation during weight loss."
            ),
            GeminiAnalysisResult(
                title = "3 Steamed Idlis with Sambar & Coconut Chutney",
                portionDescription = "3 medium idlis, 1 bowl vegetable sambar, 2 tbsp coconut chutney",
                calories = 340,
                proteinG = 10.5f,
                carbsG = 58.0f,
                fatG = 7.5f,
                fiberG = 6.2f,
                sugarG = 2.8f,
                sodiumMg = 430f,
                potassiumMg = 460f,
                healthInsights = "Fermented and easily digestible meal; low fat with healthy plant electrolytes."
            ),
            GeminiAnalysisResult(
                title = "Chicken Curry with 1 Cup Jeera Brown Rice",
                portionDescription = "150g home-cooked chicken curry (boneless breast) + 1 cup jeera rice",
                calories = 520,
                proteinG = 38.0f,
                carbsG = 56.0f,
                fatG = 15.0f,
                fiberG = 5.5f,
                sugarG = 2.0f,
                sodiumMg = 560f,
                potassiumMg = 720f,
                healthInsights = "High lean protein meal that promotes satiety and muscle recovery."
            )
        )

        val selected = sampleMeals.random()
        return MealLog(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = date,
            timestamp = System.currentTimeMillis(),
            mealType = mealType,
            title = selected.title,
            calories = selected.calories,
            proteinG = selected.proteinG,
            carbsG = selected.carbsG,
            fatG = selected.fatG,
            micronutrients = Micronutrients(
                fiberG = selected.fiberG,
                sugarG = selected.sugarG,
                sodiumMg = selected.sodiumMg,
                potassiumMg = selected.potassiumMg
            ),
            aiInsights = selected.healthInsights,
            aiSource = "OFFLINE_MOCK",
            aiErrorMessage = errorMessage,
            items = listOf(
                FoodItem(
                    name = selected.title,
                    portionDescription = selected.portionDescription,
                    calories = selected.calories,
                    proteinG = selected.proteinG,
                    carbsG = selected.carbsG,
                    fatG = selected.fatG,
                    micronutrients = Micronutrients(
                        fiberG = selected.fiberG,
                        sugarG = selected.sugarG,
                        sodiumMg = selected.sodiumMg,
                        potassiumMg = selected.potassiumMg
                    )
                )
            )
        )
    }
}
