package com.example.fitnessapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.AppThemeMode
import com.example.fitnessapp.data.security.AppLockManager
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.data.model.ActivityLevel
import com.example.fitnessapp.data.model.DeficitLevel
import com.example.fitnessapp.data.model.Gender
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember(profile) { mutableStateOf(profile.name) }
    var gender by remember(profile) { mutableStateOf(profile.gender) }
    var ageText by remember(profile) { mutableStateOf(profile.age.toString()) }
    var heightText by remember(profile) { mutableStateOf(profile.heightCm.toInt().toString()) }
    var currentWeightText by remember(profile) { mutableStateOf(profile.currentWeightKg.toString()) }
    var startWeightText by remember(profile) { mutableStateOf(profile.startWeightKg.toString()) }
    var targetWeightText by remember(profile) { mutableStateOf(profile.targetWeightKg.toString()) }
    var activityLevel by remember(profile) { mutableStateOf(profile.activityLevel) }
    var deficitLevel by remember(profile) { mutableStateOf(profile.deficitLevel) }
    var customDeficitText by remember(profile) { mutableStateOf(profile.customDeficitKcal.toString()) }
    var apiKey by remember(profile) { mutableStateOf(profile.geminiApiKey) }
    var selectedTargetBmi by remember(profile) { mutableStateOf(profile.targetBmi) }
    var autoCalculateTarget by remember(profile) { mutableStateOf(profile.autoCalculateTargetFromBmi) }
    var dailyStepTargetText by remember(profile) { mutableStateOf(profile.dailyStepTarget.toString()) }
    var dailyWaterTargetText by remember(profile) { mutableStateOf(profile.dailyWaterTargetMl.toString()) }

    // API Key test state
    var isTestingApiKey by remember { mutableStateOf(false) }
    var apiKeyTestResult by remember { mutableStateOf<String?>(null) }
    var apiKeyTestSuccess by remember { mutableStateOf<Boolean?>(null) }

    val currentHeight = heightText.toFloatOrNull() ?: profile.heightCm
    val idealRange = com.example.fitnessapp.domain.BmiCalculator.getIdealWeightRange(currentHeight)
    val currentEnteredTargetWeight = targetWeightText.toFloatOrNull() ?: profile.targetWeightKg
    val resultingTargetBmi = com.example.fitnessapp.domain.BmiCalculator.calculateBmi(currentEnteredTargetWeight, currentHeight)
    val resultingCategory = com.example.fitnessapp.domain.BmiCalculator.getCategory(resultingTargetBmi)

    fun applyTargetBmi(bmi: Float) {
        selectedTargetBmi = bmi
        autoCalculateTarget = true
        if (currentHeight > 50f) {
            val sWeight = startWeightText.toFloatOrNull() ?: profile.startWeightKg
            val calcWeight = com.example.fitnessapp.domain.BmiCalculator.calculateTargetWeight(
                heightCm = currentHeight,
                startWeightKg = sWeight,
                targetBmi = bmi,
                deficitLevel = deficitLevel
            )
            if (calcWeight > 0f) {
                targetWeightText = calcWeight.toString()
            }
        }
    }

    LaunchedEffect(currentHeight, startWeightText, deficitLevel, selectedTargetBmi, autoCalculateTarget) {
        if (autoCalculateTarget && currentHeight > 50f) {
            val sWeight = startWeightText.toFloatOrNull() ?: profile.startWeightKg
            val calc = com.example.fitnessapp.domain.BmiCalculator.calculateTargetWeight(
                heightCm = currentHeight,
                startWeightKg = sWeight,
                targetBmi = selectedTargetBmi,
                deficitLevel = deficitLevel
            )
            if (calc > 0f) {
                targetWeightText = calc.toString()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile & Calorie Deficit Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
            // Bio Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Physical Attributes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gender chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.entries.forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(if (g == Gender.MALE) "Male 👨" else "Female 👩") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Age (yrs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startWeightText,
                            onValueChange = {
                                startWeightText = it
                                if (currentWeightText.isBlank() || currentWeightText == "0" || currentWeightText == profile.startWeightKg.toString()) {
                                    currentWeightText = it
                                }
                            },
                            label = { Text("Starting Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currentWeightText,
                            onValueChange = { currentWeightText = it },
                            label = { Text("Current Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Weight & BMI Calculation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Target Weight & BMI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Auto-calculate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = autoCalculateTarget,
                                onCheckedChange = { checked ->
                                    autoCalculateTarget = checked
                                    if (checked && currentHeight > 50f) {
                                        applyTargetBmi(selectedTargetBmi)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Healthy weight range for ${currentHeight.toInt()}cm: ${idealRange.first} - ${idealRange.second} kg (BMI 18.5 - 24.9)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Target BMI Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = (selectedTargetBmi == 21.0f),
                            onClick = {
                                autoCalculateTarget = true
                                applyTargetBmi(21.0f)
                            },
                            label = { Text("Lean 21.0", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (selectedTargetBmi == 22.0f),
                            onClick = {
                                autoCalculateTarget = true
                                applyTargetBmi(22.0f)
                            },
                            label = { Text("⭐ Optimal 22.0", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (selectedTargetBmi == 23.5f),
                            onClick = {
                                autoCalculateTarget = true
                                applyTargetBmi(23.5f)
                            },
                            label = { Text("Fit 23.5", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = targetWeightText,
                        onValueChange = {
                            targetWeightText = it
                            autoCalculateTarget = false
                        },
                        label = { Text("Target Goal Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Result Indicator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target: $targetWeightText kg  ➔  BMI: $resultingTargetBmi (${resultingCategory.label})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(resultingCategory.colorHex)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calorie Deficit Customization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customizable Calorie Deficit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Choose your fat loss pace. Mifflin-St Jeor equation automatically calculates your daily budget.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DeficitLevel.entries.forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            FilterChip(
                                selected = deficitLevel == level,
                                onClick = {
                                    deficitLevel = level
                                    if (autoCalculateTarget) {
                                        when (level) {
                                            DeficitLevel.AGGRESSIVE -> selectedTargetBmi = 21.0f
                                            DeficitLevel.MODERATE -> selectedTargetBmi = 22.0f
                                            DeficitLevel.MILD -> selectedTargetBmi = 23.5f
                                            DeficitLevel.CUSTOM -> {}
                                        }
                                    }
                                },
                                label = { Text(level.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (deficitLevel == DeficitLevel.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customDeficitText,
                            onValueChange = { customDeficitText = it },
                            label = { Text("Custom Deficit (kcal/day)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Lifestyle & Activity Targets Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily Activity & Hydration Targets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Set your daily step goal and target water intake for personalized progress tracking.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Step Goal
                    Text(
                        text = "DAILY STEP GOAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = dailyStepTargetText,
                        onValueChange = { dailyStepTargetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Daily Step Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(6000, 8000, 10000, 12000).forEach { steps ->
                            val isSel = dailyStepTargetText == steps.toString()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { dailyStepTargetText = steps.toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${steps / 1000}k",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Water Intake Target
                    Text(
                        text = "DAILY WATER TARGET (ML)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = dailyWaterTargetText,
                        onValueChange = { dailyWaterTargetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Daily Water Target (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(2000, 2500, 3000, 3500).forEach { ml ->
                            val isSel = dailyWaterTargetText == ml.toString()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { dailyWaterTargetText = ml.toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${ml / 1000}.${(ml % 1000) / 100}L".replace(".0L", "L"),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Gemini Vision API Key Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Gemini Vision API Key", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Used for AI food meal recognition powered by Gemini 3.8 Flash (with automatic fallback to Gemini 3.5 & 2.0). Free key available at aistudio.google.com. Tip: For multi-device or couples sharing, each person using their own free key avoids rate limits.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            apiKeyTestResult = null
                            apiKeyTestSuccess = null
                        },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (apiKey.isBlank()) {
                                    apiKeyTestSuccess = false
                                    apiKeyTestResult = "Please paste an API key first."
                                    return@OutlinedButton
                                }
                                scope.launch {
                                    isTestingApiKey = true
                                    apiKeyTestResult = null
                                    val res = viewModel.testApiKey(apiKey)
                                    isTestingApiKey = false
                                    apiKeyTestSuccess = res.isValid
                                    apiKeyTestResult = res.message
                                }
                            },
                            enabled = !isTestingApiKey
                        ) {
                            if (isTestingApiKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify API Key", fontSize = 12.sp)
                            }
                        }
                    }

                    // Test Feedback Banner
                    apiKeyTestResult?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        val isSuccess = apiKeyTestSuccess == true
                        val bannerColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        val textColor = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                        val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = bannerColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Notifications & Reminders Card
            val context = LocalContext.current
            var hydrationRemindersEnabled by remember { mutableStateOf(true) }
            var mealRemindersEnabled by remember { mutableStateOf(true) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Smart Reminders & Notifications", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Local on-device alerts to help you hit your hydration goals, fasts, and meal tracking consistency.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hydration Reminders", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Gentle water alerts every 2 hours (9 AM - 9 PM)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hydrationRemindersEnabled,
                            onCheckedChange = {
                                hydrationRemindersEnabled = it
                                if (it) {
                                    com.example.fitnessapp.data.notification.ReminderScheduler.scheduleNextHydration(context)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Meal Logging Prompts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Lunch (1:30 PM) & Dinner (8:30 PM) check-ins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = mealRemindersEnabled,
                            onCheckedChange = {
                                mealRemindersEnabled = it
                                if (it) {
                                    com.example.fitnessapp.data.notification.ReminderScheduler.scheduleNextMeal(context, true)
                                    com.example.fitnessapp.data.notification.ReminderScheduler.scheduleNextMeal(context, false)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            com.example.fitnessapp.data.notification.FitnessNotificationHelper.showHydrationNotification(context, 1500, 3000)
                            scope.launch {
                                snackbarHostState.showSnackbar("Test notification sent! Check your notification shade.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test Notification", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance & Theme Card
            val repository = FitnessApplication.instance.repository
            val currentThemeMode by repository.themeMode.collectAsStateWithLifecycle()
            val haptic = LocalHapticFeedback.current

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance & Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Personalize the app interface or match your device's system appearance.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeOptions = listOf(
                            Triple(AppThemeMode.SYSTEM, Icons.Default.BrightnessAuto, "System"),
                            Triple(AppThemeMode.LIGHT, Icons.Default.LightMode, "Light"),
                            Triple(AppThemeMode.DARK, Icons.Default.DarkMode, "Dark")
                        )

                        themeOptions.forEach { (mode, icon, label) ->
                            val isSelected = currentThemeMode == mode
                            val containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                            val contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val border = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        repository.setThemeMode(mode)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = containerColor,
                                    contentColor = contentColor
                                ),
                                border = border
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(22.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mode.subtitle,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Lock & Biometric Security Card
            val isAppLockEnabled by repository.isAppLockEnabled.collectAsStateWithLifecycle()
            val activity = context as? FragmentActivity
            val hasSystemLock = remember(context) { AppLockManager.canAuthenticate(context) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Lock & Biometric Privacy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Require system biometric (Fingerprint, Face) or device PIN/Pattern whenever NutriFit AI is opened or resumed.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable App Lock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                if (hasSystemLock) "Uses your device's default security" else "No screen lock setup on this device",
                                fontSize = 11.sp,
                                color = if (hasSystemLock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            enabled = hasSystemLock,
                            onCheckedChange = { enable ->
                                if (enable && activity != null) {
                                    AppLockManager.authenticate(
                                        activity = activity,
                                        title = "Enable NutriFit App Lock",
                                        subtitle = "Verify your identity to activate app lock",
                                        onSuccess = {
                                            repository.setAppLockEnabled(true)
                                            AppLockManager.setUnlocked(true)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("App Lock activated with system defaults!")
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Could not enable: $error")
                                            }
                                        }
                                    )
                                } else {
                                    repository.setAppLockEnabled(false)
                                    AppLockManager.setUnlocked(true)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("App Lock disabled.")
                                    }
                                }
                            }
                        )
                    }

                    if (isAppLockEnabled && activity != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                AppLockManager.authenticate(
                                    activity = activity,
                                    title = "Test App Unlock",
                                    subtitle = "Confirming system authentication works properly",
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Authentication successful! System defaults working.")
                                        }
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Auth result: $error")
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Unlock Now", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    val updated = profile.copy(
                        name = name,
                        gender = gender,
                        age = ageText.toIntOrNull() ?: profile.age,
                        heightCm = heightText.toFloatOrNull() ?: profile.heightCm,
                        startWeightKg = startWeightText.toFloatOrNull() ?: profile.startWeightKg,
                        currentWeightKg = currentWeightText.toFloatOrNull() ?: profile.currentWeightKg,
                        targetWeightKg = targetWeightText.toFloatOrNull() ?: profile.targetWeightKg,
                        targetBmi = selectedTargetBmi,
                        autoCalculateTargetFromBmi = autoCalculateTarget,
                        activityLevel = activityLevel,
                        deficitLevel = deficitLevel,
                        customDeficitKcal = customDeficitText.toIntOrNull() ?: profile.customDeficitKcal,
                        geminiApiKey = apiKey.trim(),
                        dailyStepTarget = dailyStepTargetText.toIntOrNull()?.coerceIn(1000, 50000) ?: profile.dailyStepTarget,
                        dailyWaterTargetMl = dailyWaterTargetText.toIntOrNull()?.coerceIn(1000, 8000) ?: profile.dailyWaterTargetMl
                    )
                    viewModel.saveProfile(updated)
                    scope.launch {
                        snackbarHostState.showSnackbar("Profile & targets saved successfully!")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile & Recalculate Targets", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToOnboarding,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Revisit Welcome & Permissions Tour")
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
