package com.example.fitnessapp.ui.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.Executors

@Composable
fun FoodScannerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = ImageUtils.decodeSampledBitmapFromUri(context, uri)
            if (bitmap != null) {
                viewModel.processCapturedBitmap(bitmap)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Unable to load selected photo from gallery.")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "AI Food Scanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Camera Viewfinder Preview
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setTargetResolution(Size(1440, 1080))
                                    .build()
                                imageCapture = capture

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                // Camera binding fallback for emulator without physical camera
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera access is needed to scan food meals.",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Re-open Last Scan Floating Banner
            val lastMeal by viewModel.lastScannedMeal.collectAsStateWithLifecycle()
            if (lastMeal != null && uiState is ScannerUiState.Idle) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { viewModel.restoreLastScan() },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xEE0F172A),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Resume: ${lastMeal?.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "Accidentally closed? Tap here to re-open & log",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearLastScan() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Food Targeting Viewfinder Reticle Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 90.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Align food dish inside frame",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Controls Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preset Test Foods (Super handy for emulator or quick logging!)
                Text(
                    text = "Quick Food AI Presets (Instant Test):",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.processCapturedBitmap(createSampleBitmap("🫓 Roti & Dal"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("🫓 Roti & Dal", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.processCapturedBitmap(createSampleBitmap("🧀 Paneer Bhurji"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("🧀 Paneer", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.processCapturedBitmap(createSampleBitmap("🍛 Idli Sambar"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("🍛 Idli Sambar", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Direct Gallery Upload Button
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Food Photo from Library", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera Shutter & Pick Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pick from Gallery
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick Photo",
                            tint = Color.White
                        )
                    }

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                val capture = imageCapture
                                if (capture != null) {
                                    val executor = Executors.newSingleThreadExecutor()
                                    capture.takePicture(
                                        executor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                try {
                                                    val bmp = image.toBitmap()
                                                    viewModel.processCapturedBitmap(bmp)
                                                } finally {
                                                    image.close()
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                // Fallback to sample bitmap if camera capture fails on emulator
                                                viewModel.processCapturedBitmap(createSampleBitmap("Food Dish"))
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.processCapturedBitmap(createSampleBitmap("Food Dish"))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Snap Food",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Gemini AI Indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = Color(0xFFFFCA28)
                        )
                    }
                }
            }

            // Analyzing Spinner Overlay
            AnimatedVisibility(
                visible = uiState is ScannerUiState.Analyzing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing Food with Gemini AI...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Estimating calories, macros & micronutrients",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Review & Confirm Meal Dialog
            val currentState = uiState
            if (currentState is ScannerUiState.Review) {
                MealReviewDialog(
                    meal = currentState.mealLog,
                    todaySummary = todaySummary,
                    onConfirm = {
                        viewModel.confirmMeal(it)
                        onNavigateBack()
                    },
                    onDismiss = { viewModel.dismissReview() }
                )
            }
        }
    }
}

private fun createSampleBitmap(label: String): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val bgPaint = Paint().apply { color = AndroidColor.rgb(45, 125, 80) }
    canvas.drawRect(0f, 0f, 400f, 400f, bgPaint)

    val textPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(label, 200f, 200f, textPaint)
    return bitmap
}
