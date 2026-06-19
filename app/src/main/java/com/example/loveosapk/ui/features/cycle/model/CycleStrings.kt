package com.example.loveosapk.ui.features.cycle.model

import com.example.loveosapk.domain.model.Symptom
import com.example.loveosapk.domain.model.Mood
import com.example.loveosapk.domain.model.DayPhase

object CycleStrings {
    fun symptom(symptom: Symptom): String = when (symptom) {
        Symptom.HEADACHE -> "Головная боль"
        Symptom.BLOATING -> "Вздутие"
        Symptom.CRAMPS -> "Спазмы"
        Symptom.ACNE -> "Прыщи"
        Symptom.BACK_PAIN -> "Боль в спине"
        Symptom.BREAST_TENDERNESS -> "Нагрубание"
        Symptom.NAUSEA -> "Тошнота"
        Symptom.INSOMNIA -> "Бессонница"
        Symptom.SPOTTING -> "Мазня"
    }
    
    fun mood(mood: Mood): String = when (mood) {
        Mood.HAPPY -> "Счастливое"
        Mood.CALM -> "Спокойное"
        Mood.IRRITABLE -> "Раздражённое"
        Mood.SAD -> "Грустное"
        Mood.ANXIOUS -> "Тревожное"
        Mood.ENERGETIC -> "Энергичное"
        Mood.TIRED -> "Усталое"
    }
    
    fun moodEmoji(mood: Mood): String = when (mood) {
        Mood.HAPPY -> "😊"
        Mood.CALM -> "😌"
        Mood.IRRITABLE -> "😤"
        Mood.SAD -> "😢"
        Mood.ANXIOUS -> "😰"
        Mood.ENERGETIC -> "⚡"
        Mood.TIRED -> "😴"
    }
    
    fun phase(phase: DayPhase): String = when (phase) {
        DayPhase.PERIOD -> "Месячные"
        DayPhase.OVULATION -> "Овуляция"
        DayPhase.FERTILE -> "Фертильность"
        DayPhase.LUTEAL -> "Лютеиновая фаза"
        DayPhase.FOLLICULAR -> "Фолликулярная фаза"
        DayPhase.UNKNOWN -> "Не определено"
    }
}
