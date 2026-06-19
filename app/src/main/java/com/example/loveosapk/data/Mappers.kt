package com.example.loveosapk.data

import com.example.loveosapk.data.local.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val mapperJson = Json { ignoreUnknownKeys = true }

fun Wish.toEntity() = WishEntity(id, text, category, author, imageUrl, priority, date, done, ts)
fun WishEntity.toDomain() = Wish(
    id = id,
    text = text,
    category = category,
    author = author,
    imageUrl = imageUrl,
    priority = priority,
    date = date,
    done = done,
    ts = ts
)

fun Task.toEntity() = TaskEntity(id, text, owner, deadline, done, ts)
fun TaskEntity.toDomain() = Task(id, text, owner, deadline, done, ts)

fun Saving.toEntity() = SavingEntity(id, name, goal, current, currency, ts)
fun SavingEntity.toDomain() = Saving(id, name, goal, current, currency, ts)

fun Note.toEntity() = NoteEntity(id, text, date, author, ts)
fun NoteEntity.toDomain() = Note(id, text, date, author, ts)

fun TimeCapsule.toEntity() = TimeCapsuleEntity(id, text, created, openAfter, opened, ts)
fun TimeCapsuleEntity.toDomain() = TimeCapsule(id, text, created, openAfter, opened, ts)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    sender = sender,
    text = text,
    imageUrl = imageUrl,
    voiceUrl = voiceUrl,
    voiceDuration = voiceDuration,
    videoUrl = videoUrl,
    videoDuration = videoDuration,
    reactionsJson = mapperJson.encodeToString(reactions),
    ts = ts,
    status = status
)
fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    sender = sender,
    text = text,
    imageUrl = imageUrl,
    voiceUrl = voiceUrl,
    voiceDuration = voiceDuration,
    videoUrl = videoUrl,
    videoDuration = videoDuration,
    reactions = runCatching { mapperJson.decodeFromString<Map<String, Int>>(reactionsJson) }.getOrDefault(emptyMap()),
    ts = ts,
    status = status
)

/*
fun DayData.toEntity(date: String) = DayEntity(
    date = date,
    phase = phase,
    symptomsJson = Json.encodeToString(symptoms),
    intimacy = intimacy,
    note = note
)
fun DayEntity.toDomain() = DayData(
    phase = phase,
    symptoms = Json.decodeFromString(symptomsJson),
    intimacy = intimacy,
    note = note
)
*/
