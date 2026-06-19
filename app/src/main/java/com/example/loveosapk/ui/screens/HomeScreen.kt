package com.example.loveosapk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.data.AppState
import com.example.loveosapk.ui.components.*
import com.example.loveosapk.ui.theme.Accent
import com.example.loveosapk.ui.theme.AccentSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(appState: AppState, viewModel: com.example.loveosapk.ui.MainViewModel) {
    val context = LocalContext.current
    var triggerHearts by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.incomingHearts.collect {
            android.widget.Toast.makeText(context, "💖 Получено сердечко!", android.widget.Toast.LENGTH_SHORT).show()
            triggerHearts = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AtmosphericBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                
                RomanticButton(
                    onClick = { 
                        viewModel.sendHeart()
                        triggerHearts = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💓", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("Отправить сердечко", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

                RelationshipHeroCard(appState.startDate)

                TimeDetailsGrid(appState.startDate)

                var showMeetingDialog by remember { mutableStateOf(false) }
                MeetingCountdownCard(appState.meetingDate) { showMeetingDialog = true }

                if (showMeetingDialog) {
                    SetMeetingDialog(onDismiss = { showMeetingDialog = false }, onSave = { viewModel.updateMeetingDate(it) })
                }

                DailyQuestionCard(
                    myName = appState.me.name,
                    partnerName = appState.partner.name,
                    onSave = { prompt -> viewModel.addNote(prompt) }
                )

                CareRitualCard(
                    myName = appState.me.name,
                    partnerName = appState.partner.name,
                    onSave = { ritual -> viewModel.addTask(ritual, "both") }
                )

                RouletteCard(onSave = { idea -> viewModel.addWish(idea, "date", priority = "MEDIUM") })
                
                Spacer(Modifier.height(24.dp))
            }
        }
        
        HeartBurstOverlay(trigger = triggerHearts, onAnimationEnd = { triggerHearts = false })
    }
}

@Composable
fun RelationshipHeroCard(startDateStr: String) {
    if (startDateStr.isEmpty()) return
    
    val startDate = remember(startDateStr) { runCatching { LocalDate.parse(startDateStr) }.getOrNull() } ?: return
    
    // Optimize: Only recompose when totalDays actually changes (once per day)
    val totalDays by remember(startDate) {
        derivedStateOf {
            ChronoUnit.DAYS.between(startDate, LocalDate.now())
        }
    }
    
    val period by remember(startDate) {
        derivedStateOf {
            Period.between(startDate, LocalDate.now())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = Brush.linearGradient(listOf(Accent.copy(alpha = 0.8f), AccentSecondary.copy(alpha = 0.8f))),
                shape = RoundedCornerShape(24.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("МЫ ВМЕСТЕ УЖЕ", fontSize = 12.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            Row(verticalAlignment = Alignment.Bottom) {
                OdometerCounter(value = totalDays, style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary))
                Spacer(Modifier.width(8.dp))
                Text("дней", fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 12.dp))
            }
            Text(
                "${period.years} лет • ${period.months} мес • ${period.days} дн",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DailyQuestionCard(
    myName: String,
    partnerName: String,
    onSave: (String) -> Unit
) {
    val prompts = remember {
        listOf(
            "Что я сделал(а) недавно, из-за чего ты почувствовал(а) себя любимым человеком?",
            "Какой маленький ритуал мы можем добавить в эту неделю?",
            "О чём ты давно хотел(а) поговорить спокойно и без спешки?",
            "Какой момент за последнее время хочется повторить?",
            "В какой ситуации тебе особенно нужна моя поддержка?",
            "Что в наших отношениях стало сильнее за последний месяц?",
            "Какой совместный план даст нам больше близости?"
        )
    }
    val dayIndex = remember { LocalDate.now().dayOfYear % prompts.size }
    val prompt = prompts[dayIndex]
    var saved by rememberSaveable(prompt) { mutableStateOf(false) }
    val names = listOf(myName, partnerName).filter { it.isNotBlank() }.joinToString(" + ").ifBlank { "Вы двое" }

    GlassCard {
        Text("Вопрос дня", style = MaterialTheme.typography.labelMedium, color = Accent)
        Spacer(Modifier.height(8.dp))
        Text(names, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(12.dp))
        Text(prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onSave("Вопрос дня: $prompt")
                saved = true
            },
            enabled = !saved,
            colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(alpha = 0.18f)),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (saved) "Сохранено" else "В заметки", color = Accent)
        }
    }
}

@Composable
fun CareRitualCard(
    myName: String,
    partnerName: String,
    onSave: (String) -> Unit
) {
    val rituals = remember {
        listOf(
            "10 минут без телефонов: каждый говорит, за что благодарен другому",
            "Выбрать один бытовой пункт и закрыть его вместе сегодня",
            "Отправить партнёру голосовое с тремя тёплыми словами",
            "Запланировать короткую прогулку без обсуждения дел",
            "Спросить: «Что я могу сделать, чтобы твой день стал легче?»",
            "Приготовить или заказать то, что любит партнёр",
            "Вечером назвать один момент, где вы были командой"
        )
    }
    val dayIndex = remember { (LocalDate.now().dayOfYear + 3) % rituals.size }
    val ritual = rituals[dayIndex]
    var saved by rememberSaveable(ritual) { mutableStateOf(false) }
    val title = if (myName.isNotBlank() && partnerName.isNotBlank()) "$myName и $partnerName" else "Ритуал для двоих"

    GlassCard {
        Text("Ритуал заботы", style = MaterialTheme.typography.labelMedium, color = AccentSecondary)
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(12.dp))
        Text(ritual, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onSave(ritual)
                saved = true
            },
            enabled = !saved,
            colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary.copy(alpha = 0.18f)),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (saved) "В задачах" else "Добавить задачу", color = AccentSecondary)
        }
    }
}

