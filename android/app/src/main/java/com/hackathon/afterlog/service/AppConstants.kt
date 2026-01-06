package com.hackathon.afterlog.service

object AppConstants {
    object Audio {
        const val SCREAM_THRESHOLD_DB = 80.0
        const val SAMPLE_RATE = 16000 // 16kHz
        const val BUFFER_SIZE = 1024
        const val SCREAM_COOLDOWN_MS = 5000L
    }
    
    object Video {
        const val CHUNK_DURATION_MS = 30_000L // 30 seconds
        const val BUFFER_CAPACITY = 6
        const val ROLLING_BUFFER_DURATION_SEC = 150 // 2.5 minutes coverage
    }
    
    object Camera {
        const val TIMELAPSE_INTERVAL_MS = 5000L // 5 seconds
    }
}
