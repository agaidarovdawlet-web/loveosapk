package com.example.loveosapk.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class UserProfile(
    val name: String = "",
    val avatar: String = "❤️",
    val color: String = "❤️ Красный",
    val food: String = "🍕 Пицца",
    val movie: String = "🎬 Начало",
    val song: String = "🎵 Bohemian Rhapsody",
    val hobby: String = "🎮 Игры",
    val dream: String = "✈️ Япония",
    val loveLang: String = "",
    val moods: List<MoodEntry> = emptyList(),
    val known: List<String> = emptyList()
)

@Immutable
@Serializable
data class MoodEntry(
    val emoji: String = "",
    val comment: String = "",
    val date: String = "" // ISO string
)

@Immutable
@Serializable
data class Heart(
    val sender: String = "",
    val ts: Long = 0L
)

@Immutable
@Serializable
data class Wish(
    val id: String = "",
    val text: String = "",
    val category: String = "default",
    val author: String = "",
    val imageUrl: String? = null,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val date: String? = null,
    val done: Boolean = false,
    val ts: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class Task(
    val id: String = "",
    val text: String = "",
    val owner: String = "both", // "me", "partner", "both"
    val deadline: String? = null,
    val done: Boolean = false,
    val ts: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class Saving(
    val id: String = "",
    val name: String = "",
    val goal: Double = 0.0,
    val current: Double = 0.0,
    val currency: String = "₽",
    val ts: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class Note(
    val id: String = "",
    val text: String = "",
    val date: String = "",
    val author: String = "",
    val ts: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class TimeCapsule(
    val id: String = "",
    val text: String = "",
    val created: String = "",
    val openAfter: Long = 0L,
    val opened: Boolean = false,
    val ts: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val voiceDuration: Long? = null,
    val videoUrl: String? = null,
    val videoDuration: Long? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val ts: Long = 0L,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENT, DELIVERED, READ
}

@Immutable
@Serializable
data class CycleConfig(
    val length: Int = 28,
    val periodLength: Int = 5,
    val lastPeriod: String = ""
)

@Immutable
@Serializable
data class DayData(
    val phase: String = "",
    val symptoms: List<String> = emptyList(),
    val intimacy: Boolean? = null,
    val note: String = ""
)

@Immutable
@Serializable
data class AppState(
    val setup: Boolean = false,
    val me: UserProfile = UserProfile(),
    val partner: UserProfile = UserProfile(avatar = "💙", color = "💙 Синий"),
    val startDate: String = "",
    val myRole: String = "me",
    val meetingDate: Long? = null,
    val partnerCode: String? = null,
    val syncEnabled: Boolean = false,
    val theme: String = "dark"
)
