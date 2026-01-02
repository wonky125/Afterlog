package com.example.afterlog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaType {
    IMAGE, // Time-lapse capture
    AUDIO_RAW, // Full audio recording reference
    SCREAM_EVENT // 80dB+ Trigger Event
}

@Entity(
    tableName = "media_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Indexes to speed up queries by SessionID and Timestamp
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "timestamp"])
    ]
)
data class MediaLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val type: MediaType,
    val filePath: String? = null, // Can be null for SCREAM_EVENT markers without separate files
    val timestamp: Long = System.currentTimeMillis(),
    val decibel: Int? = null,
    val isSynced: Boolean = false
)
