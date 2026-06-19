package com.example.loveosapk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
object AnimationConstants {
    val EnterTransition = slideInVertically(
        initialOffsetY = { it / 10 },
        animationSpec = tween(400)
    ) + fadeIn(animationSpec = tween(400))

    val ExitTransition = slideOutVertically(
        targetOffsetY = { it / 10 },
        animationSpec = tween(400)
    ) + fadeOut(animationSpec = tween(400))

    val PopEnterTransition = slideInVertically(
        initialOffsetY = { -it / 10 },
        animationSpec = tween(400)
    ) + fadeIn(animationSpec = tween(400))

    val PopExitTransition = slideOutVertically(
        targetOffsetY = { -it / 10 },
        animationSpec = tween(400)
    ) + fadeOut(animationSpec = tween(400))
}

/**
 * Shared elements are experimental, so we wrap the modifiers for cleaner usage.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedAvatar(
    key: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
): Modifier = with(sharedTransitionScope) {
    this@sharedAvatar.sharedElement(
        rememberSharedContentState(key = key),
        animatedVisibilityScope = animatedContentScope
    )
}
