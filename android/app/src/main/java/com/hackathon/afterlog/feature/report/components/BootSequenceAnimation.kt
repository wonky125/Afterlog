package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.ui.theme.SpaceTerminalColors
import com.hackathon.afterlog.ui.theme.SpaceTerminalTypography
import kotlinx.coroutines.delay

@Composable
fun BootSequenceAnimation(
    onBootComplete: () -> Unit
) {
    var bootLines by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        val sequence = listOf(
            "BIOS_CHECK... OK",
            "MEMORY_TEST... 64TB OK",
            "LOADING_KERNEL... V9.3.1",
            "CHECKING_PERIPHERALS...",
            "> CAMERA_1: OFFLINE",
            "> CAMERA_2: OFFLINE",
            "> CAMERA_3: SIGNAL_WEEK",
            "> BLACK_BOX: CONNECTED",
            "INIT_CRYPTO_MODULE... DONE",
            "ESTABLISHING_UPLINK...",
            "ACCESS_GRANTED: USER_MOTH_ER"
        )

        sequence.forEachIndexed { index, line ->
            delay(if (index < 3) 200 else 100) // Early lines play slower
            bootLines = bootLines + line
        }
        delay(800)
        onBootComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceTerminalColors.Background)
            .padding(32.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            bootLines.forEach { line ->
                Text(
                    text = line,
                    style = SpaceTerminalTypography.systemStatus.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    color = SpaceTerminalColors.PrimaryGreen,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            // Blinking cursor
            BlinkingCursor()
        }
    }
}

@Composable
fun BlinkingCursor() {
    val showCursor by produceState(initialValue = true) {
        while (true) {
            delay(500)
            value = !value
        }
    }
    
    if (showCursor) {
        Text(
            text = "_",
            style = SpaceTerminalTypography.systemStatus,
            color = SpaceTerminalColors.PrimaryGreen
        )
    }
}
