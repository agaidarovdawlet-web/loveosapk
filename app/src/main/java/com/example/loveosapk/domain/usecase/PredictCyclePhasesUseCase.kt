package com.example.loveosapk.domain.usecase

import com.example.loveosapk.domain.model.DayPhase
import com.example.loveosapk.domain.model.CycleLog
import com.example.loveosapk.domain.model.CycleStats
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PredictCyclePhasesUseCase {
    fun predict(logs: List<CycleLog>, targetDate: LocalDate): DayPhase {
        val stats = calculateStats(logs)
        return calculatePhase(targetDate, stats)
    }

    fun calculateStats(logs: List<CycleLog>): CycleStats {
        val periodDates = logs.filter { it.isPeriod }.map { it.date }.sorted()
        
        if (periodDates.isEmpty()) {
            return CycleStats(28, 5, null, null, DayPhase.UNKNOWN, null)
        }

        val periodStarts = mutableListOf<LocalDate>()
        if (periodDates.isNotEmpty()) {
            periodStarts.add(periodDates.first())
            for (i in 1 until periodDates.size) {
                if (ChronoUnit.DAYS.between(periodDates[i - 1], periodDates[i]) > 7) {
                    periodStarts.add(periodDates[i])
                }
            }
        }

        val periodStartsDesc = periodStarts.sortedDescending()
        val today = LocalDate.now()

        if (periodStartsDesc.size < 2) {
            val lastStart = periodStartsDesc.first()
            val nextPeriod = lastStart.plusDays(28)
            val nextOvulation = nextPeriod.minusDays(14)
            return CycleStats(
                averageCycleLength = 28,
                averagePeriodLength = 5,
                nextPeriodDate = nextPeriod,
                nextOvulationDate = nextOvulation,
                currentPhase = calculatePhase(today, lastStart, nextPeriod, nextOvulation),
                daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriod).toInt()
            )
        }

        val cycles = periodStartsDesc.zipWithNext { a, b -> ChronoUnit.DAYS.between(b, a).toInt() }
        val avgCycle = cycles.average().toInt().coerceIn(21, 35)
        val lastPeriod = periodStartsDesc.first()
        val nextPeriod = lastPeriod.plusDays(avgCycle.toLong())
        val nextOvulation = nextPeriod.minusDays(14)

        return CycleStats(
            averageCycleLength = avgCycle,
            averagePeriodLength = 5,
            nextPeriodDate = nextPeriod,
            nextOvulationDate = nextOvulation,
            currentPhase = calculatePhase(today, lastPeriod, nextPeriod, nextOvulation),
            daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriod).toInt()
        )
    }

    private fun calculatePhase(today: LocalDate, stats: CycleStats): DayPhase {
        val lastPeriod = stats.nextPeriodDate?.minusDays(stats.averageCycleLength.toLong()) ?: return DayPhase.UNKNOWN
        return calculatePhase(today, lastPeriod, stats.nextPeriodDate, stats.nextOvulationDate ?: today)
    }

    private fun calculatePhase(
        target: LocalDate,
        lastPeriod: LocalDate,
        nextPeriod: LocalDate,
        nextOvulation: LocalDate
    ): DayPhase {
        val daysFromStart = ChronoUnit.DAYS.between(lastPeriod, target)
        val daysToNext = ChronoUnit.DAYS.between(target, nextPeriod)
        val daysToOvulation = ChronoUnit.DAYS.between(target, nextOvulation)

        return when {
            daysFromStart in 0..4 -> DayPhase.PERIOD
            daysToOvulation in -2..2 -> DayPhase.OVULATION
            daysToOvulation in -5..-3 || daysToOvulation in 3..5 -> DayPhase.FERTILE
            target.isBefore(nextOvulation) -> DayPhase.FOLLICULAR
            else -> DayPhase.LUTEAL
        }
    }
}
