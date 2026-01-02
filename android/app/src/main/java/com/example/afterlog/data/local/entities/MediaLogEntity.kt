package com.example.afterlog.data.local.entities

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
    val filePath: String, // 로컬 절대 경로
    val timestamp: Long, // NTP 보정된 시간
    val decibel: Int? = null, // 오디오인 경우
    val isSynced: Boolean = false // 서버 업로드 여부
)
