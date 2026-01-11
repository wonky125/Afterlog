# Hour 2 - Safe temp cleanup after finalize

Goal:
Avoid deleting temp chunks before they are finalized and copied.

Files:
- `android/app/src/main/java/com/hackathon/afterlog/service/VideoManager.kt`

Steps:
- Track pending finalizations (increment on Start, decrement on Finalize).
- In `stopRecording`, defer `clearTempFiles()` until pending == 0.
- Keep the rolling buffer in memory, but do not delete in-use files.

Verification:
- Start/stop recording quickly.
- Confirm no "file not found" errors during finalize or copy.

Push:
- Message suggestion: "defer temp cleanup until video finalize completes"

