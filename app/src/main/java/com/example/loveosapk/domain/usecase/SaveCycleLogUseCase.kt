package com.example.loveosapk.domain.usecase

import com.example.loveosapk.domain.model.CycleLog
import com.example.loveosapk.domain.repository.CycleRepository

class SaveCycleLogUseCase(
    private val repository: CycleRepository
) {
    suspend operator fun invoke(log: CycleLog) {
        repository.saveLog(log)
    }
}
