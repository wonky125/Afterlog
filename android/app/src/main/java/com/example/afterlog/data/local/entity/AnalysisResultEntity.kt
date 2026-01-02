package com.example.afterlog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
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
    ]
)
data class AnalysisResultEntity(
    @PrimaryKey
    val sessionId: String,
    val jsonContent: String, // Gemini Response JSON
    val summary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
