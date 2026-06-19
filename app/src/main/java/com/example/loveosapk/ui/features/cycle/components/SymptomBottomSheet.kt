package com.example.loveosapk.ui.features.cycle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.domain.model.CycleLog
import com.example.loveosapk.domain.model.Mood
import com.example.loveosapk.domain.model.Symptom
import com.example.loveosapk.ui.features.cycle.model.CycleStrings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SymptomBottomSheet(
    date: LocalDate,
    existingLog: CycleLog?,
    onDismiss: () -> Unit,
    onSave: (CycleLog) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var isPeriod by remember { mutableStateOf(existingLog?.isPeriod ?: false) }
    var flowIntensity by remember { mutableIntStateOf(existingLog?.flowIntensity ?: 0) }
    var selectedSymptoms by remember { mutableStateOf(existingLog?.symptoms ?: emptyList()) }
    var selectedMood by remember { mutableStateOf(existingLog?.mood) }
    var pain by remember { mutableFloatStateOf(existingLog?.painLevel?.toFloat() ?: 0f) }
    var energy by remember { mutableFloatStateOf(existingLog?.energyLevel?.toFloat() ?: 5f) }
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A2E),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Заголовок
            Text(
                text = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Месячные
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Месячные", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = isPeriod,
                    onCheckedChange = { 
                        isPeriod = it
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFE91E63),
                        checkedTrackColor = Color(0xFFE91E63).copy(alpha = 0.5f)
                    )
                )
            }
            
            AnimatedVisibility(visible = isPeriod) {
                Column {
                    Text("Интенсивность", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = flowIntensity.toFloat(),
                        onValueChange = { flowIntensity = it.toInt() },
                        valueRange = 0f..3f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE91E63),
                            activeTrackColor = Color(0xFFE91E63)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Симптомы (Chips)
            Text("Симптомы", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Symptom.entries.forEach { symptom ->
                    val selected = symptom in selectedSymptoms
                    FilterChip(
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            selectedSymptoms = if (selected) {
                                selectedSymptoms - symptom
                            } else {
                                selectedSymptoms + symptom
                            }
                        },
                        label = { Text(CycleStrings.symptom(symptom), color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE91E63),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            // Настроение
            Text("Настроение", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Mood.entries.forEach { mood ->
                    val selected = mood == selectedMood
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            selectedMood = if (selected) null else mood 
                        }
                    ) {
                        Text(
                            text = CycleStrings.moodEmoji(mood),
                            fontSize = 28.sp,
                            modifier = Modifier.alpha(if (selected) 1f else 0.4f)
                        )
                        Text(
                            text = CycleStrings.mood(mood),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color(0xFFE91E63) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Слайдеры
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥 Боль: ${pain.toInt()}/10", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                }
                Slider(
                    value = pain, 
                    onValueChange = { pain = it }, 
                    valueRange = 0f..10f, 
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFE91E63),
                        activeTrackColor = Color(0xFFE91E63)
                    )
                )
            }
            
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💪 Энергия: ${energy.toInt()}/10", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                }
                Slider(
                    value = energy, 
                    onValueChange = { energy = it }, 
                    valueRange = 0f..10f, 
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF9C27B0),
                        activeTrackColor = Color(0xFF9C27B0)
                    )
                )
            }
            
            // Заметки
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметки", color = Color.White.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE91E63),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingLog != null && existingLog.source == "me") {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDelete()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Удалить")
                    }
                }
                
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSave(
                            CycleLog(
                                date = date,
                                isPeriod = isPeriod,
                                flowIntensity = flowIntensity,
                                symptoms = selectedSymptoms,
                                mood = selectedMood,
                                painLevel = pain.toInt(),
                                energyLevel = energy.toInt(),
                                notes = notes,
                                isOvulation = false,
                                source = "me"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
