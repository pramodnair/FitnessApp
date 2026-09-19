package com.example.fitnessapp.ui.bodyprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fitnessapp.data.model.BodyPhoto
import java.io.File
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    beforePhoto: BodyPhoto,
    afterPhoto: BodyPhoto,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }
    var containerWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Date & Weight Comparison Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Before Info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("BEFORE (${beforePhoto.date})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${beforePhoto.weightKg} kg", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB74D))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Weight Delta Badge
            val delta = afterPhoto.weightKg - beforePhoto.weightKg
            val deltaColor = if (delta <= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            Surface(
                shape = CircleShape,
                color = deltaColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "${if (delta > 0) "+" else ""}${String.format("%.1f", delta)} kg",
                    color = deltaColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // After Info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.End) {
                    Text("AFTER (${afterPhoto.date})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${afterPhoto.weightKg} kg", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF81C784))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Split Slider Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
                .onGloballyPositioned { coordinates ->
                    containerWidthPx = coordinates.size.width.toFloat()
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (containerWidthPx > 0) {
                            val newFraction = splitFraction + (dragAmount / containerWidthPx)
                            splitFraction = newFraction.coerceIn(0.05f, 0.95f)
                        }
                    }
                }
        ) {
            // Layer 1: After Photo (Background)
            AsyncImage(
                model = File(afterPhoto.photoPath),
                contentDescription = "After transformation photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Before Photo (Clipped to split fraction)
            val beforeWidthDp = with(density) { (containerWidthPx * splitFraction).toDp() }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(beforeWidthDp)
                    .clip(RectangleShape)
            ) {
                AsyncImage(
                    model = File(beforePhoto.photoPath),
                    contentDescription = "Before transformation photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { containerWidthPx.toDp() })
                )
            }

            // Layer 3: Draggable Divider Bar & Handle
            val dividerOffsetPx = containerWidthPx * splitFraction
            Box(
                modifier = Modifier
                    .offset { IntOffset(dividerOffsetPx.roundToInt() - 14, 0) }
                    .fillMaxHeight()
                    .width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                // Vertical Line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(Color.White)
                )

                // Circle handle
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = "Slide to compare",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "👈 Drag slider horizontally to compare body changes & muscle definition 👉",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
