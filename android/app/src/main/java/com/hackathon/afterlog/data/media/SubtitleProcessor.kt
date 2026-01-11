package com.hackathon.afterlog.data.media

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextOverlay

/**
 * Helper object and classes for subtitle processing.
 * 
 * Handles SRT parsing and caption overlay rendering for video burn-in.
 * Used internally by VideoStitcher.
 */

/**
 * Parses SRT content and provides utility functions for subtitle processing.
 */
@UnstableApi
internal object SubtitleProcessor {
    
    /**
     * Parses SRT content into a list of CaptionCue objects.
     */
    fun parseSrtToCues(srtContent: String): List<VideoStitcher.CaptionCue> {
        val normalized = srtContent.replace("\r", "").trimStart('\uFEFF')
        if (normalized.isBlank()) return emptyList()

        val cues = mutableListOf<VideoStitcher.CaptionCue>()
        val blocks = normalized.split(Regex("\\n\\s*\\n"))
        for (block in blocks) {
            val lines = block.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex == -1) continue

            val timeLine = lines[timeLineIndex]
            val parts = timeLine.split("-->")
            if (parts.size < 2) continue

            val startMs = parseSrtTimestamp(parts[0].trim())
            val endPart = parts[1].trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            val endMs = parseSrtTimestamp(endPart)

            val textLines = lines.drop(timeLineIndex + 1)
            val text = cleanSrtText(textLines.joinToString(" "))
            if (startMs != null && endMs != null && text.isNotBlank()) {
                cues.add(
                    VideoStitcher.CaptionCue(
                        startMs = startMs.coerceAtLeast(0L),
                        endMs = maxOf(endMs, startMs + 500L),
                        text = text
                    )
                )
            }
        }
        return cues
    }

    /**
     * Cleans SRT text by removing HTML tags and normalizing whitespace.
     */
    fun cleanSrtText(text: String): String {
        return text
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\{\\\\.*?\\}"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Parses an SRT timestamp string to milliseconds.
     */
    fun parseSrtTimestamp(value: String): Long? {
        val match = Regex("(\\d{2}):(\\d{2}):(\\d{2})([.,](\\d{1,3}))?").find(value) ?: return null
        val hours = match.groupValues[1]
        val minutes = match.groupValues[2]
        val seconds = match.groupValues[3]
        val millisPart = match.groupValues[5]
        val millis = when (millisPart.length) {
            0 -> 0
            1 -> millisPart.toInt() * 100
            2 -> millisPart.toInt() * 10
            else -> millisPart.take(3).toInt()
        }
        return hours.toLong() * 3_600_000L +
            minutes.toLong() * 60_000L +
            seconds.toLong() * 1_000L +
            millis.toLong()
    }
}

/**
 * TextOverlay implementation for burning captions into video.
 * 
 * Renders styled text at the bottom of the video frame based on timing cues.
 */
@UnstableApi
internal class CaptionTextOverlay(
    cues: List<VideoStitcher.CaptionCue>
) : TextOverlay() {
    
    private data class PreparedCue(val startMs: Long, val endMs: Long, val text: SpannableString)

    private val emptyText = buildInvisibleSpan()
    private val preparedCues = cues.mapNotNull { cue ->
        val text = cue.text.trim()
        if (text.isBlank()) {
            null
        } else {
            PreparedCue(
                startMs = cue.startMs,
                endMs = cue.endMs,
                text = buildSpan(text)
            )
        }
    }.sortedBy { it.startMs }
    
    @Volatile 
    private var lastIndex = -1

    private val overlaySettings = OverlaySettings.Builder()
        .setBackgroundFrameAnchor(0f, -0.6f)
        .setOverlayFrameAnchor(0f, -1f)
        .build()

    override fun getText(presentationTimeUs: Long): SpannableString {
        val timeMs = presentationTimeUs / 1000
        val cue = findCue(timeMs) ?: return emptyText
        return cue.text
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    private fun findCue(timeMs: Long): PreparedCue? {
        val idx = lastIndex
        if (idx in preparedCues.indices) {
            val current = preparedCues[idx]
            if (timeMs in current.startMs..current.endMs) {
                return current
            }
        }

        for (i in preparedCues.indices) {
            val cue = preparedCues[i]
            if (timeMs in cue.startMs..cue.endMs) {
                lastIndex = i
                return cue
            }
        }
        return null
    }

    private fun buildSpan(text: String): SpannableString {
        val spanText = SpannableString(text)
        if (text.isNotEmpty()) {
            spanText.setSpan(
                ForegroundColorSpan(Color.WHITE),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanText.setSpan(
                BackgroundColorSpan(Color.argb(160, 0, 0, 0)),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanText.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanText.setSpan(
                RelativeSizeSpan(0.85f),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spanText
    }

    private fun buildInvisibleSpan(): SpannableString {
        val spanText = SpannableString(".")
        spanText.setSpan(
            ForegroundColorSpan(Color.TRANSPARENT),
            0,
            spanText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spanText.setSpan(
            BackgroundColorSpan(Color.TRANSPARENT),
            0,
            spanText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spanText
    }
}
