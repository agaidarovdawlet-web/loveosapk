package com.example.loveosapk.ui.features.cycle.model

import com.example.loveosapk.domain.model.CycleLog
import com.example.loveosapk.domain.model.DayPhase
import java.time.LocalDate

data class DayInfo(
    val date: LocalDate,
    val phase: DayPhase,
    val log: CycleLog?,
    val isToday: Boolean,
    val isSelected: Boolean
)
