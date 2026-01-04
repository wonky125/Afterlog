package com.hackathon.afterlog.service

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun playFile(file: File) {
        stop() // Stop any previous playback

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    true // Handled
                }
                prepareAsync()
            }
            // Note: _isPlaying will be set to true in onPrepared
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
        }
    }

    fun release() {
        stop()
    }
}
