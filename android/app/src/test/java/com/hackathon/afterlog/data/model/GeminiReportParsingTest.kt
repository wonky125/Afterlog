package com.hackathon.afterlog.data.model

import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.data.model.TimelineEvent
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*

class GeminiReportParsingTest {
    
    // Exact same configuration as in ViewModel
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true 
    }
    
    @Test
    fun `valid JSON parsing success`() {
        val validJson = """
            {
              "headline": "SHADOWS IN THE HALLWAY",
              "summary": "A chilling investigation into the unknown.",
              "atmosphere": "Dark, damp, and full of whispers.",
              "article": "The night began as all nights do in Arkham—with the creaking of old floorboards and the distant howl of something not quite human. Four investigators gathered, their faces grim.\n\nBy midnight, the truth had revealed itself in blood and shadow.",
              "timeline": [
                {
                  "timestamp": "00:01:30",
                  "speaker": "Speaker A (Male)",
                  "event": "Footsteps heard",
                  "description": "Heavy boots on wooden floor.",
                  "decibel": 85
                }
              ],
              "verdict": "Unexplained phenomenon confirmed."
            }
        """.trimIndent()
        
        val report = json.decodeFromString<GeminiReport>(validJson)
        
        assertEquals("SHADOWS IN THE HALLWAY", report.headline)
        assertEquals("A chilling investigation into the unknown.", report.summary)
        assertEquals(1, report.timeline.size)
        assertEquals("00:01:30", report.timeline[0].timestamp)
        assertEquals(85, report.timeline[0].decibel)
    }
    
    @Test
    fun `missing optional fields still succeeds`() {
        // Timeline event without decibel
        val minimalJson = """
            {
              "headline": "MINIMAL REPORT",
              "summary": "Short summary.",
              "atmosphere": "Cold.",
              "article": "Nothing happened. The end.",
              "timeline": [
                {
                    "timestamp": "00:00:01",
                    "speaker": "Unknown",
                    "event": "Start",
                    "description": "Nothing yet"
                }
              ],
              "verdict": "Open"
            }
        """.trimIndent()
        
        val report = json.decodeFromString<GeminiReport>(minimalJson)
        assertNotNull(report)
        assertNull(report.timeline[0].decibel)
    }
}
