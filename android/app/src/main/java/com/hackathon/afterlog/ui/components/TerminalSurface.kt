package com.hackathon.afterlog.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import com.hackathon.afterlog.ui.theme.SpaceTerminalColors

@Composable
fun TerminalSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "terminal_flicker")
    
    // Subtle flicker animation
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceTerminalColors.Background)
    ) {
        // CRT Background Effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Draw scanlines
            val scanlineHeight = 4f
            var y = 0f
            while (y < canvasHeight) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.2f),
                    topLeft = Offset(0f, y),
                    size = androidx.compose.ui.geometry.Size(canvasWidth, 2f)
                )
                y += scanlineHeight
            }
            
            // Draw subtle glow vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        SpaceTerminalColors.Background.copy(alpha = 0.5f)
                    ),
                    center = center,
                    radius = canvasWidth * 0.8f
                )
            )
        }
        
        // Content with flicker
        Box(modifier = Modifier.fillMaxSize().then(Modifier.background(Color.White.copy(alpha = 1f - flickerAlpha)))) {
            content()
        }
    }
}
