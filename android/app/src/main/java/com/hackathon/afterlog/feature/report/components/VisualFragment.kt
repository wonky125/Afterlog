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
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography

@Composable
fun VisualFragment(
    imagePath: String?,
    label: String = "EVIDENCE",
    timestamp: String? = null,
    modifier: Modifier = Modifier
) {
    if (imagePath == null) return

    // Sepia Matrix for 1920s look
    val sepiaMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    Column(
        modifier = modifier
            .background(Color.White) // Photo border
            .padding(4.dp) // White border width
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
                contentDescription = "Evidence Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(sepiaMatrix)
            )
            
            // "CONFIDENTIAL" stamp style label
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .border(2.dp, NoirColors.BloodRed.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                 Text(
                    text = label.uppercase(),
                    style = NewspaperTypography.caption.copy(
                        color = NoirColors.BloodRed.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
        
        if (timestamp != null) {
            Text(
                text = "DATE: $timestamp",
                style = NewspaperTypography.caption,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
