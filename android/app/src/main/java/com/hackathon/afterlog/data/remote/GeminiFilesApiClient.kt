package com.hackathon.afterlog.data.remote

import android.util.Log
import com.hackathon.afterlog.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modular HTTP client for Gemini Files API.
 * Handles file upload, status polling, and URI retrieval.
 * 
 * Reference: https://ai.google.dev/api/files
 */
@Singleton
class GeminiFilesApiClient @Inject constructor() {

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val baseUrl = "https://generativelanguage.googleapis.com"

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)  // Large file uploads need more time
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Uploads a file to Gemini Files API and returns the file URI.
     * This is a two-step process:
     * 1. Upload the file (returns a file name like "files/abc123")
     * 2. Poll until the file is ACTIVE
     * 
     * @param file The audio file to upload
     * @param mimeType MIME type (e.g., "audio/mpeg", "audio/wav")
     * @return The Gemini file URI to use in generateContent, or null on failure
     */
    suspend fun uploadFile(file: File, mimeType: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting file upload: ${file.name} (${file.length()} bytes)")

            // Step 1: Upload the file
            val uploadResponse = performUpload(file, mimeType)
            if (uploadResponse == null) {
                Log.e(TAG, "Upload failed: no response")
                return@withContext null
            }

            val fileName = uploadResponse.file.name
            Log.d(TAG, "Upload successful, file name: $fileName")

            // Step 2: Poll until file is ACTIVE
            val activeUri = pollUntilActive(fileName)
            return@withContext activeUri

        } catch (e: Exception) {
            Log.e(TAG, "File upload failed", e)
            return@withContext null
        }
    }

    /**
     * Performs the actual HTTP upload to Gemini Files API.
     */
    private fun performUpload(file: File, mimeType: String): UploadResponse? {
        val url = "$baseUrl/upload/v1beta/files?key=$apiKey"

        val requestBody = file.asRequestBody(mimeType.toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("X-Goog-Upload-Protocol", "raw")
            .header("X-Goog-Upload-File-Name", file.name)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()

        return if (response.isSuccessful) {
            response.body?.string()?.let { body ->
                json.decodeFromString<UploadResponse>(body)
            }
        } else {
            Log.e(TAG, "Upload HTTP error: ${response.code} - ${response.body?.string()}")
            null
        }
    }

    /**
     * Polls the file status until it becomes ACTIVE.
     * Files need processing time after upload before they can be used.
     * 
     * @param fileName The file name returned from upload (e.g., "files/abc123")
     * @return The file URI if active, null if failed or timed out
     */
    private suspend fun pollUntilActive(fileName: String, maxAttempts: Int = 30): String? {
        val url = "$baseUrl/v1beta/$fileName?key=$apiKey"

        repeat(maxAttempts) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@repeat
                    val fileInfo = json.decodeFromString<FileInfoResponse>(body)

                    when (fileInfo.state) {
                        "ACTIVE" -> {
                            Log.d(TAG, "File is ACTIVE: ${fileInfo.uri}")
                            return fileInfo.uri
                        }
                        "PROCESSING" -> {
                            Log.d(TAG, "File still PROCESSING, attempt ${attempt + 1}/$maxAttempts")
                            delay(2000) // Wait 2 seconds before next poll
                        }
                        else -> {
                            Log.e(TAG, "Unexpected file state: ${fileInfo.state}")
                            return null
                        }
                    }
                } else {
                    Log.e(TAG, "Poll HTTP error: ${response.code}")
                    delay(2000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Poll failed", e)
                delay(2000)
            }
        }

        Log.e(TAG, "Polling timed out after $maxAttempts attempts")
        return null
    }

    companion object {
        private const val TAG = "GeminiFilesApiClient"
    }
}

// --- Response Data Classes ---

@Serializable
data class UploadResponse(
    val file: FileMetadata
)

@Serializable
data class FileMetadata(
    val name: String,           // "files/abc123xyz"
    val displayName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
    val expirationTime: String? = null,
    val sha256Hash: String? = null,
    val uri: String? = null,
    val state: String? = null   // "PROCESSING", "ACTIVE", "FAILED"
)

@Serializable
data class FileInfoResponse(
    val name: String,
    val uri: String,
    val state: String,
    val mimeType: String? = null,
    val sizeBytes: String? = null
)
