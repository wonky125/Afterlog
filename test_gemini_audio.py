import google.generativeai as genai
import time
import sys
import io

# Force UTF-8 output
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Load API Key
try:
    with open("android/local.properties", "r") as f:
        api_key = None
        for line in f:
            if line.startswith("GEMINI_API_KEY"):
                api_key = line.split("=")[1].strip()
                break
        
    if api_key:
        genai.configure(api_key=api_key)
        print("API Key loaded.")
    else:
        print("GEMINI_API_KEY not found.")
        sys.exit(1)
except Exception as e:
    print(f"Error loading key: {e}")
    sys.exit(1)

# Upload Audio
audio_path = "test_5min.mp3"
print(f"Uploading {audio_path}...")

try:
    audio_file = genai.upload_file(audio_path)
    print(f"Upload initiated. URI: {audio_file.uri}")
except Exception as e:
    print(f"Upload failed: {e}")
    sys.exit(1)

# Poll State
while audio_file.state.name == "PROCESSING":
    print("Processing audio...")
    time.sleep(2)
    audio_file = genai.get_file(audio_file.name)

if audio_file.state.name == "FAILED":
    print("Audio processing failed.")
    sys.exit(1)

print("Audio is ACTIVE. Generating report...")

# Generate Content (User requested gemini-2.5-flash)
model = genai.GenerativeModel("gemini-2.5-flash")

prompt = """
# PERSONA
You are a hard-boiled investigative journalist working for the Arkham Chronicle in the 1920s. 

# TASK
Listen to the provided AUDIO recording of a tabletop game session.
Write a sensational news report about the events.

# OUTPUT FORMAT (JSON ONLY)
{
  "headline": "SENSATIONAL HEADLINE",
  "summary": "Short summary",
  "atmosphere": "Noir atmosphere description",
  "article": "2-3 paragraphs of narrative journalism.",
  "timeline": [
    { "timestamp": "MM:SS", "speaker": "Speaker", "event": "Event", "description": "Desc" }
  ],
  "verdict": "Final verdict"
}
"""

try:
    response = model.generate_content([audio_file, prompt])
    print("\n--- REPORT ---\n")
    print(response.text)
    
    with open("gemini_audio_result.json", "w", encoding="utf-8") as f:
        f.write(response.text)
    print("Saved to gemini_audio_result.json")

except Exception as e:
    print(f"Generation failed: {e}")
