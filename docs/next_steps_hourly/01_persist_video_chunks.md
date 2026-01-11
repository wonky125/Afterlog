# Hour 1 - Persist non-highlight video chunks

Goal:
Ensure replay fallback has real video chunks even when no highlights exist.

Files:
- `android/app/src/main/java/com/hackathon/afterlog/data/local/FileManager.kt`
- `android/app/src/main/java/com/hackathon/afterlog/service/VideoManager.kt`

Steps:
- Add a FileManager helper like `getVideoChunkFile(sessionId, timestamp)`.
- On `VideoRecordEvent.Finalize`, copy the temp file to session media.
- Log `MediaType.VIDEO_CHUNK` for the persisted file (do not log duplicates).

Verification:
- Record > 60s on device.
- Confirm `VIDEO_CHUNK` logs exist and files are in `session_media`.

Push:
- Message suggestion: "persist video chunks for replay fallback"
