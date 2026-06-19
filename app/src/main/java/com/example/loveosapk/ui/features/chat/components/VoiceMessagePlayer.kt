package com.example.loveosapk.ui.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.media3.common.Player

@Composable
fun VoiceMessagePlayer(
    url: String,
    duration: Long?,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val waveformHeights = remember(url) { List(15) { (10..24).random().dp } }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    exoPlayer.seekTo(0)
                }
            }

            override fun onIsPlayingChanged(isPlayerPlaying: Boolean) {
                isPlaying = isPlayerPlaying
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .semantics {
                    role = Role.Button
                    contentDescription = if (isPlaying) "Поставить голосовое на паузу" else "Воспроизвести голосовое"
                }
                .clickable {
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                    isPlaying = !isPlaying
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Simple waveform representation
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            waveformHeights.forEach { height ->
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(height)
                        .background(
                            if (isMe) Color.White.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        duration?.let {
            val seconds = (it / 1000) % 60
            val minutes = (it / (1000 * 60)) % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontSize = 11.sp,
                color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
