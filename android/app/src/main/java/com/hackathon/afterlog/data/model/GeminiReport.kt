package com.hackathon.afterlog.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiReport(
    val headline: String,
    val summary: String,
    val atmosphere: String = "",
    val article: String = "",         // Main narrative prose (2-3 paragraphs)
    val timeline: List<TimelineEvent>,
    val verdict: String,
    @SerialName("highlight_segments")
    val highlightSegments: List<HighlightSegment> = emptyList(),
    val imagePath: String? = null     // Path to the main evidence image
)

@Serializable
data class TimelineEvent(
    val timestamp: String,        // "00:02:15"
    val speaker: String,          // "Investigator A (Female)"
    val event: String,            // "Door creaks open"
    val description: String,      // Detailed noir description
    val decibel: Int? = null,     // Optional
    val imagePath: String? = null // Path to specific event image
)
