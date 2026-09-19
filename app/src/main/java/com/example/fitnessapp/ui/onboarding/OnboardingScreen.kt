package com.example.fitnessapp.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.data.model.DeficitLevel
import com.example.fitnessapp.data.model.Gender
import com.example.fitnessapp.domain.BmiCalculator
import com.example.fitnessapp.domain.NutritionEngine
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    var currentStep by remember { mutableIntStateOf(1) } // 1: Welcome, 2: Profile Setup, 3: Permissions

    val context = LocalContext.current

    // Profile form state initialized from repository profile
    var name by remember(activeProfile) { mutableStateOf(activeProfile.name) }
    var gender by remember(activeProfile) { mutableStateOf(activeProfile.gender) }
    var ageText by remember(activeProfile) { mutableStateOf(activeProfile.age.toString()) }
    var heightText by remember(activeProfile) { mutableStateOf(activeProfile.heightCm.toInt().toString()) }
    var startWeightText by remember(activeProfile) { mutableStateOf(activeProfile.startWeightKg.toString()) }
    var currentWeightText by remember(activeProfile) { mutableStateOf(activeProfile.currentWeightKg.toString()) }
    var targetWeightText by remember(activeProfile) { mutableStateOf(activeProfile.targetWeightKg.toString()) }
    var selectedTargetBmi by remember(activeProfile) { mutableStateOf(activeProfile.targetBmi) }
    var autoCalculateTarget by remember(activeProfile) { mutableStateOf(activeProfile.autoCalculateTargetFromBmi) }
    var deficitLevel by remember(activeProfile) { mutableStateOf(activeProfile.deficitLevel) }

    val currentHeight = heightText.toFloatOrNull() ?: activeProfile.heightCm

    LaunchedEffect(currentHeight, startWeightText, deficitLevel, selectedTargetBmi, autoCalculateTarget) {
        if (autoCalculateTarget && currentHeight > 50f) {
            val sWeight = startWeightText.toFloatOrNull() ?: activeProfile.startWeightKg
            val calc = BmiCalculator.calculateTargetWeight(
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

    // Permissions check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] ?: hasCameraPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = result[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasActivityPermission = result[Manifest.permission.ACTIVITY_RECOGNITION] ?: hasActivityPermission
        }
        viewModel.completeOnboarding()
        onComplete()
    }

    fun finishOnboarding() {
        viewModel.completeOnboarding()
        onComplete()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step dots
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { step ->
                            Box(
                                modifier = Modifier
                                    .size(if (step == currentStep) 24.dp else 8.dp, 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (step == currentStep) MaterialTheme.colorScheme.primary
                                        else if (step < currentStep) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back")
                            }
                        }

                        when (currentStep) {
                            1 -> {
                                Button(
                                    onClick = { currentStep = 2 },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Get Started")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            2 -> {
                                Button(
                                    onClick = {
                                        // Save configured profile
                                        val updated = activeProfile.copy(
                                            name = name.ifBlank { activeProfile.name },
                                            gender = gender,
                                            age = ageText.toIntOrNull() ?: activeProfile.age,
                                            heightCm = heightText.toFloatOrNull() ?: activeProfile.heightCm,
                                            startWeightKg = startWeightText.toFloatOrNull() ?: activeProfile.startWeightKg,
                                            currentWeightKg = currentWeightText.toFloatOrNull() ?: activeProfile.currentWeightKg,
                                            targetWeightKg = targetWeightText.toFloatOrNull() ?: activeProfile.targetWeightKg,
                                            targetBmi = selectedTargetBmi,
                                            autoCalculateTargetFromBmi = autoCalculateTarget,
                                            deficitLevel = deficitLevel
                                        )
                                        viewModel.saveProfile(updated)
                                        currentStep = 3
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Next: Permissions")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            3 -> {
                                Button(
                                    onClick = {
                                        val perms = mutableListOf(Manifest.permission.CAMERA)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
                                        }
                                        permissionsLauncher.launch(perms.toTypedArray())
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Grant & Start")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                } else {
                    slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                }
            },
            label = "OnboardingStepTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { step ->
            when (step) {
                1 -> WelcomeSlide()
                2 -> ProfileSetupSlide(
                    name = name,
                    onNameChange = { name = it },
                    gender = gender,
                    onGenderChange = { gender = it },
                    ageText = ageText,
                    onAgeChange = { ageText = it },
                    heightText = heightText,
                    onHeightChange = { heightText = it },
                    startWeightText = startWeightText,
                    onStartWeightChange = {
                        startWeightText = it
                        if (currentWeightText.isBlank() || currentWeightText == "0" || currentWeightText == activeProfile.startWeightKg.toString()) {
                            currentWeightText = it
                        }
                    },
                    currentWeightText = currentWeightText,
                    onCurrentWeightChange = { currentWeightText = it },
                    targetWeightText = targetWeightText,
                    onTargetWeightChange = {
                        targetWeightText = it
                        autoCalculateTarget = false
                    },
                    selectedTargetBmi = selectedTargetBmi,
                    onTargetBmiChange = {
                        selectedTargetBmi = it
                        autoCalculateTarget = true
                    },
                    autoCalculateTarget = autoCalculateTarget,
                    onAutoCalculateChange = { checked ->
                        autoCalculateTarget = checked
                    },
                    deficitLevel = deficitLevel,
                    onDeficitChange = { level ->
                        deficitLevel = level
                        if (autoCalculateTarget) {
                            when (level) {
                                DeficitLevel.AGGRESSIVE -> selectedTargetBmi = 21.0f
                                DeficitLevel.MODERATE -> selectedTargetBmi = 22.0f
                                DeficitLevel.MILD -> selectedTargetBmi = 23.5f
                                DeficitLevel.CUSTOM -> {}
                            }
                        }
                    }
                )
                3 -> PermissionsPrimerSlide(
                    hasCameraPermission = hasCameraPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasActivityPermission = hasActivityPermission,
                    onSkip = { finishOnboarding() }
                )
            }
        }
    }
}

@Composable
private fun WelcomeSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // App Logo & Badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NutriFit AI",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your Personal AI Nutrition & Fitness Companion",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Key Value Propositions
        FeatureHighlightCard(
            icon = Icons.Default.AutoAwesome,
            title = "AI Vision Meal Scanner",
            description = "Point your camera or upload food photos to instantly calculate calories, protein, carbs & fats with Indian diet support."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureHighlightCard(
            icon = Icons.Default.Mic,
            title = "Voice Logging & Plate Builder",
            description = "Speak your entire meal naturally or multi-item search with quantity multipliers and instant plate staging."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureHighlightCard(
            icon = Icons.Default.Timer,
            title = "Intermittent Fasting Tracker",
            description = "16:8, 14:10, or custom schedules with live countdowns, fasting stages, and hydration reminders."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureHighlightCard(
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            title = "7-Day Smoothed Weight Trends",
            description = "Mifflin-St Jeor metabolic engine with 7-day exponential moving average to filter daily water fluctuations."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureHighlightCard(
            icon = Icons.Default.EmojiEvents,
            title = "3-Pillar Local Partner Duel",
            description = "Compete in calories, steps & hydration over local Wi-Fi with encrypted, zero-cloud peer-to-peer sync."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureHighlightCard(
            icon = Icons.Default.Fingerprint,
            title = "System App Lock & Privacy",
            description = "Protect your personal health logs with system biometric, fingerprint, face, or device PIN/pattern lock."
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileSetupSlide(
    name: String,
    onNameChange: (String) -> Unit,
    gender: Gender,
    onGenderChange: (Gender) -> Unit,
    ageText: String,
    onAgeChange: (String) -> Unit,
    heightText: String,
    onHeightChange: (String) -> Unit,
    startWeightText: String,
    onStartWeightChange: (String) -> Unit,
    currentWeightText: String,
    onCurrentWeightChange: (String) -> Unit,
    targetWeightText: String,
    onTargetWeightChange: (String) -> Unit,
    selectedTargetBmi: Float,
    onTargetBmiChange: (Float) -> Unit,
    autoCalculateTarget: Boolean,
    onAutoCalculateChange: (Boolean) -> Unit,
    deficitLevel: DeficitLevel,
    onDeficitChange: (DeficitLevel) -> Unit
) {
    val h = heightText.toFloatOrNull() ?: 163f
    val w = currentWeightText.toFloatOrNull() ?: 78f
    val sWeight = startWeightText.toFloatOrNull() ?: w
    val bmi = BmiCalculator.calculateBmi(w, h)
    val cat = BmiCalculator.getCategory(bmi)

    val idealRange = BmiCalculator.getIdealWeightRange(h)
    val currentTargetWeight = targetWeightText.toFloatOrNull() ?: 70f
    val resultingTargetBmi = BmiCalculator.calculateBmi(currentTargetWeight, h)
    val resultingCategory = BmiCalculator.getCategory(resultingTargetBmi)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tell Us About Yourself",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "We use your biological stats to calculate your BMR and optimal daily calorie deficit.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Gender.entries.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { onGenderChange(g) },
                            label = { Text(if (g == Gender.MALE) "Male 👨" else "Female 👩") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = onAgeChange,
                        label = { Text("Age (yrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = onHeightChange,
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startWeightText,
                        onValueChange = onStartWeightChange,
                        label = { Text("Starting Wt (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currentWeightText,
                        onValueChange = onCurrentWeightChange,
                        label = { Text("Current Wt (kg)") },
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
                        Text("Target Goal Weight & BMI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-calculate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = autoCalculateTarget,
                            onCheckedChange = onAutoCalculateChange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Healthy range for ${h.toInt()}cm: ${idealRange.first} - ${idealRange.second} kg (BMI 18.5 - 24.9)",
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
                        onClick = { onTargetBmiChange(21.0f) },
                        label = { Text("Lean 21.0", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = (selectedTargetBmi == 22.0f),
                        onClick = { onTargetBmiChange(22.0f) },
                        label = { Text("⭐ Optimal 22.0", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = (selectedTargetBmi == 23.5f),
                        onClick = { onTargetBmiChange(23.5f) },
                        label = { Text("Fit 23.5", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetWeightText,
                    onValueChange = onTargetWeightChange,
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
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Deficit Pace Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Target Fat Loss Pace", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val paceOptions = remember { listOf(DeficitLevel.MILD, DeficitLevel.MODERATE, DeficitLevel.AGGRESSIVE) }
                for (level in paceOptions) {
                    val isSelected = deficitLevel == level
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable { onDeficitChange(level) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(level.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("~${level.deficitKcal} kcal/day deficit (~${level.paceKgPerWeek} kg/week)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live calculation summary pill
        val weightToLose = (sWeight - currentTargetWeight).coerceAtLeast(0f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Baseline BMI: $bmi (${cat.label})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Goal: Lose ${(weightToLose * 10f).roundToInt() / 10f} kg ➔ Target BMI $resultingTargetBmi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(cat.colorHex).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(cat.label, color = Color(cat.colorHex), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionsPrimerSlide(
    hasCameraPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasActivityPermission: Boolean,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Permissions & Privacy",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "NutriFit AI processes all data locally on your device. We request only the permissions needed for your features.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Step Counter & Activity Permission Card
        PermissionCard(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            title = "Physical Activity (Step Counter)",
            description = "Needed to read hardware step sensors and accurately count daily steps and burn calories in real time.",
            isGranted = hasActivityPermission,
            tag = if (hasActivityPermission) "Granted ✅" else "Recommended"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Camera Permission Card
        PermissionCard(
            icon = Icons.Default.CameraAlt,
            title = "Camera Access",
            description = "Needed to scan and identify your meals with AI Vision. No photos are saved to your public gallery without your permission.",
            isGranted = hasCameraPermission,
            tag = if (hasCameraPermission) "Granted ✅" else "Required for Scan"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Notifications Permission Card
        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            description = "Timely reminders to log meals, celebrate daily deficit milestones, and receive cheers from your partner.",
            isGranted = hasNotificationPermission,
            tag = if (hasNotificationPermission) "Granted ✅" else "Recommended"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy note for photos
        PermissionCard(
            icon = Icons.Default.PhotoLibrary,
            title = "Photo Library (Gallery)",
            description = "Uses Android's secure Photo Picker. You only share the specific photo you choose—no storage permission needed.",
            isGranted = true,
            tag = "Zero-Permission 🔒"
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                "Skip permissions for now & go to Dashboard",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    tag: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isGranted) Color(0xFF2E7D32).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isGranted) Color(0xFF2E7D32).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
