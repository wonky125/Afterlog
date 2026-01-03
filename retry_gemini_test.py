import google.generativeai as genai
import time
import sys

# Configure File Logging
log_file = open("debug_gemini.log", "w", encoding="utf-8")

def log(message):
    print(message)
    log_file.write(message + "\n")
    log_file.flush()

log("Starting Gemini Test Script...")

# 1. Load API Key
try:
    with open("android/local.properties", "r") as f:
        api_key = None
        for line in f:
            if line.startswith("GEMINI_API_KEY"):
                api_key = line.split("=")[1].strip()
                break
        
        if api_key:
            log("API Key found.")
            genai.configure(api_key=api_key)
        else:
            log("ERROR: GEMINI_API_KEY not found in local.properties")
            sys.exit(1)
except Exception as e:
    log(f"ERROR reading local.properties: {e}")
    sys.exit(1)

# 2. Upload Video
video_path = "test_5min.mp4"
log(f"Uploading {video_path}...")

try:
    video_file = genai.upload_file(video_path)
    log(f"Upload initiated. URI: {video_file.uri}")
    log(f"Initial State: {video_file.state.name}")
except Exception as e:
    log(f"ERROR uploading file: {e}")
    sys.exit(1)

# 3. Poll for Active State
while video_file.state.name == "PROCESSING":
    log("Processing video... buffering...")
    time.sleep(5)
    video_file = genai.get_file(video_file.name)

log(f"Final State: {video_file.state.name}")

if video_file.state.name == "FAILED":
    log("ERROR: Video processing failed.")
    sys.exit(1)

# 4. Generate Content
log("Generating content with gemini-1.5-flash...")
model = genai.GenerativeModel("gemini-1.5-flash")

prompt = """
Respond with valid JSON only.
{
  "headline": "TEST HEADLINE",
  "article": "Short test article.",
  "timeline": []
}
"""

try:
    response = model.generate_content([video_file, prompt])
    log("Generation complete.")
    log("--- RESPONSE START ---")
    log(response.text)
    log("--- RESPONSE END ---")
    
    with open("gemini_result.json", "w", encoding="utf-8") as f:
        f.write(response.text)
    log("Saved to gemini_result.json")

except Exception as e:
    log(f"ERROR generating content: {e}")

log_file.close()
