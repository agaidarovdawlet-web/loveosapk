package com.example.loveosapk.ui.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.data.ChatMessage
import com.example.loveosapk.data.MessageStatus
import com.example.loveosapk.ui.theme.Accent
import com.example.loveosapk.ui.theme.AccentSecondary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PremiumMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onImageClick: (String) -> Unit
) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    val bubbleBackground = if (isMe) {
        Brush.linearGradient(listOf(Accent, AccentSecondary))
    } else {
        Brush.linearGradient(listOf(
            Color.White.copy(alpha = 0.1f), 
            Color.White.copy(alpha = 0.05f)
        ))
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleBackground, bubbleShape)
                .padding(12.dp)
        ) {
            if (message.imageUrl != null) {
                ImageBubble(
                    imageUrl = message.imageUrl,
                    onClick = { onImageClick(message.imageUrl) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (message.voiceUrl != null) {
                VoiceMessagePlayer(
                    url = message.voiceUrl,
                    duration = message.voiceDuration,
                    isMe = isMe,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (message.videoUrl != null) {
                VideoMessagePlayer(
                    url = message.videoUrl,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text, 
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.ts)),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                if (isMe) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = when(message.status) {
                            MessageStatus.SENT -> Icons.Default.Done
                            MessageStatus.DELIVERED -> Icons.Default.DoneAll
                            MessageStatus.READ -> Icons.Default.DoneAll
                        },
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (message.status == MessageStatus.READ) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
