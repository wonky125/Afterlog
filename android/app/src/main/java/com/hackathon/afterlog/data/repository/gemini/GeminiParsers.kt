package com.hackathon.afterlog.data.repository.gemini

import android.util.Log
import com.hackathon.afterlog.data.model.HighlightSegment
import com.hackathon.afterlog.data.repository.GeminiRepository
import com.hackathon.afterlog.data.util.GeminiJsonUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object GeminiParsers {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseCaptions(raw: String): List<GeminiRepository.CaptionLine> {
        return try {
            val cleaned = GeminiJsonUtils.cleanMarkdownJson(raw)
            val root = json.parseToJsonElement(cleaned).jsonObject
            val events = root["events"]?.jsonArray ?: return emptyList()
            events.mapNotNull { el ->
                runCatching {
                    val obj = el.jsonObject
                    val start = obj["start_ms"]?.jsonPrimitive?.longOrNull ?: return@runCatching null
                    val end = obj["end_ms"]?.jsonPrimitive?.longOrNull ?: return@runCatching null
                    val text = obj["text"]?.jsonPrimitive?.content ?: return@runCatching null
                    GeminiRepository.CaptionLine(startMs = start, endMs = end, text = text)
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Caption parse failed", e)
            emptyList()
        }
    }

    fun parseHighlightSegmentsFromRaw(rawText: String): List<HighlightSegment> {
        return try {
            val cleaned = GeminiJsonUtils.cleanMarkdownJson(rawText)
            val root = json.parseToJsonElement(cleaned).jsonObject
            val segments = root["highlight_segments"]?.jsonArray ?: return emptyList()
            segments.mapNotNull { element ->
                val obj = element.jsonObject
                val start = obj["start_sec"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["start_timestamp"]?.jsonPrimitive?.doubleOrNull
                val end = obj["end_sec"]?.jsonPrimitive?.doubleOrNull
                    ?: obj["end_timestamp"]?.jsonPrimitive?.doubleOrNull
                if (start == null || end == null || end <= start) return@mapNotNull null
                val reason = obj["reason"]?.jsonPrimitive?.content ?: ""
                HighlightSegment(startSec = start, endSec = end, reason = reason)
            }
        } catch (e: Exception) {
            Log.w("GeminiRepo", "Highlight-only parse failed", e)
            emptyList()
        }
    }
}
