package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.ui.components.NoirSurface
import com.hackathon.afterlog.ui.components.NoirButton
import com.hackathon.afterlog.ui.components.NoirLabel
import com.hackathon.afterlog.ui.components.NoirSectionHeader
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NoirTypography
import com.hackathon.afterlog.ui.theme.NewspaperTypography

@Composable
fun InvestigationReportView(
    report: GeminiReport,
    videoPath: String? = null,
    isPlaying: Boolean = false,
    isTtsLoading: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        NoirSurface {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // ... (Existing Header, Headline, Summary) ...
                // Newspaper Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "THE AFTERLOG",
                            style = NewspaperTypography.masthead,
                            color = NoirColors.TextHeading
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = NoirColors.BloodRed, thickness = 2.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("VOL. 1923", style = NewspaperTypography.caption, color = NoirColors.TextSecondary)
                            Text("ARCHIVE NO. ${report.headline.hashCode().coerceAtLeast(0).toString().take(6)}", style = NewspaperTypography.caption, color = NoirColors.TextSecondary)
                            Text("EST. 1872", style = NewspaperTypography.caption, color = NoirColors.TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = NoirColors.Border, thickness = 1.dp)
                    }
                }

                // Headline
                item {
                    Text(
                        text = report.headline,
                        style = NewspaperTypography.headline,
                        color = NoirColors.TextHeading,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Summary (Lead)
                item {
                    Text(
                        text = report.summary,
                        style = NewspaperTypography.subheadline,
                        color = NoirColors.TextBody,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                // Video Evidence (New)
                if (videoPath != null) {
                    item {
                        NoirSectionHeader("MOVING PICTURE EVIDENCE")
                        // Simplified Video Placeholder / Launcher
                        NoirButton(
                            text = "PLAY REEL",
                            onClick = { /* TODO: Launch Video Player */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Main Image
                if (report.imagePath != null) {
                    item {
                        VisualFragment(
                            imagePath = report.imagePath,
                            label = "EVIDENCE_A",
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }

                // Article / Analysis
                item {
                   Column(
                       modifier = Modifier
                           .fillMaxWidth()
                           .border(1.dp, NoirColors.Border)
                           .padding(16.dp)
                   ) {
                       Text(
                           text = report.article,
                           style = NewspaperTypography.body,
                           color = NoirColors.TextBody
                       )
                   }
                   Spacer(modifier = Modifier.height(24.dp))
                }

                // Verdict
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .border(4.dp, NoirColors.BloodRed)
                                .padding(16.dp)
                                .background(NoirColors.BloodRed.copy(alpha = 0.1f))
                        ) {
                             Text(
                                text = "OFFICIAL VERDICT",
                                style = NoirTypography.h1.copy(fontSize = 18.sp),
                                color = NoirColors.BloodRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report.verdict.uppercase(),
                                style = NoirTypography.h1, // Gothic
                                color = NoirColors.BloodRed
                            )
                        }
                    }
                }

                // Timeline
                item {
                    NoirSectionHeader("CASE CHRONOLOGY")
                }

                items(report.timeline) { event ->
                    LogSequenceEntry(event)
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Extra space for FAB
                    Text(
                        text = "--- END OF FILE ---",
                        style = NewspaperTypography.caption,
                        color = NoirColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Floating Play Button (Noir Style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            NoirButton(
                text = if (isPlaying) "STOP READING" else "READ REPORT",
                onClick = onPlayClick,
                icon = {
                     if (isTtsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                     } else {
                        Text(if (isPlaying) "⏹" else "▶", color = Color.White)
                     }
                }
            )
        }
    }
}
