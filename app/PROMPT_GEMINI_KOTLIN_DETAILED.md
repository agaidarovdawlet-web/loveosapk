# Промт для Gemini: миграция LoveOS с HTML/JS на Kotlin (Jetpack Compose + Firebase)

## Контекст проекта

LoveOS — это личное Android-приложение для пары (я и моя девушка). Не коммерческое, не публикуется в сторы. Сейчас работает как PWA на GitHub Pages, нужно перенести на нативный Android с Jetpack Compose.

**Что НЕ нужно:**
- ❌ iOS / Kotlin Multiplatform
- ❌ PWA / Web-версия (полный переход на Android)
- ❌ Публикация в Google Play (APK для личного использования)

**Что НУЖНО:**
- ✅ Android-виджеты на главный экран (таймер, фаза цикла, следующая встреча)
- ✅ Push-уведомления через Firebase Cloud Messaging
- ✅ Biometric lock (отпечаток / Face ID) для входа
- ✅ Material Design 3 с кастомной темой
- ✅ Анимации на уровне нативного приложения
- ✅ Экспорт/импорт данных (JSON)
- ✅ Резервное копирование в облако (Firebase Storage)
- ✅ Тёмная/светлая тема + системная
- ✅ Haptic feedback везде
- ✅ Автоматический backup в Firebase при изменениях

---

## Текущая архитектура (HTML/JS)

### Структура данных (Firebase Realtime Database)

```json
{
  "loveos_shared": {
    "profiles": {
      "{deviceId}": {
        "role": "me" | "partner",
        "name": "string",
        "avatar": "emoji",
        "color": "string",
        "food": "string",
        "movie": "string",
        "song": "string",
        "hobby": "string",
        "dream": "string",
        "loveLang": "string",
        "moods": [{"emoji": "string", "comment": "string", "date": "ISO"}],
        "known": ["string"]
      }
    },
    "partnerInfo": {
      "{deviceId}": {
        "role": "me" | "partner",
        "name": "string",
        "avatar": "emoji"
      }
    },
    "startDate": "YYYY-MM-DD",
    "config": {"theme": "dark" | "light"},
    "hearts": [{"sender": "me" | "partner", "ts": timestamp}],
    "ttt": {
      "board": [null | "X" | "O", ...],
      "turn": "X" | "O",
      "scores": {"X": int, "O": int},
      "lastSender": "me" | "partner",
      "winner": null | "X" | "O"
    },
    "wishes": {
      "{id}": {"id": "string", "text": "string", "category": "travel|gift|experience|other", "date": "YYYY-MM-DD", "done": bool, "ts": timestamp}
    },
    "tasks": {
      "{id}": {"id": "string", "text": "string", "owner": "me|partner|both", "deadline": "YYYY-MM-DD", "done": bool, "ts": timestamp}
    },
    "notes": {
      "{id}": {"id": "string", "text": "string", "date": "ISO", "author": "string", "ts": timestamp}
    },
    "capsules": {
      "{id}": {"id": "string", "text": "string", "created": "ISO", "openAfter": timestamp, "opened": bool, "ts": timestamp}
    },
    "savings": {
      "{id}": {"id": "string", "name": "string", "goal": float, "current": float, "currency": "₽|$|€", "ts": timestamp}
    },
    "chat": {
      "{pushId}": {"id": "string", "sender": "me|partner", "text": "string", "ts": timestamp, "_status": "pending|delivered|failed"}
    },
    "cycle": {
      "config": {"length": int, "periodLength": int, "lastPeriod": "YYYY-MM-DD"},
      "periods": {"YYYY-MM-DD": true, ...},
      "days": {
        "YYYY-MM-DD": {
          "phase": "menstrual|follicular|ovulation|luteal",
          "symptoms": ["string"],
          "intimacy": bool,
          "note": "string"
        }
      }
    },
    "presence": {
      "me": timestamp,
      "partner": timestamp
    },
    "_sync": {"lastPush": timestamp}
  }
}
```

### Ключевые бизнес-правила (сохранить!)

