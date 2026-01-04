package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.data.model.TimelineEvent
import com.hackathon.afterlog.ui.theme.SpaceTerminalColors
import com.hackathon.afterlog.ui.theme.SpaceTerminalTypography

@Composable
fun LogSequenceEntry(event: TimelineEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, SpaceTerminalColors.PrimaryGreen.copy(alpha = 0.1f))
            .background(SpaceTerminalColors.Surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[ ${event.timestamp} ]",
                style = SpaceTerminalTypography.timestamp,
                color = SpaceTerminalColors.SecondaryCyan
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = event.speaker.uppercase(),
                style = SpaceTerminalTypography.systemStatus,
                color = SpaceTerminalColors.PrimaryGreen
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "> ${event.event.uppercase()}",
            style = SpaceTerminalTypography.logBody.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = SpaceTerminalColors.TextMain
        )
        
        Text(
            text = event.description,
            style = SpaceTerminalTypography.logBody,
            color = SpaceTerminalColors.TextDim
        )
        
        if (event.decibel != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "LVL: ${event.decibel}dB [ ${"|".repeat((event.decibel / 10).coerceAtLeast(1))} ]",
                style = SpaceTerminalTypography.systemStatus.copy(fontSize = 10.sp),
                color = if (event.decibel > 70) SpaceTerminalColors.WarningRed else SpaceTerminalColors.PrimaryGreen
            )
        }
        
        if (event.imagePath != null) {
            Spacer(modifier = Modifier.height(12.dp))
            VisualFragment(
                imagePath = event.imagePath,
                label = "EV_IMG_${event.timestamp.replace(":", "")}",
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}
