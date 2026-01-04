package com.hackathon.afterlog.feature.report.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.ui.theme.NewspaperColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography
import kotlinx.coroutines.delay

@Composable
fun CinematicLoadingView(count: Int) {
    val startMessage = if (count > 0) "Reviewing $count pieces of evidence..." else "Scanning archives..."
    var loadingText by remember { mutableStateOf(startMessage) }

    LaunchedEffect(Unit) {
        val messages = listOf(
            startMessage,
            "Connecting the dots...",
            "Compiling investigative logs...",
            "Drafting the headline...",
            "Generating your report...",
            "Setting the type...",
            "Inking the printing rollers...",
            "Printing the Extra edition..."
        )

        var index = 0
        while (true) {
            delay(1500)
            index = (index + 1) % messages.size
            loadingText = messages[index]
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NewspaperColors.HeadlineRed)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = loadingText,
                style = NewspaperTypography.subheadline,
                textAlign = TextAlign.Center,
                color = NewspaperColors.InkBlack,
                modifier = Modifier.animateContentSize()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Do not close the case file.",
                style = NewspaperTypography.caption,
                color = NewspaperColors.InkGray
            )
        }
    }
}

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Case Closed (Error): $message",
            style = NewspaperTypography.body,
            color = NewspaperColors.ErrorRed
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = NewspaperTypography.sectionHeader,
        color = NewspaperColors.InkGray,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun RawTextFallbackView(rawText: String, logs: List<MediaLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Unformatted Report",
                style = NewspaperTypography.sectionHeader,
                color = NewspaperColors.ErrorRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rawText,
                style = NewspaperTypography.timestamp,
                color = NewspaperColors.InkBlack
            )
        }
        items(logs) { log ->
            Text(
                text = log.toString(),
                style = NewspaperTypography.caption,
                color = NewspaperColors.InkGray
            )
        }
    }
}
