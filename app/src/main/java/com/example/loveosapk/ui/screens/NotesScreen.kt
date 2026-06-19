package com.example.loveosapk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.data.Note
import com.example.loveosapk.data.TimeCapsule
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.theme.Accent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(viewModel: MainViewModel) {
    val notes by viewModel.notes.collectAsState()
    val capsules by viewModel.capsules.collectAsState()
    
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Заметки", "Капсулы")
    
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddCapsuleDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (selectedTab == 0) {
            NotesTab(notes, onAddClick = { showAddNoteDialog = true }, onDeleteClick = { viewModel.deleteNote(it) })
        } else {
            CapsulesTab(capsules, onAddClick = { showAddCapsuleDialog = true }, onDeleteClick = { viewModel.deleteCapsule(it) })
        }
    }

    if (showAddNoteDialog) {
        AddNoteDialog(onDismiss = { showAddNoteDialog = false }, onAdd = { viewModel.addNote(it) })
    }
    if (showAddCapsuleDialog) {
        AddCapsuleDialog(onDismiss = { showAddCapsuleDialog = false }, onAdd = { text, delay -> viewModel.addCapsule(text, delay) })
    }
}

@Composable
fun NotesTab(notes: List<Note>, onAddClick: () -> Unit, onDeleteClick: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Заметок пока нет", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    NoteItem(note, onDelete = { onDeleteClick(note.id) })
                }
            }
        }
        
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Accent
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
        }
    }
}

@Composable
fun NoteItem(note: Note, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (note.author.isNotEmpty()) "От: ${note.author}" else "Анонимно",
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                }
                Text(note.date, fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(note.text, color = Color.White)
        }
    }
}

@Composable
fun CapsulesTab(capsules: List<TimeCapsule>, onAddClick: () -> Unit, onDeleteClick: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (capsules.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Капсул времени нет", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(capsules) { capsule ->
                    CapsuleItem(capsule, onDelete = { onDeleteClick(capsule.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Accent
        ) {
            Icon(Icons.Default.Mail, contentDescription = "Add", tint = Color.White)
        }
    }
}

@Composable
fun CapsuleItem(capsule: TimeCapsule, onDelete: () -> Unit) {
    val isLocked = System.currentTimeMillis() < capsule.openAfter
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Mail,
                    contentDescription = null,
                    tint = if (isLocked) Color.Gray else Accent
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isLocked) "Закрыто до ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(capsule.openAfter))}" else "Открыто!",
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) Color.Gray else Accent,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                }
            }
            if (!isLocked) {
                Spacer(Modifier.height(8.dp))
                Text(capsule.text)
            }
        }
    }
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая заметка") },
        text = {
            TextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(onClick = { onAdd(text); onDismiss() }) { Text("Ок") }
        }
    )
}

@Composable
fun AddCapsuleDialog(onDismiss: () -> Unit, onAdd: (String, Long) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("7") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Капсула времени") },
        text = {
            Column {
                TextField(value = text, onValueChange = { text = it }, placeholder = { Text("Письмо в будущее...") })
                Spacer(Modifier.height(8.dp))
                TextField(value = days, onValueChange = { days = it }, label = { Text("Открыть через (дней)") })
            }
        },
        confirmButton = {
            Button(onClick = { 
                val delay = (days.toLongOrNull() ?: 7) * 86400000L
                onAdd(text, System.currentTimeMillis() + delay)
                onDismiss()
            }) { Text("Ок") }
        }
    )
}
