package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hackathon.afterlog.data.model.TimelineEvent
import com.hackathon.afterlog.ui.theme.NewspaperColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography
import com.hackathon.afterlog.ui.theme.SpecialEliteFamily

@Composable
fun TimelineEventCard(event: TimelineEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NewspaperColors.AgedPaper.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Using the modular EvidenceCard if image exists
            if (event.imagePath != null) {
                EvidenceCard(
                    imagePath = event.imagePath,
                    size = EvidenceSize.Small,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .width(60.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.timestamp,
                        style = NewspaperTypography.timestamp,
                        color = NewspaperColors.InkGray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = event.speaker,
                        style = NewspaperTypography.timestamp.copy(
                            fontFamily = SpecialEliteFamily
                        ),
                        color = NewspaperColors.SpeakerA
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.event,
                    style = NewspaperTypography.body.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = NewspaperColors.InkBlack
                )
                Text(
                    text = event.description,
                    style = NewspaperTypography.body,
                    color = NewspaperColors.InkGray
                )
                if (event.decibel != null) {
                    Text(
                        text = "Analyzed Volume: ${event.decibel} dB",
                        style = NewspaperTypography.caption,
                        color = NewspaperColors.LightRule
                    )
                }
            }
        }
    }
}
