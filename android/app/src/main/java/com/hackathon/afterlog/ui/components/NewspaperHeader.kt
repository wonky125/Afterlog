package com.hackathon.afterlog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackathon.afterlog.ui.theme.NewspaperColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography
import java.text.SimpleDateFormat
import java.util.*

/**
 * Newspaper masthead header with logo, title, and date information.
 * Styled after classic 1920s newspaper headers.
 *
 * @param modifier Modifier for the header
 * @param sessionDate Optional date to display (defaults to current date)
 * @param volumeNumber Volume/edition number
 */
@Composable
fun NewspaperHeader(
    modifier: Modifier = Modifier,
    sessionDate: Date = Date(),
    volumeNumber: Int = 1,
    onPlayClick: (() -> Unit)? = null,
    isPlaying: Boolean = false,
    isLoading: Boolean = false
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH) }
    val formattedDate = dateFormat.format(sessionDate)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top rule line
        HorizontalDivider(
            color = NewspaperColors.RuleLine,
            thickness = 2.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Draw Noir Logo - REMOVED per user feedback
        // Just the title now for a clean look
        
        // Masthead Title
        Text(
            text = "THE AFTERLOG",
            style = NewspaperTypography.masthead,
            color = NewspaperColors.HeadlineBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VOL. $volumeNumber",
                style = NewspaperTypography.timestamp,
                color = NewspaperColors.InkGray
            )

            Text(
                text = "• DETECTIVE'S REPORT •",
                style = NewspaperTypography.sectionHeader,
                color = NewspaperColors.InkBlack
            )

            Text(
                text = formattedDate.uppercase(),
                style = NewspaperTypography.timestamp,
                color = NewspaperColors.InkGray
            )
        }


        if (onPlayClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPlayClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NewspaperColors.InkBlack,
                    containerColor = NewspaperColors.FreshPaper.copy(alpha = 0.5f)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(NewspaperColors.InkBlack)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NewspaperColors.InkBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATING AUDIO...", style = NewspaperTypography.caption)
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "STOP NARRATION" else "LISTEN TO REPORT",
                        style = NewspaperTypography.caption
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom rule line  
        HorizontalDivider(
            color = NewspaperColors.RuleLine,
            thickness = 1.dp
        )
    }
}

/**
 * Compact version of the header for scrolled state or smaller screens.
 */
@Composable
fun NewspaperHeaderCompact(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = NewspaperColors.RuleLine, thickness = 1.dp)
        
        Text(
            text = "THE AFTERLOG",
            style = NewspaperTypography.sectionHeader.copy(
                letterSpacing = NewspaperTypography.masthead.letterSpacing
            ),
            color = NewspaperColors.HeadlineBlack,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        
        HorizontalDivider(color = NewspaperColors.RuleLine, thickness = 1.dp)
    }
}
