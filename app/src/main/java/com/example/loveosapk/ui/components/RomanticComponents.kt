package com.example.loveosapk.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveosapk.ui.theme.Accent
import com.example.loveosapk.ui.theme.AccentSecondary
import com.example.loveosapk.ui.theme.GlassBorder
import com.example.loveosapk.ui.theme.GlassWhite

@Composable
fun RomanticButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp), spotColor = Accent.copy(alpha = 0.25f))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Accent, AccentSecondary),
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .bounceClickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).bounceClickable { onClick() } else Modifier)
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun GradientText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val brush = remember {
        Brush.linearGradient(
            colors = listOf(Accent, AccentSecondary)
        )
    }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(brush = brush)
    )
}

@Composable
fun AtmosphericBackground(
    particleCount: Int = 15,
    speedMultiplier: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(backgroundColor, surfaceColor.copy(alpha = 0.92f), backgroundColor)
                )
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun ParticleSystem(count: Int, speed: Float) {
    val particles = remember { List(count) { BackgroundParticle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    particles.forEach { particle ->
        val yOffset by infiniteTransition.animateFloat(
            initialValue = 1200f,
            targetValue = -100f,
            animationSpec = infiniteRepeatable(
                animation = tween((particle.duration / speed).toInt(), delayMillis = particle.delay),
                repeatMode = RepeatMode.Restart
            ),
            label = "p_y"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = particle.maxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.duration / 4, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "p_a"
        )

        Box(
            modifier = Modifier
                .offset(x = particle.x.dp, y = yOffset.dp)
                .size(particle.size.dp)
                .background(Color.White.copy(alpha = alpha), CircleShape)
        )
    }
}

class BackgroundParticle {
    val x = (0..400).random().toFloat()
    val size = (2..6).random().toFloat()
    val duration = (10000..20000).random()
    val delay = (0..5000).random()
    val maxAlpha = (10..40).random() / 100f
}

@Composable
fun AnimatedBlobBackground(content: @Composable () -> Unit) {
    AtmosphericBackground(content = content)
}
