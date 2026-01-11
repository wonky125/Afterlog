package com.hackathon.afterlog.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Persistent storage for session media (Audio, Highlights, Photos)
    // Files/session_media/
    val sessionMediaDir: File by lazy {
        File(context.filesDir, "session_media").apply { mkdirs() }
    }
    
    // Temporary cache for rolling video buffer
    // Cache/temp_videos/
    private val tempVideoDir: File by lazy {
        File(context.cacheDir, "temp_videos").apply { mkdirs() }
    }

    fun getAudioFile(sessionId: String): File {
        return File(sessionMediaDir, "audio_${sessionId}.pcm")
    }

    fun getImageFile(sessionId: String, timestamp: Long): File {
        return File(sessionMediaDir, "${sessionId}_${timestamp}.jpg")
    }

    fun getTempVideoFile(sessionId: String, timestamp: Long): File {
        return File(tempVideoDir, "temp_vid_${sessionId}_$timestamp.mp4")
    }

    fun getHighlightVideoFile(sessionId: String, originalName: String): File {
        return File(sessionMediaDir, "highlight_${sessionId}_$originalName")
    }

    fun getVideoChunkFile(sessionId: String, timestamp: Long): File {
        return File(sessionMediaDir, "chunk_${sessionId}_$timestamp.mp4")
    }

    fun deleteSessionMediaFiles(sessionId: String) {
        sessionMediaDir.listFiles()
            ?.filter { it.name.contains(sessionId) }
            ?.forEach { it.delete() }
    }

    fun deleteSessionArtifacts(sessionId: String) {
        val rootFiles = context.filesDir.listFiles().orEmpty()
        rootFiles.filter { file ->
            file.isFile && file.name.contains(sessionId) && !file.path.contains("session_media")
        }.forEach { it.delete() }
    }

    fun clearTempFiles() {
        try {
            tempVideoDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            // Ignore error during cleanup
        }
    }
    

}
