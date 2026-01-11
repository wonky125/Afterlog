package com.hackathon.afterlog.data.util

/**
 * Shared utility for cleaning Gemini API JSON responses.
 * Handles markdown code fences and extracts valid JSON.
 */
object GeminiJsonUtils {

    /**
     * Cleans markdown-wrapped JSON from Gemini responses.
     * Removes ```json or ``` fences and extracts the JSON object.
     */
    fun cleanMarkdownJson(raw: String): String {
        var cleaned = raw.trim()

        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                .removePrefix("```json")
                .removePrefix("```")

            val endFence = cleaned.lastIndexOf("```")
            if (endFence >= 0) {
                cleaned = cleaned.substring(0, endFence)
            }
        }

        cleaned = cleaned.trim()

        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        if (startIdx >= 0 && endIdx > startIdx) {
            cleaned = cleaned.substring(startIdx, endIdx + 1)
        }

        return cleaned
    }
}
