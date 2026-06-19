package com.example.loveosapk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.loveosapk.data.MessageStatus

@Entity(tableName = "wishes")
data class WishEntity(
    @PrimaryKey val id: String,
    val text: String,
    val category: String,
    val author: String = "",
    val imageUrl: String? = null,
    val priority: String = "MEDIUM",
    val date: String?,
    val done: Boolean,
    val ts: Long
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val text: String,
    val owner: String,
    val deadline: String?,
    val done: Boolean,
    val ts: Long
)

@Entity(tableName = "savings")
data class SavingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goal: Double,
    val current: Double,
    val currency: String,
    val ts: Long
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val date: String,
    val author: String,
    val ts: Long
)

@Entity(tableName = "time_capsules")
data class TimeCapsuleEntity(
    @PrimaryKey val id: String,
    val text: String,
    val created: String,
    val openAfter: Long,
    val opened: Boolean,
    val ts: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val text: String,
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val voiceDuration: Long? = null,
    val videoUrl: String? = null,
    val videoDuration: Long? = null,
    val reactionsJson: String = "{}",
    val ts: Long,
    val status: MessageStatus
)

@Entity(tableName = "cycle_logs")
data class CycleLogEntity(
    @PrimaryKey val date: String, // ISO date "yyyy-MM-dd"
    val isPeriod: Boolean = false,
    val flowIntensity: Int = 0,        // 0=нет, 1=лёгкие, 2=средние, 3=обильные
    val symptomsJson: String = "[]",
    val mood: String? = null,
    val painLevel: Int = 0,            // 0-10
    val energyLevel: Int = 5,          // 0-10
    val notes: String = "",
    val isOvulation: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "me"
)
