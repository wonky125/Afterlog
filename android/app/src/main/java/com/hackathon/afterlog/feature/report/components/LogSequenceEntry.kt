package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.data.model.TimelineEvent
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography

@Composable
fun LogSequenceEntry(event: TimelineEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${event.timestamp} // ${event.speaker.uppercase()}",
                style = NewspaperTypography.timestamp,
                color = NoirColors.BloodRed
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = event.description,
            style = NewspaperTypography.body.copy(fontSize = 14.sp),
            color = NoirColors.TextBody
        )
        
        if (event.imagePath != null) {
            Spacer(modifier = Modifier.height(8.dp))
            VisualFragment(
                imagePath = event.imagePath,
                label = "EXHIBIT_${event.timestamp.replace(":", "")}",
                modifier = Modifier.width(120.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = NoirColors.Border.copy(alpha = 0.5f))
    }
}
