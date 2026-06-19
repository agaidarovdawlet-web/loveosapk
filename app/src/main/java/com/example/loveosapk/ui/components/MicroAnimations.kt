package com.example.loveosapk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Bounce effect on click with haptic feedback
 */
fun Modifier.bounceClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounceScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled
    ) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    }
}

/**
 * Odometer style digit animation
 */
@Composable
fun OdometerDigit(
    digit: Char,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height } + fadeOut())
            } else {
                (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> height } + fadeOut())
            }.using(
                SizeTransform(clip = false)
            )
        },
        label = "odometer"
    ) { char ->
        Text(text = char.toString(), style = style, modifier = modifier)
    }
}

/**
 * Full odometer counter
 */
@Composable
fun OdometerCounter(
    value: Long,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        value.toString().forEach { char ->
            OdometerDigit(digit = char, style = style)
        }
    }
}

/**
 * Animated Checkbox with Path drawing
 */
@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val checkProgress = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "checkProgress"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            drawRect(
                color = color.copy(alpha = 0.5f),
                style = Stroke(width = 2.dp.toPx())
            )
            
            if (checkProgress.value > 0f) {
                val path = Path().apply {
                    moveTo(width * 0.2f, height * 0.5f)
                    lineTo(width * 0.45f, height * 0.75f)
                    lineTo(width * 0.8f, height * 0.25f)
                }
                
                val pathMeasure = PathMeasure()
                pathMeasure.setPath(path, false)
                val segmentPath = Path()
                pathMeasure.getSegment(0f, checkProgress.value * pathMeasure.length, segmentPath, true)
                
                drawPath(
                    path = segmentPath,
                    color = color,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

/**
 * Animated Strikethrough Text
 */
@Composable
fun AnimatedTaskText(
    text: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val progress by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = tween(400),
        label = "strikethrough"
    )
    
    val opacity by animateFloatAsState(
        targetValue = if (done) 0.6f else 1.0f,
        animationSpec = tween(400),
        label = "opacity"
    )

    Box(modifier = modifier.alpha(opacity), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            style = style
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            if (progress > 0f) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, size.height / 2 + 1.dp.toPx()),
                    end = Offset(size.width * progress, size.height / 2 + 1.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

/**
 * Floating hearts effect
 */
@Composable
fun HeartBurstOverlay(
    trigger: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (!trigger) return

    val particles = remember { List(8) { HeartParticle() } }
    
    LaunchedEffect(trigger) {
        delay(1500)
        onAnimationEnd()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            HeartParticleView(particle)
        }
    }
}

@Composable
fun HeartParticleView(particle: HeartParticle) {
    val transition = rememberInfiniteTransition(label = "heart")
    val yOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -400f,
        animationSpec = infiniteRepeatable(
            animation = tween(particle.duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yOffset"
    )
    val xOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = particle.xSpread,
        animationSpec = infiniteRepeatable(
            animation = tween(particle.duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "xOffset"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(particle.duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = (particle.startX + xOffset).dp, y = (particle.startY + yOffset).dp)
    ) {
        Text(
            text = "❤️",
            fontSize = particle.size.sp,
            modifier = Modifier.alpha(alpha)
        )
    }
}

data class HeartParticle(
    val startX: Int = Random.nextInt(50, 350),
    val startY: Int = 600,
    val xSpread: Float = Random.nextInt(-150, 150).toFloat(),
    val duration: Int = Random.nextInt(1000, 2000),
    val size: Int = Random.nextInt(16, 32)
)

/**
 * Confetti effect for roulette
 */
@Composable
fun ConfettiOverlay(trigger: Boolean) {
    if (!trigger) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta, Color.Cyan)
    
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(30) { index ->
            ConfettiParticle(index, colors.random(), infiniteTransition)
        }
    }
}

@Composable
fun ConfettiParticle(index: Int, color: Color, transition: InfiniteTransition) {
    val startX = remember { Random.nextInt(0, 400).dp }
    val delay = index * 50
    
    val yOffset by transition.animateFloat(
        initialValue = -50f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delay, easing = FastOutLinearInEasing)
        ),
        label = "y"
    )
    
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, delayMillis = delay)
        ),
        label = "r"
    )

    Box(
        modifier = Modifier
            .offset(x = startX, y = yOffset.dp)
            .size(Random.nextInt(5, 12).dp)
            .graphicsLayer(rotationZ = rotation)
            .alpha(if (yOffset > 800f) 0f else 1f)
            .background(color)
    )
}
