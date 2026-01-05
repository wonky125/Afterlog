# Afterlog (Cinematic Replay) 🎞️

> **"If board games had a 'Replay' feature..."**
>
> *Transforming fleeting screams and moments into an eternal cinematic horror movie using Gemini 3 Pro.*

**Afterlog** is an Android application designed for the "Google Gemini 3.0" Hackathon. It utilizes multimodal AI (Vision + Audio) to capture, analyze, and reenact tabletop game sessions as a cinematic "Newspaper Report" or highlight video.

---

### 1. Project Overview & Deadline 🗓️

*   **Project Name**: Afterlog
*   **Core Concept**: Captures audio (screams) and vision (board state) during tabletop games to generate a cinematic highlight log tailored to the game's world.
*   **Core Engine Versatility**: While this demo showcases a **1920s Noir Mystery** theme, the core engine is genre-agnostic. By simply swapping the System Persona and UI Skin, Afterlog can transform into:
    *   🧙‍♂️ **Fantasy RPM**: A Bard rewriting your D&D session into an epic ballad.
    *   🔍 **Mystery (Current Demo)**: A Private Eye compiling evidence for a murder mystery.
    *   🚀 **Space Horror**: A Station AI decrypting black box data.
*   **Hackathon Deadline**: Feb 9, 2026, 5:00 PM (PST).
*   **Deliverables**:
    *   New Android App
    *   Demo Video (< 3 min)
    *   Public Code Repository (GitHub)

### 2. Concept: "What if Board Games bad a 'Replay'?"

> "In video games like LoL, you can watch a replay of your 'God Play' moments. But in board games? You laugh for 3 hours, fold the board, and the memories fade. We wanted to fill this gap with AI."

To achieve this, we need to understand both the **visual changes of the board** and the **voices (screams) of the players** simultaneously. With the arrival of **Gemini 3 Pro**'s overwhelming multimodal capabilities, this is finally possible. **[Afterlog]** preserves the fleeting analog experience as an eternal digital 'Cinematic Replay'.

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
