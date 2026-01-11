package com.hackathon.afterlog.data.util

/**
 * Shared utility for cleaning Gemini API JSON responses.
 * Handles markdown code fences and extracts valid JSON.
 */
object GeminiJsonUtils {

    /**
     * Cleans markdown-wrapped JSON from Gemini responses.
     * Removes ```json or ``` fences and extracts the JSON object.
     *
     * @param raw The raw response string from Gemini
     * @return Cleaned JSON string ready for parsing
     */
    fun cleanMarkdownJson(raw: String): String {
        var cleaned = raw.trim()
        
        // Remove markdown code fences
        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                .removePrefix("```json")
                .removePrefix("```")
            
            // Remove trailing backticks
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"))
            }
        }
        
        cleaned = cleaned.trim()
        
        // Extract JSON object if there's surrounding text
        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        if (startIdx >= 0 && endIdx > startIdx) {
            cleaned = cleaned.substring(startIdx, endIdx + 1)
        }
        
        return cleaned
    }
}
