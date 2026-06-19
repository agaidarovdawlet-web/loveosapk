package com.example.loveosapk.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveosapk.data.AppState
import com.example.loveosapk.data.PreferenceManager
import com.example.loveosapk.data.local.LoveOsDatabase
import com.example.loveosapk.data.local.CycleLogEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.loveosapk.data.repository.SyncRepository
import com.example.loveosapk.data.repository.CycleRepositoryImpl
import com.example.loveosapk.data.remote.MediaStorage
import com.example.loveosapk.data.sync.ImageCompressor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LoveOsDatabase.getDatabase(application)
    val preferenceManager = PreferenceManager(application)
    val repository = SyncRepository(application, database.dao(), database.cycleDao(), preferenceManager)
    val cycleRepository = CycleRepositoryImpl(database.cycleDao(), preferenceManager)
    private val mediaStorage = MediaStorage()
    private val imageCompressor = ImageCompressor(application)

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    val appState = repository.appState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState()
    )

    val wishes = repository.wishes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tasks = repository.tasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savings = repository.savings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatMessages = repository.chatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notes = repository.notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val capsules = repository.capsules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val days = repository.days.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val incomingHearts = repository.incomingHearts

    val latestPeriod = repository.periods.map { it.lastOrNull() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun sendChatMessage(
        text: String,
        imageUrl: String? = null,
        voiceUrl: String? = null,
        voiceDuration: Long? = null,
        videoUrl: String? = null,
        videoDuration: Long? = null
    ) {
        viewModelScope.launch {
            repository.sendChatMessage(text, imageUrl, voiceUrl, voiceDuration, videoUrl, videoDuration)
        }
    }

    fun uploadAndSendPhoto(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val compressedFile = imageCompressor.compress(uri)
                if (compressedFile != null) {
                    val url = mediaStorage.uploadPhoto(Uri.fromFile(compressedFile))
                    repository.sendChatMessage("", imageUrl = url)
                    Toast.makeText(getApplication(), "Фото отправлено ✨", Toast.LENGTH_SHORT).show()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("UPLOAD", "Photo upload failed", e)
                Toast.makeText(getApplication(), "Ошибка загрузки фото ❌", Toast.LENGTH_SHORT).show()
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun uploadAndSendVoice(file: File, duration: Long) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val url = mediaStorage.uploadVoice(file)
                repository.sendChatMessage("", voiceUrl = url, voiceDuration = duration)
                Toast.makeText(getApplication(), "Голосовое отправлено 🎙️", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("UPLOAD", "Voice upload failed", e)
                Toast.makeText(getApplication(), "Ошибка отправки голосового ❌", Toast.LENGTH_SHORT).show()
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun uploadAndSendVideo(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val contentType = contentResolver.getType(uri) ?: "video/mp4"
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(contentType)
                    ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                    ?: "mp4"
                val url = mediaStorage.uploadVideo(uri, extension, contentType)
                repository.sendChatMessage("", videoUrl = url)
                Toast.makeText(getApplication(), "Видео отправлено", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("UPLOAD", "Video upload failed", e)
                Toast.makeText(getApplication(), "Ошибка загрузки видео", Toast.LENGTH_SHORT).show()
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun addWish(text: String, category: String, priority: String = "MEDIUM", imageUrl: String? = null) {
        viewModelScope.launch {
            repository.addWish(com.example.loveosapk.data.Wish(
                id = java.util.UUID.randomUUID().toString(),
                text = text,
                category = category,
                author = appState.value.me.name,
                priority = priority,
                imageUrl = imageUrl
            ))
        }
    }

    fun toggleWish(wish: com.example.loveosapk.data.Wish) {
        viewModelScope.launch { repository.toggleWish(wish) }
    }

    fun deleteWish(id: String) {
        viewModelScope.launch { repository.deleteWish(id) }
    }

    fun addTask(text: String, owner: String, deadline: String? = null) {
        viewModelScope.launch {
            repository.addTask(com.example.loveosapk.data.Task(
                id = java.util.UUID.randomUUID().toString(),
                text = text,
                owner = owner,
                deadline = deadline
            ))
        }
    }

    fun toggleTask(task: com.example.loveosapk.data.Task) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch { repository.deleteTask(id) }
    }

    fun addSaving(name: String, goal: Double, currency: String) {
        viewModelScope.launch {
            repository.addSaving(com.example.loveosapk.data.Saving(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                goal = goal,
                currency = currency
            ))
        }
    }

    fun updateSaving(saving: com.example.loveosapk.data.Saving, current: Double) {
        viewModelScope.launch {
            repository.updateSaving(saving.copy(current = current, ts = System.currentTimeMillis()))
        }
    }

    fun deleteSaving(id: String) {
        viewModelScope.launch { repository.deleteSaving(id) }
    }

    fun updateProfile(profile: com.example.loveosapk.data.UserProfile) {
        viewModelScope.launch { repository.updateMe(profile) }
    }

    fun updateMood(emoji: String, comment: String) {
        viewModelScope.launch {
            val currentProfile = appState.value.me
            val newMood = com.example.loveosapk.data.MoodEntry(emoji, comment, java.time.LocalDateTime.now().toString())
            val updatedProfile = currentProfile.copy(moods = currentProfile.moods + newMood)
            repository.updateMe(updatedProfile)
        }
    }

    fun sendHeart() {
        viewModelScope.launch { repository.sendHeart() }
    }

    fun markPeriodToday() {
        viewModelScope.launch {
            cycleRepository.saveLog(com.example.loveosapk.domain.model.CycleLog(
                date = java.time.LocalDate.now(),
                isPeriod = true,
                flowIntensity = 0,
                symptoms = emptyList(),
                mood = null,
                painLevel = 0,
                energyLevel = 5,
                notes = "",
                isOvulation = false
            ))
        }
    }

    fun updateDayData(date: String, symptoms: List<String>, note: String) {
        viewModelScope.launch {
            val localDate = java.time.LocalDate.parse(date)
            val current = cycleRepository.getLogForDate(localDate).first() ?: com.example.loveosapk.domain.model.CycleLog(
                date = localDate,
                isPeriod = false,
                flowIntensity = 0,
                symptoms = emptyList(),
                mood = null,
                painLevel = 0,
                energyLevel = 5,
                notes = note,
                isOvulation = false
            )
            cycleRepository.saveLog(current.copy(
                symptoms = try { symptoms.map { com.example.loveosapk.domain.model.Symptom.valueOf(it) } } catch (e: Exception) { emptyList() },
                notes = note
            ))
        }
    }

    fun addNote(text: String) {
        viewModelScope.launch {
            repository.addNote(com.example.loveosapk.data.Note(
                id = java.util.UUID.randomUUID().toString(),
                text = text,
                date = java.time.LocalDate.now().toString(),
                author = appState.value.me.name
            ))
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    fun addCapsule(text: String, openAfterMs: Long) {
        viewModelScope.launch {
            repository.addTimeCapsule(com.example.loveosapk.data.TimeCapsule(
                id = java.util.UUID.randomUUID().toString(),
                text = text,
                created = java.time.LocalDate.now().toString(),
                openAfter = openAfterMs
            ))
        }
    }

    fun deleteCapsule(id: String) {
        viewModelScope.launch { repository.deleteTimeCapsule(id) }
    }

    fun updateMeetingDate(ts: Long) {
        viewModelScope.launch {
            val current = appState.value
            repository.updateAppState(current.copy(meetingDate = ts))
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val current = appState.value
            val newTheme = if (current.theme == "dark") "light" else "dark"
            repository.updateAppState(current.copy(theme = newTheme))
        }
    }

    fun finishSetup(
        myName: String,
        partnerName: String,
        startDate: String,
        myRole: String,
        syncCode: String? = null
    ) {
        viewModelScope.launch {
            val currentState = appState.value
            val normalizedSyncCode = syncCode
                ?.trim()
                ?.uppercase()
                ?.replace(Regex("[^A-Z0-9_-]"), "")
                ?.takeIf { it.isNotBlank() }
            val newState = currentState.copy(
                setup = true,
                me = currentState.me.copy(name = myName),
                partner = currentState.partner.copy(name = partnerName),
                startDate = startDate,
                myRole = myRole,
                partnerCode = normalizedSyncCode,
                syncEnabled = normalizedSyncCode != null
            )
            preferenceManager.updateAppState(newState)
        }
    }

    override fun onCleared() {
        repository.close()
        cycleRepository.close()
        super.onCleared()
    }
}
