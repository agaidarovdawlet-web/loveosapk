package com.example.loveosapk.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.ui.MainViewModel
import com.example.loveosapk.ui.theme.Accent
import java.time.LocalDate

@Composable
fun SetupScreen(viewModel: MainViewModel) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var myName by rememberSaveable { mutableStateOf("") }
    var partnerName by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var myRole by rememberSaveable { mutableStateOf("me") }
    var syncCode by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("💕", fontSize = 64.sp)
            Text("LoveOS", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Accent)
            
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(24.dp)) {
                    AnimatedContent(targetState = step, label = "setupSteps") { targetStep ->
                        when (targetStep) {
                            1 -> StepOne(myName, partnerName, onMyNameChange = { myName = it }, onPartnerNameChange = { partnerName = it })
                            2 -> StepTwo(startDate, myRole, syncCode, onDateChange = { startDate = it }, onRoleChange = { myRole = it }, onSyncCodeChange = { syncCode = it })
                            3 -> StepThree()
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step > 1) {
                            Button(
                                onClick = { step-- },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Text("Назад", color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (step < 3) {
                                    if (step == 1 && (myName.isBlank() || partnerName.isBlank())) {
                                        // Show error
                                    } else {
                                        step++
                                    }
                                } else {
                                    viewModel.finishSetup(myName, partnerName, startDate, myRole, syncCode)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Text(if (step < 3) "Дальше" else "Начать 💕", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepOne(myName: String, partnerName: String, onMyNameChange: (String) -> Unit, onPartnerNameChange: (String) -> Unit) {
    Column {
        Text("👋 Давай знакомиться", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = myName,
            onValueChange = onMyNameChange,
            label = { Text("Твоё имя") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = partnerName,
            onValueChange = onPartnerNameChange,
            label = { Text("Имя второй половинки") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StepTwo(startDate: String, myRole: String, syncCode: String, onDateChange: (String) -> Unit, onRoleChange: (String) -> Unit, onSyncCodeChange: (String) -> Unit) {
    Column {
        Text("💝 Наша история", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = startDate,
            onValueChange = onDateChange,
            label = { Text("Вместе с (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Text("Кто ты на этом устройстве?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = myRole == "me", onClick = { onRoleChange("me") })
            Text("Я")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = myRole == "partner", onClick = { onRoleChange("partner") })
            Text("Половинка")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = syncCode,
            onValueChange = onSyncCodeChange,
            label = { Text("Код синхронизации (если есть)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("LOVE-XXXXXX") }
        )
    }
}

@Composable
fun StepThree() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🎉", fontSize = 48.sp)
        Text("Готово!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Приложение настроено.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
