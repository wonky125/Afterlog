package com.hackathon.afterlog.feature.report.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.ui.components.*
import com.hackathon.afterlog.ui.theme.LoraFamily
import com.hackathon.afterlog.ui.theme.NewspaperColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography

private const val TAG = "CinematicNewspaperView"

@Composable
fun CinematicNewspaperView(
    report: GeminiReport,
    isPlaying: Boolean = false,
    isTtsLoading: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    Log.d(TAG, "CinematicNewspaperView composed. Headline: ${report.headline}")
    var hasLanded by remember { mutableStateOf(false) }
    var headlineComplete by remember { mutableStateOf(false) }

    NewspaperEntranceAnimation(
        modifier = Modifier.fillMaxSize(),
        onLanded = {
            Log.d(TAG, "NewspaperEntranceAnimation: onLanded callback received.")
            hasLanded = true
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            item {
                NewspaperHeader(
                    onPlayClick = onPlayClick,
                    isPlaying = isPlaying,
                    isLoading = isTtsLoading
                )
            }

            item {
                if (hasLanded) {
                    TypewriterText(
                        text = report.headline.uppercase(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = NewspaperTypography.headline,
                        color = NewspaperColors.HeadlineBlack,
                        charDelayMs = 25L,
                        onComplete = {
                            headlineComplete = true
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }

            if (report.imagePath != null) {
                item {
                    FadeInContent(
                        visible = headlineComplete,
                        delayMs = 50
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            EvidenceCard(
                                imagePath = report.imagePath,
                                caption = "Fig 1. Visual evidence from the scene.",
                                size = EvidenceSize.Large,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 100
                ) {
                    Text(
                        text = report.summary,
                        style = NewspaperTypography.subheadline,
                        color = NewspaperColors.InkGray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 300
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        HorizontalDivider(
                            color = NewspaperColors.RuleLine,
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = report.article,
                            style = NewspaperTypography.body,
                            color = NewspaperColors.InkBlack,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(
                            color = NewspaperColors.RuleLine,
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            item {
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 500
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("ATMOSPHERE")
                            Text(
                                text = report.atmosphere,
                                style = NewspaperTypography.body,
                                color = NewspaperColors.InkBlack
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("VERDICT")
                            Text(
                                text = report.verdict,
                                style = NewspaperTypography.body.copy(
                                    fontFamily = LoraFamily,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = NewspaperColors.HeadlineRed
                            )
                        }
                    }
                    HorizontalDivider(
                        color = NewspaperColors.RuleLine,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 700
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        text = "TIMELINE OF EVENTS",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            items(report.timeline) { event ->
                FadeInContent(
                    visible = headlineComplete,
                    delayMs = 800
                ) {
                    TimelineEventCard(event)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
