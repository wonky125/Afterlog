package com.hackathon.afterlog.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long? = null,
    val title: String? = null,
    @ColumnInfo(name = "perspectiveGuideJson") val perspectiveGuideJson: String? = null
)
