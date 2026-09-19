package com.example.fitnessapp.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.fitnessapp.data.model.DayDeficitStatus
import com.example.fitnessapp.data.model.WeeklyNutritionSummary
import java.util.Locale

@Composable
fun WeeklyDeficitCard(
    summary: WeeklyNutritionSummary,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isNetDeficit = summary.totalNetDeficit >= 0
    val deficitSign = if (isNetDeficit) "+" else ""
    val netColor = if (isNetDeficit) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isNetDeficit) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = netColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "7-DAY DEFICIT & ROLLUP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$deficitSign${summary.totalNetDeficit} kcal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7-day pill timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (day in summary.days) {
                    DayPill(day = day)
                }
            }

            // Expanded details: fat lost, daily average, on-track count
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "EST. FAT IMPACT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val fatKgStr = String.format(Locale.getDefault(), "%.2f", kotlin.math.abs(summary.estimatedKgLost))
                            Text(
                                text = if (summary.estimatedKgLost >= 0) "~$fatKgStr kg lost" else "~$fatKgStr kg surplus",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = netColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DAILY AVERAGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${summary.averageDailyCalories} kcal/day",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Days within deficit budget:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.daysOnTrack} / 7 days",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPill(day: DayDeficitStatus) {
    val pillBg = when {
        day.isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        day.caloriesConsumed == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        day.isUnderBudget -> Color(0xFF2E7D32).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    }

    val iconColor = when {
        day.isFuture || day.caloriesConsumed == 0 -> MaterialTheme.colorScheme.outline
        day.isUnderBudget -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Text(
            text = day.dayLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(pillBg),
            contentAlignment = Alignment.Center
        ) {
            when {
                day.isFuture || day.caloriesConsumed == 0 -> {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "No data",
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                day.isUnderBudget -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "On track",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Over budget",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (day.caloriesConsumed > 0) "${day.caloriesConsumed}" else "—",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1
        )
    }
}
