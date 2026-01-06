package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hackathon.afterlog.ui.theme.SpaceTerminalColors
import com.hackathon.afterlog.ui.theme.SpaceTerminalTypography

@Composable
fun VisualFragment(
    imagePath: String?,
    label: String = "SENS_RECORD_01",
    timestamp: String? = null,
    modifier: Modifier = Modifier
) {
    if (imagePath == null) return

    val cctvMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                0.3f, 0.3f, 0.3f, 0f, 0f,
                0.3f, 0.3f, 0.3f, 0f, 0f,
                0.3f, 0.3f, 0.3f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    Column(
        modifier = modifier
            .border(1.dp, SpaceTerminalColors.PrimaryGreen.copy(alpha = 0.3f))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = "Visual Fragment",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(cctvMatrix)
            )
            
            // Overlay Labels
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "REC $label",
                    style = SpaceTerminalTypography.systemStatus.copy(
                        color = SpaceTerminalColors.PrimaryGreen,
                        fontSize = 10.sp
                    )
                )
                if (timestamp != null) {
                    Text(
                        text = "T-OFS: $timestamp",
                        style = SpaceTerminalTypography.systemStatus.copy(
                            color = SpaceTerminalColors.PrimaryGreen,
                            fontSize = 10.sp
                        )
                    )
                }
            }
            
            // Camera corners effect (optional, simplified)
            Box(Modifier.fillMaxSize().border(4.dp, SpaceTerminalColors.PrimaryGreen.copy(alpha = 0.1f)))
        }
        
        // Metadata footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceTerminalColors.PrimaryGreen.copy(alpha = 0.05f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ENCRYPTED_STREAM_V3",
                style = SpaceTerminalTypography.systemStatus.copy(fontSize = 8.sp, color = SpaceTerminalColors.TextDim)
            )
            Text(
                text = "FRAME_SYNC: OK",
                style = SpaceTerminalTypography.systemStatus.copy(fontSize = 8.sp, color = SpaceTerminalColors.PrimaryGreen)
            )
        }
    }
}
