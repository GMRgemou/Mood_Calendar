package com.example.myapplication.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Smooth page transition: new screen slides in from right + fades in,
 * old screen slides out to left + fades out.
 */
fun smoothPageEnterTransition(direction: SlideDirection = SlideDirection.RIGHT_TO_LEFT): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { fullWidth ->
            when (direction) {
                SlideDirection.RIGHT_TO_LEFT -> fullWidth
                SlideDirection.LEFT_TO_RIGHT -> -fullWidth
            }
        }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        initialAlpha = 0.3f
    )
}

fun smoothPageExitTransition(direction: SlideDirection = SlideDirection.LEFT_TO_RIGHT): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { fullWidth ->
            when (direction) {
                SlideDirection.LEFT_TO_RIGHT -> fullWidth
                SlideDirection.RIGHT_TO_LEFT -> -fullWidth
            }
        }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        )
    )
}

fun smoothPopEnterTransition(): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { -it / 3 }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        initialAlpha = 0.3f
    )
}

fun smoothPopExitTransition(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { it }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        )
    )
}

enum class SlideDirection {
    RIGHT_TO_LEFT,
    LEFT_TO_RIGHT
}

/**
 * Item entrance animation: slides up + fades in with a delay based on index.
 */
fun staggeredItemAnimation(delayMs: Int = 0): EnterTransition {
    return slideInVertically(
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        ),
        initialOffsetY = { it / 4 }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Simple fade-in + slide-up animation for non-list content.
 * Unlike staggeredItemAnimation, this uses a fixed delay instead of per-item stagger.
 */
fun fadeInSlideUp(delayMs: Int = 0): EnterTransition {
    return slideInVertically(
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        ),
        initialOffsetY = { it / 8 }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Spring-based scale animation for button press feedback.
 * Returns a Modifier that scales down on press.
 */
@Composable
fun Modifier.buttonPressAnimation(): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    return this.then(
        Modifier.scale(
            if (isPressed) 0.92f else 1f
        )
    ).then(
        Modifier.animateContentSize()
    )
}

/**
 * Card entrance: scales up slightly + fades in.
 */
fun cardEntranceAnimation(delayMs: Int = 0): EnterTransition {
    return scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.01f
        ),
        initialScale = 0.92f
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Smooth shared axis transition for bottom-navigation-style page switching.
 */
fun sharedAxisEnterTransition(): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { it / 4 }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        )
    )
}

fun sharedAxisExitTransition(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { -it / 4 }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearEasing
        )
    )
}

/**
 * Fade-through transition matching Material Design guidelines for
 * bottom navigation tab switches.
 */
val fadeThroughSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = 350,
    easing = FastOutSlowInEasing
)