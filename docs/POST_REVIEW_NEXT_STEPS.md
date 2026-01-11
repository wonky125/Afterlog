# Afterlog - Post Review Next Steps

This file captures the minimal fixes and verification steps based on the
replay pipeline review. The goal is to maximize demo stability with minimal
code changes.

## MVP status
- Feature set matches the intended MVP (capture, highlight, report, replay).
- Demo stability depends on the critical fixes below.

## Must fix before demo (critical)
1) Persist non-highlight video chunks so the fallback replay path has data.
   - Files: `android/app/src/main/java/com/hackathon/afterlog/service/VideoManager.kt`,
     `android/app/src/main/java/com/hackathon/afterlog/data/local/FileManager.kt`
   - Change: copy finalized chunks to session media, log `VIDEO_CHUNK`.
2) Use device min buffer size for AudioRecord read/write loops.
   - File: `android/app/src/main/java/com/hackathon/afterlog/service/AudioMonitor.kt`
   - Change: store effective buffer size and use it for ByteArray/read.
3) Avoid deleting temp chunks before they are finalized/persisted.
   - File: `android/app/src/main/java/com/hackathon/afterlog/service/VideoManager.kt`
   - Change: delay cleanup or only delete after copy/log completes.

## High priority next
4) Stabilize rotation during recording to prevent mixed-orientation clips.
   - File: `android/app/src/main/java/com/hackathon/afterlog/service/CameraUseCaseManager.kt`
   - Change: freeze target rotation for session or lock orientation in UI.
5) Subtitle burn-in safety for varied aspect ratios.
   - Files: `android/app/src/main/java/com/hackathon/afterlog/data/media/VideoStitcher.kt`,
     `android/app/src/main/java/com/hackathon/afterlog/domain/MediaPipelineUseCase.kt`
   - Change: make burn-in optional for demo or adjust overlay anchors.
6) Default ROI guide when none is provided.
   - File: `android/app/src/main/java/com/hackathon/afterlog/service/AfterLogService.kt`
   - Change: fall back to last saved guide or `PerspectiveGuideConfig.default()`.

## Suggested minimal patch order
1) Persist non-highlight chunks + safe cleanup.
2) Audio buffer sizing fix.
3) Rotation handling (freeze or lock).
4) Subtitle burn-in fallback.
5) ROI guide default.

## Quick verification checklist (device)
- Start recording, wait >1 minute, confirm `VIDEO_CHUNK` logs exist.
- Trigger a scream event, confirm highlight clips are saved.
- Generate replay, confirm output video plays with audio.
- If burn-in enabled, check captions appear and are on-screen.
- Rotate device during recording (if allowed) and validate replay orientation.

## Demo configuration notes
- For higher-quality narration, set `GOOGLE_CLOUD_KEY` in `local.properties`.
- Ensure the Google Cloud Text-to-Speech API is enabled for that key.
- For reliability, consider pre-generating narration audio before the demo.

## Gradle wrapper recovery (CI/CLI)
From `android/`:
```
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
```
Commit:
- `android/gradlew`
- `android/gradlew.bat`
- `android/gradle/wrapper/gradle-wrapper.jar`

## Submission checklist
- Devpost requirements summary: `docs/DEVPOST_SUBMISSION_CHECKLIST.md`

## Demo video script (2:45 target, <= 3:00)
Use English VO or add English subtitles if VO is not English.

0:00-0:08 Hook
- Screen: board game reaction + 1s highlight cut
- Overlay: "Cinematic replay for board games"
- VO(EN): "If board games had a replay button, this is it."

0:08-0:25 Problem -> Solution
- Screen: board is packed away, memories fade
- VO(EN): "Board game nights are unforgettable until the board folds. Afterlog captures those moments and turns them into a cinematic replay."

0:25-0:45 Start Recording
- Screen: app home -> start session -> recording active
- Overlay: "Camera + Mic capture"
- VO(EN): "We record the board with CameraX and the room audio in real time."

