package com.example.loveosapk.ui.features.cycle.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.loveosapk.ui.features.cycle.model.DayInfo

@Composable
fun DayDetailsCard(day: DayInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Details for ${day.date}", color = Color.White, fontWeight = FontWeight.Bold)
            day.log?.let { log ->
                if (log.isPeriod) Text("• Period (Intensity: ${log.flowIntensity})", color = Color(0xFFE91E63))
                log.mood?.let { Text("• Mood: $it", color = Color.White) }
                if (log.notes.isNotEmpty()) Text("• Notes: ${log.notes}", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
