package com.hackathon.afterlog.data.repository.gemini

import android.util.Log
import com.hackathon.afterlog.data.util.GeminiJsonUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

object GeminiLogUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun logLongMessage(tag: String, label: String, message: String) {
        val chunkSize = 1000
        if (message.length <= chunkSize) {
            Log.d(tag, "$label: $message")
            return
        }

        Log.d(tag, "$label: (len=${message.length})")
        var start = 0
        var index = 1
        while (start < message.length) {
            val end = minOf(message.length, start + chunkSize)
            val chunk = message.substring(start, end)
            Log.d(tag, "$label[$index]: $chunk")
            start = end
            index++
        }
    }

    fun logHighlightSegmentsFromRaw(rawText: String) {
        try {
            val cleaned = GeminiJsonUtils.cleanMarkdownJson(rawText)
            val root = json.parseToJsonElement(cleaned).jsonObject
            val segments = root["highlight_segments"]?.jsonArray ?: emptyList()
            if (segments.isEmpty()) {
                Log.d("GeminiRepo", "Gemini highlight segments in JSON: none")
                return
            }

            val formatted = segments.mapNotNull { element ->
                val obj = element.jsonObject
                val start = obj["start_sec"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["start_timestamp"]?.jsonPrimitive?.doubleOrNull
                val end = obj["end_sec"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["end_timestamp"]?.jsonPrimitive?.doubleOrNull
                if (start == null || end == null) return@mapNotNull null
                val reason = obj["reason"]?.jsonPrimitive?.content?.trim().orEmpty()
                val reasonSuffix = if (reason.isBlank()) "" else " ($reason)"
                "${formatSeconds(start)}-${formatSeconds(end)}$reasonSuffix"
            }

            if (formatted.isEmpty()) {
                Log.d("GeminiRepo", "Gemini highlight segments in JSON: none")
            } else {
                Log.d(
                    "GeminiRepo",
                    "Gemini highlight segments in JSON (${formatted.size}): ${formatted.joinToString(", ")}"
                )
            }
        } catch (e: Exception) {
            Log.w("GeminiRepo", "Failed to parse highlight segments from raw JSON", e)
        }
    }

    fun formatSeconds(value: Double): String {
        return String.format(Locale.US, "%.3f", value)
    }
}
