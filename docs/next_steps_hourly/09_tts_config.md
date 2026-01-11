# Hour 9 - TTS configuration for demo

Goal:
Enable higher-quality narration while keeping demo reliability.

Files:
- `local.properties`
- `android/app/build.gradle.kts` (already reads `GOOGLE_CLOUD_KEY`)

Steps:
- Add `GOOGLE_CLOUD_KEY` to `local.properties`.
- Enable Google Cloud Text-to-Speech API for that key and confirm billing.
- Run a short TTS call to verify audio output is generated.
- Pre-generate narration audio for the demo session if possible.

Verification:
- A short test prompt produces an MP3 in cache.
- Replay generation uses the narration audio without errors.

Push:
- Message suggestion: "document TTS demo configuration"

