package com.example.fitnessapp.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.data.model.CalorieWarningLevel
import com.example.fitnessapp.data.model.DailyNutritionSummary

@Composable
fun CalorieWarningBanner(
    summary: DailyNutritionSummary,
    modifier: Modifier = Modifier
) {
    if (summary.warningLevel == CalorieWarningLevel.SAFE) return

    val (bgColor, contentColor, icon, headline) = when (summary.warningLevel) {
        CalorieWarningLevel.EXCEEDED -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.Error,
            "Calorie Limit Exceeded!"
        )
        CalorieWarningLevel.NEAR_LIMIT -> Quadruple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            Icons.Default.Warning,
            "Near Calorie Limit Warning (90%+)"
        )
        CalorieWarningLevel.APPROACHING -> Quadruple(
            Color(0xFFFFF8E1),
            Color(0xFFF57F17),
            Icons.Default.Warning,
            "Approaching Caloric Limit (80%+)"
        )
        CalorieWarningLevel.SAFE -> Quadruple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            Icons.Default.CheckCircle,
            "On Track"
        )
    }

    AnimatedVisibility(
        visible = summary.warningMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = headline,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    summary.warningMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = contentColor.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
