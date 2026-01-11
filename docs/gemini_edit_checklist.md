# Gemini Edit Verification Checklist

Use this checklist to verify the Gemini-driven edit path is working end-to-end on a real device.

## A. Build & Runtime Sanity
- [ ] App builds with no compile errors (MediaType.IMAGE removal should not break anything).
- [ ] App runs and starts a new session without crashes.

## B. Recording & Storage
- [ ] New session creates `session_media/chunk_{sessionId}_{timestamp}.mp4` files.
- [ ] `VIDEO_CHUNK` logs exist in DB for those files (timestamp matches file name).
- [ ] `audio_{sessionId}.pcm` file exists and has non-zero size.
- [ ] Starting a new session deletes the previous session’s files/logs (expected test-only behavior).

## C. Timeline Alignment
- [ ] Replay generation logs show `Timeline alignment OK` OR `Timeline drift warning`.
- [ ] If warning appears, record the drift (ms) for later tuning.

## D. Gemini Response
- [ ] `Gemini highlight segments: N` is logged and `N > 0` in a normal session.
- [ ] `Gemini highlight windows (sec)` log shows the start/end list in seconds.
- [ ] Gemini JSON contains `highlight_segments` with start/end seconds.
- [ ] Narration text extracted is not blank.

## E. Edit Construction
- [ ] Trimmed clip files are created for Gemini segments (`replay_{sessionId}_gemini_...`).
- [ ] `Gemini match` logs show clip start/end and source file names.
- [ ] Final replay video is produced and exists on disk.
- [ ] Replay duration is <= 4 minutes after final trim.

## F. Playback Spot Check
- [ ] Replay plays without errors in the player.
- [ ] Selected moments feel aligned with the spoken/interesting parts.

## G. Fallback Behavior
- [ ] If Gemini returns no highlights, scream/motion highlight fallback still builds a replay.

## Notes
- If `VIDEO_CHUNK` files are missing, Gemini timestamps cannot map to source clips.
- If `audio_{sessionId}.pcm` is missing, both highlight selection and captions may degrade.
