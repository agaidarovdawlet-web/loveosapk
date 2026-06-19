package com.example.loveosapk.ui.features.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.loveosapk.ui.components.bounceClickable
import com.example.loveosapk.ui.theme.Accent
import com.example.loveosapk.ui.theme.AccentSecondary

@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onAddVideo: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onCancelVoice: () -> Unit,
    onSend: () -> Unit,
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(24.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = !isRecording) {
            Row {
                IconButton(onClick = onAddPhoto, enabled = enabled) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Добавить фото", tint = Color.White.copy(alpha = if (enabled) 0.64f else 0.28f))
                }

                IconButton(onClick = onAddVideo, enabled = enabled) {
                    Icon(Icons.Default.Videocam, contentDescription = "Добавить видео", tint = Color.White.copy(alpha = if (enabled) 0.64f else 0.28f))
                }
            }
        }

        if (isRecording) {
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Accent, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Запись ${formatRecordingDuration(recordingDurationMs)}",
                    color = Accent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ваше послание...", color = Color.White.copy(alpha = 0.4f)) },
                enabled = enabled,
                maxLines = 5,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() && enabled) onSend()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Accent
                ),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )
        }
        
        val showSend = text.isNotBlank() && !isRecording && enabled

        AnimatedVisibility(visible = isRecording) {
            IconButton(onClick = onCancelVoice) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Отменить запись",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        IconButton(
            onClick = { if (isRecording) onStopVoice() else onStartVoice() },
            enabled = enabled || isRecording,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isRecording) "Отправить голосовое" else "Записать голосовое",
                tint = if (isRecording) Accent else Color.White.copy(alpha = if (enabled) 0.64f else 0.28f)
            )
        }

        AnimatedVisibility(visible = showSend) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Brush.linearGradient(listOf(Accent, AccentSecondary)), CircleShape)
                    .clip(CircleShape)
                    .bounceClickable { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send", 
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatRecordingDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
