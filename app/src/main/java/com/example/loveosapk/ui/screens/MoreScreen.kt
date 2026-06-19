package com.example.loveosapk.ui.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.data.UserProfile
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.components.sharedAvatar
import coil3.compose.AsyncImage
import com.example.loveosapk.ui.components.PerformanceChecklist
import com.example.loveosapk.ui.theme.Accent
import com.example.loveosapk.ui.theme.AccentSecondary

import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import com.example.loveosapk.ui.navigation.Screen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val appState by viewModel.appState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isDebugMode by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileSection(
            profile = appState.me,
            title = "Вы",
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            onClick = { navController.navigate(Screen.Profile.route) }
        )
        
        ProfileSection(
            profile = appState.partner,
            title = "Партнёр",
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            onClick = { 
                android.widget.Toast.makeText(context, "Профиль партнёра редактируется им самим ✨", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        SectionTitle("Развлечения")
        MoreMenuItem(Icons.Default.Games, "Игры", "Крестики-нолики, Виселица") { navController.navigate(Screen.Games.route) }
        MoreMenuItem(Icons.AutoMirrored.Filled.StickyNote2, "Заметки", "Общие записи и капсулы") { navController.navigate(Screen.Notes.route) }
        MoreMenuItem(Icons.Default.Quiz, "Викторины", "Насколько хорошо вы знаете друг друга?") { 
            android.widget.Toast.makeText(context, "Викторины скоро появятся! 🧠", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        SectionTitle("Настройки")
        var showSyncDialog by remember { mutableStateOf(false) }
        MoreMenuItem(Icons.Default.Sync, "Синхронизация", "Код: ${appState.partnerCode ?: "Не привязан"}") { 
            showSyncDialog = true
        }

        if (showSyncDialog) {
            var tempCode by remember { mutableStateOf(appState.partnerCode ?: "") }
            AlertDialog(
                onDismissRequest = { showSyncDialog = false },
                title = { Text("Настройка синхронизации") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Введите код партнёра для объединения данных.", fontSize = 14.sp)
                        OutlinedTextField(
                            value = tempCode,
                            onValueChange = { tempCode = it.uppercase().replace(Regex("[^A-Z0-9_-]"), "") },
                            label = { Text("Код партнёра") },
                            placeholder = { Text("LOVE-XXXXXX") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = {
                                tempCode = "LOVE-" + java.util.UUID.randomUUID()
                                    .toString()
                                    .replace("-", "")
                                    .take(8)
                                    .uppercase()
                            }
                        ) {
                            Text("Сгенерировать код")
                        }
                        if (appState.partnerCode != null) {
                            TextButton(
                                onClick = { 
                                    viewModel.finishSetup(
                                        appState.me.name,
                                        appState.partner.name,
                                        appState.startDate,
                                        appState.myRole,
                                        null
                                    )
                                    showSyncDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Отключить синхронизацию")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.finishSetup(
                            appState.me.name,
                            appState.partner.name,
                            appState.startDate,
                            appState.myRole,
                            tempCode
                        )
                        showSyncDialog = false
                    }) {
                        Text("Сохранить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSyncDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
        MoreMenuItem(Icons.Default.Fingerprint, "Биометрия", "Защита входа") { 
            android.widget.Toast.makeText(context, "Биометрия будет добавлена в следующем обновлении 🔒", android.widget.Toast.LENGTH_SHORT).show()
        }
        MoreMenuItem(Icons.Default.Palette, "Оформление", "Тема: ${if(appState.theme == "dark") "Темная" else "Светлая"}") { 
            viewModel.toggleTheme()
        }
        
        if (isDebugMode) {
            SectionTitle("Разработка")
            PerformanceChecklist()
        }
        
        SectionTitle("О приложении")
        var debugTapCount by remember { mutableIntStateOf(0) }
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LoveOS v3.1 💕",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        debugTapCount++
                        if (debugTapCount >= 7) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isDebugMode = true
                            android.widget.Toast.makeText(context, "Режим разработчика активирован 🛠️", android.widget.Toast.LENGTH_SHORT).show()
                            debugTapCount = 0
                        }
                    }
                )
                Text(
                    text = "Создано с любовью для двоих",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileSection(
    profile: UserProfile,
    title: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .sharedAvatar(
                        key = "avatar_${if (title == "Вы") "me" else "partner"}",
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope
                    )
                    .background(Accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profile.avatar.startsWith("http")) {
                    AsyncImage(
                        model = profile.avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(profile.avatar, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 12.sp, color = Accent)
                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun MoreMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Accent)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
