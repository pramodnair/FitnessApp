package com.example.fitnessapp.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.data.model.CalorieWarningLevel
import com.example.fitnessapp.data.model.DailyNutritionSummary

@Composable
fun CalorieMeterCard(
    summary: DailyNutritionSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DAILY CALORIE BUDGET",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Calorie Ring
            val progressFraction = if (summary.calorieBudget > 0) {
                (summary.netCalories.toFloat() / summary.calorieBudget.toFloat()).coerceAtMost(1.0f)
            } else 0f

            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = tween(durationMillis = 800),
                label = "ring_progress"
            )

            val ringColor = when (summary.warningLevel) {
                CalorieWarningLevel.EXCEEDED -> Color(0xFFE53935)
                CalorieWarningLevel.NEAR_LIMIT -> Color(0xFFFF9800)
                CalorieWarningLevel.APPROACHING -> Color(0xFFFFB300)
                CalorieWarningLevel.SAFE -> Color(0xFF43A047)
            }

            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    // Background track
                    drawCircle(
                        color = Color(0xFFEEEEEE),
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress arc
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.remainingCalories.coerceAtLeast(0)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (summary.remainingCalories >= 0) "kcal remaining" else "kcal over budget",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Consumed vs Budget Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Consumed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${summary.caloriesConsumed} kcal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${summary.calorieBudget} kcal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Macros Progress Bars (Protein, Carbs, Fat)
            MacroProgressBar(
                name = "Protein",
                consumed = summary.proteinConsumedG,
                target = summary.proteinTargetG,
                color = Color(0xFF3F51B5)
            )
            Spacer(modifier = Modifier.height(8.dp))
            MacroProgressBar(
                name = "Carbs",
                consumed = summary.carbsConsumedG,
                target = summary.carbsTargetG,
                color = Color(0xFF009688)
            )
            Spacer(modifier = Modifier.height(8.dp))
            MacroProgressBar(
                name = "Fat",
                consumed = summary.fatConsumedG,
                target = summary.fatTargetG,
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun MacroProgressBar(
    name: String,
    consumed: Float,
    target: Float,
    color: Color
) {
    val fraction = if (target > 0f) (consumed / target).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${consumed.toInt()}g / ${target.toInt()}g",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}
