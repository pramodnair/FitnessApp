package com.example.fitnessapp.data.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SavedMealComboTest {

    @Test
    fun testComboTotalsAndItemConversion() {
        val roti = FoodDatabase.preloadedFoods.first { it.name.contains("Roti", ignoreCase = true) }
        val dal = FoodDatabase.preloadedFoods.first { it.name.contains("Dal Tadka", ignoreCase = true) }

        val scaledRoti = roti.scale(2.0f)
        val scaledDal = dal.scale(1.5f)

        val comboItems = listOf(
            SavedComboItem(
                foodId = roti.id,
                foodName = roti.name,
                servingUnit = roti.servingUnit,
                quantity = 2.0f,
                calories = scaledRoti.calories,
                proteinG = scaledRoti.proteinG,
                carbsG = scaledRoti.carbsG,
                fatG = scaledRoti.fatG
            ),
            SavedComboItem(
                foodId = dal.id,
                foodName = dal.name,
                servingUnit = dal.servingUnit,
                quantity = 1.5f,
                calories = scaledDal.calories,
                proteinG = scaledDal.proteinG,
                carbsG = scaledDal.carbsG,
                fatG = scaledDal.fatG
            )
        )

        val combo = SavedMealCombo(
            id = UUID.randomUUID().toString(),
            name = "Lunch Classic",
            items = comboItems,
            totalCalories = comboItems.sumOf { it.calories },
            totalProtein = comboItems.sumOf { it.proteinG.toDouble() }.toFloat(),
            totalCarbs = comboItems.sumOf { it.carbsG.toDouble() }.toFloat(),
            totalFat = comboItems.sumOf { it.fatG.toDouble() }.toFloat()
        )

        assertEquals("Lunch Classic", combo.name)
        assertEquals(2, combo.items.size)
        assertEquals(scaledRoti.calories + scaledDal.calories, combo.totalCalories)
        assertEquals(scaledRoti.proteinG + scaledDal.proteinG, combo.totalProtein, 0.01f)
        assertTrue(combo.totalCalories > 0)
    }

    @Test
    fun testReconstructedFoodItemDefinitionFromCombo() {
        val roti = FoodDatabase.preloadedFoods.first { it.name.contains("Roti", ignoreCase = true) }
        val scaledRoti = roti.scale(3.0f)
        val comboItem = SavedComboItem(
            foodId = roti.id,
            foodName = roti.name,
            servingUnit = roti.servingUnit,
            quantity = 3.0f,
            calories = scaledRoti.calories,
            proteinG = scaledRoti.proteinG,
            carbsG = scaledRoti.carbsG,
            fatG = scaledRoti.fatG
        )

        val foodDef = comboItem.toFoodItemDefinition()
        assertEquals(roti.name, foodDef.name)
        assertEquals(roti.servingUnit, foodDef.servingUnit)
        // Scaled back to 1.0 base unit
        assertEquals(roti.calories, foodDef.calories)
        assertEquals(roti.proteinG, foodDef.proteinG, 0.1f)
    }
}
