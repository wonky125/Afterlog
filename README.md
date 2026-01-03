# Afterlog (Cinematic Replay) 🎞️

> **"If board games had a 'Replay' feature..."**
>
> *Transforming fleeting screams and moments into an eternal cinematic horror movie using Gemini 3 Pro.*

**Afterlog** is an Android application designed for the "Google Gemini 3.0" Hackathon. It utilizes multimodal AI (Vision + Audio) to capture, analyze, and reenact tabletop game sessions as a cinematic "Newspaper Report" or highlight video.

---

## 💡 Key Features

### 1. Intelligent Capture (The Eyes & Ears)
- **Scream Detection**: Automatically triggers recording when audio levels exceed **80dB** (e.g., screams of terror or joy).
- **Time-Lapse Vision**: Silently captures the board state every 5 seconds in the background.
- **Privacy-First**: Uses on-device pre-processing to filter non-relevant data.

### 2. Gemini 3.0 Pro Analysis (The Brain)
- **Multimodal Understanding**: Analyzes hours of audio and photo sequences to understand the "context" of the game.
- **Narrative Generation**: Reconstructs the chaotic game session into a coherent story, written in the persona of a **1920s Investigative Journalist**.
- **Speaker Diarization**: Identifies who screamed and why, using Gemini's native audio processing capabilities.

### 3. Cinematic Replay (The Experience)
- **Newspaper View**: Presents the game log as a vintage immersive newspaper article.
- **Hybrid Video Synthesis**: Merges real video clips, time-lapse photos, and AI-generated narration (TTS) into a final MP4 video.
- **Sync-Play**: Click on any "investigation log" in the report to instantly jump to that moment in the replay video.

---

## 🛠️ Tech Stack

| Category | Technology | Usage |
| :--- | :--- | :--- |
| **Mobile** | **Android (Kotlin)** | Jetpack Compose, Material 3, Hilt |
| **AI Model** | **Development**: `gemini-2.5-flash` <br> **Production**: `gemini-3-pro-preview` | Multimodal analysis (Long Context Window) |
| **Vision** | **CameraX** | Background time-lapse & video recording |
| **Audio** | **AudioRecord** | Real-time decibel metering & PCM buffer management |
| **Database** | **Room** | Offline-first local data persistence |
| **Media** | **ExoPlayer** | Seamless video playback & sync |
| **TTS** | **Google Cloud TTS** | Generating narration for accessibility |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana or later
- Android Device (Min SDK 26) running Android 10+
- **Google AI Studio API Key**

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/afterlog.git
   ```
2. Create `local.properties` in the root directory and add your key:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
3. Open the project in Android Studio and sync Gradle.
4. Run on a physical device (Camera/Mic features may not work on emulators).

---

## 📂 Project Structure
- `app/src/main/java/com/afterlog/app`
  - `di`: Hilt modules
  - `domain`: UseCases and Repository interfaces
  - `data`: Room DB, Gemini Client, Forensic Service
  - `ui`: Jetpack Compose screens (NewspaperView, etc.)
- `docs/`: Detailed implementation plans and roadmap.

---

## 📜 License
This project is submitted for the Google Gemini Hackathon 2026.
Codes are available under the MIT License.
