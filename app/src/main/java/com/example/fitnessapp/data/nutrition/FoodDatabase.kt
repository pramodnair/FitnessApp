package com.example.fitnessapp.data.nutrition

import com.example.fitnessapp.data.model.FoodItem
import com.example.fitnessapp.data.model.Micronutrients
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FoodItemDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "General", // "Breads", "Curries & Dals", "Rice & Grains", "Proteins & Dairy", "South Indian", "Fruits & Veggies", "Snacks & Drinks"
    val servingUnit: String = "serving", // "piece", "katori", "cup", "gram", "tbsp", "scoop"
    val servingSizeDescription: String = "1 serving",
    val baseQuantity: Float = 1f,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f,
    val potassiumMg: Float = 0f,
    val isCustomOrAi: Boolean = false
) {
    fun scale(quantity: Float): FoodItem {
        val factor = if (baseQuantity > 0f) quantity / baseQuantity else quantity
        val qtyDisplay = if (quantity % 1f == 0f) quantity.toInt().toString() else "%.1f".format(quantity)
        return FoodItem(
            name = name,
            portionDescription = "$qtyDisplay $servingUnit ($servingSizeDescription)",
            calories = (calories * factor).toInt().coerceAtLeast(0),
            proteinG = ((proteinG * factor * 10f).toInt() / 10f).coerceAtLeast(0f),
            carbsG = ((carbsG * factor * 10f).toInt() / 10f).coerceAtLeast(0f),
            fatG = ((fatG * factor * 10f).toInt() / 10f).coerceAtLeast(0f),
            micronutrients = Micronutrients(
                fiberG = ((fiberG * factor * 10f).toInt() / 10f).coerceAtLeast(0f),
                sugarG = ((sugarG * factor * 10f).toInt() / 10f).coerceAtLeast(0f),
                sodiumMg = ((sodiumMg * factor * 10f).toInt() / 10f).coerceAtLeast(0f),
                potassiumMg = ((potassiumMg * factor * 10f).toInt() / 10f).coerceAtLeast(0f)
            )
        )
    }
}

object FoodDatabase {

    val categories = listOf(
        "All",
        "Breads",
        "Curries & Dals",
        "Rice & Grains",
        "Proteins & Dairy",
        "South Indian",
        "Fruits & Veggies",
        "Snacks & Drinks"
    )

