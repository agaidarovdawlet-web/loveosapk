package com.example.loveosapk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import com.example.loveosapk.data.MessageStatus
import com.example.loveosapk.data.sync.VoiceRecorder
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.components.AtmosphericBackground
import com.example.loveosapk.ui.features.chat.components.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var text by rememberSaveable { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    
    val voiceRecorder = remember { VoiceRecorder(context) }
    val listState = rememberLazyListState()

    fun startRecording() {
        try {
            voiceRecorder.start()
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
            recordingElapsedMs = 0L
        } catch (e: Exception) {
            isRecording = false
            voiceRecorder.release()
            android.widget.Toast.makeText(context, "Не удалось начать запись", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelRecording() {
        if (!isRecording) return
        isRecording = false
        recordingElapsedMs = 0L
        runCatching { voiceRecorder.stop()?.delete() }
            .onFailure { voiceRecorder.release() }
    }

    DisposableEffect(voiceRecorder) {
        onDispose {
            voiceRecorder.release()
        }
    }

    LaunchedEffect(isRecording, recordingStartTime) {
        while (isRecording) {
            recordingElapsedMs = System.currentTimeMillis() - recordingStartTime
            delay(250)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAndSendPhoto(it) }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAndSendVideo(it) }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            android.widget.Toast.makeText(context, "Нужен доступ к микрофону 🎙️", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val chatRows = remember(messages) {
        buildList {
            messages.groupBy { message ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(message.ts))
            }.forEach { (dateKey, dayMessages) ->
                add(ChatRow.Header(dateKey, formatChatDate(dayMessages.first().ts)))
                dayMessages.forEach { add(ChatRow.Message(it)) }
            }
        }
    }

    LaunchedEffect(chatRows.size) {
        if (chatRows.isNotEmpty()) {
            listState.animateScrollToItem(chatRows.lastIndex)
        }
    }

    LaunchedEffect(messages, appState.myRole) {
        val unreadIncoming = messages.filter { message ->
            message.sender != appState.myRole && message.status != MessageStatus.READ
        }
        if (unreadIncoming.isNotEmpty()) {
            viewModel.markChatMessagesRead(unreadIncoming)
        }
    }

    AtmosphericBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (chatRows.isEmpty()) {
                    EmptyChatState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = chatRows,
                            key = { it.key },
                            contentType = { row -> if (row is ChatRow.Header) "header" else "message" }
                        ) { row ->
                            when (row) {
                                is ChatRow.Header -> DateDivider(row.label)
                                is ChatRow.Message -> PremiumMessageBubble(
                                    message = row.message,
                                    isMe = row.message.sender == appState.myRole,
                                    onImageClick = { selectedImageUrl = it }
                                )
                            }
                        }
                    }
                }                
            }

            AnimatedVisibility(
                visible = false,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TypingIndicator(modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
            }

            QuickMessageChips(
                visible = text.isBlank() && !isRecording,
                enabled = !isUploading,
                onSend = { quickText -> viewModel.sendChatMessage(quickText) }
            )

            MessageInputBar(
                text = text,
                onTextChange = { text = it },
                onAddPhoto = {
                    photoPickerLauncher.launch("image/*")
                },
                onAddVideo = {
                    videoPickerLauncher.launch("video/*")
                },
                onStartVoice = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPermission) {
                        startRecording()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopVoice = {
                    isRecording = false
                    val duration = System.currentTimeMillis() - recordingStartTime
                    recordingElapsedMs = 0L
                    val file = try {
                        voiceRecorder.stop()
                    } catch (e: Exception) {
                        voiceRecorder.release()
                        android.widget.Toast.makeText(context, "Не удалось сохранить запись", android.widget.Toast.LENGTH_SHORT).show()
                        null
                    }
                    if (file != null && duration > 500) {
                        viewModel.uploadAndSendVoice(file, duration)
                    } else if (file != null) {
                        file.delete()
                    }
                },
                onCancelVoice = { cancelRecording() },
                onSend = {
                    if (text.isNotBlank()) {
                        viewModel.sendChatMessage(text)
                        text = ""
                    }
                },
                isRecording = isRecording,
                recordingDurationMs = recordingElapsedMs,
                enabled = !isUploading
            )
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.28f), androidx.compose.foundation.shape.CircleShape)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = com.example.loveosapk.ui.theme.Accent)
                }
            }
        }
    }

    if (selectedImageUrl != null) {
        SimpleImageViewer(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null },
            onSave = { /* Save to gallery */ }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QuickMessageChips(
    visible: Boolean,
    enabled: Boolean,
    onSend: (String) -> Unit
) {
    val quickMessages = remember {
        listOf(
            "Скучаю по тебе",
            "Обнимаю",
            "Как твой день?",
            "Я рядом",
            "Люблю тебя",
            "Давай вечером созвонимся"
        )
    }

    AnimatedVisibility(visible = visible) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            quickMessages.forEach { message ->
                FilterChip(
                    selected = false,
                    enabled = enabled,
                    onClick = { onSend(message) },
                    label = { Text(message) }
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.38f),
            modifier = Modifier.size(58.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Чат пока пустой",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Напишите первое сообщение или отправьте быстрое признание.",
            color = Color.White.copy(alpha = 0.62f)
        )
    }
}

private sealed class ChatRow {
    abstract val key: String

    data class Header(
        val dateKey: String,
        val label: String
    ) : ChatRow() {
        override val key: String = "header_$dateKey"
    }

    data class Message(
        val message: com.example.loveosapk.data.ChatMessage
    ) : ChatRow() {
        override val key: String = message.id
    }
}

private fun formatChatDate(timestamp: Long): String {
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    fun Calendar.sameDay(other: Calendar): Boolean =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

    return when {
        messageCalendar.sameDay(today) -> "Сегодня"
        messageCalendar.sameDay(yesterday) -> "Вчера"
        else -> SimpleDateFormat("d MMMM", Locale.forLanguageTag("ru")).format(Date(timestamp))
    }
}
