package com.hackathon.afterlog.feature.report.components

import android.view.ViewGroup
import android.widget.FrameLayout
import android.content.pm.ActivityInfo
import android.app.Activity
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.unit.sp
import com.hackathon.afterlog.ui.components.NoirIconButton
import com.hackathon.afterlog.ui.components.NoirSurface
import com.hackathon.afterlog.ui.theme.NoirColors
import com.hackathon.afterlog.ui.theme.NoirTypography
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resolvedUri = remember(videoPath) {
        when {
            videoPath.startsWith("content://", ignoreCase = true) -> Uri.parse(videoPath)
            videoPath.startsWith("file://", ignoreCase = true) -> Uri.parse(videoPath)
            else -> Uri.fromFile(File(videoPath))
        }
    }
    
    // Initialize ExoPlayer
    val exoPlayer = remember(resolvedUri) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(resolvedUri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            activity?.requestedOrientation = originalOrientation
            exoPlayer.release()
        }
    }

    NoirSurface {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Player View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        // Style the player view if possible, but basic ExoPlayer UI is okay for now
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                }
            )

            // Header Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoirIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "EVIDENCE_REEL",
                    style = NoirTypography.h1.copy(color = Color.White, fontSize = 20.sp)
                )
            }
        }
    }
}
