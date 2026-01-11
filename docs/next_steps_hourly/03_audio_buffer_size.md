# Hour 3 - AudioRecord buffer sizing fix

Goal:
Use the device min buffer size to reduce read errors and dropouts.

Files:
- `android/app/src/main/java/com/hackathon/afterlog/service/AudioMonitor.kt`

Steps:
- Store an `effectiveBufferSize` from `getMinBufferSize` (or min * 2).
- Allocate read/write buffers using `effectiveBufferSize`.
- Use the same size in mock mode to keep file sizes consistent.

Verification:
- Record for 1-2 minutes.
- Confirm no repeated read errors and audio file size grows steadily.

Push:
- Message suggestion: "use min buffer size for AudioRecord reads"