private data class DateIdea(
    val title: String,
    val category: String,
    val homeFriendly: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteCard(onSave: (String) -> Unit) {
    val ideas = remember {
        listOf(
            DateIdea("Пикник с термосом и пледом", "На улице", false),
            DateIdea("Домашнее кино с меню из трёх закусок", "Дома", true),
            DateIdea("Настольные игры на желание", "Дома", true),
            DateIdea("Прогулка ночью с горячим напитком", "На улице", false),
            DateIdea("Готовить новое блюдо вместе", "Дома", true),
            DateIdea("Фотоохота: найти 10 красивых кадров", "На улице", false),
            DateIdea("Вечер вопросов и честных ответов", "Близость", true),
            DateIdea("Мини-спа дома: музыка, свечи, уход", "Близость", true),
            DateIdea("Маршрут по местам ваших воспоминаний", "На улице", false),
            DateIdea("Письмо друг другу через год", "Близость", true)
        )
    }
    val categories = remember { listOf("Все", "Дома", "На улице", "Близость") }
    var selectedCategory by rememberSaveable { mutableStateOf("Все") }
    var onlyHome by rememberSaveable { mutableStateOf(false) }
    val filteredIdeas = remember(selectedCategory, onlyHome) {
        ideas.filter { idea ->
            (selectedCategory == "Все" || idea.category == selectedCategory) && (!onlyHome || idea.homeFriendly)
        }.ifEmpty { ideas }
    }
    var currentIdea by rememberSaveable { mutableStateOf(filteredIdeas.first().title) }
    var isSpinning by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var saved by rememberSaveable(currentIdea) { mutableStateOf(false) }

    GlassCard {
        Text("Рулетка свиданий", style = MaterialTheme.typography.labelMedium, color = Accent)
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category, fontSize = 12.sp) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = onlyHome, onCheckedChange = { onlyHome = it })
            Text("только дома", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }

        Spacer(Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentIdea,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            ConfettiOverlay(trigger = showConfetti)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            OutlinedButton(
                onClick = {
                    onSave(currentIdea)
                    saved = true
                },
                enabled = !saved
            ) {
                Text(if (saved) "В желаниях" else "Сохранить")
            }

            Button(
                onClick = {
                    if (!isSpinning) {
                        isSpinning = true
                        showConfetti = false
                    }
                },
                enabled = !isSpinning,
                colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(alpha = 0.15f))
            ) {
                Text("Крутить", color = Accent)
            }
        }

        if (isSpinning) {
            LaunchedEffect(selectedCategory, onlyHome) {
                var delayTime = 50L
                repeat(20) {
                    currentIdea = filteredIdeas.random().title
                    saved = false
                    delay(delayTime)
                    delayTime += 10L
                }
                isSpinning = false
                showConfetti = true
                delay(3000)
                showConfetti = false
            }
        }
    }
}

@Composable
fun MeetingCountdownCard(meetingDateMs: Long?, onClick: () -> Unit) {
    if (meetingDateMs == null) {
        GlassCard(onClick = onClick) {
            Text("📅 До встречи", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Text("Нажми, чтобы установить дату", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        return
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val diff = meetingDateMs - now
    if (diff <= 0) {
        GlassCard(onClick = onClick) {
            Text("🎉 Ура! Мы встретились!", fontWeight = FontWeight.Bold, color = Accent, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    } else {
        val days = diff / 86400000
        val hours = (diff % 86400000) / 3600000
        val minutes = (diff % 3600000) / 60000
        val seconds = (diff % 60000) / 1000

        GlassCard(onClick = onClick) {
            Text("📅 До встречи", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeetingTimeBox("$days", "дней", Modifier.weight(1f))
                MeetingTimeBox("$hours", "час", Modifier.weight(1f))
                MeetingTimeBox("$minutes", "мин", Modifier.weight(1f))
                MeetingTimeBox("$seconds", "сек", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SetMeetingDialog(onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var dateStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Дата встречи") },
        text = {
            Column {
                Text("Введите дату в формате ГГГГ-ММ-ДД ЧЧ:ММ", fontSize = 12.sp, color = if (error != null) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { 
                        dateStr = it
                        error = null 
                    },
                    placeholder = { Text("2026-06-15 12:00") },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = Color.Red, fontSize = 10.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    sdf.isLenient = false
                    val date = sdf.parse(dateStr.trim())
                    if (date != null) {
                        onSave(date.time)
                        onDismiss()
                    } else {
                        error = "Некорректная дата"
                    }
                } catch (e: Exception) {
                    error = "Формат: ГГГГ-ММ-ДД ЧЧ:ММ"
                }
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun MeetingTimeBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Accent)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
fun TimeDetailsGrid(startDateStr: String) {
    if (startDateStr.isEmpty()) return
    
    val startDateTime = remember(startDateStr) {
        runCatching { LocalDate.parse(startDateStr).atStartOfDay() }.getOrNull()
    } ?: return
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    // Optimize: derivedStateOf for time parts to prevent unnecessary grid updates
    val hours by remember { derivedStateOf { (ChronoUnit.SECONDS.between(startDateTime, now) / 3600) % 24 } }
    val minutes by remember { derivedStateOf { (ChronoUnit.SECONDS.between(startDateTime, now) / 60) % 60 } }
    val seconds by remember { derivedStateOf { ChronoUnit.SECONDS.between(startDateTime, now) % 60 } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TimeBox(hours.toString(), "часов", Modifier.weight(1f))
        TimeBox(minutes.toString(), "минут", Modifier.weight(1f))
        TimeBox(seconds.toString(), "секунд", Modifier.weight(1f))
    }
}

@Composable
fun TimeBox(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}
