package com.example.loveosapk.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoveOsDao {
    @Query("SELECT * FROM wishes ORDER BY ts DESC")
    fun getAllWishes(): Flow<List<WishEntity>>

    @Upsert
    suspend fun upsertWish(wish: WishEntity)

    @Delete
    suspend fun deleteWish(wish: WishEntity)

    @Query("DELETE FROM wishes WHERE id = :id")
    suspend fun deleteWishById(id: String)

    @Query("SELECT * FROM tasks ORDER BY ts DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT * FROM savings ORDER BY ts DESC")
    fun getAllSavings(): Flow<List<SavingEntity>>

    @Upsert
    suspend fun upsertSaving(saving: SavingEntity)

    @Delete
    suspend fun deleteSaving(saving: SavingEntity)

    @Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteSavingById(id: String)

    @Query("SELECT * FROM notes ORDER BY ts DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM time_capsules ORDER BY ts DESC")
    fun getAllTimeCapsules(): Flow<List<TimeCapsuleEntity>>

    @Upsert
    suspend fun upsertTimeCapsule(capsule: TimeCapsuleEntity)

    @Delete
    suspend fun deleteTimeCapsule(capsule: TimeCapsuleEntity)

    @Query("DELETE FROM time_capsules WHERE id = :id")
    suspend fun deleteTimeCapsuleById(id: String)

    @Query("SELECT * FROM chat_messages ORDER BY ts ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Upsert
    suspend fun upsertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteChatMessageById(id: String)
}

@Dao
interface CycleLogDao {
    @Query("SELECT * FROM cycle_logs WHERE date BETWEEN :start AND :end ORDER BY date")
    fun getLogsForRange(start: String, end: String): Flow<List<CycleLogEntity>>
    
    @Query("SELECT * FROM cycle_logs WHERE date = :date LIMIT 1")
    fun getLogByDate(date: String): Flow<CycleLogEntity?>
    
    @Upsert
    suspend fun upsertLog(entity: CycleLogEntity)
    
    @Query("DELETE FROM cycle_logs WHERE date = :date")
    suspend fun deleteLog(date: String)
    
    @Query("SELECT * FROM cycle_logs WHERE isPeriod = 1 ORDER BY date DESC LIMIT 6")
    suspend fun getLastPeriods(): List<CycleLogEntity>
    
    @Query("SELECT * FROM cycle_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<CycleLogEntity>>
}
