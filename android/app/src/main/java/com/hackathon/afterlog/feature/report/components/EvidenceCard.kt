package com.hackathon.afterlog.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hackathon.afterlog.ui.theme.NewspaperColors
import com.hackathon.afterlog.ui.theme.NewspaperTypography

enum class EvidenceSize(val height: Dp) {
    Small(60.dp),
    Medium(150.dp),
    Large(250.dp)
}

@Composable
fun EvidenceCard(
    imagePath: String?,
    caption: String? = null,
    timestamp: String? = null,
    size: EvidenceSize = EvidenceSize.Medium,
    modifier: Modifier = Modifier
) {
    if (imagePath == null) return

    val vintageMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    Column(modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NewspaperColors.InkGray.copy(alpha = 0.1f)),
            shape = androidx.compose.ui.graphics.RectangleShape // Sharp edges for photo look
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(size.height)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = caption ?: "Evidence Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(vintageMatrix)
                )
            }
        }

        if (caption != null || timestamp != null) {
            Spacer(modifier = Modifier.height(4.dp))
            if (caption != null) {
                Text(
                    text = caption,
                    style = NewspaperTypography.caption,
                    color = NewspaperColors.InkGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            if (timestamp != null) {
                Text(
                    text = timestamp,
                    style = NewspaperTypography.caption.copy(fontSize = NewspaperTypography.caption.fontSize * 0.8f),
                    color = NewspaperColors.InkGray.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}