1. **Device ID** — генерируется один раз, хранится навсегда
2. **Role** — "me" или "partner", выбирается при setup
3. **Periods** — только первые дни цикла, без дубликатов, сортированные
4. **Chat merge** — по `id`, более новый `ts` побеждает
5. **Offline-first** — всё работает без сети, sync при подключении
6. **Trim** — chat > 200, hearts > 100, остальное > 200
7. **Version migration** — при обновлении app мигрировать старую схему

---

## Архитектура Android-приложения

### Модули Gradle

```
loveos-android/
├── app/                              # Android application module
│   ├── src/main/
│   │   ├── kotlin/com/loveos/
│   │   │   ├── MainActivity.kt
│   │   │   ├── LoveOSApplication.kt
│   │   │   ├── MainViewModel.kt
│   │   │   └── di/
│   │   │       ├── AppModule.kt
│   │   │       └── DatabaseModule.kt
│   │   └── res/
│   └── build.gradle.kts
│
├── core/
│   ├── common/                       # Общие утилиты, extensions
│   ├── data/                         # Data layer (Room, DataStore, Firebase)
│   ├── domain/                       # Domain layer (models, use cases, repository interfaces)
│   └── ui/                           # Общие UI компоненты, тема
│
├── feature/
│   ├── setup/                        # Setup Wizard (3 экрана)
│   ├── home/                         # Главный экран (таймер, встреча, рулетка)
│   ├── cycle/                        # Трекер цикла (календарь, фазы, график)
│   ├── chat/                         # Чат (список, ввод, синхронизация)
│   ├── stuff/                        # Желания, задачи, копилки
│   ├── notes/                        # Заметки и капсулы времени
│   ├── games/                        # Крестики-нолики, виселица
│   ├── profiles/                     # Профили, тесты, настроение
│   └── settings/                     # Настройки, экспорт/импорт
│
├── widget/                           # Android App Widgets
│   └── src/main/
│       ├── kotlin/com/loveos/widget/
│       │   ├── TimerWidget.kt
│       │   ├── CycleWidget.kt
│       │   └── MeetingWidget.kt
│       └── res/xml/widget_info.xml
│
└── build.gradle.kts                  # Root build script
```

### Технологический стек

| Компонент | Библиотека |
|-----------|-----------|
| UI | Jetpack Compose + Material Design 3 |
| Навигация | Navigation Compose |
| DI | Hilt |
| Локальная БД | Room (SQLite) |
| Локальные настройки | DataStore Preferences |
| Сеть | Firebase Realtime Database |
| Push | Firebase Cloud Messaging |
| Облако | Firebase Storage |
| Биометрия | BiometricPrompt |
| Анимации | Compose Animation API |
| Графики | Vico (Compose charts) |
| Дата/время | kotlinx-datetime |
| Сериализация | kotlinx.serialization |
| Coroutines | Kotlin Coroutines + Flow |
| Тестирование | JUnit5, Turbine, Compose Testing |

---

## Детальные требования по экранам

### 1. Setup Wizard

**3 экрана с горизонтальным свайпом (HorizontalPager):**

```kotlin
@Composable
fun SetupScreen(
    onComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    HorizontalPager(state = pagerState) { page ->
        when (page) {
            0 -> NamesScreen(
                myName = viewModel.myName,
                partnerName = viewModel.partnerName,
                onMyNameChange = viewModel::setMyName,
                onPartnerNameChange = viewModel::setPartnerName
            )
            1 -> DateAndRoleScreen(
                startDate = viewModel.startDate,
                role = viewModel.role,
                onDateChange = viewModel::setStartDate,
                onRoleChange = viewModel::setRole
            )
            2 -> CompletionScreen(onComplete = onComplete)
        }
    }

    // Индикатор страниц (step dots)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { index ->
            val width by animateDpAsState(
                targetValue = if (pagerState.currentPage == index) 24.dp else 8.dp
            )
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (pagerState.currentPage == index) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
```

**Валидация:**
- Имена: обязательные, max 16 символов
- Дата: не в будущем
- Роль: обязательный выбор

### 2. Home Screen

