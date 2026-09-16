package com.example.fitnessapp.ui.bodyprogress

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fitnessapp.data.model.BodyPhoto
import com.example.fitnessapp.data.model.BodyPose
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: BodyProgressViewModel = viewModel()
) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val allPhotos by viewModel.bodyPhotos.collectAsStateWithLifecycle()
    val selectedBefore by viewModel.selectedBeforePhoto.collectAsStateWithLifecycle()
    val selectedAfter by viewModel.selectedAfterPhoto.collectAsStateWithLifecycle()

    val userPhotos = allPhotos.filter { it.userId == activeProfile.id }

    var showUploadDialog by remember { mutableStateOf(false) }

    // Auto-select first and last photo for comparison if not chosen
    val before = selectedBefore ?: userPhotos.lastOrNull()
    val after = selectedAfter ?: userPhotos.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Transformation & Comparison", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUploadDialog = true },
                icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                text = { Text("Log Progress Photo") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            // Privacy lock info badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Encrypted private internal storage. Photos are invisible in system gallery apps.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Before & After Slider
            if (before != null && after != null && before.id != after.id) {
                Text(
                    text = "BEFORE & AFTER COMPARISON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                BeforeAfterSlider(beforePhoto = before, afterPhoto = after)
                Spacer(modifier = Modifier.height(20.dp))
            } else if (userPhotos.size >= 2) {
                Text(
                    text = "Select two photos from history below to activate Before & After Slider",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sub-Menu Filter for Poses
            var selectedPoseFilter by remember { mutableStateOf<BodyPose?>(null) }
            val filteredPhotos = remember(userPhotos, selectedPoseFilter) {
                if (selectedPoseFilter == null) userPhotos
                else userPhotos.filter { it.pose == selectedPoseFilter }
            }

            // Photo Timeline & Selector Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PROGRESS PHOTO TIMELINE (${filteredPhotos.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pose filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAllSel = selectedPoseFilter == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAllSel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .clickable { selectedPoseFilter = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "All Poses",
                            fontSize = 11.sp,
                            fontWeight = if (isAllSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    BodyPose.entries.forEach { pose ->
                        val isSel = selectedPoseFilter == pose
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { selectedPoseFilter = if (isSel) null else pose }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pose.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (filteredPhotos.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (userPhotos.isEmpty()) "No progress photos logged yet" else "No photos for ${selectedPoseFilter?.label}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Upload periodic photos (front, side, back) to compare body tone & gains over time.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (userPhotos.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val bmp1 = createBodyDemoBitmap("Day 1 Baseline (85 kg)", AndroidColor.rgb(180, 70, 70))
                                    val bmp2 = createBodyDemoBitmap("Day 30 Progress (81 kg)", AndroidColor.rgb(40, 140, 90))
                                    viewModel.saveBodyPhoto(bmp1, BodyPose.FRONT, 85f, "Starting baseline")
                                    viewModel.saveBodyPhoto(bmp2, BodyPose.FRONT, 81f, "1 month noticeable reduction")
                                }
                            ) {
                                Text("Generate Sample Before & After Photos (Test)")
                            }
                        }
                    }
                }
            } else {
                filteredPhotos.forEach { photo ->
                    PhotoHistoryCard(
                        photo = photo,
                        isBeforeSelected = before?.id == photo.id,
                        isAfterSelected = after?.id == photo.id,
                        onSelectBefore = { viewModel.selectBefore(photo) },
                        onSelectAfter = { viewModel.selectAfter(photo) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }

        if (showUploadDialog) {
            AddPhotoDialog(
                currentWeight = activeProfile.currentWeightKg,
                onSave = { bmp, pose, w, notes ->
                    viewModel.saveBodyPhoto(bmp, pose, w, notes)
                    showUploadDialog = false
                },
                onDismiss = { showUploadDialog = false }
            )
        }
    }
}

@Composable
private fun PhotoHistoryCard(
    photo: BodyPhoto,
    isBeforeSelected: Boolean,
    isAfterSelected: Boolean,
    onSelectBefore: () -> Unit,
    onSelectAfter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = File(photo.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(photo.date, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${photo.pose.label} • ${photo.weightKg} kg",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (photo.notes.isNotBlank()) {
                    Text(photo.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isBeforeSelected,
                        onClick = onSelectBefore,
                        label = { Text("Set Before", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = isAfterSelected,
                        onClick = onSelectAfter,
                        label = { Text("Set After", fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPhotoDialog(
    currentWeight: Float,
    onSave: (Bitmap, BodyPose, Float, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPose by remember { mutableStateOf(BodyPose.FRONT) }
    var weightText by remember { mutableStateOf(currentWeight.toString()) }
    var notesText by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) capturedBitmap = bmp
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Body Progress Photo", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Pose Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BodyPose.entries.forEach { pose ->
                        FilterChip(
                            selected = selectedPose == pose,
                            onClick = { selectedPose = pose },
                            label = { Text(pose.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (e.g. waist feeling tighter)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (capturedBitmap == null) "Take Photo" else "Retake Photo")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bmp = capturedBitmap ?: createBodyDemoBitmap("Body Photo", AndroidColor.rgb(60, 110, 160))
                    val w = weightText.toFloatOrNull() ?: currentWeight
                    onSave(bmp, selectedPose, w, notesText)
                }
            ) {
                Text("Save Photo")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun createBodyDemoBitmap(label: String, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val bgPaint = Paint().apply { this.color = color }
    canvas.drawRect(0f, 0f, 480f, 640f, bgPaint)

    val textPaint = Paint().apply {
        this.color = AndroidColor.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(label, 240f, 320f, textPaint)
    return bitmap
}
