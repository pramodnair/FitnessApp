package com.example.fitnessapp.data.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodDatabaseTest {

    @Test
    fun testPreloadedDatabaseContainsStaplesAndCategories() {
        val foods = FoodDatabase.preloadedFoods

        // Ensure we have over 70 items
        assertTrue("Food database should have at least 70 items, found ${foods.size}", foods.size >= 70)

        // Ensure all 7 functional categories exist
        val categories = foods.map { it.category }.distinct()
        assertTrue(categories.contains("Breads"))
        assertTrue(categories.contains("Curries & Dals"))
        assertTrue(categories.contains("Rice & Grains"))
        assertTrue(categories.contains("Proteins & Dairy"))
        assertTrue(categories.contains("South Indian"))
        assertTrue(categories.contains("Fruits & Veggies"))
        assertTrue(categories.contains("Snacks & Drinks"))

        // Ensure each food has reasonable positive nutritional numbers
        for (food in foods) {
            assertTrue("Food ${food.name} should have valid calories", food.calories > 0)
            assertTrue("Food ${food.name} should have non-negative protein", food.proteinG >= 0f)
            assertTrue("Food ${food.name} should have non-negative carbs", food.carbsG >= 0f)
            assertTrue("Food ${food.name} should have non-negative fat", food.fatG >= 0f)
            assertTrue("Food ${food.name} should have non-blank serving unit", food.servingUnit.isNotBlank())
        }
    }

    @Test
    fun testDynamicScalingOfPortions() {
        val roti = FoodDatabase.preloadedFoods.first { it.name.contains("Roti", ignoreCase = true) }

        // 1 roti base
        val scaled1 = roti.scale(1.0f)
        assertEquals(roti.calories, scaled1.calories)
        assertEquals(roti.proteinG, scaled1.proteinG, 0.01f)

        // 3 rotis: calories and macros should triple
        val scaled3 = roti.scale(3.0f)
        assertEquals(roti.calories * 3, scaled3.calories)
        assertEquals(roti.proteinG * 3f, scaled3.proteinG, 0.1f)
        assertEquals(roti.carbsG * 3f, scaled3.carbsG, 0.1f)
        assertEquals(roti.fatG * 3f, scaled3.fatG, 0.1f)
        assertTrue(scaled3.portionDescription.contains("3 piece"))

        // Fractional scaling: 1.5 katori dal
        val dal = FoodDatabase.preloadedFoods.first { it.name.contains("Dal Tadka", ignoreCase = true) }
        val scaledDal = dal.scale(1.5f)
        assertEquals((dal.calories * 1.5f).toInt(), scaledDal.calories)
        assertEquals(dal.proteinG * 1.5f, scaledDal.proteinG, 0.1f)
        assertTrue(scaledDal.portionDescription.contains("1.5"))
    }

    @Test
    fun testMicronutrientScaling() {
        val dal = FoodDatabase.preloadedFoods.first { it.name.contains("Dal", ignoreCase = true) }
        val scaled2 = dal.scale(2.0f)

        assertEquals(dal.fiberG * 2f, scaled2.micronutrients.fiberG, 0.1f)
        assertEquals(dal.sodiumMg * 2f, scaled2.micronutrients.sodiumMg, 0.1f)
        assertEquals(dal.potassiumMg * 2f, scaled2.micronutrients.potassiumMg, 0.1f)
    }

    @Test
    fun testEdgeCaseZeroQuantity() {
        val apple = FoodDatabase.preloadedFoods.first { it.name.contains("Apple", ignoreCase = true) }
        val scaledZero = apple.scale(0f)
        assertEquals(0, scaledZero.calories)
        assertEquals(0f, scaledZero.proteinG, 0.01f)
        assertEquals(0f, scaledZero.carbsG, 0.01f)
        assertEquals(0f, scaledZero.fatG, 0.01f)
    }
}
