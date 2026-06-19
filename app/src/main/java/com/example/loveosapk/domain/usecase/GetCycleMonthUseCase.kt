package com.example.loveosapk.domain.usecase

import com.example.loveosapk.domain.repository.CycleRepository
import com.example.loveosapk.ui.features.cycle.model.DayInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

class GetCycleMonthUseCase(
    private val repository: CycleRepository,
    private val predictPhases: PredictCyclePhasesUseCase
) {
    operator fun invoke(month: YearMonth): Flow<List<DayInfo>> = combine(
        repository.getLogsForRange(month.atDay(1).minusDays(7), month.atEndOfMonth().plusDays(7)),
        repository.getAllLogs()
    ) { currentLogs, allLogs ->
        val today = LocalDate.now()
        val daysInMonth = mutableListOf<DayInfo>()
        
        var current = month.atDay(1)
        val end = month.atEndOfMonth()
        
        while (!current.isAfter(end)) {
            daysInMonth.add(
                DayInfo(
                    date = current,
                    phase = predictPhases.predict(allLogs, current),
                    log = currentLogs.find { it.date == current },
                    isToday = current == today,
                    isSelected = false
                )
            )
            current = current.plusDays(1)
        }
        daysInMonth
    }
}
