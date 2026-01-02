package com.example.afterlog.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // 초대 코드 겸용
    val startTime: Long,
    val endTime: Long? = null,
    val title: String? = null // 나중에 AI가 지어준 제목
)
