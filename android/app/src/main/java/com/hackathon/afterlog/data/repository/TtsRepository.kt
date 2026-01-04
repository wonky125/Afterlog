package com.hackathon.afterlog.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.hackathon.afterlog.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Construct URL dynamically to ensure we use the latest BuildConfig value
    private fun getUrl(): String {
        val key = BuildConfig.GOOGLE_CLOUD_KEY
        if (key.isEmpty() || key.startsWith("PLACEHOLDER") || key == "\"\"") {
            Log.e("TtsRepository", "CRITICAL: GOOGLE_CLOUD_KEY is empty or missing! Check local.properties")
            return ""
        }
        Log.d("TtsRepository", "Using API Key (Length: ${key.length}, Starts with: ${key.take(4)}...)")
        return "https://texttospeech.googleapis.com/v1/text:synthesize?key=$key"
    }

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Synthesizes text to speech and returns the path to the saved audio file.
     * Uses cache directory to store the file.
     */
    suspend fun synthesizeText(text: String, filename: String = "narration_audio.mp3"): File? = withContext(Dispatchers.IO) {
        val requestBody = TtsRequest(
            input = TtsInput(text = text),
            voice = TtsVoice(languageCode = "en-US", name = "en-US-Neural2-F"), // MOTH_ER AI Female Voice
            audioConfig = TtsAudioConfig(
                audioEncoding = "MP3",
                pitch = 0.0, // Neutral pitch for clear AI tone
                speakingRate = 1.05 // Slightly faster for efficient machine communication
            )
        )

        val jsonBody = json.encodeToString(requestBody)
        val url = getUrl()
        if (url.isEmpty()) {
            Log.e("TtsRepository", "Cannot make TTS request: Invalid API key")
            return@withContext null
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            // FIX: Use 'use' to ensure response is closed, and read body string once
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e("TtsRepository", "TTS API Error: ${response.code} - ${response.message}")
                    responseBodyStr?.let { Log.e("TtsRepository", "Error Body: $it") }
                    return@use null
                }

                if (responseBodyStr == null) return@use null

                val ttsResponse = json.decodeFromString<TtsResponse>(responseBodyStr)
                val audioBytes = Base64.decode(ttsResponse.audioContent, Base64.DEFAULT)

                // Save to cache dir
                val audioFile = File(context.cacheDir, filename)
                FileOutputStream(audioFile).use { it.write(audioBytes) }

                Log.d("TtsRepository", "Audio saved to: ${audioFile.absolutePath}")
                return@use audioFile
            }
        } catch (e: Exception) {
            Log.e("TtsRepository", "TTS Exception", e)
            return@withContext null
        }
    }
}

// --- Data Models ---

@Serializable
data class TtsRequest(
    val input: TtsInput,
    val voice: TtsVoice,
    val audioConfig: TtsAudioConfig
)

@Serializable
data class TtsInput(
    val text: String
)

@Serializable
data class TtsVoice(
    val languageCode: String,
    val name: String
)

@Serializable
data class TtsAudioConfig(
    val audioEncoding: String,
    val pitch: Double? = null,
    val speakingRate: Double? = null
)

@Serializable
data class TtsResponse(
    val audioContent: String // Base64 encoded
)
