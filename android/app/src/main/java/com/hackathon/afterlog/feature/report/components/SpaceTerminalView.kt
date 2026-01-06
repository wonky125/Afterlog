package com.hackathon.afterlog.feature.report.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.ui.components.TerminalSurface
import com.hackathon.afterlog.ui.components.TypewriterText
import com.hackathon.afterlog.ui.theme.SpaceTerminalColors
import com.hackathon.afterlog.ui.theme.SpaceTerminalTypography

private const val TAG = "SpaceTerminalView"

@Composable
fun SpaceTerminalView(
    report: GeminiReport,
    isPlaying: Boolean = false,
    isTtsLoading: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TerminalSurface {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ... existing header, headline, summary, fragments, analysis, verdict, timeline ...
                // Header
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STATUS: DECRYPTED",
                                style = SpaceTerminalTypography.systemStatus,
                                color = SpaceTerminalColors.PrimaryGreen
                            )
                            Text(
                                text = "STATION: AEGIS-7",
                                style = SpaceTerminalTypography.systemStatus,
                                color = SpaceTerminalColors.SecondaryCyan
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = SpaceTerminalColors.PrimaryGreen.copy(alpha = 0.5f)
                        )
                    }
                }

                // Headline
                item {
                    TypewriterText(
                        text = report.headline.uppercase(),
                        style = SpaceTerminalTypography.logTitle,
                        color = SpaceTerminalColors.WarningRed,
                        modifier = Modifier.padding(vertical = 16.dp),
                        charDelayMs = 40L
                    )
                }

                // Summary
                item {
                    Text(
                        text = "RECOVERY_SUMMARY:",
                        style = SpaceTerminalTypography.systemStatus,
                        color = SpaceTerminalColors.SecondaryCyan
                    )
                    Text(
                        text = report.summary,
                        style = SpaceTerminalTypography.logBody,
                        color = SpaceTerminalColors.TextMain,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Main Image
                if (report.imagePath != null) {
                    item {
                        VisualFragment(
                            imagePath = report.imagePath,
                            label = "STATION_PRIME_CAM",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }

                // Article / Analysis
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SpaceTerminalColors.TextDim.copy(alpha = 0.3f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = report.article,
                            style = SpaceTerminalTypography.logBody,
                            color = SpaceTerminalColors.TextDim
                        )
                    }
                }

                // Verdict
                item {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .fillMaxWidth()
                            .border(2.dp, SpaceTerminalColors.WarningRed)
                            .background(SpaceTerminalColors.WarningRed.copy(alpha = 0.1f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FINAL VERDICT",
                            style = SpaceTerminalTypography.systemStatus,
                            color = SpaceTerminalColors.WarningRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = report.verdict.uppercase(),
                            style = SpaceTerminalTypography.logTitle.copy(fontSize = 20.sp),
                            color = SpaceTerminalColors.WarningRed
                        )
                    }
                }

                // Timeline
                item {
                    Text(
                        text = "SEQUENCE_LOGS:",
                        style = SpaceTerminalTypography.systemStatus,
                        color = SpaceTerminalColors.SecondaryCyan,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(report.timeline) { event ->
                    LogSequenceEntry(event)
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Extra space for FAB
                    Text(
                        text = "-- END OF DATA STREAM --",
                        style = SpaceTerminalTypography.systemStatus,
                        color = SpaceTerminalColors.TextDim,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Terminal Style Play Button (Floating)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = onPlayClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpaceTerminalColors.Surface,
                    contentColor = SpaceTerminalColors.PrimaryGreen
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isPlaying) SpaceTerminalColors.WarningRed else SpaceTerminalColors.PrimaryGreen
                ),
                shape = androidx.compose.ui.graphics.RectangleShape,
                modifier = Modifier.height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTtsLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SpaceTerminalColors.PrimaryGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DECODING...", style = SpaceTerminalTypography.systemStatus)
                    } else {
                        Text(
                            if (isPlaying) "STOP_AUDIO_LOG" else "PLAY_VOICE_LOG",
                            style = SpaceTerminalTypography.systemStatus,
                            color = if (isPlaying) SpaceTerminalColors.WarningRed else SpaceTerminalColors.PrimaryGreen
                        )
                    }
                }
            }
        }
    }
}
