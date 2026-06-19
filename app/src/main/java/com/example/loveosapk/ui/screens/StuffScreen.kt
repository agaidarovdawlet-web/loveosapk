package com.example.loveosapk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import com.example.loveosapk.data.Saving
import com.example.loveosapk.data.Task
import com.example.loveosapk.data.Wish
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.components.*
import com.example.loveosapk.ui.theme.Accent
import com.example.loveosapk.ui.theme.AccentSecondary

@Composable
fun StuffScreen(viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Желания", "Задачи", "Копилка")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Accent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Accent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (selectedTab) {
            0 -> WishListTab(viewModel)
            1 -> TaskListTab(viewModel)
            2 -> SavingListTab(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListTab(viewModel: MainViewModel) {
    val wishes by viewModel.wishes.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (wishes.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Star,
                title = "Пока пусто",
                subtitle = "Добавьте первое желание — например, поход в кино"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = wishes,
                    key = { it.id },
                    contentType = { "wish" }
                ) { wish ->
                    WishItem(wish, onToggle = { viewModel.toggleWish(wish) }, onDelete = { viewModel.deleteWish(wish.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Accent,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Wish")
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            AddWishSheetContent(
                onDismiss = { showSheet = false },
                onAdd = { text, cat, priority, url -> viewModel.addWish(text, cat, priority, url) }
            )
        }
    }
}

@Composable
fun AddWishSheetContent(onDismiss: () -> Unit, onAdd: (String, String, String, String?) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("MEDIUM") }
    var imageUrl by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Добавить желание", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Напр: Пойти в кино") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        
        Spacer(Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("LOW" to "🍃", "MEDIUM" to "⭐", "HIGH" to "🔥").forEach { (p, emoji) ->
                val selected = priority == p
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .background(if (selected) Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { priority = p }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            placeholder = { Text("URL картинки (опционально)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { 
                if (text.isNotBlank()) {
                    onAdd(text, "default", priority, imageUrl.takeIf { it.isNotBlank() })
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Добавить", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun WishItem(wish: Wish, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().bounceClickable { onToggle() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            if (wish.imageUrl != null) {
                AsyncImage(
                    model = wish.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedCheckbox(checked = wish.done, onCheckedChange = { onToggle() }, color = Accent)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedTaskText(
                            text = wish.text,
                            done = wish.done,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when(wish.priority) { "HIGH" -> "🔥"; "LOW" -> "🍃"; else -> "⭐" },
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "От: ${wish.author}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun TaskListTab(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) {
            EmptyState(
                icon = Icons.Default.CheckCircle,
                title = "Нет задач",
                subtitle = "Добавьте совместную задачу"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = tasks,
                    key = { it.id },
                    contentType = { "task" }
                ) { task ->
                    TaskItem(task, onToggle = { viewModel.toggleTask(task) }, onDelete = { viewModel.deleteTask(task.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Accent,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    if (showDialog) {
        AddTaskDialog(onDismiss = { showDialog = false }, onAdd = { text, owner, deadline -> viewModel.addTask(text, owner, deadline) })
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().bounceClickable { onToggle() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedCheckbox(checked = task.done, onCheckedChange = { onToggle() }, color = Accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AnimatedTaskText(
                    text = task.text,
                    done = task.done
                )
                Text(
                    text = when(task.owner) { "me" -> "Моё"; "partner" -> "Партнёра"; else -> "Общее" },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                task.deadline?.takeIf { it.isNotBlank() }?.let { deadline ->
                    Text(
                        text = "До: $deadline",
                        fontSize = 12.sp,
                        color = Accent.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun SavingListTab(viewModel: MainViewModel) {
    val savings by viewModel.savings.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (savings.isEmpty()) {
            EmptyState(
                icon = Icons.Default.AccountBalance,
                title = "Начните копить",
                subtitle = "Добавьте цель — отпуск, подарок, мечта"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = savings,
                    key = { it.id },
                    contentType = { "saving" }
                ) { saving ->
                    SavingItem(saving, onUpdate = { viewModel.updateSaving(saving, it) }, onDelete = { viewModel.deleteSaving(saving.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Accent,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Saving")
        }
    }

    if (showDialog) {
        AddSavingDialog(onDismiss = { showDialog = false }, onAdd = { name, goal, cur -> viewModel.addSaving(name, goal, cur) })
    }
}

@Composable
fun SavingItem(saving: Saving, onUpdate: (Double) -> Unit, onDelete: () -> Unit) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().bounceClickable { showUpdateDialog = true },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(saving.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            val progress = if (saving.goal > 0.0) {
                (saving.current / saving.goal).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Accent,
                trackColor = Accent.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${saving.current} / ${saving.goal} ${saving.currency}", fontSize = 14.sp)
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Accent)
            }
        }
    }

    if (showUpdateDialog) {
        var amount by rememberSaveable(saving.id) { mutableStateOf("") }
        var replaceMode by rememberSaveable(saving.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(if (replaceMode) "Установить сумму" else "Пополнить копилку") },
            text = {
                Column {
                    TextField(
                        value = amount,
                        onValueChange = { amount = it.replace(',', '.') },
                        placeholder = { Text(if (replaceMode) "Текущая сумма" else "Сколько добавить") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = replaceMode, onCheckedChange = { replaceMode = it })
                        Text("Заменить текущую сумму")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = amount.toDoubleOrNull() ?: 0.0
                    val newAmount = if (replaceMode) value else saving.current + value
                    onUpdate(newAmount)
                    showUpdateDialog = false
                }) { Text("Ок") }
            }
        )
    }
}

@Composable
fun AddWishDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить желание") },
        text = {
            TextField(value = text, onValueChange = { text = it }, placeholder = { Text("Напр: Пойти в кино") })
        },
        confirmButton = {
            Button(onClick = { onAdd(text, "default"); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Добавить")
            }
        }
    )
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String?) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf("both") }
    var deadline by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить задачу") },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = false
                    },
                    placeholder = { Text("Напр: Купить продукты") },
                    isError = error
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    placeholder = { Text("Дедлайн, например 2026-06-30") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = owner == "me", onClick = { owner = "me" })
                    Text("Я")
                    RadioButton(selected = owner == "partner", onClick = { owner = "partner" })
                    Text("Партнёр")
                    RadioButton(selected = owner == "both", onClick = { owner = "both" })
                    Text("Оба")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (text.isBlank()) {
                    error = true
                } else {
                    onAdd(text.trim(), owner, deadline.trim().takeIf { it.isNotBlank() })
                    onDismiss()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Добавить")
            }
        }
    )
}

@Composable
fun AddSavingDialog(onDismiss: () -> Unit, onAdd: (String, Double, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("₽") }
    var error by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая цель") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, placeholder = { Text("Напр: Отпуск") })
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = goal,
                    onValueChange = { 
                        goal = it.replace(',', '.')
                        error = false
                    },
                    placeholder = { Text("Сумма") },
                    isError = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("₽", "$", "€").forEach { option ->
                        FilterChip(
                            selected = currency == option,
                            onClick = { currency = option },
                            label = { Text(option) }
                        )
                    }
                }
                if (error) {
                    Text("Введите название и сумму больше нуля", color = Color.Red, fontSize = 10.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val g = goal.toDoubleOrNull()
                if (g != null && g > 0.0 && name.isNotBlank()) {
                    onAdd(name.trim(), g, currency)
                    onDismiss()
                } else {
                    error = true
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Добавить")
            }
        }
    )
}
