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
        headline = "TRAGEDY AT THE ARKHAM SANITARIUM: UNSPEAKABLE HORRORS REVEALED",
        summary = "A local journalist uncovers the macabre truth behind the strange lights and rhythmic chanting echoing from the archives.",
        atmosphere = "Thick, damp fog clings to the cold stone walls. The air is heavy with the scent of old paper and something... ancient. A low, rhythmic thumping vibrates through the floorboards.",
        article = "ARKHAM, OCTOBER 1923 — What began as a routine investigation into missing hospital records has spiraled into a nightmare beyond human comprehension. Last night, local authorities were called to the Miskatonic Sanitarium after neighbors reported 'agonizing screams' and a pulsating violet glow emanating from the basement archives. \n\nUpon entering the scene, it was clear that no normal medical practice had occurred. The walls were scrawled with symbols that defy any known alphabet, and the air pulsed with a vibration that seemed to curdle the very blood in one's veins. Detective Murphy, the first on the scene, remains in a state of catatonic shock. Whatever took place in those depths has left a permanent scar on our fair city. The archives remain sealed until further notice.",
        verdict = "A MALIGNANT PRESENCE REMAINS. THE TRUTH IS BURIED DEEP.",
        imagePath = "android.resource://com.hackathon.afterlog/drawable/sample_evidence",
        timeline = listOf(
            TimelineEvent("00:12", "The Journalist", "Entry into Sanitarium", "The front door creaked open. The archive basement beckoned."),
            TimelineEvent("00:45", "The Orderly", "Nervous Whispers", "Claims he heard 'the singing' from the walls again. Eyes were bloodshot."),
            TimelineEvent("01:30", "Unknown", "Acoustic Spike", "A high-pitched, inhuman shriek echoed through the main hall.", 98),
            TimelineEvent("02:15", "The Detective", "Discovery of Symbols", "Glowing ink found on the floor. It smells like sea salt and rot."),
            TimelineEvent("03:00", "The Witness", "Final Sighting", "It wasn't a man. It had too many... angles.")
        )
    )

    val terrorInArchives = realDataSimulation
    val sampleMediaLogs = emptyList<MediaLogEntity>()
}
