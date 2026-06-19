package com.example.loveosapk.ui.features.cycle.model

import com.example.loveosapk.domain.model.CycleStats
import java.time.YearMonth
import java.time.LocalDate

sealed class CycleUiState {
    object Loading : CycleUiState()
    data class Success(
        val currentMonth: YearMonth,
        val days: List<DayInfo>,
        val selectedDate: LocalDate?,
        val cycleStats: CycleStats,
        val partnerLogs: List<DayInfo> = emptyList()
    ) : CycleUiState()
    data class Error(val message: String) : CycleUiState()
}
