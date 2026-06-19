package com.example.loveosapk.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A utility to visualize recompositions of a specific block of UI.
 */
@Composable
fun RecompositionVisualizer(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var count by remember { mutableIntStateOf(0) }
    SideEffect {
        count++
        Log.v("RECOMPOSITION", "[$label] recomposed #$count")
    }

    Box(modifier = modifier) {
        content()
        
        // Count indicator in corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Red.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Performance Checklist UI to show optimization status
 */
@Composable
fun PerformanceChecklist() {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ОПТИМИЗАЦИЯ", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            CheckItem("Lazy List Keys", true)
            CheckItem("Content Types", true)
            CheckItem("Stability (@Immutable)", true)
            CheckItem("Derived State Usage", true)
            CheckItem("Image Caching (Coil)", true)
        }
    }
}

@Composable
private fun CheckItem(label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (active) "✅" else "❌", fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, color = Color.White)
    }
}
