package com.example.loveosapk.ui.features.cycle.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.loveosapk.domain.model.DayPhase
import com.example.loveosapk.ui.features.cycle.model.DayInfo

@Composable
fun DayCell(
    day: DayInfo,
    onClick: (DayInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val phaseColor = when (day.phase) {
        DayPhase.PERIOD -> Color(0xFFE91E63).copy(alpha = 0.9f)
        DayPhase.OVULATION -> Color(0xFF9C27B0).copy(alpha = 0.7f)
        DayPhase.FERTILE -> Color(0xFFFF69B4).copy(alpha = 0.4f)
        DayPhase.LUTEAL -> Color(0xFF4A148C).copy(alpha = 0.5f)
        DayPhase.FOLLICULAR -> Color(0xFF1A237E).copy(alpha = 0.3f)
        DayPhase.UNKNOWN -> Color.White.copy(alpha = 0.05f)
    }

    val borderColor = when {
        day.isToday -> Color.White
        day.isSelected -> Color(0xFFE91E63)
        day.log?.isPeriod == true -> Color(0xFFFF1744)
        else -> Color.White.copy(alpha = 0.15f)
    }

    val borderWidth = when {
        day.isToday -> 2.5.dp
        day.isSelected -> 2.dp
        day.log?.isPeriod == true -> 1.dp
        else -> 1.dp
    }

    val scale by animateFloatAsState(
        targetValue = if (day.isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .size(52.dp)
            .clip(CircleShape)
            .background(phaseColor)
            .border(borderWidth, borderColor, CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = "День ${day.date.dayOfMonth}, ${day.phase.name.lowercase()}"
            }
            .clickable { onClick(day) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
        )
        
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Data indicators
            if (day.log?.source == "me") {
                IndicatorDot(Color.White)
            }
            if (day.log?.source == "partner") {
                IndicatorDot(Color(0xFF00E5FF))
            }
            if (day.log?.notes?.isNotBlank() == true) {
                IndicatorDot(Color(0xFF76FF03))
            }
        }
    }
}

@Composable
private fun IndicatorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .background(color, CircleShape)
    )
}
