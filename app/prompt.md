

---

```markdown
# 🎯 АВТОНОМНАЯ СЕССИЯ: LoveOS Refactor Session
**Время:** 2–3 часа  
**Режим:** Полная автономность. Не прерывай работу для уточнений. Если не уверен — выбери самый консервативный вариант.  
**Цель:** Улучшить стабильность и архитектуру, не сломав существующий функционал.

---

## 📋 КОНТЕКСТ ПРОЕКТА

**LoveOS** — нативное Android-приложение для пары (Jetpack Compose, Clean Architecture, pet project).  
**Стек:** Kotlin, Jetpack Compose (Material 3), Room, Firebase Realtime Database, DataStore, Coil 3, Media3, ручная DI (Factory/Singleton), Navigation Compose.

**Структура пакетов:**
```
com.example.loveosapk/
├── data/
│   ├── local/          # Room (Entity, DAO, Database)
│   ├── remote/         # Firebase Realtime Database
│   ├── repository/     # Реализации репозиториев
│   ├── Models.kt        # DTO
│   └── PreferenceManager.kt
├── domain/
│   ├── model/          # Доменные модели
│   ├── repository/     # Интерфейсы репозиториев
│   └── usecase/        # UseCases
└── ui/
├── components/     # UI-компоненты, анимации
├── features/       # Фичи (Cycle и др.)
├── navigation/     # Навигация
├── screens/        # Экраны
├── theme/          # Темы
└── MainViewModel.kt
```

**Важно:** Это pet project для двоих. Не нужен оверинжиниринг. Не добавляй Hilt/Koin, не переписывай архитектуру полностью. Работай в рамках существующей структуры.

---

## ⚠️ ПРАВИЛА РАБОТЫ (обязательны)

1. **Перед изменениями:** Выполни `git status`, убедись что рабочая директория чистая. Создай ветку: `git checkout -b refactor/autonomous-session`.
2. **После каждой задачи:** `git add . && git commit -m "task N: краткое описание"`.
3. **Не ломай билд:** После каждой задачи проект должен компилироваться (`./gradlew :app:compileDebugKotlin` или аналог через Gradle wrapper). Если не компилируется — откати изменения этой задачи и перейди к следующей.


---

## 🔥 ЗАДАЧА 1: Firebase Flow Wrapper (устранение утечек памяти)
**Приоритет:** P0 | **Ожидаемое время:** 40–60 мин

**Проблема:** Сейчас Firebase Realtime Database слушатели (`ValueEventListener`) скорее всего не отписываются корректно. Это вызывает утечки памяти и лишний трафик.

**Что сделать:**

### 1.1 Создай абстракцию в domain-слое
Создай файл `domain/repository/RemoteDataSource.kt`:
```kotlin
interface RemoteDataSource {
    fun <T> observeValue(path: String, clazz: Class<T>): Flow<Result<T>>
    suspend fun <T> getValueOnce(path: String, clazz: Class<T>): Result<T>
    suspend fun <T> setValue(path: String, value: T): Result<Unit>
    suspend fun <T> updateChildren(path: String, values: Map<String, T>): Result<Unit>
}
```
(Адаптируй под реальные нужды проекта, если в проекте уже есть похожий интерфейс — используй его, но дополни методами.)

### 1.2 Создай реализацию в data-слое
Создай файл `data/remote/FirebaseRemoteDataSource.kt`:
- Используй `callbackFlow { ... }` из kotlinx.coroutines.
- Внутри `awaitClose { /* отписка от listener */ }` обязательно вызывай `ref.removeEventListener(listener)`.
- Обрабатывай `onCancelled` — отправляй `Result.failure(exception)`.
- Для десериализации используй `snapshot.getValue(clazz)` (Firebase SDK).

### 1.3 Рефакторинг существующих репозиториев
Найди репозитории в `data/repository/`, которые напрямую работают с Firebase DatabaseReference.  
Замени прямую работу с Firebase на использование `RemoteDataSource` (через конструктор/фабрику, сохрани ручной DI).  
Убедись, что во ViewModel/UseCase вызывается `.collect {}` или `stateIn()` с корректным `viewModelScope`.

**Критерий готовности:**
- [ ] Создан `RemoteDataSource` в domain
- [ ] Создан `FirebaseRemoteDataSource` с `callbackFlow` и отпиской в `awaitClose`
- [ ] Хотя бы ОДИН существующий репозиторий мигрирован на новую абстракцию
- [ ] Проект компилируется

---

## 🔥 ЗАДАЧА 2: Выделение CycleViewModel из MainViewModel
**Приоритет:** P1 | **Ожидаемое время:** 50–70 мин

**Проблема:** `MainViewModel.kt` скорее всего содержит логику всех фич (Home, Cycle, Wishlist, Tasks, Games). Это God Object.

**Что сделать:**

### 2.1 Проанализируй MainViewModel
Открой `ui/MainViewModel.kt`. Найди ВСЕ поля/методы, относящиеся к фиче "Женский календарь" (Cycle):
- Состояния календаря (фазы, даты, симптомы)
- Методы логирования симптомов
- Любые `StateFlow`/`LiveData` с названиями cycle/period/ovulation/symptoms

### 2.2 Создай CycleViewModel
Создай `ui/features/cycle/CycleViewModel.kt`:
- Перенеси ВСЮ cycle-логику из MainViewModel.
- Используй ту же ручную DI (передай зависимости через конструктор/фабрику, как сделано сейчас для MainViewModel).
- Определи чёткий UiState:
```kotlin
data class CycleUiState(
    val currentPhase: CyclePhase = CyclePhase.UNKNOWN,
    val selectedDate: LocalDate = LocalDate.now(),
    val symptoms: List<Symptom> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### 2.3 Обнови Cycle-экран
Найди экран Cycle (вероятно, `ui/screens/CycleScreen.kt` или `ui/features/cycle/`).  
Замени обращение к `mainViewModel.cycleState` на `cycleViewModel.uiState`.  
Сохрани ручное создание VM (через `viewModel { CycleViewModel(...) }` или фабрику проекта).

### 2.4 Очисти MainViewModel
Удали из `MainViewModel` всё, что относится к Cycle. Оставь только Home-логику (счётчик отношений, таймер, рулетка).

**Критерий готовности:**
- [ ] Создан `CycleViewModel` с полным состоянием фичи
- [ ] `MainViewModel` больше не содержит cycle-логику
- [ ] Cycle-экран использует новый ViewModel
- [ ] Проект компилируется
- [ ] (Опционально) Если есть другие фичи в MainViewModel — оставь их, не трогай в этой задаче

---

## 🔥 ЗАДАЧА 3: ProGuard / R8 Rules для Release
**Приоритет:** P1 | **Ожидаемое время:** 30–40 мин

**Проблема:** При сборке release (`minifyEnabled true`) Firebase, Room и Compose могут сломаться из-за обфускации.

**Что сделать:**

### 3.1 Найди/создай ProGuard-файл
Найди `app/proguard-rules.pro` (или создай, если отсутствует).

### 3.2 Добавь правила
Добавь следующие правила (адаптируй package name под `com.example.loveosapk`):

```proguard
# Room
-keep class com.example.loveosapk.data.local.entities.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-keepattributes Signature
-keepattributes *Annotation*

# DataStore / Preferences
-keep class androidx.datastore.** { *; }
-keep class com.example.loveosapk.data.PreferenceManager { *; }

# Kotlin Coroutines / Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Jetpack Compose (если minifyEnabled)
-keep class androidx.compose.** { *; }
-keep class com.example.loveosapk.ui.theme.** { *; }
-keep class com.example.loveosapk.ui.components.** { *; }

# Serializable / Parcelable (если используется)
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepnames class * implements java.io.Serializable

# Navigation
-keep class com.example.loveosapk.ui.navigation.** { *; }
```

### 3.3 Проверь build.gradle (Module: app)
Убедись, что в `buildTypes { release { ... } }` указано:
```gradle
proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
```
Если `minifyEnabled false` — оставь false (не включай без тестирования), но файл rules должен быть готов.

**Критерий готовности:**
- [ ] Файл `proguard-rules.pro` создан/обновлён с правилами для Room, Firebase, Compose
- [ ] `build.gradle` корректно ссылается на proguard-файл
- [ ] Debug-сборка компилируется (release проверять не обязательно, если minifyEnabled=false)

---

## 📊 ФОРМАТ ФИНАЛЬНОГО ОТЧЁТА

По завершении (или если время вышло) создай файл `REFACTOR_REPORT.md` в корне проекта с содержимым:

```markdown
# Отчёт автономной сессии LoveOS

## Время начала: [время]
## Время окончания: [время]

### ✅ Выполнено
1. [Задача N]: [краткое описание, какие файлы созданы/изменены]
2. ...

### ⚠️ Частично выполнено / Требует доработки
1. [Задача N]: [что не успели, почему]

### ❌ Не выполнено
1. [Задача N]: [причина]

### 📝 Следующие шаги (рекомендации)
- Что стоит сделать в следующей сессии
- Какие задачи остались

### 🔍 Примечания
- Любые проблемы, с которыми столкнулся агент
- Предупреждения о потенциальных регрессиях
```

---



## ✅ УСПЕХ СЕССИИ

Сессия считается успешной, если:
1. Хотя бы Задача 1 (Firebase Wrapper) выполнена полностью.
2. Проект компилируется.
3. Все изменения закоммичены в ветку `refactor/autonomous-session`.
4. Создан `REFACTOR_REPORT.md`.

**Начинай работу. Удачи!**
```

-