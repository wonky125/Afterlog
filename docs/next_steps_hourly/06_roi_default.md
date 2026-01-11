# Hour 6 - Default ROI guide

Goal:
Ensure motion-based highlights can trigger even when no guide is provided.

Files:
- `android/app/src/main/java/com/hackathon/afterlog/service/AfterLogService.kt`
- `android/app/src/main/java/com/hackathon/afterlog/service/VideoManager.kt`

Steps:
- If no guide is passed, load the last saved guide or use default.
- Apply the guide before starting the recording loop.

Verification:
- Start a session without a guide.
- Confirm ROI motion highlights are still possible.

Push:
- Message suggestion: "default ROI guide when none provided"

