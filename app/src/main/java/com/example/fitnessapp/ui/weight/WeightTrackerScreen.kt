package com.example.fitnessapp.ui.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.data.model.WeightLog
import com.example.fitnessapp.domain.BmiCalculator
import com.example.fitnessapp.domain.BmiCategory
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerScreen(
    modifier: Modifier = Modifier,
    viewModel: WeightTrackerViewModel = viewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val weightHistory by viewModel.weightHistory.collectAsStateWithLifecycle()
    val rec = viewModel.getNutritionRecommendations()

    var showAddWeightDialog by remember { mutableStateOf(false) }
    var showEditStartWeightDialog by remember { mutableStateOf(false) }
    var logToEdit by remember { mutableStateOf<WeightLog?>(null) }
    var logToDelete by remember { mutableStateOf<WeightLog?>(null) }

    val idealRange = BmiCalculator.getIdealWeightRange(activeProfile.heightCm)
    val userLogs = weightHistory.filter { it.userId == activeProfile.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Weight Loss & BMI Journey",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddWeightDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Weight")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // BMI Overview Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CURRENT BMI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${rec.currentBmi}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val category = BmiCalculator.getCategory(rec.currentBmi)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(category.colorHex).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category.label,
                                color = Color(category.colorHex),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress towards target weight
                    val startWeight = activeProfile.startWeightKg
                    val currentWeight = activeProfile.currentWeightKg
                    val targetWeight = activeProfile.targetWeightKg

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showEditStartWeightDialog = true }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
                            Text("Start: ${startWeight} kg", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Starting Weight",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text("Current: ${currentWeight} kg", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Target: ${targetWeight} kg", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val progressFraction = if (startWeight > targetWeight) {
                        ((startWeight - currentWeight) / (startWeight - targetWeight)).coerceIn(0f, 1f)
                    } else 1.0f

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Estimated Target Timeline & Ideal Range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Healthy Weight Range", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${idealRange.first} - ${idealRange.second} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Est. Days to Goal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("~${rec.estimatedDaysToGoal} days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Milestone Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Caloric Deficit: ${activeProfile.deficitLevel.label}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Daily Deficit: -${(rec.tdee - rec.dailyCalorieBudget).toInt()} kcal | Safe BMR: ${rec.bmr.toInt()} kcal",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Milestone Countdown & Progress Card
            val startingWeight = activeProfile.startWeightKg
            val totalLostSoFar = (startingWeight - activeProfile.currentWeightKg).coerceAtLeast(0f)
            val remainingToTarget = (activeProfile.currentWeightKg - activeProfile.targetWeightKg).coerceAtLeast(0f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "JOURNEY MILESTONE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (totalLostSoFar > 0f) "🔥 ${String.format(Locale.getDefault(), "%.1f", totalLostSoFar)} kg lost so far!" else "🎯 Starting your journey",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", remainingToTarget)} kg to go",
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7-Day Smoothed EMA Trend Chart
            WeightTrendChart(
                logs = userLogs,
                targetWeightKg = activeProfile.targetWeightKg,
                startWeightKg = activeProfile.startWeightKg
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Sub-Menu Filter for History
            var historyFilter by remember { mutableStateOf("ALL") }
            val nowMs = remember { System.currentTimeMillis() }
            val filteredLogs = remember(userLogs, historyFilter) {
                when (historyFilter) {
                    "7_DAYS" -> userLogs.filter {
                        val d = try { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)?.time } catch (e: Exception) { null }
                        d != null && (nowMs - d) <= 7L * 24 * 3600 * 1000
                    }
                    "30_DAYS" -> userLogs.filter {
                        val d = try { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)?.time } catch (e: Exception) { null }
                        d != null && (nowMs - d) <= 30L * 24 * 3600 * 1000
                    }
                    else -> userLogs
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEIGHT HISTORY (${filteredLogs.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                // Filter tabs
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ALL" to "All", "30_DAYS" to "30 Days", "7_DAYS" to "7 Days").forEach { (key, label) ->
                        val isSel = historyFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { historyFilter = key }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (filteredLogs.isEmpty()) {
                Text(
                    text = "No weights logged in this timeframe.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                filteredLogs.forEach { log ->
                    WeightLogRow(
                        log = log,
                        onEdit = { logToEdit = log },
                        onDelete = { logToDelete = log }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }

        if (showAddWeightDialog) {
            AddWeightDialog(
                currentWeight = activeProfile.currentWeightKg,
                onConfirm = { weight, notes ->
                    viewModel.addWeightEntry(weight, notes)
                    showAddWeightDialog = false
                },
                onDismiss = { showAddWeightDialog = false }
            )
        }

        if (logToEdit != null) {
            EditWeightEntryDialog(
                log = logToEdit!!,
                onConfirm = { weight, notes ->
                    viewModel.editWeightEntry(logToEdit!!.id, weight, notes)
                    logToEdit = null
                },
                onDismiss = { logToEdit = null }
            )
        }

        if (logToDelete != null) {
            AlertDialog(
                onDismissRequest = { logToDelete = null },
                title = { Text("Delete Weight Entry", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Are you sure you want to delete the entry of ${logToDelete!!.weightKg} kg on ${logToDelete!!.date}?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteWeightEntry(logToDelete!!.id)
                            logToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { logToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEditStartWeightDialog) {
            EditStartWeightDialog(
                currentStartWeight = activeProfile.startWeightKg,
                heightCm = activeProfile.heightCm,
                onConfirm = { weight ->
                    viewModel.updateStartingWeight(weight)
                    showEditStartWeightDialog = false
                },
                onDismiss = { showEditStartWeightDialog = false }
            )
        }
    }
}

@Composable
private fun WeightLogRow(
    log: WeightLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MonitorWeight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(log.date, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (log.notes.isNotBlank()) {
                    Text(log.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text("${log.weightKg} kg", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("BMI ${log.bmi}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddWeightDialog(
    currentWeight: Float,
    onConfirm: (Float, String) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf(currentWeight.toString()) }
    var notesText by remember { mutableStateOf("") }

    fun adjustWeight(delta: Float) {
        val curr = weightText.toFloatOrNull() ?: currentWeight
        val next = (curr + delta).coerceIn(30f, 250f)
        weightText = String.format(Locale.getDefault(), "%.1f", next)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight Check-In", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quick increment/decrement buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { adjustWeight(-0.5f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("-0.5", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(-0.1f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("-0.1", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(0.1f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("+0.1", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(0.5f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("+0.5", fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (e.g. morning, fasted)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightText.toFloatOrNull() ?: currentWeight
                    onConfirm(w, notesText)
                }
            ) {
                Text("Save Weight")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditWeightEntryDialog(
    log: WeightLog,
    onConfirm: (Float, String) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf(log.weightKg.toString()) }
    var notesText by remember { mutableStateOf(log.notes) }

    fun adjustWeight(delta: Float) {
        val curr = weightText.toFloatOrNull() ?: log.weightKg
        val next = (curr + delta).coerceIn(30f, 250f)
        weightText = String.format(Locale.getDefault(), "%.1f", next)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Weight Entry (${log.date})", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { adjustWeight(-0.5f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("-0.5", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(-0.1f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("-0.1", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(0.1f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("+0.1", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(0.5f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("+0.5", fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightText.toFloatOrNull() ?: log.weightKg
                    onConfirm(w, notesText)
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditStartWeightDialog(
    currentStartWeight: Float,
    heightCm: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf(currentStartWeight.toString()) }

    fun adjustWeight(delta: Float) {
        val curr = weightText.toFloatOrNull() ?: currentStartWeight
        val next = (curr + delta).coerceIn(30f, 250f)
        weightText = String.format(Locale.getDefault(), "%.1f", next)
    }

    val enteredWeight = weightText.toFloatOrNull() ?: currentStartWeight
    val startBmi = BmiCalculator.calculateBmi(enteredWeight, heightCm)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Starting Weight", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Set your baseline starting weight. Total kilograms lost and progress towards your target are calculated from this baseline.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Starting Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quick increment/decrement buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { adjustWeight(-1.0f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("-1.0", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(-0.1f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("-0.1", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(0.1f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("+0.1", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { adjustWeight(1.0f) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text("+1.0", fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Baseline BMI: $startBmi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    val cat = BmiCalculator.getCategory(startBmi)
                    Text(cat.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(cat.colorHex))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightText.toFloatOrNull() ?: currentStartWeight
                    onConfirm(w)
                }
            ) {
                Text("Save Baseline")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

