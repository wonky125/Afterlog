import google.generativeai as genai
import os
import time
import json
import re

# 1. Load API Key from local.properties
def load_api_key():
    try:
        with open("android/local.properties", "r") as f:
            for line in f:
                if line.startswith("GEMINI_API_KEY"):
                    return line.split("=")[1].strip()
    except Exception as e:
        print(f"Error loading API key: {e}")
        return None

API_KEY = load_api_key()
if not API_KEY:
    print("API Key not found in android/local.properties")
    sys.exit(1)

# FORCE UTF-8 for Windows Console
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

genai.configure(api_key=API_KEY)

# 2. Upload File (Video)
video_path = "test_5min.mp4"
print(f"Uploading {video_path}...")

try:
    video_file = genai.upload_file(video_path)
    print(f"Upload complete: {video_file.uri}")
except Exception as e:
    print(f"Upload failed: {e}")
    sys.exit(1)

# 3. Poll for State
while video_file.state.name == "PROCESSING":
    print("Processing video...")
    time.sleep(5)
    video_file = genai.get_file(video_file.name)

if video_file.state.name == "FAILED":
    print("Video processing failed.")
    sys.exit(1)

print("Video is ACTIVE. Generating content...")

# 4. Generate Content with Prompt
model = genai.GenerativeModel(model_name="gemini-1.5-flash") 
# Note: switched to 1.5-flash for better quota limits

prompt = """
# PERSONA
You are a hard-boiled investigative journalist working for the Arkham Chronicle in the 1920s. 
Your writing style is noir—sharp, atmospheric, and dripping with cynical wit.

# TASK
Analyze the provided VIDEO to reconstruct a tabletop game session report.

# RULES
1. **Cross-Validation**: Match audio events (screams, gasps, dialogue) to visual changes.
2. **No Hallucination**: If unclear, state "Unidentified".
3. **Speaker Identification**: Label speakers.
4. **Timestamps**: MM:SS format.
5. **Noir Atmosphere**: Use evocative language.

# OUTPUT FORMAT
Respond with ONLY valid JSON.

{
  "headline": "ALL CAPS SENSATIONAL TITLE (max 60 chars)",
  "summary": "One-sentence hook (max 120 chars)",
  "atmosphere": "Scene-setting description (max 200 chars)",
  "article": "2-3 paragraphs of narrative journalism. Tell the story of what happened during this game session as if writing for the Arkham Chronicle. Use vivid prose, dramatic pacing, and noir atmosphere. (400-600 words)",
  "timeline": [
    {
      "timestamp": "MM:SS",
      "speaker": "Speaker",
      "event": "Event Title",
      "description": "Description",
      "decibel": 85
    }
  ],
  "verdict": "Final deduction"
}
"""

try:
    response = model.generate_content([video_file, prompt])
    print("\n--- GEMINI RESPONSE ---\n")
    print(response.text)
    
    # Save to file
    with open("gemini_result.json", "w", encoding="utf-8") as f:
        f.write(response.text)
        
except Exception as e:
    print(f"Generation failed: {e}")
