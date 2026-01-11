package com.hackathon.afterlog.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HighlightSegment(
    @SerialName("start_sec") val startSec: Double,
    @SerialName("end_sec") val endSec: Double,
    val reason: String = ""
)
