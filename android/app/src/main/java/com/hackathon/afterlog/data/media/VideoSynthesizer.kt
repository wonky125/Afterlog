package com.hackathon.afterlog.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Build
import android.util.Log
import android.view.Surface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VideoSynthesizer: Combines audio narration with image to create MP4 videos.
 * 
 * Uses Android Native Media APIs + EGL for Input Surface.
 * 1. Transcodes Audio (MP3 -> AAC)
 * 2. Encodes Video (Image -> H.264)
 * 3. Muxes both into MP4 container
 */
@Singleton
class VideoSynthesizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun synthesize(
        outputSessionId: String,
        audioFile: File,
        images: List<File>,
        imageDurationSec: Int = 5
    ): File? = withContext(Dispatchers.IO) {
        
        if (!audioFile.exists()) return@withContext null
        val imageFile = images.firstOrNull() ?: return@withContext null
        
        val outputFile = File(context.filesDir, "replay_$outputSessionId.mp4")
        if (outputFile.exists()) outputFile.delete()
        
        Log.d(TAG, "Starting Synthesis: Audio=${audioFile.name}, Image=${imageFile.name}")
        
        var muxer: MediaMuxer? = null
        var audioRef: AudioProcessor? = null
        var videoRef: VideoProcessor? = null
        
        try {
            val m = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = m
            val a = AudioProcessor(audioFile)
            audioRef = a
            val v = VideoProcessor(imageFile, m)
            videoRef = v
            
            // 1. Prepare Audio
            a.prepare()
            val durationUs = a.getDurationUs() ?: (imageDurationSec * 1_000_000L)
            
            Log.d(TAG, "Target Duration: ${durationUs / 1_000_000.0} sec")
            
            // 2. Prepare Video
            v.prepare(1280, 720, durationUs)
            
            // 3. Start Processing
            m.setOrientationHint(0)
            
            processInterleaved(m, a, v)
            
            Log.d(TAG, "✅ Synthesis Complete: ${outputFile.length()} bytes")
            return@withContext outputFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            outputFile.delete()
            return@withContext null
        } finally {
            // Ensure resources are released even if muxer fails
            try { 
                muxer?.stop() 
            } catch (e: Exception) {
                Log.w(TAG, "Muxer stop failed", e)
            }
            try { 
                muxer?.release() 
            } catch (e: Exception) {
                Log.w(TAG, "Muxer release failed", e)
            }
            audioRef?.release()
            videoRef?.release()
        }
    }

    // --- Helper Classes ---
    
    private data class PendingSample(
        val trackIndex: Int,
        val bufferInfo: MediaCodec.BufferInfo,
        val buffer: ByteBuffer
    )
    
    private fun processInterleaved(muxer: MediaMuxer, audio: AudioProcessor, video: VideoProcessor) {
        var muxerStarted = false
        var audioTrackIndex = -1
        var videoTrackIndex = -1
        
        // Internal buffers for pre-muxer samples
        val pendingSamples = mutableListOf<PendingSample>()
        
        // Start encoders
        audio.start()
        video.start()
        
        val bufferInfo = MediaCodec.BufferInfo()
        var audioDone = false
        var videoDone = false
        
        // Helper to flush if ready
        fun flushPendingIfExists() {
            if (muxerStarted && pendingSamples.isNotEmpty()) {
                Log.d(TAG, "Flushing ${pendingSamples.size} pending samples")
                for (sample in pendingSamples) {
                    muxer.writeSampleData(sample.trackIndex, sample.buffer, sample.bufferInfo)
                }
                pendingSamples.clear()
            }
        }
        
        fun checkStartMuxer() {
            if (!muxerStarted && audioTrackIndex >= 0 && videoTrackIndex >= 0) {
                 muxer.start()
                 muxerStarted = true
                 Log.d(TAG, "Muxer started!")
                 
                 // Fix up track indices in pending samples? 
                 // Actually we store trackIndex relative to what addTrack returned.
                 // So they are correct.
                 flushPendingIfExists()
            }
        }

        while (!audioDone || !videoDone) {
            // --- Audio Pump ---
            if (!audioDone) {
                audioDone = audio.drainEncoder(bufferInfo) { encodedData, isConfig ->
                    if (isConfig) {
                        audioTrackIndex = muxer.addTrack(audio.outputFormat!!)
                        checkStartMuxer()
                    } else if (audioTrackIndex >= 0) {
                        // Copy buffer because it will be released
                        val dataCopy = ByteBuffer.allocate(encodedData.remaining())
                        dataCopy.put(encodedData)
                        dataCopy.flip()
                        
                        val infoCopy = MediaCodec.BufferInfo()
                        infoCopy.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)
                        
                        if (muxerStarted) {
                            muxer.writeSampleData(audioTrackIndex, dataCopy, infoCopy)
                        } else {
                            pendingSamples.add(PendingSample(audioTrackIndex, infoCopy, dataCopy))
                        }
                    }
                }
            }
            
            // --- Video Pump ---
            if (!videoDone) {
                 videoDone = video.drainEncoder(bufferInfo) { encodedData, isConfig ->
                    if (isConfig) {
                        videoTrackIndex = muxer.addTrack(video.outputFormat!!)
                        checkStartMuxer()
                    } else if (videoTrackIndex >= 0) {
                         // Copy buffer
                        val dataCopy = ByteBuffer.allocate(encodedData.remaining())
                        dataCopy.put(encodedData)
                        dataCopy.flip()
                        
                        val infoCopy = MediaCodec.BufferInfo()
                        infoCopy.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)

                        if (muxerStarted) {
                            muxer.writeSampleData(videoTrackIndex, dataCopy, infoCopy)
                        } else {
                            pendingSamples.add(PendingSample(videoTrackIndex, infoCopy, dataCopy))
                        }
                    }
                }
            }
        }
    }
    
    // --- Audio Processor ---
    private class AudioProcessor(val inputFile: File) {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        
        var outputFormat: MediaFormat? = null
        var inputDone = false
        var decoderDone = false
        var encoderDone = false
        
        fun prepare() {
            extractor = MediaExtractor().apply { setDataSource(inputFile.absolutePath) }
        }
        
        fun getDurationUs(): Long? {
            val trackIndex = selectAudioTrack()
            if (trackIndex < 0) return null
            val format = extractor!!.getTrackFormat(trackIndex)
            return try { format.getLong(MediaFormat.KEY_DURATION) } catch(e: Exception) { null }
        }
        
        fun start() {
            val trackIndex = selectAudioTrack()
            if (trackIndex < 0) throw IllegalStateException("No audio track found in ${inputFile.name}")
            extractor!!.selectTrack(trackIndex)
            val inputFormat = extractor!!.getTrackFormat(trackIndex)
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            
            Log.d(TAG, "AudioProcessor: Input SR=$sampleRate, Ch=$channelCount")
            
            // Match output sample rate to input to avoid pitch-shift if we don't implement resampling
            val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
            aacFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            aacFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            aacFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val currentEncoder = encoder!!
            currentEncoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            currentEncoder.start()
            
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""
            decoder = MediaCodec.createDecoderByType(mime)
            val currentDecoder = decoder!!
            currentDecoder.configure(inputFormat, null, null, 0)
            currentDecoder.start()
        }
        
        fun drainEncoder(bufferInfo: MediaCodec.BufferInfo, onOutput: (ByteBuffer, Boolean) -> Unit): Boolean {
            if (encoderDone) return true
            
            if (!inputDone) {
                val idx = decoder!!.dequeueInputBuffer(TIMEOUT_US)
                if (idx >= 0) {
                    val buffer = decoder!!.getInputBuffer(idx) ?: return false
                    val size = extractor!!.readSampleData(buffer, 0)
                    if (size < 0) {
                        decoder!!.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder!!.queueInputBuffer(idx, 0, size, extractor!!.sampleTime, 0)
                        extractor!!.advance()
                    }
                }
            }
            
            if (!decoderDone) {
                val idx = decoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (idx >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) decoderDone = true
                    
                    if (bufferInfo.size > 0) {
                        val decodedBuf = decoder!!.getOutputBuffer(idx) ?: return false
                        decodedBuf.position(bufferInfo.offset)
                        decodedBuf.limit(bufferInfo.offset + bufferInfo.size)
                        
                        val encIdx = encoder!!.dequeueInputBuffer(TIMEOUT_US)
                        if (encIdx >= 0) {
                            val encBuffer = encoder!!.getInputBuffer(encIdx) ?: return false
                            encBuffer.clear()
                            encBuffer.put(decodedBuf)
                            val flags = if (decoderDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                            encoder!!.queueInputBuffer(encIdx, 0, encBuffer.position(), bufferInfo.presentationTimeUs, flags)
                        }
                    } else if (decoderDone) {
                         val encIdx = encoder!!.dequeueInputBuffer(TIMEOUT_US)
                         if (encIdx >= 0) encoder!!.queueInputBuffer(encIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                    decoder!!.releaseOutputBuffer(idx, false)
                }
            }
            
            val idx = encoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputFormat = encoder!!.outputFormat
                onOutput(ByteBuffer.allocate(0), true)
            } else if (idx >= 0) {
                val encodedBuf = encoder!!.getOutputBuffer(idx) ?: return false
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bufferInfo.size = 0
                
                if (bufferInfo.size > 0) {
                    encodedBuf.position(bufferInfo.offset)
                    encodedBuf.limit(bufferInfo.offset + bufferInfo.size)
                    onOutput(encodedBuf, false)
                }
                encoder!!.releaseOutputBuffer(idx, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    encoderDone = true
                    return true
                }
            }
            return false
        }
        
        fun release() {
            try { extractor?.release(); decoder?.stop(); decoder?.release(); encoder?.stop(); encoder?.release() } catch(e:Exception){}
        }
        
        private fun selectAudioTrack(): Int {
             for (i in 0 until extractor!!.trackCount) {
                if (extractor!!.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) return i
             }
             return -1
        }
    }
    
    // --- Video Processor ---
    private class VideoProcessor(val imageFile: File, val muxer: MediaMuxer) {
        var encoder: MediaCodec? = null
        var inputSurface: Surface? = null
        var eglInputSurface: InputSurface? = null
        var outputFormat: MediaFormat? = null
        var generatedDurationUs: Long = 0
        var targetDurationUs: Long = 0
        
        var textureRenderer: TextureRenderer? = null
        var textureId: Int = -1
        
        fun prepare(width: Int, height: Int, durationUs: Long) {
            targetDurationUs = durationUs
            
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val currentEncoder = encoder!!
            currentEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = currentEncoder.createInputSurface()
            eglInputSurface = InputSurface(inputSurface!!)
            eglInputSurface!!.makeCurrent()
        }
        
        fun start() {
            encoder!!.start()
        }
        
        fun drainEncoder(bufferInfo: MediaCodec.BufferInfo, onOutput: (ByteBuffer, Boolean) -> Unit): Boolean {
            if (generatedDurationUs < targetDurationUs) {
                drawFrame(generatedDurationUs)
                generatedDurationUs += 33333L // 30fps
                if (generatedDurationUs >= targetDurationUs) {
                    encoder!!.signalEndOfInputStream()
                }
            }
            
            val idx = encoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
             if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputFormat = encoder!!.outputFormat
                onOutput(ByteBuffer.allocate(0), true)
            } else if (idx >= 0) {
                 val encodedBuf = encoder!!.getOutputBuffer(idx) ?: return false
                 if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bufferInfo.size = 0
                 
                 if (bufferInfo.size > 0) {
                    encodedBuf.position(bufferInfo.offset)
                    encodedBuf.limit(bufferInfo.offset + bufferInfo.size)
                    onOutput(encodedBuf, false)
                 }
                 encoder!!.releaseOutputBuffer(idx, false)
                 if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return true
            }
            return false
        }
        
        private fun drawFrame(presentationTimeUs: Long) {
             eglInputSurface!!.setPresentationTime(presentationTimeUs * 1000)
             
             // Lazy init texture renderer
             if (textureRenderer == null) {
                 textureRenderer = TextureRenderer()
                 textureRenderer!!.surfaceCreated()
                 
                 // Load bitmap
                 val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                     ?: throw IllegalStateException("Failed to decode image: ${imageFile.name}")
                 
                 textureId = textureRenderer!!.loadTexture(bitmap)
                 bitmap.recycle()
             }
             
             android.opengl.GLES20.glViewport(0, 0, 1280, 720)
             android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
             android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
             
             if (textureId != -1) {
                textureRenderer!!.draw(textureId)
             }
             
             eglInputSurface!!.swapBuffers()
        }

        fun release() {
            try { 
                textureRenderer?.cleanup()
                eglInputSurface?.release()
                encoder?.stop()
                encoder?.release() 
            } catch(e:Exception){}
        }
    }

    private class InputSurface(surface: Surface) {
        private var eglDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface = EGL14.EGL_NO_SURFACE
        
        init {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
            
            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
            if (numConfigs[0] == 0 || configs[0] == null) {
                throw RuntimeException("Failed to find suitable EGL config")
            }
            
            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
        }
        
        fun makeCurrent() { EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext) }
        fun swapBuffers() { EGL14.eglSwapBuffers(eglDisplay, eglSurface) }
        fun setPresentationTime(nsecs: Long) { EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs) }
        
        fun release() {
            try {
                if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    try { EGL14.eglDestroySurface(eglDisplay, eglSurface) } catch (e: Exception) { Log.w("InputSurface", "Surface cleanup failed", e) }
                    try { EGL14.eglDestroyContext(eglDisplay, eglContext) } catch (e: Exception) { Log.w("InputSurface", "Context cleanup failed", e) }
                    try { EGL14.eglTerminate(eglDisplay) } catch (e: Exception) { Log.w("InputSurface", "Display cleanup failed", e) }
                }
                eglDisplay = EGL14.EGL_NO_DISPLAY
                eglContext = EGL14.EGL_NO_CONTEXT
                eglSurface = EGL14.EGL_NO_SURFACE
            } catch (e: Exception) {
                Log.e("InputSurface", "Release failed", e)
            }
        }
    }

    private class TextureRenderer {
        private val vertexShaderCode =
            "attribute vec4 vPosition;" +
            "attribute vec2 vTexCoordinate;" +
            "varying vec2 v_TexCoordinate;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "  v_TexCoordinate = vTexCoordinate;" +
            "}"

        private val fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D u_Texture;" +
            "varying vec2 v_TexCoordinate;" +
            "void main() {" +
            "  gl_FragColor = texture2D(u_Texture, v_TexCoordinate);" +
            "}"

        private var programId = 0
        private var positionHandle = 0
        private var texCoordHandle = 0
        private var textureHandle = 0

        // Full screen quad
        private val vertexBuffer = java.nio.ByteBuffer.allocateDirect(8 * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(floatArrayOf(
                    -1f, -1f,  // Bottom Left
                     1f, -1f,  // Bottom Right
                    -1f,  1f,  // Top Left
                     1f,  1f   // Top Right
                ))
                position(0)
            }

        // Texture coordinates (flipped vertically for Android Bitmap vs OpenGL)
        private val texBuffer = java.nio.ByteBuffer.allocateDirect(8 * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(floatArrayOf(
                    0f, 1f,
                    1f, 1f,
                    0f, 0f,
                    1f, 0f
                ))
                position(0)
            }

        fun surfaceCreated() {
            val endpointInt = IntArray(1)
            val vertexShader = loadShader(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

            programId = android.opengl.GLES20.glCreateProgram()
            android.opengl.GLES20.glAttachShader(programId, vertexShader)
            android.opengl.GLES20.glAttachShader(programId, fragmentShader)
            android.opengl.GLES20.glLinkProgram(programId)
            
            positionHandle = android.opengl.GLES20.glGetAttribLocation(programId, "vPosition")
            texCoordHandle = android.opengl.GLES20.glGetAttribLocation(programId, "vTexCoordinate")
            textureHandle = android.opengl.GLES20.glGetUniformLocation(programId, "u_Texture")
        }

        fun loadTexture(bitmap: Bitmap): Int {
            val textureIds = IntArray(1)
            android.opengl.GLES20.glGenTextures(1, textureIds, 0)
            val textureId = textureIds[0]

            android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, textureId)
            android.opengl.GLES20.glTexParameterf(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MIN_FILTER, android.opengl.GLES20.GL_LINEAR.toFloat())
            android.opengl.GLES20.glTexParameterf(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MAG_FILTER, android.opengl.GLES20.GL_LINEAR.toFloat())
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_WRAP_S, android.opengl.GLES20.GL_CLAMP_TO_EDGE)
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_WRAP_T, android.opengl.GLES20.GL_CLAMP_TO_EDGE)

            android.opengl.GLUtils.texImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            return textureId
        }

        fun draw(textureId: Int) {
            android.opengl.GLES20.glUseProgram(programId)

            android.opengl.GLES20.glEnableVertexAttribArray(positionHandle)
            android.opengl.GLES20.glVertexAttribPointer(positionHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 8, vertexBuffer)

            android.opengl.GLES20.glEnableVertexAttribArray(texCoordHandle)
            android.opengl.GLES20.glVertexAttribPointer(texCoordHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 8, texBuffer)

            android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE0)
            android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, textureId)
            android.opengl.GLES20.glUniform1i(textureHandle, 0)

            android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4)

            android.opengl.GLES20.glDisableVertexAttribArray(positionHandle)
            android.opengl.GLES20.glDisableVertexAttribArray(texCoordHandle)
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = android.opengl.GLES20.glCreateShader(type)
            android.opengl.GLES20.glShaderSource(shader, shaderCode)
            android.opengl.GLES20.glCompileShader(shader)
            return shader
        }

        fun cleanup() {
            if (programId != 0) {
                android.opengl.GLES20.glDeleteProgram(programId)
                programId = 0
            }
        }
    }
    
    companion object {
        private const val TAG = "VideoSynthesizer"
        private const val TIMEOUT_US = 10000L
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }
}