**Компоненты:**

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating hearts background (аналог heart-layer)
        item { FloatingHeartsOverlay() }

        // Send heart button (big, gradient)
        item {
            GradientButton(
                onClick = viewModel::sendHeart,
                text = "💓 Отправить сердечко",
                gradient = Brush.linearGradient(
                    colors = listOf(Pink, Purple)
                )
            )
        }

        // Timer card (hero)
        item {
            TimerCard(
                days = state.daysTogether,
                years = state.years,
                months = state.months,
                days = state.days,
                hours = state.hours,
                minutes = state.minutes,
                seconds = state.seconds
            )
        }

        // Meeting countdown
        item {
            state.meetingDate?.let { date ->
                MeetingCard(
                    targetDate = date,
                    days = state.meetingDays,
                    hours = state.meetingHours,
                    minutes = state.meetingMinutes
                )
            } ?: MeetingInputCard(onSet = viewModel::setMeetingDate)
        }

        // Roulette
        item {
            RouletteCard(
                currentIdea = state.currentIdea,
                isSpinning = state.isSpinning,
                onSpin = viewModel::spinRoulette
            )
        }
    }
}
```

**TimerCard — анимированный:**
```kotlin
@Composable
fun TimerCard(
    days: Long,
    years: Int,
    months: Int,
    daysDetail: Int,
    hours: Int,
    minutes: Int,
    seconds: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Мы вместе уже",
                style = MaterialTheme.typography.bodyMedium
            )

            // Анимированное число дней
            AnimatedContent(
                targetState = days,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { -it } + fadeOut()
                }
            ) { targetDays ->
                Text(
                    text = "$targetDays",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "$years лет, $months мес, $daysDetail дн",
                style = MaterialTheme.typography.bodySmall
            )

            // Grid: hours, minutes, seconds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeUnit(value = hours, label = "часов")
                TimeUnit(value = minutes, label = "минут")
                TimeUnit(value = seconds, label = "секунд")
            }
        }
    }
}
```

**Roulette с анимацией:**
```kotlin
@Composable
fun RouletteCard(
    currentIdea: String,
    isSpinning: Boolean,
    onSpin: () -> Unit
) {
    var rotation by remember { mutableFloatStateOf(0f) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Pink, Purple)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Анимация текста при вращении
                Crossfade(
                    targetState = currentIdea,
                    animationSpec = tween(100)
                ) { idea ->
                    Text(
                        text = idea,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    rotation += 360f
                    onSpin()
                },
                enabled = !isSpinning
            ) {
                Text("🎲 Случайная идея")
            }
        }
    }
}
```

### 3. Cycle Screen

```kotlin
@Composable
fun CycleScreen(
    viewModel: CycleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Phase card
        item {
            PhaseCard(
                phase = state.currentPhase,
                day = state.cycleDay,
                advice = state.phaseAdvice,
                progress = state.cycleProgress
            )
        }

        // Prediction
        item {
            PredictionCard(
                daysUntil = state.daysUntilNextPeriod,
                nextDates = state.nextPredictions,
                averageLength = state.averageCycleLength
            )
        }

        // Calendar
        item {
            CycleCalendar(
                currentMonth = state.currentMonth,
                selectedDate = state.selectedDate,
                periodDates = state.periodDates,
                ovulationDates = state.ovulationDates,
                today = state.today,
                onDateClick = viewModel::onDateClick,
                onMonthChange = viewModel::changeMonth
            )
        }

        // Stats
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${state.cycleDay}",
                    label = "день цикла"
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${state.daysUntilNextPeriod}",
                    label = "до месячных"
                )
            }
        }

        // Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::markPeriodToday,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🩸 Отметить месячные сегодня")
                }
                OutlinedButton(
                    onClick = viewModel::showChart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📊 График циклов")
                }
            }
        }

        // History
        item {
            CycleHistoryList(
                periods = state.periodHistory,
                onDelete = viewModel::deletePeriod
            )
        }
    }
}
```

**Calendar — кастомный Composable:**
```kotlin
@Composable
fun CycleCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    periodDates: Set<LocalDate>,
    ovulationDates: Set<LocalDate>,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, "Previous")
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, "Next")
                }
            }

            // Day names
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Days grid
            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7

            Column {
                var dayCounter = 1 - firstDayOfWeek
                repeat(6) { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { dayOfWeek ->
                            val date = currentMonth.atDay(1).plusDays(dayCounter.toLong())
                            val isCurrentMonth = date.month == currentMonth.month

                            DayCell(
                                date = date,
                                isCurrentMonth = isCurrentMonth,
                                isToday = date == today,
                                isPeriod = periodDates.contains(date),
                                isOvulation = ovulationDates.contains(date),
                                isSelected = date == selectedDate,
                                onClick = { onDateClick(date) },
                                modifier = Modifier.weight(1f)
                            )
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isPeriod: Boolean,
    isOvulation: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isPeriod -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        isOvulation -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isCurrentMonth) {
            Text(
                text = "${date.dayOfMonth}",
                color = when {
                    isPeriod -> MaterialTheme.colorScheme.error
                    isOvulation -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            // Dots for symptoms/intimacy
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ... dots
            }
        }
    }
}
```

### 4. Chat Screen

```kotlin
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                ChatBubble(
                    message = message,
                    isMe = message.sender == viewModel.myRole,
                    onRetry = { viewModel.retryMessage(message.id) }
                )
            }
        }

        // Input
        ChatInputField(
            value = input,
            onValueChange = viewModel::onInputChange,
            onSend = viewModel::sendMessage,
            modifier = Modifier.imePadding()
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isMe: Boolean,
    onRetry: () -> Unit
) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) {
        Brush.linearGradient(listOf(Pink, Purple))
    } else {
        SolidColor(MaterialTheme.colorScheme.surfaceVariant)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) Color.Transparent 
                                else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .background(bubbleColor)
                    .padding(12.dp)
            ) {
                Column {
                    if (!isMe) {
                        Text(
                            text = "${message.senderAvatar} ${message.senderName}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = message.text,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) Color.White.copy(alpha = 0.7f) 
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isMe) {
                            when (message.status) {
                                MessageStatus.PENDING -> CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp
                                )
                                MessageStatus.FAILED -> {
                                    IconButton(onClick = onRetry, modifier = Modifier.size(16.dp)) {
                                        Icon(Icons.Default.Refresh, "Retry")
                                    }
                                }
                                MessageStatus.DELIVERED -> Text("✓", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### 5. Bottom Navigation

```kotlin
@Composable
fun LoveOSBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem("home", "Главная", Icons.Default.Home),
        NavItem("cycle", "Цикл", Icons.Default.DateRange),
        NavItem("chat", "Чат", Icons.Default.Chat),
        NavItem("stuff", "Желания", Icons.Default.Favorite),
        NavItem("notes", "Заметки", Icons.Default.Edit),
        NavItem("more", "Ещё", Icons.Default.MoreVert)
    )

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label) },
                selected = selected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Pink,
                    selectedTextColor = Pink,
                    indicatorColor = Pink.copy(alpha = 0.1f)
                )
            )
        }
    }
}
```

---

## Виджеты (App Widgets)

### 1. TimerWidget

```kotlin
class TimerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = remember { getTimerState(context) }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Pink)
                    .cornerRadius(16.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "💕 Вместе",
                        style = TextStyle(color = ColorProvider(Color.White))
                    )
                    Text(
                        text = "${state.days} дней",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = state.subtitle,
                        style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.8f)))
                    )
                }
            }
        }
    }
}
```

### 2. CycleWidget

Показывает текущую фазу и дни до следующих месячных.

### 3. MeetingWidget

Обратный отсчёт до следующей встречи.

**Обновление виджетов:**
```kotlin
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(TimerWidget::class.java)
        glanceIds.forEach { TimerWidget().update(applicationContext, it) }
        return Result.success()
    }
}

// Запуск каждую минуту
val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(1, TimeUnit.MINUTES)
    .build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "widget_update",
    ExistingPeriodicWorkPolicy.KEEP,
    workRequest
)
```

---

## Biometric Lock

```kotlin
class BiometricManager @Inject constructor(
    private val activity: FragmentActivity
) {
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("LoveOS")
            .setSubtitle("Подтвердите личность")
            .setNegativeButtonText("Отмена")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
```

---

## Push-уведомления (FCM)

```kotlin
class LoveOSMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        when (remoteMessage.data["type"]) {
            "new_message" -> showMessageNotification(remoteMessage)
            "heart" -> showHeartNotification()
            "period_delay" -> showPeriodNotification(remoteMessage)
        }
    }

    private fun showMessageNotification(message: RemoteMessage) {
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💬 Новое сообщение")
            .setContentText(message.data["text"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIF_MESSAGE_ID, notification)
    }
}
```

---

## Тема (Material Design 3)

```kotlin
private val LoveOSColors = darkColorScheme(
    primary = Color(0xFFE91E63),           // Pink
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A1A2E),   // Dark surface
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9C27B0),         // Purple
    onSecondary = Color.White,
    background = Color(0xFF0F0F1A),         // Deep dark
    onBackground = Color.White,
    surface = Color(0xFF1A1A2E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A3E),
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = Color(0xFFF44336),
    onError = Color.White,
    outline = Color(0x33FFFFFF)
)

private val LoveOSTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp
    ),
    // ... остальные стили
)

@Composable
fun LoveOSTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LoveOSColors,
        typography = LoveOSTypography,
        content = content
    )
}
```

---

## Data Layer (Room + DataStore)

```kotlin
// Entities
@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 1,
    val json: String // serialized AppState
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val status: String,
    val synced: Boolean = false
)

// DAO
@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE synced = 0")
    suspend fun getUnsynced(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

// DataStore для настроек
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loveos_settings")

class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val theme: Flow<String> = dataStore.data
        .map { it[THEME_KEY] ?: "dark" }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[THEME_KEY] = theme }
    }

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        val SETUP_COMPLETE_KEY = booleanPreferencesKey("setup_complete")
    }
}
```

---

## Sync Manager (offline-first)

```kotlin
@Singleton
class SyncManager @Inject constructor(
    private val localDb: AppDatabase,
    private val firebaseRepo: FirebaseRepository,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) syncPending()
            }
        }
    }

    private suspend fun syncPending() {
        // Sync chat
        val unsyncedMessages = localDb.chatMessageDao().getUnsynced()
        unsyncedMessages.forEach { msg ->
            firebaseRepo.pushChatMessage(msg.toDomain())
                .onSuccess { localDb.chatMessageDao().markSynced(msg.id) }
        }

        // Sync other data
        val pendingActions = localDb.pendingActionDao().getAll()
        pendingActions.forEach { action ->
            firebaseRepo.executeAction(action)
                .onSuccess { localDb.pendingActionDao().delete(action.id) }
        }
    }

    fun observeRemoteChanges() {
        scope.launch {
            firebaseRepo.observeChat().collect { remoteMessages ->
                val localMessages = localDb.chatMessageDao().getAll()
                val merged = mergeChat(localMessages, remoteMessages)
                localDb.chatMessageDao().insertAll(merged.map { it.toEntity() })
            }
        }
    }
}
```

---

## Экспорт/Импорт (JSON)

```kotlin
class BackupManager @Inject constructor(
    private val context: Context,
    private val localDb: AppDatabase
) {
    suspend fun export(): Uri {
        val state = localDb.appStateDao().get()
        val json = Json.encodeToString(state.toDomain())

        val file = File(context.cacheDir, "loveos_backup_${LocalDate.now()}.json")
        file.writeText(json)

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    suspend fun import(uri: Uri): Result<Unit> = runCatching {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("Cannot read file")

        val state = Json.decodeFromString<AppState>(json)
        localDb.appStateDao().insert(state.toEntity())
    }
}
```

---

## Полный чеклист реализации

### Phase 1: Foundation (День 1-2)
- [ ] Создать проект в Android Studio (Empty Compose Activity)
- [ ] Настроить Gradle: Hilt, Compose, Room, DataStore, Firebase
- [ ] Создать `LoveOSApplication` + `AppModule`
- [ ] Создать domain models (все data classes из JSON-структуры)
- [ ] Создать Room database + DAOs
- [ ] Создать DataStore для настроек
- [ ] Подключить Firebase (google-services.json)
- [ ] Создать `FirebaseRepository` с Flow
- [ ] Создать `SyncManager` (offline-first)
- [ ] Создать `BiometricManager`

### Phase 2: Core UI (День 3-4)
- [ ] Создать `LoveOSTheme` (цвета, типографика)
- [ ] Создать `MainActivity` с biometric check
- [ ] Создать `LoveOSNavGraph` (7 экранов)
- [ ] Создать `LoveOSBottomNav`
- [ ] Создать общие компоненты: `GradientButton`, `LoveCard`, `LoveTextField`
- [ ] Создать `FloatingHeartsOverlay` (анимация)

### Phase 3: Setup (День 5)
- [ ] `SetupScreen` с HorizontalPager (3 страницы)
- [ ] Валидация полей
- [ ] Сохранение в DataStore + Room
- [ ] Генерация deviceId

### Phase 4: Home (День 6-7)
- [ ] `TimerCard` с анимированными числами
- [ ] `MeetingCard` / `MeetingInputCard`
- [ ] `RouletteCard` с анимацией вращения
- [ ] `sendHeart()` + floating hearts animation
- [ ] ViewModel с таймером (1 сек)

### Phase 5: Cycle (День 8-10)
- [ ] `CycleCalendar` (кастомный Composable)
- [ ] `PhaseCard` с прогресс-баром
- [ ] `PredictionCard`
- [ ] `DayDetailBottomSheet` (фаза, симптомы, интимность)
- [ ] `CycleChart` (Vico)
- [ ] `CycleHistoryList`
- [ ] UseCases: `PredictNextPeriod`, `GetDayPhase`, `CalculateAverageCycle`

### Phase 6: Chat (День 11-12)
- [ ] `ChatBubble` (me/partner разные стили)
- [ ] `ChatInputField` с auto-resize
- [ ] `LazyColumn` с reverse layout
- [ ] Auto-scroll to bottom
- [ ] Retry failed messages
- [ ] Offline indicator
- [ ] Push notifications (FCM)

### Phase 7: Stuff (День 13-14)
- [ ] `WishesScreen` + `WishItem` + progress
- [ ] `TasksScreen` + `TaskItem`
- [ ] `SavingsScreen` + `SavingItem` + progress bar
- [ ] CRUD операции
- [ ] FAB для добавления

### Phase 8: Notes (День 15)
- [ ] `NotesList` (grid)
- [ ] `CapsuleCard` (locked/unlocked)
- [ ] Time capsule logic (открытие по дате)

### Phase 9: Games (День 16-17)
- [ ] `TicTacToeScreen` (3x3 grid)
- [ ] `HangmanScreen` (клавиатура, виселица)
- [ ] Firebase sync для TicTacToe
- [ ] Анимации победы

### Phase 10: Profiles (День 18)
- [ ] `ProfileCard`
- [ ] `LoveLanguageQuiz` (10 вопросов)
- [ ] `KnowYourPartnerQuiz` (6 вопросов)
- [ ] `MoodSelector` (5 эмодзи)

### Phase 11: Settings (День 19)
- [ ] `SettingsScreen`
- [ ] Theme toggle
- [ ] Notifications toggle
- [ ] Sync toggle + code display
- [ ] Export/Import JSON
- [ ] Clear data + Reset all

### Phase 12: Widgets (День 20-21)
- [ ] `TimerWidget` (Glance)
- [ ] `CycleWidget`
- [ ] `MeetingWidget`
- [ ] Widget update worker (каждую минуту)
- [ ] Widget configuration

### Phase 13: Polish (День 22-23)
- [ ] Haptic feedback везде
- [ ] Анимации переходов (shared element)
- [ ] Splash screen
- [ ] App icon (adaptive)
- [ ] Edge-to-edge
- [ ] Dynamic colors (опционально)

### Phase 14: Testing (День 24-25)
- [ ] Unit tests для UseCases
- [ ] Integration tests для Repository
- [ ] UI tests для critical paths
- [ ] Performance profiling

---

## Критические требования (не нарушать!)

1. **Firebase структура БД — идентичная web-версии** — иначе sync сломается
2. **Device ID — генерируется один раз, живёт вечно**
3. **Role (me/partner) — определяется при setup**
4. **Offline-first — всё работает без сети**
5. **Chat ordering — по timestamp, merge по id**
6. **Periods — только первые дни, без дубликатов**
7. **Version migration — при обновлении app**
8. **Biometric — обязателен при входе**
9. **Backup — автоматический в Firebase при изменениях**
10. **Widgets — обновление каждую минуту**

---

## Пример структуры файлов (готовый шаблон)

```
app/src/main/kotlin/com/loveos/
├── MainActivity.kt
├── LoveOSApplication.kt
├── di/
│   ├── AppModule.kt
│   └── DatabaseModule.kt
├── navigation/
│   ├── LoveOSNavGraph.kt
│   └── Screen.kt
├── theme/
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
├── core/
│   ├── biometric/
│   │   └── BiometricManager.kt
│   ├── notification/
│   │   └── NotificationManager.kt
│   └── widget/
│       ├── TimerWidget.kt
│       ├── CycleWidget.kt
│       └── MeetingWidget.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── ChatMessageDao.kt
│   │   │   ├── AppStateDao.kt
│   │   │   └── PendingActionDao.kt
│   │   └── entity/
│   │       ├── ChatMessageEntity.kt
│   │       ├── AppStateEntity.kt
│   │       └── PendingActionEntity.kt
│   ├── remote/
│   │   └── FirebaseRepository.kt
│   └── repository/
│       └── LoveOSRepository.kt
├── domain/
│   ├── model/
│   │   ├── AppState.kt
│   │   ├── Profile.kt
│   │   ├── ChatMessage.kt
│   │   ├── CycleData.kt
│   │   └── ... (все модели)
│   └── usecase/
│       ├── CalculateTimerUseCase.kt
│       ├── PredictNextPeriodUseCase.kt
│       ├── GetDayPhaseUseCase.kt
│       └── MergeChatUseCase.kt
├── feature/
│   ├── setup/
│   │   ├── SetupScreen.kt
│   │   ├── SetupViewModel.kt
│   │   └── components/
│   │       ├── NamesScreen.kt
│   │       ├── DateAndRoleScreen.kt
│   │       └── CompletionScreen.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   └── components/
│   │       ├── TimerCard.kt
│   │       ├── MeetingCard.kt
│   │       └── RouletteCard.kt
│   ├── cycle/
│   │   ├── CycleScreen.kt
│   │   ├── CycleViewModel.kt
│   │   └── components/
│   │       ├── CycleCalendar.kt
│   │       ├── PhaseCard.kt
│   │       └── DayDetailBottomSheet.kt
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   ├── ChatViewModel.kt
│   │   └── components/
│   │       ├── ChatBubble.kt
│   │       └── ChatInputField.kt
│   ├── stuff/
│   │   ├── StuffScreen.kt
│   │   ├── StuffViewModel.kt
│   │   └── components/
│   │       ├── WishItem.kt
│   │       ├── TaskItem.kt
│   │       └── SavingItem.kt
│   ├── notes/
│   │   ├── NotesScreen.kt
│   │   └── components/
│   │       ├── NoteCard.kt
│   │       └── CapsuleCard.kt
│   ├── games/
│   │   ├── GamesScreen.kt
│   │   └── components/
│   │       ├── TicTacToeBoard.kt
│   │       └── HangmanGame.kt
│   ├── profiles/
│   │   ├── ProfilesScreen.kt
│   │   └── components/
│   │       ├── ProfileCard.kt
│   │       ├── QuizScreen.kt
│   │       └── MoodSelector.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       └── SettingsViewModel.kt
└── widget/
    ├── TimerWidget.kt
    ├── CycleWidget.kt
    ├── MeetingWidget.kt
    └── WidgetUpdateWorker.kt
```

---

## Готовый код для старта

### build.gradle.kts (app level)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.loveos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loveos"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // Glance (widgets)
    implementation("androidx.glance:glance-appwidget:1.0.0")

    // Charts
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.14")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## Финальный результат

Полностью работающее Android-приложение с:
- ✅ Material Design 3 с кастомной темой (розовый/фиолетовый)
- ✅ Offline-first (Room + DataStore)
- ✅ Firebase sync (Realtime Database)
- ✅ Push-уведомления (FCM)
- ✅ Biometric lock
- ✅ 3 виджета на главный экран
- ✅ Анимации на уровне нативного приложения
- ✅ Haptic feedback
- ✅ Автоматический backup
- ✅ Экспорт/импорт JSON
- ✅ Тёмная/светлая тема
- ✅ Edge-to-edge

Готовое к сборке APK для личного использования.
