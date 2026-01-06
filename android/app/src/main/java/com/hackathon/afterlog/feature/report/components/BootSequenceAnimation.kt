package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.ui.components.NoirSurface
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography
import kotlinx.coroutines.delay

@Composable
fun BootSequenceAnimation(
    onBootComplete: () -> Unit
) {
    var displayText by remember { mutableStateOf("") }
    val fullText = "THE MISKATONIC ARCHIVES...\nACCESSING CASE FILE #1923..."

    LaunchedEffect(Unit) {
        fullText.forEach { char ->
            delay(50L)
            displayText += char
        }
        delay(1000) // Hold for a second
        onBootComplete()
    }

    NoirSurface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = NewspaperTypography.headline.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp
                ),
                color = NoirColors.BloodRed,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}
