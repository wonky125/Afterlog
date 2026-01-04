package com.hackathon.afterlog.feature.report.data

import com.hackathon.afterlog.data.local.entities.MediaLogEntity
import com.hackathon.afterlog.data.model.GeminiReport
import com.hackathon.afterlog.data.model.TimelineEvent

object MockReportData {
    // ------------------------------------------------------------------------
    // REAL DATA SIMULATION
    // Based on user provided 'gemini_audio_result.json' and 'sample_evidence.jpg'
    // ------------------------------------------------------------------------
    val realDataSimulation = GeminiReport(
        headline = "ARKHAM MANOR PLUNGED INTO SHADOW: Heiress Vanishes Amidst Whispers and Winged Horrors!",
        summary = "Brave investigators delve into a haunted Arkham manor, confronting unseen terrors and blasphemous whispers, only to uncover a deeper mystery surrounding a missing heiress and ancient family secrets, as monstrous creatures lurk in the dark.",
        atmosphere = "The air hangs heavy with the scent of old dust and unspeakable dread. Shadows dance with every flicker of candlelight, hiding secrets that claw at the edges of sanity. A pervasive sense of cosmic unease clings to the decaying grandeur of the manor, where every creak and whisper hints at the thin veil between our world and something far more ancient and terrifying. It's a dark night in Arkham, and the truth is a twisted thing.",
        article = "The grim curtain of night has fallen once more upon Arkham, but within the decaying walls of a forgotten manor, something truly disturbing has unfurled. Our intrepid, if perhaps foolhardy, investigators—anonymous heroes in a city teeming with shadows—braved dusty corridors only to stumble upon a hidden safe. Inside, amidst forgotten currencies and peculiar cards, lay a golden key—a symbol, perhaps, of some deeper, locked secret. Yet, their delve into the manor's mysteries was swiftly interrupted.\n\nNo sooner had they gathered their bearings than the house itself seemed to recoil. A chilling, unearthly cry rent the air, ushering forth a monstrous 'feathered serpent'—a creature of nightmare, thankfully small but no less deadly. As one investigator wrestled with this serpentine horror, another was assailed by disembodied whispers, his mind battling blasphemies that sought to unravel his very sanity. But the true enigma emerged when a wounded woman, gasping and bleeding, was discovered. Her story was a fractured tale of an ambush, of 'brutes' and a missing Lady Helena—the very heiress whose help our investigators had been summoned to provide.\n\nLady Helena, it seems, had foreseen dark tides, planning an escape from the manor she claimed 'felt a little off.' As the wounded survivor made her panicked exit, leaving our heroes to face the growing gloom, further investigation yielded a cryptic tome of regional folklore. Hidden within its brittle pages was a legend of the Helena family: a treacherous pact, a fairy, and a stone statue granting 'unspecified powers.' The pieces of this macabre puzzle are falling into place, revealing a tale far older and more sinister than mere robbery. The shadows deepen, Arkham, and Lady Helena's fate remains chillingly uncertain.",
        verdict = "Arkham must brace itself; the true horrors of this manor are only just beginning to unfurl their terrible wings.",
        imagePath = "android.resource://com.hackathon.afterlog/drawable/sample_evidence",
        timeline = listOf(
            TimelineEvent("00:00", "Narrator", "Safe Discovered", "An investigator opens a safe, revealing a Golden Key, old currencies, and 'strange thin cards'."),
            TimelineEvent("00:16", "Female Player 1", "Strategic Hold", "An investigator decides to stay put, planning to unlock a door on the next turn."),
            TimelineEvent("00:45", "Narrator", "Mythos Phase Begins", "A 'terrible creature' cries out, signaling the spawn of a 'feathered serpent' in the manor."),
            TimelineEvent("01:19", "Narrator", "Psychic Assault", "A 'disembodied voice' whispers 'blasphemies' directly into the ear of the investigator with the highest Lore, who successfully resists the mental attack."),
            TimelineEvent("01:53", "Narrator", "Serpent's Hunt", "The feathered serpent actively seeks out its prey, moving towards and targeting one of the investigators."),
            TimelineEvent("02:14", "Narrator", "Willpower Test", "The feathered serpent coils, having caught 'small prey,' but the targeted investigator successfully maintains composure despite the gruesome sight."),
            TimelineEvent("02:32", "Female Player 1", "Rescue Mission", "An investigator prepares to save a 'wounded lady' discovered within the manor's confines."),
            TimelineEvent("02:46", "Wounded Lady (NPC)", "Wounded Testimony", "The injured lady, bleeding from a cheek wound, recounts being ambushed by a 'brute' and reveals that Lady Helena, the target of the investigation, has vanished after ordering the manor closed."),
            TimelineEvent("03:21", "Wounded Lady (NPC)", "Strange Sightings", "The wounded lady describes encountering bizarrely dressed individuals, one of whom carried a peculiar 'tiny metal contraption... a camera,' before Lady Helena's disappearance."),
            TimelineEvent("03:34", "Wounded Lady (NPC)", "Helena's Premonition", "The wounded lady implies Lady Helena sensed something was 'a little off' about the manor and planned to leave, hinting at foreknowledge of impending danger before making her own panicked escape."),
            TimelineEvent("04:08", "Male Player", "Puzzle Solved", "An investigator opts to solve a complex puzzle, successfully uncovering a hidden tome."),
            TimelineEvent("04:29", "Narrator", "Ancient Lore Unveiled", "The discovered tome is identified as regional folklore, detailing a legend about Lady Helena's family, a fae creature, and a 'small stone statue' bestowing 'unspecified powers'."),
            TimelineEvent("04:53", "Narrator", "New Clues", "The investigators gain vital clues from the ancient folklore, suggesting a deeper, more supernatural mystery at play.")
        )
    )

    // Standard Mock Data for Previews
    val terrorInArchives = GeminiReport(
        headline = "Terror in the Archives: A Night of Unspeakable Horror",
        summary = "Our brave investigators ventured into the haunted library, only to discover horrors beyond imagination lurking in the shadows.",
        imagePath = "android.resource://com.hackathon.afterlog/drawable/sample_evidence",
        article = "The evening began innocuously enough, with our party of four entering the estate's grand library. Little did they know that ancient evil awaited...",
        atmosphere = "Tense, claustrophobic, with moments of sheer panic.",
        verdict = "INVESTIGATION FAILED - Madness consumed all.",
        timeline = listOf(
            TimelineEvent(
                timestamp = "00:15:32",
                speaker = "Speaker A",
                event = "First Encounter",
                description = "A strange noise echoed from the basement.",
                decibel = 85,
                imagePath = "android.resource://com.hackathon.afterlog/drawable/sample_evidence"
            ),
            TimelineEvent(
                timestamp = "00:42:17",
                speaker = "Speaker B",
                event = "The Revelation",
                description = "Ancient texts revealed the entity's true name.",
                decibel = null
            )
        )
    )

    val sampleMediaLogs = emptyList<MediaLogEntity>()
}