0:45-1:05 Highlight Trigger
- Screen: loud reaction / scream moment, highlight saved
- Overlay: "Scream detected -> Highlight saved"
- VO(EN): "When excitement spikes, Afterlog marks a highlight and keeps the surrounding context."

1:05-1:30 Gemini Analysis
- Screen: analysis/loading -> noir report appears
- Overlay: "Gemini 3 multimodal analysis"
- VO(EN): "Gemini 3 analyzes video frames and audio to generate a noir-style investigative report."

1:30-2:00 Replay Generation
- Screen: replay render -> highlight video playback
- Overlay: "Media3 stitching + TTS narration"
- VO(EN): "We stitch the best moments, add narration, and generate a shareable cinematic replay."

2:00-2:20 Captions + Highlights
- Screen: replay with captions visible, highlight segment plays
- Overlay: "Auto-generated captions"
- VO(EN): "Auto captions and highlights make the replay easy to follow."

2:20-2:40 Why it matters
- Screen: friends reacting + replay highlights
- VO(EN): "Afterlog preserves the analog magic so your tabletop stories live on."

2:40-2:45 Close
- Screen: app logo/title
- VO(EN): "Afterlog. Cinematic replay for board games."

## Post-demo (optional) - Advanced effects prompts
These are suitable for a later phase (not required for MVP stability).
Feed them one by one to an AI tool.

### Phase 0 - Context setting
```
I am developing an Android app called "Afterlog" using Kotlin and Jetpack Compose.
The app creates cinematic highlights from board game sessions.
I am using androidx.media3:media3-transformer to process videos.
I need to implement advanced video editing effects (Camera Shake, Zoom, Filters)
and Audio Ducking programmatically. Please generate code compatible with the
latest Media3 Transformer API.
```

### Feature 1 - Camera shake and dynamic zoom
```
I need to implement a "Camera Shake" and "Dynamic Zoom" effect using Android
Media3 Transformer.

Create a custom class named CameraMovementEffect that implements
MatrixTransformation.

It should accept parameters: startTimeMs, endTimeMs, shakeIntensity, and
zoomTarget (x, y coordinates).

Inside the getMatrix method:
- For Shake: apply random small translations (x, y) based on the current
  timestamp to simulate handheld vibration during the specified duration.
- For Zoom: smoothly scale up the matrix (Zoom In) toward zoomTarget using
  linear interpolation over time.

Please provide the full Kotlin class implementation.
```

### Feature 2 - Noir horror filter (vignette + noise)
```
I want to apply a "Noir Horror" visual filter to my video using Media3
Transformer.

Create a custom GlEffect implementation that uses a GLSL fragment shader.

The shader should:
- Vignette: darken the corners of the video frame.
- Film grain/noise: add dynamic random noise to simulate old film texture.

Provide both the GLSL code string and the Kotlin wrapper class to apply this
effect in the Transformer pipeline.
```

### Feature 3 - Audio ducking
```
I am composing a video using Media3 Transformer.Composition. I have two audio
tracks: Background Music (BGM) and TTS narration (Voice).

I need to implement audio ducking. Please write a function that configures the
EditedMediaItem for the BGM track. It should reduce the BGM volume to 0.3 during
the timestamps where the TTS track is playing, and return to 1.0 when the TTS is
silent. Show how to use VolumeProcessor or RampedAudioProcessor to achieve this
volume automation based on a list of TTS timestamps.
```

### Feature 4 - Freeze frame
```
I need to create a "Freeze Frame" effect for a specific moment in the video
(e.g., at a scream peak).

Using Media3 Transformer or a custom editing logic:
How can I take a 1-second video segment at a specific timestamp and repeat its
last frame for 2 seconds to create a pause effect?

Please show me how to sequence these MediaItems in a Composition so the video
plays normally, pauses (freezes), and then resumes.
```
