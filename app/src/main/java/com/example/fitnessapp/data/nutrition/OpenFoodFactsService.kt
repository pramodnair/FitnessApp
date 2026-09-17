package com.example.fitnessapp.data.nutrition

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class OpenFoodFactsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Looks up barcode on Open Food Facts REST API v2.
     * Returns FoodItemDefinition if found, or null if not found.
     */
    suspend fun lookupBarcode(barcode: String): Result<FoodItemDefinition?> = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) {
            return@withContext Result.success(null)
        }

        val url = "https://world.openfoodfacts.org/api/v2/product/$cleanBarcode.json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NutriFitAI-Android/1.0 (FitnessApp; contact: pramodnair)")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("OpenFoodFacts HTTP ${response.code}"))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val status = json.optInt("status", 0)

            if (status != 1) {
                // Product not found
                return@withContext Result.success(null)
            }

            val product = json.optJSONObject("product") ?: return@withContext Result.success(null)

            val rawName = product.optString("product_name", "").ifBlank {
                product.optString("product_name_en", "Scanned Product")
            }
            val brand = product.optString("brands", "").trim()
            val fullName = if (brand.isNotBlank() && !rawName.contains(brand, ignoreCase = true)) {
                "$brand $rawName"
            } else {
                rawName
            }

            val servingSize = product.optString("serving_size", "").trim()
            val nutriments = product.optJSONObject("nutriments")

            // Parse calories
            var calories = 0
            if (nutriments != null) {
                val cal100 = nutriments.optDouble("energy-kcal_100g", Double.NaN)
                val calServing = nutriments.optDouble("energy-kcal_serving", Double.NaN)
                val calVal = nutriments.optDouble("energy-kcal_value", Double.NaN)
                val energyKj = nutriments.optDouble("energy_100g", Double.NaN)

                calories = when {
                    !calServing.isNaN() && calServing > 0 -> calServing.toInt()
                    !cal100.isNaN() && cal100 > 0 -> cal100.toInt()
                    !calVal.isNaN() && calVal > 0 -> calVal.toInt()
                    !energyKj.isNaN() && energyKj > 0 -> (energyKj / 4.184).toInt()
                    else -> 0
                }
            }

            // Parse macros (per serving if available, else per 100g)
            var protein = 0f
            var carbs = 0f
            var fat = 0f
            var fiber = 0f
            var sugar = 0f
            var sodiumMg = 0f
            var potassiumMg = 0f

            if (nutriments != null) {
                protein = getNutrientValue(nutriments, "proteins")
                carbs = getNutrientValue(nutriments, "carbohydrates")
                fat = getNutrientValue(nutriments, "fat")
                fiber = getNutrientValue(nutriments, "fiber")
                sugar = getNutrientValue(nutriments, "sugars")
                
                // Sodium is usually in grams in OFF, convert to mg
                val sodiumG = getNutrientValue(nutriments, "sodium")
                val sodiumDirectMg = nutriments.optDouble("sodium_serving", Double.NaN)
                sodiumMg = if (sodiumG > 0f) sodiumG * 1000f else if (!sodiumDirectMg.isNaN()) sodiumDirectMg.toFloat() else 0f

                val potG = getNutrientValue(nutriments, "potassium")
                potassiumMg = if (potG > 0f) potG * 1000f else 0f
            }

            val unit = if (servingSize.isNotBlank()) servingSize else "serving (100g)"

            val foodDef = FoodItemDefinition(
                id = "barcode_$cleanBarcode",
                name = fullName,
                category = "Packaged Food",
                servingUnit = unit,
                baseQuantity = 1.0f,
                calories = calories,
                proteinG = protein,
                carbsG = carbs,
                fatG = fat,
                fiberG = fiber,
                sugarG = sugar,
                sodiumMg = sodiumMg,
                potassiumMg = potassiumMg,
                isCustomOrAi = true
            )

            Result.success(foodDef)
        } catch (e: Exception) {
            Log.e("OpenFoodFactsService", "Error looking up barcode $cleanBarcode: ${e.message}")
            Result.failure(e)
        }
    }

    private fun getNutrientValue(nutriments: JSONObject, key: String): Float {
        val serving = nutriments.optDouble("${key}_serving", Double.NaN)
        if (!serving.isNaN() && serving >= 0) return ((serving * 10).toInt() / 10f)
        val per100g = nutriments.optDouble("${key}_100g", Double.NaN)
        if (!per100g.isNaN() && per100g >= 0) return ((per100g * 10).toInt() / 10f)
        val direct = nutriments.optDouble(key, Double.NaN)
        if (!direct.isNaN() && direct >= 0) return ((direct * 10).toInt() / 10f)
        return 0f
    }
}
