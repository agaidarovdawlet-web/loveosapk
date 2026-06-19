package com.example.loveosapk.ui.features.cycle.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.domain.model.CycleStats
import com.example.loveosapk.domain.model.DayPhase
import com.example.loveosapk.ui.components.GlassCard
import com.example.loveosapk.ui.features.cycle.model.CycleStrings
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CycleStatsCard(
    stats: CycleStats,
    modifier: Modifier = Modifier
) {
    val phaseGradient = when (stats.currentPhase) {
        DayPhase.PERIOD -> Brush.verticalGradient(listOf(Color(0xFFE91E63).copy(alpha = 0.2f), Color.Transparent))
        DayPhase.OVULATION -> Brush.verticalGradient(listOf(Color(0xFF9C27B0).copy(alpha = 0.2f), Color.Transparent))
        else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent))
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .background(phaseGradient)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Сейчас: ${CycleStrings.phase(stats.currentPhase)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            stats.daysUntilNextPeriod?.let { days ->
                Text(
                    text = if (days >= 0) "До месячных: $days дней" else "Задержка: ${-days} дней",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            stats.nextOvulationDate?.let { date ->
                val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
                Text(
                    text = "Следующая овуляция: ${date.format(formatter)}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
