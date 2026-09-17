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
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.fitnessapp.data.model.FastingProtocol
import com.example.fitnessapp.data.model.FastingState
import com.example.fitnessapp.data.model.MetabolicStage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntermittentFastingCard(
    fastingState: FastingState,
    onStartFast: (targetHours: Int, startTimeMs: Long) -> Unit,
    onEndFast: () -> Unit,
    onUpdateTarget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Ticking state every 30 seconds for live countdown
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(fastingState.isFasting) {
        while (fastingState.isFasting) {
            currentTimeMs = System.currentTimeMillis()
            delay(10000L) // Refresh every 10s
        }
    }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var selectedHours by remember(fastingState.targetHours) { mutableStateOf(fastingState.targetHours) }

    val elapsedMillis = fastingState.getElapsedMillis(currentTimeMs)
    val elapsedHoursTotal = elapsedMillis / (1000f * 3600f)
    val elapsedH = (elapsedMillis / (1000 * 3600)).toInt()
    val elapsedM = ((elapsedMillis % (1000 * 3600)) / (1000 * 60)).toInt()

    val targetMillis = fastingState.targetHours * 3600 * 1000L
    val remainingMillis = (targetMillis - elapsedMillis).coerceAtLeast(0L)
    val remainingH = (remainingMillis / (1000 * 3600)).toInt()
    val remainingM = ((remainingMillis % (1000 * 3600)) / (1000 * 60)).toInt()

    val progress = (elapsedHoursTotal / fastingState.targetHours.toFloat()).coerceIn(0f, 1f)
    val isGoalAchieved = elapsedMillis >= targetMillis
    val currentStage = fastingState.getCurrentStage(currentTimeMs)
    val stageColor = Color(currentStage.colorHex)

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (fastingState.isFasting) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (fastingState.isFasting) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (fastingState.isFasting) stageColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (fastingState.isFasting) Icons.Default.LocalFireDepartment else Icons.Default.AvTimer,
                                contentDescription = null,
                                tint = if (fastingState.isFasting) stageColor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "INTERMITTENT FASTING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fastingState.isFasting) stageColor else MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (fastingState.isFasting) "${fastingState.targetHours}h Fast in Progress" else "Eating Window Open",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (fastingState.isFasting) stageColor.copy(alpha = 0.15f) else Color(0xFF2E7D32).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (fastingState.isFasting) (if (isGoalAchieved) "Target Met! 🏆" else "Fasting 🔥") else "Eating Window 🥗",
                        color = if (fastingState.isFasting) stageColor else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (fastingState.isFasting) {
                // Active Fast Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${elapsedH}h ${elapsedM}m",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isGoalAchieved) "Fast Completed! Extended by ${elapsedH - fastingState.targetHours}h ${elapsedM}m"
                            else "${remainingH}h ${remainingM}m remaining until ${fastingState.targetHours}h goal",
                            fontSize = 12.sp,
                            color = if (isGoalAchieved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isGoalAchieved) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Progress Percentage Pill
                    Surface(
                        shape = CircleShape,
                        color = stageColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = stageColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = stageColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metabolic Zone Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = stageColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(stageColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentStage.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = stageColor
                            )
                            Text(
                                text = currentStage.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Started at note & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val startStr = timeFormatter.format(Date(fastingState.startTimeMs))
                    Text(
                        text = "Started at $startStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAdjustDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adjust", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onEndFast,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End Fast", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                // Inactive Fasting Screen: Protocol selection & Start
                Text(
                    text = "Select your fasting goal for today. Fasting promotes cellular autophagy, burns stored body fat, and optimizes insulin sensitivity.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Protocol selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        14 to "14:10",
                        16 to "16:8",
                        18 to "18:6",
                        20 to "20:4"
                    ).forEach { (hours, label) ->
                        val isSelected = selectedHours == hours
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedHours = hours
                                    onUpdateTarget(hours)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${hours}h fast",
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onStartFast(selectedHours, System.currentTimeMillis()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Fast Now (${selectedHours}h Goal)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Adjust Fasting Dialog
    if (showAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Adjust Fasting Start Time", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Did you finish dinner earlier? Adjust when your fast started:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "Current Time" to 0L,
                        "30 minutes ago" to (30 * 60 * 1000L),
                        "1 hour ago" to (60 * 60 * 1000L),
                        "2 hours ago (After Dinner)" to (2 * 60 * 60 * 1000L),
                        "3 hours ago" to (3 * 60 * 60 * 1000L)
                    ).forEach { (label, offsetMs) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newStart = System.currentTimeMillis() - offsetMs
                                    onStartFast(fastingState.targetHours, newStart)
                                    showAdjustDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
