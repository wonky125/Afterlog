package com.hackathon.afterlog.data.repository.gemini

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.delay

class GeminiRetryPolicy(
    private val generativeModel: GenerativeModel
) {
    private val retryableErrorHints = listOf(
        "503",
        "429",
        "unavailable",
        "overloaded",
        "resource_exhausted",
        "temporarily",
        "timeout"
    )

    suspend fun generateWithRetry(inputContent: Content): GenerateContentResponse {
        val maxAttempts = 3
        var delayMs = 1000L
        var lastError: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                return generativeModel.generateContent(inputContent)
            } catch (e: Exception) {
                lastError = e
                val isLastAttempt = attempt == maxAttempts - 1
                if (isLastAttempt || !isRetryableGeminiError(e)) {
                    throw e
                }
                Log.w("GeminiRepo", "Gemini overloaded/unavailable. Retrying in ${delayMs}ms", e)
                delay(delayMs)
                delayMs *= 2
            }
        }
        throw lastError ?: IllegalStateException("Gemini generateContent failed with unknown error.")
    }

    fun isRetryableGeminiError(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val message = current.message?.lowercase() ?: ""
            if (retryableErrorHints.any { hint -> message.contains(hint) }) return true
            val simpleName = current.javaClass.simpleName.lowercase()
            if (simpleName.contains("serverexception") || simpleName.contains("apiexception")) {
                // Check for specific HTTP status codes or clearer messages
                if (message.contains("503") || message.contains("504") || message.contains("unavailable")) return true
            }
            current = current.cause
        }
        return false
    }

    fun isMaxTokensError(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val message = current.message?.lowercase() ?: ""
            val name = current.javaClass.simpleName.lowercase()
            if (message.contains("max_tokens") || message.contains("max tokens")) {
                return true
            }
            if (name.contains("responsestoppedexception") && message.contains("max")) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
