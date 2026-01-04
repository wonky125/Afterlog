package com.hackathon.afterlog.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.hackathon.afterlog.ui.theme.NewspaperColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography
import kotlinx.coroutines.delay

/**
 * A composable that animates text appearing character by character,
 * simulating a vintage typewriter effect.
 *
 * @param text The full text to display
 * @param modifier Modifier for the Text composable
 * @param style TextStyle to apply
 * @param color Text color
 * @param charDelayMs Delay between each character in milliseconds
 * @param onComplete Callback when typing animation completes
 */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NewspaperTypography.headline, // Reverted back to original style
    color: Color = NewspaperColors.InkBlack,
    charDelayMs: Long = 30L,
    onComplete: () -> Unit = {}
) {
    var displayedText by remember(text) { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        text.forEachIndexed { index, _ ->
            delay(charDelayMs)
            displayedText = text.take(index + 1)
        }
        onComplete()
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style,
        color = color
    )
}

/**
 * Typewriter text with a blinking cursor at the end.
 */
@Composable
fun TypewriterTextWithCursor(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NewspaperTypography.headline,
    color: Color = NewspaperColors.InkBlack,
    charDelayMs: Long = 30L,
    showCursorAfterComplete: Boolean = false,
    onComplete: () -> Unit = {}
) {
    var displayedText by remember(text) { mutableStateOf("") }
    var isComplete by remember(text) { mutableStateOf(false) }
    var showCursor by remember { mutableStateOf(true) }

    // Cursor blink animation
    LaunchedEffect(isComplete, showCursorAfterComplete) {
        if (!isComplete || showCursorAfterComplete) {
            while (true) { // The coroutine will be cancelled when the composable is disposed
                delay(500)
                showCursor = !showCursor
            }
        } else {
            showCursor = false
        }
    }

    // Typing animation
    LaunchedEffect(text) {
        displayedText = ""
        isComplete = false
        
        text.forEachIndexed { index, _ ->
            delay(charDelayMs)
            displayedText = text.take(index + 1)
        }
        
        isComplete = true
        onComplete()
    }

    val cursor = if (showCursor && (!isComplete || showCursorAfterComplete)) "▌" else ""
    
    Text(
        text = displayedText + cursor,
        modifier = modifier,
        style = style,
        color = color
    )
}
