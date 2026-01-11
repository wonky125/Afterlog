# Hour 4 - Stabilize recording rotation

Goal:
Prevent mixed-orientation clips during long sessions.

Files:
- `android/app/src/main/java/com/hackathon/afterlog/service/CameraUseCaseManager.kt`
- `android/app/src/main/java/com/hackathon/afterlog/service/AfterLogService.kt`

Steps:
- Add `lockRotation()` and `unlockRotation()` in CameraUseCaseManager.
- When recording starts, lock to current rotation and disable updates.
- When recording stops, re-enable orientation updates.

Verification:
- Start recording, rotate device, stop.
- Playback should not be rotated or cropped across clips.

Push:
- Message suggestion: "lock camera rotation during recording"

