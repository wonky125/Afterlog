# Afterlog (The Midnight Chronicle) 🕵️‍♂️📰

> **"Every game leaves a trace. We just write it down."**
>
> *Transforming fleeting screams and tabletop chaos into an eternal noir investigation log using Gemini 3 Pro.*

**Afterlog** is a versatile Android application designed for the "Google Gemini 3.0" Hackathon. It utilizes multimodal AI (Vision + Audio) to capture, analyze, and reconstruct any immersive tabletop game session into a high-fidelity cinematic replay.

> [!NOTE]
> **Core Engine Versatility**: While this demo showcases a **1920s Noir Mystery** theme, the core engine is genre-agnostic. By simply swapping the System Persona and UI Skin, Afterlog can transform into:
> *   🧙‍♂️ **Fantasy RPM**: A Bard rewriting your D&D session into an epic ballad.
> *   🔍 **Mystery (Current Demo)**: A Private Eye compiling evidence for a murder mystery.
> *   🚀 **Space Horror**: A Station AI decrypting black box data.

---

## 💡 Key Features

### 1. Intelligent Capture (The Eyes & Ears)
- **Scream Detection**: Automatically triggers recording when audio levels exceed **80dB** (e.g., screams of terror or joy).
- **Time-Lapse Vision**: Silently captures the board state every 5 seconds in the background.
- **Privacy-First**: Uses on-device pre-processing to filter non-relevant data.

### 2. Gemini 3.0 Pro Analysis (The Brain)
- **Multimodal Understanding**: Analyzes audio and photo sequences to understand the "context" of the game.
- **Narrative Generation**: Reconstructs the chaotic game session into a coherent story, written in the persona of a **Hard-boiled Investigative Journalist**.
- **Context Awareness**: Identifies key game events and matches them with player reactions.

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

## 📂 Project Structure
- `app/src/main/java/com/hackathon/afterlog`
  - `di`: Hilt modules
  - `data`: Room DB, Gemini Client, Forensic Service
  - `ui`: Jetpack Compose screens (ReportDetailScreen, NewspaperView)
  - `service`: Background recording (AudioMonitor, VideoManager)
- `docs/`: Detailed implementation plans and roadmap.

---

## 📜 License
This project is submitted for the Google Gemini Hackathon 2026.
Codes are available under the MIT License.
