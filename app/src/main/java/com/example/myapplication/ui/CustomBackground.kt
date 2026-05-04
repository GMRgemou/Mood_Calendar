package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.MaterialTheme

/**
 * Shared page background used by Timeline, Calendar and Editor.
 *
 * Keeping this in one place avoids slightly different background behavior on each
 * screen and makes future changes (blur, placeholders, loading state, etc.) much
 * easier to maintain.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CustomBackgroundContainer(
    backgroundUri: Uri?,
    overlayAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val safeOverlayAlpha = overlayAlpha.coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = backgroundUri,
            transitionSpec = {
                @Suppress("DEPRECATION")
                fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(180))
            },
            label = "custom_background"
        ) { uri ->
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = safeOverlayAlpha))
        )

        content()
    }
}