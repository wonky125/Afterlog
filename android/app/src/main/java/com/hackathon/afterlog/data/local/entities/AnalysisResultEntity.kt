package com.hackathon.afterlog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analysis_results",
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
data class AnalysisResultEntity(
    @PrimaryKey val sessionId: String,
    val jsonContent: String, // Gemini Response JSON Raw String
    val summary: String,
    val createdAt: Long
)
