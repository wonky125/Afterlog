package com.hackathon.afterlog.feature.report.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun InlineVideoPlayer(
    videoPath: String,
    subtitlePath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedUri = remember(videoPath) {
        when {
            videoPath.startsWith("content://", ignoreCase = true) -> Uri.parse(videoPath)
            videoPath.startsWith("file://", ignoreCase = true) -> Uri.parse(videoPath)
            else -> {
                val decoded = java.net.URLDecoder.decode(
                    videoPath,
                    java.nio.charset.StandardCharsets.UTF_8.name()
                )
                Uri.fromFile(File(decoded))
            }
        }
    }

    val exoPlayer = remember(resolvedUri, subtitlePath) {
        ExoPlayer.Builder(context).build().apply {
            val trackParams = TrackSelectionParameters.Builder(context)
                .setPreferredTextLanguage(Locale.getDefault().language)
                .setSelectUndeterminedTextLanguage(true)
                .build()
            setTrackSelectionParameters(trackParams)
            val builder = MediaItem.Builder().setUri(resolvedUri)
            subtitlePath?.takeIf { it.isNotBlank() }?.let { rawPath ->
                val subtitleFile = File(rawPath)
                if (subtitleFile.exists()) {
                    val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subtitleFile))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage(Locale.getDefault().language)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    builder.setSubtitleConfigurations(listOf(subtitleConfig))
                }
            }
            setMediaItem(builder.build())
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (view.player == null) {
                view.player = exoPlayer
            }
        }
    )
}
