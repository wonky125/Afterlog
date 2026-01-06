package com.hackathon.afterlog.feature.report.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.ui.theme.SpaceTerminalColors
import com.hackathon.afterlog.ui.theme.SpaceTerminalTypography
import kotlinx.coroutines.delay

@Composable
fun CinematicLoadingView(count: Int) {
    val startMessage = if (count > 0) "INITIALIZING DECRYPTION OF $count LOG_FRAGMENTS..." else "SCANNING FOR BLACK BOX SIGNAL..."
    var loadingText by remember { mutableStateOf(startMessage) }

    LaunchedEffect(Unit) {
        val messages = listOf(
            startMessage,
            "ESTABLISHING SECURE UPLINK...",
            "BYPASSING STATION FIREWALL...",
            "RECOVERING CORRUPTED DATA...",
            "DECRYPTING VIDEO STREAM...",
            "SCANNING FOR LIFEFORMS...",
            "ANALYZING BIO-SIGNS...",
            "FINALIZING INCIDENT REPORT..."
        )

        var index = 0
        while (true) {
            delay(1200)
            index = (index + 1) % messages.size
            loadingText = messages[index]
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SpaceTerminalColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            LinearProgressIndicator(
                color = SpaceTerminalColors.PrimaryGreen,
                trackColor = SpaceTerminalColors.PrimaryGreen.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = loadingText,
                style = SpaceTerminalTypography.systemStatus,
                textAlign = TextAlign.Center,
                color = SpaceTerminalColors.PrimaryGreen,
                modifier = Modifier.animateContentSize()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = ">> TERMINAL STATUS: BUSY",
                style = SpaceTerminalTypography.timestamp,
                color = SpaceTerminalColors.SecondaryCyan
            )
        }
    }
}

@Composable
fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(SpaceTerminalColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CRITICAL_SYSTEM_FAILURE",
                style = SpaceTerminalTypography.logTitle.copy(fontSize = 20.sp),
                color = SpaceTerminalColors.WarningRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "> ERROR: $message",
                style = SpaceTerminalTypography.logBody,
                color = SpaceTerminalColors.WarningRed,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = ":: ${text.uppercase()} ::",
            style = SpaceTerminalTypography.systemStatus,
            color = SpaceTerminalColors.SecondaryCyan
        )
        HorizontalDivider(color = SpaceTerminalColors.SecondaryCyan.copy(alpha = 0.3f))
    }
}

@Composable
fun RawTextFallbackView(rawText: String, logs: List<MediaLogEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceTerminalColors.Background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "SYSTEM_DUMP: RAW_DATA",
                style = SpaceTerminalTypography.systemStatus,
                color = SpaceTerminalColors.WarningRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = rawText,
                style = SpaceTerminalTypography.logBody,
                color = SpaceTerminalColors.TextMain
            )
        }
        items(logs) { log ->
            Text(
                text = log.toString(),
                style = SpaceTerminalTypography.timestamp,
                color = SpaceTerminalColors.TextDim,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
