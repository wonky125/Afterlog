package com.hackathon.afterlog.feature.report.data

import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.data.model.TimelineEvent

object MockReportData {
    // ------------------------------------------------------------------------
    // SPACE HORROR SIMULATION (Pivot 2.0)
    // Based on "Aegis-7 Station Incident"
    // ------------------------------------------------------------------------
    val realDataSimulation = GeminiReport(
        headline = "CRITICAL BREACH: STATION AEGIS-7 COMMUNICATIONS OFFLINE",
        summary = "Security logs recovered from the central mainframe indicate a total containment failure in Sector 4. Unknown biological entities detected.",
        atmosphere = "Cold, mechanical, echoing with the sound of failing life support systems. The scent of ozone and copper hangs in the recirculated air.",
        article = "EVENT LOG [RECOVERED]: At 0400 hours, Station Aegis-7 experienced a massive power surge originating from the bio-labs. Initial reports from Security Officer Vance described 'unidentified lifeforms' emerging from the ventilation shafts. By 0415, Sectors 1 through 3 were placed under automated quarantine. \n\nSurvival chances for the remaining crew are estimated at 0.04%. The main AI core has initiated the 'Scorched Earth' protocol, but manual override is required at the primary bridge. Any survivors must avoid all darkened corridors and prioritize oxygen preservation.",
        verdict = "STATION LOSS IMMINENT. NO SURVIVORS DETECTED.",
        imagePath = "android.resource://com.hackathon.afterlog/drawable/sample_evidence",
        timeline = listOf(
            TimelineEvent("04:00:12", "System AI", "Power Failure", "Substation 4 power grid offline. Backup generators at 40%."),
            TimelineEvent("04:05:30", "Officer Vance", "Hull Breach", "Movement detected in the maintenance shafts. Sending security team."),
            TimelineEvent("04:12:45", "Unknown", "Acoustic Spike", "High-frequency screeching recorded near the mess hall. Decibel peak detected.", 95),
            TimelineEvent("04:20:01", "System AI", "Life Support Warning", "Oxygen levels dropping in Sector 2. Containment seals failing."),
            TimelineEvent("04:35:10", "Survivor Lane", "Final Transmission", "They're in the vents. Don't open the airlock.")
        )
    )

    val terrorInArchives = realDataSimulation
    val sampleMediaLogs = emptyList<MediaLogEntity>()
}
