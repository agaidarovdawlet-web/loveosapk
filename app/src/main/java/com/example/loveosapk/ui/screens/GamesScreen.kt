package com.example.loveosapk.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.theme.Accent

@Composable
fun GamesScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Развлечения", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        
        TicTacToeGame()
        
        Spacer(Modifier.height(24.dp))
        
        HangmanGame()
    }
}

@Composable
fun HangmanGame() {
    val words = mapOf(
        "ЛЮБОВЬ" to "Это то, что нас объединяет",
        "СЕРДЦЕ" to "Оно бьется ради тебя",
        "СЧАСТЬЕ" to "Когда мы вместе",
        "ЗАБОТА" to "Проявляется в мелочах"
    )
    var wordData by remember { mutableStateOf(words.entries.random()) }
    var guessedLetters by remember { mutableStateOf(setOf<Char>()) }
    var mistakes by remember { mutableIntStateOf(0) }
    val maxMistakes = 6
    
    val currentWord = wordData.key
    val isWon = currentWord.all { it in guessedLetters }
    val isLost = mistakes >= maxMistakes

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔤 Виселица", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = when {
                    isWon -> "Вы угадали! 🎉"
                    isLost -> "Попытки кончились. Слово: $currentWord"
                    else -> "Ошибок: $mistakes / $maxMistakes"
                },
                color = if (isWon) Color.Green else if (isLost) Color.Red else Accent
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Hangman Figure Placeholder
            Text(
                text = when(mistakes) {
                    0 -> "😊"
                    1 -> "😐"
                    2 -> "😟"
                    3 -> "😨"
                    4 -> "😰"
                    5 -> "😱"
                    else -> "👻"
                },
                fontSize = 48.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Word progress
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                currentWord.forEach { char ->
                    Text(
                        text = if (char in guessedLetters || isLost) "$char" else "_",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (char in guessedLetters) Accent else Color.Gray
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(text = "Подсказка: ${wordData.value}", fontSize = 12.sp, color = Color.Gray)
            
            Spacer(Modifier.height(16.dp))
            
            // Keyboard
            val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                modifier = Modifier.height(160.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(alphabet.toList()) { _, char ->
                    val isGuessed = char in guessedLetters
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .background(
                                color = if (isGuessed) Color.LightGray else Accent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable(enabled = !isGuessed && !isWon && !isLost) {
                                guessedLetters = guessedLetters + char
                                if (char !in currentWord) mistakes++
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(char.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = {
                    wordData = words.entries.random()
                    guessedLetters = emptySet()
                    mistakes = 0
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Новое слово")
            }
        }
    }
}

@Composable
fun TicTacToeGame() {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var xIsNext by remember { mutableStateOf(true) }
    val winner = calculateWinner(board)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("❌⭕ Крестики-нолики", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = when {
                    winner != null -> "Победитель: $winner! 🎉"
                    board.all { it.isNotEmpty() } -> "Ничья! 🤝"
                    else -> "Ход: ${if (xIsNext) "❌" else "⭕"}"
                },
                fontSize = 14.sp,
                color = Accent
            )
            
            Spacer(Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.size(240.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(board) { index, cell ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(1.dp, Color.LightGray)
                            .clickable(enabled = cell.isEmpty() && winner == null) {
                                val newBoard = board.toMutableList()
                                newBoard[index] = if (xIsNext) "X" else "O"
                                board = newBoard
                                xIsNext = !xIsNext
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cell == "X") "❌" else if (cell == "O") "⭕" else "",
                            fontSize = 32.sp
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { board = List(9) { "" }; xIsNext = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Сброс")
            }
        }
    }
}

fun calculateWinner(squares: List<String>): String? {
    val lines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6)
    )
    for (line in lines) {
        val (a, b, c) = line
        if (squares[a].isNotEmpty() && squares[a] == squares[b] && squares[a] == squares[c]) {
            return if (squares[a] == "X") "❌" else "⭕"
        }
    }
    return null
}
