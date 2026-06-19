package com.example.loveosapk.ui.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.draw.clip
import com.example.loveosapk.data.MoodEntry
import com.example.loveosapk.data.UserProfile
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.components.LogStateChange
import com.example.loveosapk.ui.components.TraceableButton
import com.example.loveosapk.ui.components.sharedAvatar
import com.example.loveosapk.ui.theme.Accent
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val appState by viewModel.appState.collectAsState()
    val profile = appState.me
    
    var name by remember { mutableStateOf(profile.name) }
    var avatar by remember { mutableStateOf(profile.avatar) }
    var color by remember { mutableStateOf(profile.color) }
    var food by remember { mutableStateOf(profile.food) }
    var movie by remember { mutableStateOf(profile.movie) }
    var song by remember { mutableStateOf(profile.song) }
    var hobby by remember { mutableStateOf(profile.hobby) }
    var dream by remember { mutableStateOf(profile.dream) }

    // Diagnostic logs for state
    LogStateChange("Profile_Name", name)
    LogStateChange("Profile_Avatar", avatar)
    LogStateChange("AppState_Me", appState.me)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Мой профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    TraceableButton(
                        label = "SaveProfile",
                        onClick = {
                            viewModel.updateProfile(profile.copy(
                                name = name,
                                avatar = avatar,
                                color = color,
                                food = food,
                                movie = movie,
                                song = song,
                                hobby = hobby,
                                dream = dream
                            ))
                        }
                    ) {
                        Icon(Icons.Default.Save, "Save", tint = Accent)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MoodSelector { emoji, comment -> viewModel.updateMood(emoji, comment) }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .sharedAvatar(
                            key = "avatar_me",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                        .background(Accent.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar.startsWith("http")) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(avatar, fontSize = 40.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                ProfileField("Имя", name) { name = it }
            }

            ProfileField("Аватар (Эмодзи или URL)", avatar) { avatar = it }
            ProfileField("Любимый цвет", color) { color = it }
            ProfileField("Любимая еда", food) { food = it }
            ProfileField("Любимый фильм", movie) { movie = it }
            ProfileField("Любимая песня", song) { song = it }
            ProfileField("Хобби", hobby) { hobby = it }
            ProfileField("Мечта", dream) { dream = it }
        }
    }
}

@Composable
fun MoodSelector(onUpdateMood: (String, String) -> Unit) {
    val moods = listOf("😊", "🥰", "😴", "😠", "😢", "🤒", "😋", "🥳")
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var comment by remember { mutableStateOf("") }

    Column {
        Text("Как ты сейчас?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(moods) { mood ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (selectedMood == mood) Accent.copy(alpha = 0.2f) else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedMood = mood }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mood, fontSize = 24.sp)
                }
            }
        }
        if (selectedMood != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Добавь комментарий") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TraceableButton(
                label = "UpdateMood",
                onClick = { 
                    onUpdateMood(selectedMood!!, comment)
                    selectedMood = null
                    comment = ""
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Обновить настроение")
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
