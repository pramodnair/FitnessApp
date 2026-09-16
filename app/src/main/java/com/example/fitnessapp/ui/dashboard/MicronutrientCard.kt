package com.example.fitnessapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.data.model.Micronutrients

@Composable
fun MicronutrientCard(
    micronutrients: Micronutrients,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ESSENTIAL MICRONUTRIENTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MicroItem(
                    title = "Dietary Fiber",
                    value = "${micronutrients.fiberG.toInt()}g",
                    target = "Goal: >30g",
                    isGood = micronutrients.fiberG >= 20f
                )
                MicroItem(
                    title = "Sugar",
                    value = "${micronutrients.sugarG.toInt()}g",
                    target = "Limit: <35g",
                    isGood = micronutrients.sugarG <= 35f
                )
                MicroItem(
                    title = "Sodium",
                    value = "${micronutrients.sodiumMg.toInt()}mg",
                    target = "Max: 2300mg",
                    isGood = micronutrients.sodiumMg <= 2300f
                )
                MicroItem(
                    title = "Potassium",
                    value = "${micronutrients.potassiumMg.toInt()}mg",
                    target = "Goal: >3500mg",
                    isGood = micronutrients.potassiumMg >= 2000f
                )
            }
        }
    }
}

@Composable
private fun MicroItem(
    title: String,
    value: String,
    target: String,
    isGood: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGood) Color(0xFF2E7D32) else Color(0xFFD32F2F)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = target, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
    }
}
