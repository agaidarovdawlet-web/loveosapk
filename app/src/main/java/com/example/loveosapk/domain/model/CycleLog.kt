package com.example.loveosapk.domain.model

import java.time.LocalDate

data class CycleLog(
    val date: LocalDate,
    val isPeriod: Boolean,
    val flowIntensity: Int, // 0-3
    val symptoms: List<Symptom>,
    val mood: Mood?,
    val painLevel: Int, // 0-10
    val energyLevel: Int, // 0-10
    val notes: String,
    val isOvulation: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "me" // "me" или "partner" для UI разделения
)

enum class Symptom {
    HEADACHE, BLOATING, CRAMPS, ACNE, BACK_PAIN, 
    BREAST_TENDERNESS, NAUSEA, INSOMNIA, SPOTTING
}

enum class Mood { HAPPY, CALM, IRRITABLE, SAD, ANXIOUS, ENERGETIC, TIRED }

enum class DayPhase {
    PERIOD, FERTILE, OVULATION, LUTEAL, FOLLICULAR, UNKNOWN
}

data class CycleStats(
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val nextPeriodDate: LocalDate?,
    val nextOvulationDate: LocalDate?,
    val currentPhase: DayPhase,
    val daysUntilNextPeriod: Int?
)
