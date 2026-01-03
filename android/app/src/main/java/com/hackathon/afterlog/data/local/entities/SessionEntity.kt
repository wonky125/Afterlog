package com.hackathon.afterlog.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // ì´ˆë? ì½”ë“œ ê²¸ìš©
    val startTime: Long,
    val endTime: Long? = null,
    val title: String? = null // ?˜ì¤‘??AIê°€ ì§€?´ì? ?œëª©
)
