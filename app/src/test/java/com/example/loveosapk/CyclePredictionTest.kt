package com.example.loveosapk

import com.example.loveosapk.domain.model.CycleLog
import com.example.loveosapk.domain.model.DayPhase
import com.example.loveosapk.domain.usecase.PredictCyclePhasesUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CyclePredictionTest {
    private val predictor = PredictCyclePhasesUseCase()

    @Test
    fun `test cycle prediction with no data`() {
        val stats = predictor.calculateStats(emptyList())
        assertEquals(DayPhase.UNKNOWN, stats.currentPhase)
        assertEquals(28, stats.averageCycleLength)
    }

    @Test
    fun `test cycle phase calculation - period`() {
        val today = LocalDate.now()
        val logs = listOf(
            CycleLog(date = today, isPeriod = true, flowIntensity = 2, symptoms = emptyList(), mood = null, painLevel = 0, energyLevel = 5, notes = "", isOvulation = false)
        )
        val stats = predictor.calculateStats(logs)
        assertEquals(DayPhase.PERIOD, stats.currentPhase)
    }

    @Test
    fun `test cycle phase calculation - ovulation`() {
        val lastPeriod = LocalDate.now().minusDays(14)
        val logs = listOf(
            CycleLog(date = lastPeriod, isPeriod = true, flowIntensity = 2, symptoms = emptyList(), mood = null, painLevel = 0, energyLevel = 5, notes = "", isOvulation = false)
        )
        val stats = predictor.calculateStats(logs)
        // Ovulation is roughly 14 days after period start in a 28 day cycle
        assertEquals(DayPhase.OVULATION, stats.currentPhase)
    }
}
