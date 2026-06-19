package com.example.loveosapk.ui.features.cycle.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.ui.features.cycle.model.DayInfo
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CycleCalendar(
    days: List<DayInfo>,
    currentMonth: YearMonth,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Week headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth }
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth })
            },
            label = "calendar_anim"
        ) { animatedMonth ->
            val firstDayOfMonth = animatedMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            
            // Padding for empty days at the start
            val emptyDaysBefore = firstDayOfWeek - 1
            val paddedDays = mutableListOf<DayInfo?>()
            repeat(emptyDaysBefore) { paddedDays.add(null) }
            paddedDays.addAll(days)
            
            val weeks = paddedDays.chunked(7)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { day ->
                            if (day != null) {
                                DayCell(
                                    day = day,
                                    onClick = { onDateClick(day.date) }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(52.dp))
                            }
                        }
                        // Fill remaining spaces in the last week
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.size(52.dp))
                        }
                    }
                }
            }
        }
    }
}
