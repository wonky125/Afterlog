package com.hackathon.afterlog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hackathon.afterlog.ui.theme.NewspaperColors
import kotlin.random.Random

/**
 * A composable that renders a paper-like textured background
 * using procedurally generated noise for a fresh newspaper feel.
 *
 * @param modifier Modifier for the container
 * @param baseColor The base paper color
 * @param noiseIntensity How visible the noise grain is (0.0 - 1.0)
 * @param content The content to display on top of the background
 */
@Composable
fun TexturedBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = NewspaperColors.FreshPaper,
    noiseIntensity: Float = 0.03f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Base gradient background (subtle aged paper effect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            baseColor,
                            NewspaperColors.AgedPaper.copy(alpha = 0.3f).compositeOver(baseColor)
                        )
                    )
                )
        )

        // Noise overlay for paper grain texture
        // OPTIMIZATION: Removed procedural noise generation loop (drawCircle x 160,000)
        // which was causing a ~13 second freeze on the main thread.
        // TODO: Replace with a static image asset (R.drawable.paper_texture) for better performance.
        
        // Canvas(modifier = Modifier.fillMaxSize()) { ... }

        // Content on top
        content()
    }
}

/**
 * Extension function to composite colors
 */
private fun Color.compositeOver(background: Color): Color {
    val fg = this
    val alpha = fg.alpha
    return Color(
        red = fg.red * alpha + background.red * (1 - alpha),
        green = fg.green * alpha + background.green * (1 - alpha),
        blue = fg.blue * alpha + background.blue * (1 - alpha),
        alpha = 1f
    )
}
