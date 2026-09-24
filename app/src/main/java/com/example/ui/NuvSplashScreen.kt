package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash screen for DialFlow.
 *
 * Sequence:
 * 1. Fade-in & subtle scale-up (0.82f -> 1.02f -> 1.0f settling): ~380 ms
 * 2. Hold: ~280 ms
 * 3. Fade-out overlay transition to Home: ~340 ms
 * Total duration: ~1000 ms
 */
@Composable
fun NuvSplashScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val splashAlpha = remember { Animatable(1f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.82f) }

    LaunchedEffect(Unit) {
        // Phase 1: Smooth fade-in & scale-up with settling effect (~380 ms)
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 380,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            // Scale up slightly to 1.02f, then settle back smoothly to 1.0f
            logoScale.animateTo(
                targetValue = 1.02f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 80,
                    easing = FastOutSlowInEasing
                )
            )
        }

        // Wait for entrance animation to finish
        delay(380)

        // Phase 2: Rest/Hold (~280 ms)
        delay(280)

        // Phase 3: Smooth fade-out to reveal Home screen (~340 ms)
        splashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 340,
                easing = FastOutLinearInEasing
            )
        )

        // Animation finished, dismiss splash overlay
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = splashAlpha.value
            }
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Absorb click events during startup splash
            }
            .testTag("nuv_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                }
                .testTag("nuv_splash_logo_container")
        ) {
            // App logo icon: isolated rounded-square logo on dark background
            Image(
                painter = painterResource(id = R.drawable.nuv_dialer_icon_1788511273776),
                contentDescription = "DialFlow Logo",
                modifier = Modifier
                    .size(96.dp)
                    .testTag("nuv_splash_logo"),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(20.dp))

            // Brand Title & Subtitle matching existing app identity
            Text(
                text = "DialFlow",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("dialflow_splash_title")
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.splash_queue_manager),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("nuv_splash_subtitle")
            )
        }
    }
}
