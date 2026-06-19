package com.example.loveosapk.ui.features.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loveosapk.ui.features.cycle.components.*
import com.example.loveosapk.ui.features.cycle.model.CycleUiState
import java.time.LocalDate

@Composable
fun CycleScreen(
    viewModel: CycleViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    
    var showBottomSheet by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A)) // Deep Cosmic Dark
    ) {
        when (val state = uiState) {
            is CycleUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFE91E63)
                )
            }
            is CycleUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    CycleHeader(
                        month = state.currentMonth,
                        onPrevious = { viewModel.changeMonth(currentMonth.minusMonths(1)) },
                        onNext = { viewModel.changeMonth(currentMonth.plusMonths(1)) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CycleStatsCard(stats = state.cycleStats)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    PhaseLegend()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CycleCalendar(
                        days = state.days,
                        currentMonth = state.currentMonth,
                        onDateClick = { date ->
                            viewModel.selectDate(date)
                            showBottomSheet = true
                        }
                    )
                    
                    if (state.partnerLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PartnerSyncIndicator(count = state.partnerLogs.size)
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for navigation
                }
            }
            is CycleUiState.Error -> {
                Text(
                    text = "Ошибка: ${state.message}",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        if (showBottomSheet && selectedDate != null) {
            val dayInfo = (uiState as? CycleUiState.Success)?.days?.find { it.date == selectedDate }
            
            SymptomBottomSheet(
                date = selectedDate!!,
                existingLog = dayInfo?.log,
                onDismiss = { showBottomSheet = false },
                onSave = { log ->
                    viewModel.saveCycleLog(log)
                    showBottomSheet = false
                },
                onDelete = {
                    viewModel.deleteLog(selectedDate!!)
                    showBottomSheet = false
                }
            )
        }
    }
}
