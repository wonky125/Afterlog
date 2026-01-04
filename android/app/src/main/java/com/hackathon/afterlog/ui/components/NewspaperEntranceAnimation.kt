package com.hackathon.afterlog.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animation state for the newspaper entrance
 */
enum class NewspaperAnimationState {
    HIDDEN,
    DROPPING,
    LANDED,
    COMPLETE
}

/**
 * A composable that wraps content with a cinematic newspaper entrance animation.
 * The newspaper drops from above with a slight rotation, lands with a "thud"
 * haptic feedback, and then content begins appearing.
 *
 * @param modifier Modifier for the animated container
 * @param enabled Whether the animation should play (set false to skip)
 * @param onLanded Callback when the newspaper lands (triggers typewriter animation)
 * @param content The newspaper content to animate
 */
@Composable
fun NewspaperEntranceAnimation(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLanded: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    var animationState by remember { mutableStateOf(if (enabled) NewspaperAnimationState.HIDDEN else NewspaperAnimationState.COMPLETE) }
    
    // Animation values
    val dropOffset = remember { Animatable(-800f) }
    val rotation = remember { Animatable(-12f) }
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    // Trigger entrance animation
    LaunchedEffect(enabled) {
        if (!enabled) {
            // Skip animation - show immediately
            dropOffset.snapTo(0f)
            rotation.snapTo(0f)
            scale.snapTo(1f)
            alpha.snapTo(1f)
            animationState = NewspaperAnimationState.COMPLETE
            return@LaunchedEffect
        }

        animationState = NewspaperAnimationState.DROPPING
        
        // Fade in quickly
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(200, easing = LinearEasing)
        )
    }

    // Drop animation (parallel with fade)
    LaunchedEffect(animationState) {
        if (animationState != NewspaperAnimationState.DROPPING) return@LaunchedEffect

        // All animations will run in parallel within this scope
        coroutineScope {
            // Drop with spring physics
            launch {
                dropOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            // Rotation settles
            launch {
                rotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            // Scale settles
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        // This part now runs only after ALL animations in coroutineScope are finished
        triggerHapticFeedback(context)
        animationState = NewspaperAnimationState.LANDED
        onLanded()
        
        // Small delay before marking complete to ensure smooth transition
        delay(100) 
        animationState = NewspaperAnimationState.COMPLETE
    }


    Box(
        modifier = modifier
            .offset { IntOffset(0, dropOffset.value.toInt()) }
            .rotate(rotation.value)
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        content()
    }
}

/**
 * Trigger a short "thud" haptic feedback when the newspaper lands.
 */
private fun triggerHapticFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                vibrator?.vibrate(50)
            }
        }
    } catch (e: Exception) {
        // Silently fail if vibration is not available
    }
}

/**
 * Simple fade-in animation for content sections (subtitle, body, etc.)
 */
@Composable
fun FadeInContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    durationMs: Int = 400,
    content: @Composable BoxScope.() -> Unit
) {
    var shouldAnimate by remember { mutableStateOf(false) }
    val alpha = animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0f,
        animationSpec = tween(durationMs, easing = EaseOutCubic),
        label = "FadeIn"
    )

    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs.toLong())
            shouldAnimate = true
        }
    }

    Box(
        modifier = modifier.alpha(alpha.value)
    ) {
        if (visible || shouldAnimate) {
            content()
        }
    }
}
