package com.hackathon.afterlog.feature.report.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.ui.components.NoirSurface
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography
import kotlinx.coroutines.delay

@Composable
fun CinematicLoadingView(count: Int) {
    val startMessage = "SEARCHING ARCHIVES..."
    var loadingText by remember { mutableStateOf(startMessage) }

    LaunchedEffect(Unit) {
        val messages = listOf(
            startMessage,
            "CONSULTING WITNESSES...",
            "REVIEWING EVIDENCE...",
            "DEVELOPING PHOTOGRAPHS...",
            "TYPING REPORT..."
        )

        var index = 0
        while (true) {
            delay(1500)
            index = (index + 1) % messages.size
            loadingText = messages[index]
        }
    }

    NoirSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                color = NoirColors.BloodRed,
                trackColor = NoirColors.CardStart,
                modifier = Modifier.width(200.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = loadingText,
                style = NewspaperTypography.sectionHeader,
                textAlign = TextAlign.Center,
                color = NoirColors.TextBody,
                modifier = Modifier.animateContentSize()
            )
        }
    }
}

@Composable
fun ErrorView(message: String) {
    NoirSurface {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "INVESTIGATION FAILED",
                style = NewspaperTypography.headline,
                color = NoirColors.BloodRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = NewspaperTypography.body,
                color = NoirColors.TextBody,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = text.uppercase(),
            style = NewspaperTypography.sectionHeader,
            color = NoirColors.BloodRed
        )
        Divider(color = NoirColors.BloodRed.copy(alpha = 0.5f))
    }
}

@Composable
fun RawTextFallbackView(rawText: String, logs: List<MediaLogEntity>) {
    NoirSurface {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            item {
                Text(
                    text = "UNPROCESSED NOTES",
                    style = NewspaperTypography.headline,
                    color = NoirColors.TextHeading
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = rawText,
                    style = NewspaperTypography.body,
                    color = NoirColors.TextBody
                )
            }
            items(logs) { log ->
                Text(
                    text = log.toString(),
                    style = NewspaperTypography.timestamp,
                    color = NoirColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
