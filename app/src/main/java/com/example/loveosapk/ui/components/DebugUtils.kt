package com.example.loveosapk.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

@Composable
fun TraceableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    content: @Composable () -> Unit
) {
    var compositionCount by remember { mutableStateOf(0) }
    compositionCount++

    SideEffect {
        Log.d("DIAGNOSTIC", "[$label] Recomposition #$compositionCount")
    }

    Button(
        onClick = {
            Log.d("DIAGNOSTIC", "[$label] Button CLICKED")
            try {
                onClick()
                Log.d("DIAGNOSTIC", "[$label] onClick lambda EXECUTED")
            } catch (e: Exception) {
                Log.e("DIAGNOSTIC", "[$label] onClick lambda FAILED", e)
            }
        },
        modifier = modifier.semantics { testTag = "traceable_button_$label" }
    ) {
        content()
    }
}

/**
 * Helper to log state changes in Compose
 */
@Composable
fun <T> LogStateChange(name: String, value: T) {
    LaunchedEffect(value) {
        Log.d("DIAGNOSTIC", "[$name] State changed to: $value")
    }
}
