package com.example.loveosapk.domain.repository

import com.example.loveosapk.domain.model.CycleLog
import com.example.loveosapk.domain.model.CycleStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CycleRepository {
    fun getLogsForRange(start: LocalDate, end: LocalDate): Flow<List<CycleLog>>
    fun getAllLogs(): Flow<List<CycleLog>>
    fun getLogForDate(date: LocalDate): Flow<CycleLog?>
    suspend fun saveLog(log: CycleLog)
    suspend fun deleteLog(date: LocalDate)
    fun getCycleStats(): Flow<CycleStats>
}
