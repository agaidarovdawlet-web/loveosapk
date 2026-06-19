package com.example.loveosapk.ui.features.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onSend: () -> Unit,
    isRecording: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAddPhoto) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", tint = Color.White.copy(alpha = 0.6f))
        }

        IconButton(onClick = onAddVideo) {
            Icon(Icons.Default.Videocam, contentDescription = "Add Video", tint = Color.White.copy(alpha = 0.6f))
        }

        if (isRecording) {
            Text(
                text = "Запись голоса...",
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                color = Accent
            )
        } else {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ваше послание...", color = Color.White.copy(alpha = 0.4f)) },
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
        
        val showSend = text.isNotBlank() && !isRecording
        
        IconButton(
            onClick = { if (isRecording) onStopVoice() else onStartVoice() },
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice",
                tint = if (isRecording) Accent else Color.White.copy(alpha = 0.6f)
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
