package com.google.ai.edge.gallery.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.ui.common.rememberDelayedAnimationProgress
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val firstLineText = stringResource(R.string.app_name_first_part)
    val secondLineText = stringResource(R.string.app_name_second_part)

    // Animation for the splash screen
    val progress = rememberDelayedAnimationProgress(
        initialDelay = 200L,
        animationDurationMs = 1500,
        animationLabel = "splash screen animation",
    )

    LaunchedEffect(Unit) {
        delay(3000L)
        onTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress
                translationY = (16.dp * (1 - progress)).toPx()
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = firstLineText,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = secondLineText,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
