package com.example.fitnessapp.ui.weight

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.data.model.WeightLog
import java.text.SimpleDateFormat
import java.util.Locale

data class SmoothedWeightPoint(
    val dateStr: String,
    val rawWeight: Float,
    val emaWeight: Float,
    val timestampMs: Long
)

@Composable
fun WeightTrendChart(
    logs: List<WeightLog>,
    targetWeightKg: Float,
    startWeightKg: Float,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf("30_DAYS") }

    // Parse and sort chronologically
    val sortedPoints = remember(logs) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sorted = logs.mapNotNull { log ->
            val time = try {
                dateFormat.parse(log.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
            if (time > 0L) log to time else null
        }.sortedBy { it.second }

        // Compute 7-day Exponential Moving Average (alpha = 2 / (7 + 1) = 0.25)
        val alpha = 0.25f
        var runningEma = 0f
        val result = mutableListOf<SmoothedWeightPoint>()

        sorted.forEachIndexed { index, (log, timeMs) ->
            runningEma = if (index == 0) {
                log.weightKg
            } else {
                log.weightKg * alpha + runningEma * (1f - alpha)
            }
            result.add(
                SmoothedWeightPoint(
                    dateStr = log.date,
                    rawWeight = log.weightKg,
                    emaWeight = runningEma,
                    timestampMs = timeMs
                )
            )
        }
        result
    }

    val nowMs = remember { System.currentTimeMillis() }
    val displayPoints = remember(sortedPoints, selectedTimeframe) {
        when (selectedTimeframe) {
            "7_DAYS" -> sortedPoints.filter { (nowMs - it.timestampMs) <= 7L * 24 * 3600 * 1000 }
            "30_DAYS" -> sortedPoints.filter { (nowMs - it.timestampMs) <= 30L * 24 * 3600 * 1000 }
            else -> sortedPoints
        }
    }

    // Weekly Rate Calculation (difference over recent 7 days of EMA)
    val weeklyRate = remember(sortedPoints) {
        if (sortedPoints.size < 2) 0f
        else {
            val latest = sortedPoints.last().emaWeight
            // Find point roughly 7 days ago, or the earliest available
            val weekAgoTime = sortedPoints.last().timestampMs - (7L * 24 * 3600 * 1000)
            val weekAgoPoint = sortedPoints.filter { it.timestampMs <= weekAgoTime }.maxByOrNull { it.timestampMs }
                ?: sortedPoints.first()
            val dayDiff = ((sortedPoints.last().timestampMs - weekAgoPoint.timestampMs) / (24f * 3600 * 1000)).coerceAtLeast(1f)
            val totalDiff = latest - weekAgoPoint.emaWeight
            (totalDiff / dayDiff) * 7f
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Title & Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMOOTHED WEIGHT TREND",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "7-Day Moving Avg (Noise Filter)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Timeframe Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("7_DAYS" to "7D", "30_DAYS" to "30D", "ALL" to "All").forEach { (key, label) ->
                        val isSelected = selectedTimeframe == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedTimeframe = key }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trend Rate & Latest Smoothed Badge
            if (sortedPoints.isNotEmpty()) {
                val latestPoint = sortedPoints.last()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Smoothed Trend", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", latestPoint.emaWeight),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("kg", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(Raw: ${String.format(Locale.getDefault(), "%.1f", latestPoint.rawWeight)} kg)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Weekly Pace Badge
                    val isLosing = weeklyRate < -0.05f
                    val isGaining = weeklyRate > 0.05f
                    val paceColor = if (isLosing) Color(0xFF2E7D32) else if (isGaining) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                    val paceBg = paceColor.copy(alpha = 0.12f)
                    val paceIcon = if (isLosing) Icons.Default.TrendingDown else if (isGaining) Icons.Default.TrendingUp else Icons.Default.TrendingFlat

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = paceBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(paceIcon, contentDescription = null, tint = paceColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (weeklyRate.isNaN()) "Calibrating"
                                else "${if (weeklyRate > 0) "+" else ""}${String.format(Locale.getDefault(), "%.2f", weeklyRate)} kg/wk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = paceColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            if (displayPoints.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (displayPoints.isEmpty()) "Log weights to visualize your smoothed trend curve."
                        else "Need at least 2 logs in this timeframe to generate trend chart.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val targetLineColor = Color(0xFF2E7D32)

                val allWeights = displayPoints.flatMap { listOf(it.rawWeight, it.emaWeight) } + listOfNotNull(if (targetWeightKg > 0) targetWeightKg else null)
                val minY = (allWeights.minOrNull() ?: 60f) - 0.5f
                val maxY = (allWeights.maxOrNull() ?: 80f) + 0.5f
                val rangeY = (maxY - minY).coerceAtLeast(1f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 20.dp.toPx()
                    val chartHeight = height - paddingBottom

                    val stepX = width / (displayPoints.size - 1).coerceAtLeast(1)

                    fun getY(weight: Float): Float {
                        val fraction = (weight - minY) / rangeY
                        return chartHeight - (fraction * chartHeight)
                    }

                    // 1. Draw Target Baseline (if target is within reasonable view)
                    if (targetWeightKg in minY..maxY) {
                        val targetY = getY(targetWeightKg)
                        drawLine(
                            color = targetLineColor.copy(alpha = 0.4f),
                            start = Offset(0f, targetY),
                            end = Offset(width, targetY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    }

                    // 2. Build Smoothed EMA Path & Gradient Area
                    val emaPath = Path()
                    val areaPath = Path()

                    displayPoints.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = getY(pt.emaWeight)
                        if (i == 0) {
                            emaPath.moveTo(x, y)
                            areaPath.moveTo(x, chartHeight)
                            areaPath.lineTo(x, y)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevY = getY(displayPoints[i - 1].emaWeight)
                            val cX1 = prevX + (x - prevX) / 2f
                            val cY1 = prevY
                            val cX2 = prevX + (x - prevX) / 2f
                            val cY2 = y
                            emaPath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                            areaPath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                        }
                    }

                    areaPath.lineTo((displayPoints.size - 1) * stepX, chartHeight)
                    areaPath.close()

                    // Draw subtle gradient area under trend line
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.22f), primaryColor.copy(alpha = 0.01f)),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw smoothed trend line
                    drawPath(
                        path = emaPath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 3. Draw raw weigh-in scatter dots (to show daily noise)
                    displayPoints.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val rawY = getY(pt.rawWeight)
                        // Raw scatter dot
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.35f),
                            radius = 3.5.dp.toPx(),
                            center = Offset(x, rawY)
                        )
                        // EMA line anchor dot
                        val emaY = getY(pt.emaWeight)
                        if (i == displayPoints.lastIndex) {
                            drawCircle(
                                color = primaryColor,
                                radius = 5.dp.toPx(),
                                center = Offset(x, emaY)
                            )
                        }
                    }
                }

                // Legend row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("7-Day EMA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Daily Weigh-ins", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (targetWeightKg > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp, 2.dp)
                                        .background(Color(0xFF2E7D32))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Target (${targetWeightKg}kg)", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }

                    Text(
                        text = "${displayPoints.first().dateStr} → ${displayPoints.last().dateStr}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
