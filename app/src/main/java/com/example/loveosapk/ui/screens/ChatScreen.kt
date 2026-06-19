package com.example.loveosapk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.loveosapk.data.sync.VoiceRecorder
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.components.AtmosphericBackground
import com.example.loveosapk.ui.features.chat.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val appState by viewModel.appState.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val context = LocalContext.current
    
    var text by rememberSaveable { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    
    val voiceRecorder = remember { VoiceRecorder(context) }
    val listState = rememberLazyListState()

    fun startRecording() {
        try {
            voiceRecorder.start()
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
            voiceRecorder.release()
            android.widget.Toast.makeText(context, "Не удалось начать запись", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(voiceRecorder) {
        onDispose {
            voiceRecorder.release()
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

    val groupedMessages = remember(messages) {
        messages.groupBy { 
            SimpleDateFormat("d MMMM", Locale("ru")).format(Date(it.ts))
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size + groupedMessages.size - 1
            if (lastIndex >= 0) {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    AtmosphericBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedMessages.forEach { (date, msgs) ->
                    item(key = "header_$date", contentType = "header") {
                        DateDivider(date)
                    }
                    items(
                        items = msgs,
                        key = { it.id },
                        contentType = { it.sender == appState.myRole }
                    ) { msg ->
                        PremiumMessageBubble(
                            message = msg,
                            isMe = msg.sender == appState.myRole,
                            onImageClick = { selectedImageUrl = it }
                        )
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
                    val file = try {
                        voiceRecorder.stop()
                    } catch (e: Exception) {
                        voiceRecorder.release()
                        android.widget.Toast.makeText(context, "Не удалось сохранить запись", android.widget.Toast.LENGTH_SHORT).show()
                        null
                    }
                    if (file != null && duration > 500) {
                        viewModel.uploadAndSendVoice(file, duration)
                    }
                },
                onSend = {
                    if (text.isNotBlank()) {
                        viewModel.sendChatMessage(text)
                        text = ""
                    }
                },
                isRecording = isRecording
            )
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = com.example.loveosapk.ui.theme.Accent)
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
private fun QuickMessageChips(onSend: (String) -> Unit) {
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
                onClick = { onSend(message) },
                label = { Text(message) }
            )
        }
    }
}
