# Hour 5 - Subtitle burn-in safety

Goal:
Avoid burn-in failures and off-screen captions on varied aspect ratios.

Files:
- `android/app/src/main/java/com/hackathon/afterlog/domain/MediaPipelineUseCase.kt`
- `android/app/src/main/java/com/hackathon/afterlog/data/media/VideoStitcher.kt`

Steps:
- Add a simple flag to skip burn-in for demo builds (use SRT playback).
- Optionally adjust overlay anchors to a safer bottom margin.

Verification:
- Generate replay with subtitles on a real device.
- Confirm captions are visible and export succeeds.

Push:
- Message suggestion: "guard subtitle burn-in for demo stability"

