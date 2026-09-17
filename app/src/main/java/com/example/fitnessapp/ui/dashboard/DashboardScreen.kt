package com.example.fitnessapp.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnessapp.FitnessApplication
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.ui.common.CalorieWarningBanner
import com.example.fitnessapp.ui.common.ProfileSwitcherHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
    onNavigateToFoodSearch: () -> Unit = onNavigateToManualEntry,
    onNavigateToSettings: () -> Unit,
    onNavigateToPartner: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val partnerProfile by viewModel.partnerProfile.collectAsStateWithLifecycle()
    val partnerDuel by viewModel.partnerDuel.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val displaySummary by viewModel.displaySummary.collectAsStateWithLifecycle()
    val displayMeals by viewModel.displayMeals.collectAsStateWithLifecycle()
    val todaySteps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val caloriesBurned by viewModel.caloriesBurned.collectAsStateWithLifecycle()
    val fastingState by viewModel.fastingState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToScanner,
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text = { Text("AI Scan Food", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Top user identity badge + live partner Wi-Fi sync pill + Settings
            ProfileSwitcherHeader(
                activeProfile = activeProfile,
                partnerName = partnerDuel.partnerUser.userName.ifBlank { partnerProfile.name.ifBlank { "Partner" } },
                isPartnerSynced = partnerDuel.isPartnerSynced,
                onNavigateToPartner = onNavigateToPartner,
                onOpenSettings = onNavigateToSettings
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Interactive Date Navigator Stepper
                DateNavigatorBar(
                    selectedDateStr = selectedDate,
                    onPreviousDay = { viewModel.selectPreviousDay() },
                    onNextDay = { viewModel.selectNextDay() },
                    onSelectToday = { viewModel.selectToday() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Warning Banner (80% / 90% / 100%+ calorie warning)
                CalorieWarningBanner(summary = displaySummary)

                Spacer(modifier = Modifier.height(12.dp))

                // Calorie Gauge Meter Card + Macros
                CalorieMeterCard(summary = displaySummary)

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToScanner,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Food Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToFoodSearch,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search & Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Essential Micronutrients Card
                MicronutrientCard(micronutrients = displaySummary.micronutrients)

                Spacer(modifier = Modifier.height(14.dp))

                // Water Hydration Tracker
                WaterTrackerCard(
                    currentMl = displaySummary.waterIntakeMl,
                    targetMl = displaySummary.waterTargetMl,
                    onAddWater = { viewModel.addWater(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Daily Steps & Activity Tracker
                DailyStepsCard(
                    steps = todaySteps,
                    targetSteps = 10000,
                    caloriesBurned = caloriesBurned,
                    consumedCalories = displaySummary.caloriesConsumed,
                    onAddSteps = { viewModel.addManualSteps(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Intermittent Fasting Tracker
                IntermittentFastingCard(
                    fastingState = fastingState,
                    onStartFast = { targetHours, startTimeMs -> viewModel.startFast(targetHours, startTimeMs) },
                    onEndFast = { viewModel.endFast() },
                    onUpdateTarget = { viewModel.updateFastingTarget(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Meals Log for the Selected Date
                MealListCard(
                    meals = displayMeals,
                    onDeleteMeal = { viewModel.deleteMeal(it) }
                )

                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
            }
        }
    }
}

@Composable
private fun DateNavigatorBar(
    selectedDateStr: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isToday = selectedDateStr == todayStr

    val displayDate = remember(selectedDateStr) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDateStr)
            if (date != null) {
                SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(date)
            } else selectedDateStr
        } catch (e: Exception) {
            selectedDateStr
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousDay, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = MaterialTheme.colorScheme.onSurface)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSelectToday)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isToday) "Today ($displayDate)" else displayDate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isToday) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Jump to Today",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onNextDay, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun DailyStepsCard(
    steps: Int,
    targetSteps: Int,
    caloriesBurned: Int,
    consumedCalories: Int,
    onAddSteps: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress = (steps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
    val netCalories = (consumedCalories - caloriesBurned).coerceAtLeast(0)

    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
                hasActivityPermission = granted
                if (granted) {
                    FitnessApplication.instance.stepTrackerManager.startTracking()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPermission = granted
        if (granted) {
            FitnessApplication.instance.stepTrackerManager.startTracking()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF00B4D8).copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = Color(0xFF00B4D8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Steps & Activity",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "$steps / $targetSteps steps • ${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$caloriesBurned kcal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF00B4D8),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Net Calories Strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Calories",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$consumedCalories in - $caloriesBurned burn = $netCalories net kcal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enable Hardware Step Sensor", fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick add:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    OutlinedButton(
                        onClick = { onAddSteps(250) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+250", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { onAddSteps(500) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+500", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
