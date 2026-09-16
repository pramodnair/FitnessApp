package com.example.fitnessapp.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fitnessapp.data.model.MealLog
import com.example.fitnessapp.data.model.MealType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MealListCard(
    meals: List<MealLog>,
    onDeleteMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<MealType?>(null) }
    var mealToDelete by remember { mutableStateOf<MealLog?>(null) }

    val filteredMeals = if (selectedFilter == null) {
        meals
    } else {
        meals.filter { it.mealType == selectedFilter }
    }

    val totalCaloriesInView = filteredMeals.sumOf { it.calories }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOGGED MEALS (${meals.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                if (filteredMeals.isNotEmpty()) {
                    Text(
                        text = "$totalCaloriesInView kcal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-Menu Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterTabChip(
                    label = "All (${meals.size})",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                MealType.entries.forEach { type ->
                    val count = meals.count { it.mealType == type }
                    val label = when (type) {
                        MealType.BREAKFAST -> "🍳 Breakfast"
                        MealType.LUNCH -> "🍛 Lunch"
                        MealType.SNACK -> "🍎 Snack"
                        MealType.DINNER -> "🍲 Dinner"
                    }
                    FilterTabChip(
                        label = "$label ($count)",
                        isSelected = selectedFilter == type,
                        onClick = { selectedFilter = if (selectedFilter == type) null else type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredMeals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedFilter == null) "No meals logged for this day" else "No ${selectedFilter?.label} logged",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Snap or upload a photo to track calories effortlessly!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                filteredMeals.forEach { meal ->
                    ExpandableMealItemRow(
                        meal = meal,
                        onDeleteRequest = { mealToDelete = meal }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    mealToDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text("Delete Meal Log?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove \"${meal.title}\" (${meal.calories} kcal) from your daily intake?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMeal(meal.id)
                        mealToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mealToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpandableMealItemRow(
    meal: MealLog,
    onDeleteRequest: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val timeFormatted = remember(meal.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(meal.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { isExpanded = !isExpanded }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Photo thumbnail or icon
                if (!meal.photoPath.isNullOrBlank() && File(meal.photoPath).exists()) {
                    AsyncImage(
                        model = File(meal.photoPath),
                        contentDescription = meal.title,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = meal.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${meal.mealType.label} • $timeFormatted • P:${meal.proteinG.toInt()}g C:${meal.carbsG.toInt()}g F:${meal.fatG.toInt()}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${meal.calories} kcal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDeleteRequest, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded Nutrients and AI Insights Sub-Card
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(10.dp)
            ) {
                // Macros row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    NutrientPill(label = "Protein", value = "${meal.proteinG}g", color = Color(0xFF3F51B5))
                    NutrientPill(label = "Carbs", value = "${meal.carbsG}g", color = Color(0xFF009688))
                    NutrientPill(label = "Fat", value = "${meal.fatG}g", color = Color(0xFFFF9800))
                }

                // Micronutrients row
                val m = meal.micronutrients
                if (m.fiberG > 0 || m.sugarG > 0 || m.sodiumMg > 0 || m.potassiumMg > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        if (m.fiberG > 0) NutrientPill(label = "Fiber", value = "${m.fiberG}g", color = Color(0xFF4CAF50))
                        if (m.sugarG > 0) NutrientPill(label = "Sugar", value = "${m.sugarG}g", color = Color(0xFFE91E63))
                        if (m.sodiumMg > 0) NutrientPill(label = "Sodium", value = "${m.sodiumMg.toInt()}mg", color = Color(0xFF9C27B0))
                        if (m.potassiumMg > 0) NutrientPill(label = "Potassium", value = "${m.potassiumMg.toInt()}mg", color = Color(0xFF00BCD4))
                    }
                }

                // AI Insights
                if (!meal.aiInsights.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 AI Insights: ${meal.aiInsights}",
                        fontSize = 11.sp,
                        color = Color(0xFF00796B),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NutrientPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