    val preloadedFoods: List<FoodItemDefinition> = listOf(
        // === BREADS ===
        FoodItemDefinition(
            name = "Whole Wheat Roti / Chapati",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "40g",
            calories = 80,
            proteinG = 3.0f,
            carbsG = 16.0f,
            fatG = 0.5f,
            fiberG = 2.5f,
            potassiumMg = 95f
        ),
        FoodItemDefinition(
            name = "Roti with Ghee",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "45g",
            calories = 110,
            proteinG = 3.0f,
            carbsG = 16.0f,
            fatG = 3.5f,
            fiberG = 2.5f
        ),
        FoodItemDefinition(
            name = "Plain Paratha",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "70g",
            calories = 180,
            proteinG = 4.0f,
            carbsG = 24.0f,
            fatG = 7.0f,
            fiberG = 2.0f
        ),
        FoodItemDefinition(
            name = "Aloo Paratha",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "100g",
            calories = 240,
            proteinG = 5.0f,
            carbsG = 34.0f,
            fatG = 9.0f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Paneer Paratha",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "110g",
            calories = 280,
            proteinG = 10.0f,
            carbsG = 30.0f,
            fatG = 12.0f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Poori",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "35g",
            calories = 120,
            proteinG = 2.0f,
            carbsG = 14.0f,
            fatG = 6.5f
        ),
        FoodItemDefinition(
            name = "Tandoori Roti",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "50g",
            calories = 110,
            proteinG = 4.0f,
            carbsG = 22.0f,
            fatG = 0.5f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Butter Naan",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "90g",
            calories = 260,
            proteinG = 7.0f,
            carbsG = 42.0f,
            fatG = 7.0f
        ),
        FoodItemDefinition(
            name = "Brown Bread Slice",
            category = "Breads",
            servingUnit = "slice",
            servingSizeDescription = "30g",
            calories = 75,
            proteinG = 3.0f,
            carbsG = 13.0f,
            fatG = 1.0f,
            fiberG = 2.0f
        ),
        FoodItemDefinition(
            name = "White Bread Slice",
            category = "Breads",
            servingUnit = "slice",
            servingSizeDescription = "30g",
            calories = 80,
            proteinG = 2.5f,
            carbsG = 15.0f,
            fatG = 1.0f,
            fiberG = 0.5f
        ),

        // === CURRIES & DALS ===
        FoodItemDefinition(
            name = "Yellow Dal Tadka",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 145,
            proteinG = 7.0f,
            carbsG = 18.0f,
            fatG = 4.5f,
            fiberG = 4.0f,
            potassiumMg = 220f
        ),
        FoodItemDefinition(
            name = "Dal Makhani",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 260,
            proteinG = 8.0f,
            carbsG = 22.0f,
            fatG = 15.0f,
            fiberG = 5.0f
        ),
        FoodItemDefinition(
            name = "Chana Masala (Chole)",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 210,
            proteinG = 9.0f,
            carbsG = 28.0f,
            fatG = 6.0f,
            fiberG = 7.0f
        ),
        FoodItemDefinition(
            name = "Rajma Masala",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 190,
            proteinG = 9.0f,
            carbsG = 26.0f,
            fatG = 5.0f,
            fiberG = 6.0f
        ),
        FoodItemDefinition(
            name = "Sambar",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 90,
            proteinG = 4.0f,
            carbsG = 14.0f,
            fatG = 2.0f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Paneer Butter Masala",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 320,
            proteinG = 11.0f,
            carbsG = 12.0f,
            fatG = 25.0f
        ),
        FoodItemDefinition(
            name = "Palak Paneer",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 230,
            proteinG = 12.0f,
            carbsG = 8.0f,
            fatG = 16.0f,
            fiberG = 3.5f
        ),
        FoodItemDefinition(
            name = "Kadai Paneer",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 270,
            proteinG = 13.0f,
            carbsG = 10.0f,
            fatG = 19.0f
        ),
        FoodItemDefinition(
            name = "Home Chicken Curry",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 240,
            proteinG = 22.0f,
            carbsG = 6.0f,
            fatG = 14.0f
        ),
        FoodItemDefinition(
            name = "Butter Chicken",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 340,
            proteinG = 20.0f,
            carbsG = 12.0f,
            fatG = 24.0f
        ),
        FoodItemDefinition(
            name = "Egg Curry (2 Eggs)",
            category = "Curries & Dals",
            servingUnit = "bowl",
            servingSizeDescription = "200g",
            calories = 260,
            proteinG = 16.0f,
            carbsG = 8.0f,
            fatG = 17.0f
        ),
        FoodItemDefinition(
            name = "Mixed Vegetable Sabzi",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "120g",
            calories = 110,
            proteinG = 3.0f,
            carbsG = 14.0f,
            fatG = 5.0f,
            fiberG = 3.5f
        ),
        FoodItemDefinition(
            name = "Bhindi Masala (Okra)",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "120g",
            calories = 125,
            proteinG = 3.0f,
            carbsG = 11.0f,
            fatG = 7.5f,
            fiberG = 4.0f
        ),
        FoodItemDefinition(
            name = "Aloo Gobi",
            category = "Curries & Dals",
            servingUnit = "katori",
            servingSizeDescription = "130g",
            calories = 140,
            proteinG = 3.5f,
            carbsG = 18.0f,
            fatG = 6.0f,
            fiberG = 3.0f
        ),

        // === RICE & GRAINS ===
        FoodItemDefinition(
            name = "Steamed White Rice",
            category = "Rice & Grains",
            servingUnit = "cup",
            servingSizeDescription = "150g cooked",
            calories = 195,
            proteinG = 4.0f,
            carbsG = 43.0f,
            fatG = 0.5f,
            fiberG = 0.5f
        ),
        FoodItemDefinition(
            name = "Brown Rice",
            category = "Rice & Grains",
            servingUnit = "cup",
            servingSizeDescription = "150g cooked",
            calories = 180,
            proteinG = 4.5f,
            carbsG = 38.0f,
            fatG = 1.5f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Jeera Rice",
            category = "Rice & Grains",
            servingUnit = "cup",
            servingSizeDescription = "150g",
            calories = 220,
            proteinG = 4.0f,
            carbsG = 42.0f,
            fatG = 4.0f
        ),
        FoodItemDefinition(
            name = "Vegetable Pulao",
            category = "Rice & Grains",
            servingUnit = "bowl",
            servingSizeDescription = "180g",
            calories = 240,
            proteinG = 5.0f,
            carbsG = 44.0f,
            fatG = 5.0f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Moong Dal Khichdi",
            category = "Rice & Grains",
            servingUnit = "bowl",
            servingSizeDescription = "200g",
            calories = 220,
            proteinG = 8.0f,
            carbsG = 38.0f,
            fatG = 4.0f,
            fiberG = 4.0f
        ),
        FoodItemDefinition(
            name = "Chicken Biryani",
            category = "Rice & Grains",
            servingUnit = "plate",
            servingSizeDescription = "300g",
            calories = 480,
            proteinG = 28.0f,
            carbsG = 54.0f,
            fatG = 16.0f
        ),
        FoodItemDefinition(
            name = "Vegetable Biryani",
            category = "Rice & Grains",
            servingUnit = "plate",
            servingSizeDescription = "280g",
            calories = 360,
            proteinG = 8.0f,
            carbsG = 58.0f,
            fatG = 11.0f,
            fiberG = 5.0f
        ),
        FoodItemDefinition(
            name = "Poha",
            category = "Rice & Grains",
            servingUnit = "bowl",
            servingSizeDescription = "150g",
            calories = 210,
            proteinG = 4.0f,
            carbsG = 36.0f,
            fatG = 6.0f,
            fiberG = 2.5f
        ),
        FoodItemDefinition(
            name = "Sooji Upma",
            category = "Rice & Grains",
            servingUnit = "bowl",
            servingSizeDescription = "150g",
            calories = 220,
            proteinG = 5.0f,
            carbsG = 35.0f,
            fatG = 7.0f,
            fiberG = 2.0f
        ),
        FoodItemDefinition(
            name = "Oatmeal (Cooked in Water)",
            category = "Rice & Grains",
            servingUnit = "bowl",
            servingSizeDescription = "200g",
            calories = 160,
            proteinG = 6.0f,
            carbsG = 28.0f,
            fatG = 3.0f,
            fiberG = 4.0f
        ),

        // === PROTEINS & DAIRY ===
        FoodItemDefinition(
            name = "Boiled Whole Egg",
            category = "Proteins & Dairy",
            servingUnit = "piece",
            servingSizeDescription = "1 large (50g)",
            calories = 72,
            proteinG = 6.3f,
            carbsG = 0.4f,
            fatG = 4.8f
        ),
        FoodItemDefinition(
            name = "Boiled Egg White",
            category = "Proteins & Dairy",
            servingUnit = "piece",
            servingSizeDescription = "33g",
            calories = 17,
            proteinG = 3.6f,
            carbsG = 0.2f,
            fatG = 0.1f
        ),
        FoodItemDefinition(
            name = "Scrambled Eggs (2 Eggs)",
            category = "Proteins & Dairy",
            servingUnit = "serving",
            servingSizeDescription = "100g",
            calories = 180,
            proteinG = 13.0f,
            carbsG = 1.5f,
            fatG = 14.0f
        ),
        FoodItemDefinition(
            name = "Omelette (2 Eggs)",
            category = "Proteins & Dairy",
            servingUnit = "serving",
            servingSizeDescription = "110g",
            calories = 190,
            proteinG = 13.0f,
            carbsG = 2.0f,
            fatG = 15.0f
        ),
        FoodItemDefinition(
            name = "Grilled Chicken Breast",
            category = "Proteins & Dairy",
            servingUnit = "gram",
            servingSizeDescription = "100g cooked",
            baseQuantity = 100f,
            calories = 165,
            proteinG = 31.0f,
            carbsG = 0.0f,
            fatG = 3.6f,
            sodiumMg = 75f
        ),
        FoodItemDefinition(
            name = "Raw Paneer",
            category = "Proteins & Dairy",
            servingUnit = "gram",
            servingSizeDescription = "100g",
            baseQuantity = 100f,
            calories = 265,
            proteinG = 18.0f,
            carbsG = 3.0f,
            fatG = 20.0f
        ),
        FoodItemDefinition(
            name = "Tofu",
            category = "Proteins & Dairy",
            servingUnit = "gram",
            servingSizeDescription = "100g",
            baseQuantity = 100f,
            calories = 85,
            proteinG = 9.0f,
            carbsG = 2.0f,
            fatG = 5.0f
        ),
        FoodItemDefinition(
            name = "Plain Curd / Dahi",
            category = "Proteins & Dairy",
            servingUnit = "katori",
            servingSizeDescription = "150g",
            calories = 90,
            proteinG = 5.0f,
            carbsG = 7.0f,
            fatG = 4.5f
        ),
        FoodItemDefinition(
            name = "Greek Yogurt (Plain)",
            category = "Proteins & Dairy",
            servingUnit = "cup",
            servingSizeDescription = "150g",
            calories = 120,
            proteinG = 15.0f,
            carbsG = 6.0f,
            fatG = 3.0f
        ),
        FoodItemDefinition(
            name = "Whole Milk (Cow/Buffalo)",
            category = "Proteins & Dairy",
            servingUnit = "glass",
            servingSizeDescription = "250ml",
            calories = 150,
            proteinG = 8.0f,
            carbsG = 12.0f,
            fatG = 8.0f
        ),
        FoodItemDefinition(
            name = "Toned Milk",
            category = "Proteins & Dairy",
            servingUnit = "glass",
            servingSizeDescription = "250ml",
            calories = 115,
            proteinG = 8.0f,
            carbsG = 12.0f,
            fatG = 3.5f
        ),
        FoodItemDefinition(
            name = "Whey Protein Scoop",
            category = "Proteins & Dairy",
            servingUnit = "scoop",
            servingSizeDescription = "30g",
            calories = 120,
            proteinG = 24.0f,
            carbsG = 2.0f,
            fatG = 1.5f
        ),

        // === SOUTH INDIAN ===
        FoodItemDefinition(
            name = "Idli",
            category = "South Indian",
            servingUnit = "piece",
            servingSizeDescription = "45g",
            calories = 55,
            proteinG = 2.0f,
            carbsG = 12.0f,
            fatG = 0.2f,
            fiberG = 1.0f
        ),
        FoodItemDefinition(
            name = "Plain Dosa",
            category = "South Indian",
            servingUnit = "piece",
            servingSizeDescription = "80g",
            calories = 165,
            proteinG = 4.0f,
            carbsG = 28.0f,
            fatG = 4.0f,
            fiberG = 1.5f
        ),
        FoodItemDefinition(
            name = "Masala Dosa",
            category = "South Indian",
            servingUnit = "piece",
            servingSizeDescription = "150g",
            calories = 280,
            proteinG = 6.0f,
            carbsG = 42.0f,
            fatG = 9.0f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Medu Vada",
            category = "South Indian",
            servingUnit = "piece",
            servingSizeDescription = "50g",
            calories = 140,
            proteinG = 4.0f,
            carbsG = 14.0f,
            fatG = 8.0f
        ),
        FoodItemDefinition(
            name = "Coconut Chutney",
            category = "South Indian",
            servingUnit = "tbsp",
            servingSizeDescription = "20g",
            calories = 50,
            proteinG = 0.5f,
            carbsG = 1.5f,
            fatG = 4.5f
        ),
        FoodItemDefinition(
            name = "Tomato Chutney",
            category = "South Indian",
            servingUnit = "tbsp",
            servingSizeDescription = "20g",
            calories = 20,
            proteinG = 0.5f,
            carbsG = 3.0f,
            fatG = 0.8f
        ),
        FoodItemDefinition(
            name = "Onion Tomato Uttapam",
            category = "South Indian",
            servingUnit = "piece",
            servingSizeDescription = "120g",
            calories = 210,
            proteinG = 5.0f,
            carbsG = 36.0f,
            fatG = 5.0f,
            fiberG = 3.0f
        ),

        // === SNACKS & DRINKS ===
        FoodItemDefinition(
            name = "Indian Masala Chai (with Sugar)",
            category = "Snacks & Drinks",
            servingUnit = "cup",
            servingSizeDescription = "150ml",
            calories = 85,
            proteinG = 2.5f,
            carbsG = 12.0f,
            fatG = 3.0f
        ),
        FoodItemDefinition(
            name = "Chai without Sugar",
            category = "Snacks & Drinks",
            servingUnit = "cup",
            servingSizeDescription = "150ml",
            calories = 45,
            proteinG = 2.5f,
            carbsG = 4.0f,
            fatG = 3.0f
        ),
        FoodItemDefinition(
            name = "Black Coffee",
            category = "Snacks & Drinks",
            servingUnit = "cup",
            servingSizeDescription = "150ml",
            calories = 5,
            proteinG = 0.5f,
            carbsG = 0.0f,
            fatG = 0.0f
        ),
        FoodItemDefinition(
            name = "Filter Coffee (with Sugar)",
            category = "Snacks & Drinks",
            servingUnit = "cup",
            servingSizeDescription = "150ml",
            calories = 95,
            proteinG = 3.0f,
            carbsG = 13.0f,
            fatG = 3.5f
        ),
        FoodItemDefinition(
            name = "Roasted Chana",
            category = "Snacks & Drinks",
            servingUnit = "handful",
            servingSizeDescription = "30g",
            calories = 115,
            proteinG = 6.0f,
            carbsG = 17.0f,
            fatG = 2.0f,
            fiberG = 4.5f
        ),
        FoodItemDefinition(
            name = "Samosa",
            category = "Snacks & Drinks",
            servingUnit = "piece",
            servingSizeDescription = "80g",
            calories = 260,
            proteinG = 4.5f,
            carbsG = 28.0f,
            fatG = 15.0f
        ),
        FoodItemDefinition(
            name = "Almonds",
            category = "Snacks & Drinks",
            servingUnit = "handful",
            servingSizeDescription = "10 nuts (12g)",
            calories = 70,
            proteinG = 2.5f,
            carbsG = 2.5f,
            fatG = 6.0f,
            fiberG = 1.5f
        ),
        FoodItemDefinition(
            name = "Walnuts",
            category = "Snacks & Drinks",
            servingUnit = "handful",
            servingSizeDescription = "4 halves (12g)",
            calories = 80,
            proteinG = 1.8f,
            carbsG = 1.6f,
            fatG = 8.0f
        ),
        FoodItemDefinition(
            name = "Peanut Butter",
            category = "Snacks & Drinks",
            servingUnit = "tbsp",
            servingSizeDescription = "16g",
            calories = 95,
            proteinG = 4.0f,
            carbsG = 3.0f,
            fatG = 8.0f
        ),
        FoodItemDefinition(
            name = "Desi Ghee",
            category = "Snacks & Drinks",
            servingUnit = "tsp",
            servingSizeDescription = "5g",
            calories = 45,
            proteinG = 0.0f,
            carbsG = 0.0f,
            fatG = 5.0f
        ),

        // === FRUITS & VEGGIES ===
        FoodItemDefinition(
            name = "Banana",
            category = "Fruits & Veggies",
            servingUnit = "piece",
            servingSizeDescription = "1 medium (118g)",
            calories = 105,
            proteinG = 1.3f,
            carbsG = 27.0f,
            fatG = 0.3f,
            fiberG = 3.0f,
            potassiumMg = 422f
        ),
        FoodItemDefinition(
            name = "Apple",
            category = "Fruits & Veggies",
            servingUnit = "piece",
            servingSizeDescription = "1 medium (180g)",
            calories = 95,
            proteinG = 0.5f,
            carbsG = 25.0f,
            fatG = 0.3f,
            fiberG = 4.4f,
            potassiumMg = 195f
        ),
        FoodItemDefinition(
            name = "Orange",
            category = "Fruits & Veggies",
            servingUnit = "piece",
            servingSizeDescription = "1 medium (130g)",
            calories = 62,
            proteinG = 1.2f,
            carbsG = 15.0f,
            fatG = 0.2f,
            fiberG = 3.0f
        ),
        FoodItemDefinition(
            name = "Papaya Cubes",
            category = "Fruits & Veggies",
            servingUnit = "cup",
            servingSizeDescription = "140g",
            calories = 60,
            proteinG = 0.8f,
            carbsG = 15.0f,
            fatG = 0.4f,
            fiberG = 2.5f
        ),
        FoodItemDefinition(
            name = "Cucumber & Tomato Salad",
            category = "Fruits & Veggies",
            servingUnit = "bowl",
            servingSizeDescription = "150g",
            calories = 30,
            proteinG = 1.2f,
            carbsG = 6.0f,
            fatG = 0.3f,
            fiberG = 2.0f
        ),
        FoodItemDefinition(
            name = "Besan Chilla",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "1 medium chilla (~60g)",
            calories = 135,
            proteinG = 6.0f,
            carbsG = 16.0f,
            fatG = 5.0f,
            fiberG = 3.2f,
            sodiumMg = 180f
        ),
        FoodItemDefinition(
            name = "Moong Dal Chilla",
            category = "Breads",
            servingUnit = "piece",
            servingSizeDescription = "1 medium chilla (~60g)",
            calories = 120,
            proteinG = 7.5f,
            carbsG = 14.0f,
            fatG = 3.5f,
            fiberG = 3.5f,
            sodiumMg = 160f
        ),
        FoodItemDefinition(
            name = "Ragi Dosa",
            category = "South Indian",
            servingUnit = "piece",
            servingSizeDescription = "1 medium dosa (~65g)",
            calories = 130,
            proteinG = 3.2f,
            carbsG = 22.0f,
            fatG = 3.0f,
            fiberG = 3.6f,
            potassiumMg = 150f
        ),
        FoodItemDefinition(
            name = "Sattu Drink (Savory)",
            category = "Snacks & Drinks",
            servingUnit = "glass",
            servingSizeDescription = "250ml (30g sattu)",
            calories = 125,
            proteinG = 7.0f,
            carbsG = 18.0f,
            fatG = 2.0f,
            fiberG = 5.0f,
            sodiumMg = 220f
        ),
        FoodItemDefinition(
            name = "Coconut Water (Fresh)",
            category = "Snacks & Drinks",
            servingUnit = "glass",
            servingSizeDescription = "250ml",
            calories = 45,
            proteinG = 1.5f,
            carbsG = 9.0f,
            fatG = 0.5f,
            potassiumMg = 600f,
            sodiumMg = 105f
        ),
        FoodItemDefinition(
            name = "Greek Yogurt (Plain)",
            category = "Proteins & Dairy",
            servingUnit = "cup",
            servingSizeDescription = "150g",
            calories = 100,
            proteinG = 15.0f,
            carbsG = 5.0f,
            fatG = 2.0f,
            sodiumMg = 65f,
            potassiumMg = 210f
        ),
        FoodItemDefinition(
            name = "Peanut Butter (Natural)",
            category = "Proteins & Dairy",
            servingUnit = "tbsp",
            servingSizeDescription = "1 tbsp (~16g)",
            calories = 95,
            proteinG = 4.0f,
            carbsG = 3.0f,
            fatG = 8.0f,
            fiberG = 1.0f
        ),
        FoodItemDefinition(
            name = "Watermelon Slices",
            category = "Fruits & Veggies",
            servingUnit = "bowl",
            servingSizeDescription = "200g",
            calories = 60,
            proteinG = 1.2f,
            carbsG = 15.0f,
            fatG = 0.3f,
            fiberG = 0.8f,
            potassiumMg = 220f
        ),
        FoodItemDefinition(
            name = "Oats Khichdi",
            category = "Rice & Grains",
            servingUnit = "katori",
            servingSizeDescription = "1 katori (~180g)",
            calories = 175,
            proteinG = 6.5f,
            carbsG = 28.0f,
            fatG = 4.0f,
            fiberG = 4.5f,
            sodiumMg = 240f
        ),
        FoodItemDefinition(
            name = "Poha with Peanuts",
            category = "Rice & Grains",
            servingUnit = "katori",
            servingSizeDescription = "1 katori (~150g)",
            calories = 210,
            proteinG = 5.0f,
            carbsG = 34.0f,
            fatG = 6.5f,
            fiberG = 2.5f,
            sodiumMg = 220f
        )
    )

    fun search(query: String, category: String = "All"): List<FoodItemDefinition> {
        val q = query.trim().lowercase()
        return preloadedFoods.filter { food ->
            val matchCategory = category == "All" || food.category.equals(category, ignoreCase = true)
            val matchQuery = q.isBlank() || food.name.lowercase().contains(q) || food.category.lowercase().contains(q)
            matchCategory && matchQuery
        }
    }
}
