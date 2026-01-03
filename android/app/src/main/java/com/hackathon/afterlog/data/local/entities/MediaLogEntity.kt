package com.hackathon.afterlog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index(value = ["sessionId"])]
)
data class MediaLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // FK -> Session
    val type: MediaType, // IMAGE, AUDIO_CHUNK, SCREAM_EVENT
    val filePath: String, // ë¡œì»¬ ?ˆë? ê²½ë¡œ
    val timestamp: Long, // NTP ë³´ì •???œê°„
    val decibel: Int? = null, // ?¤ë””?¤ì¸ ê²½ìš°
    val isSynced: Boolean = false // ?œë²„ ?…ë¡œ???¬ë?
)
