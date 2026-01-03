import google.generativeai as genai
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

try:
    api_key = None
    with open("android/local.properties", "r") as f:
        for line in f:
            if line.startswith("GEMINI_API_KEY") and "=" in line:
                parts = line.split("=", 1)
                if len(parts) > 1:
                    api_key = parts[1].strip()
                    break
    
    if not api_key:
        print("Error: GEMINI_API_KEY not found in android/local.properties")
        sys.exit(1)
        
    genai.configure(api_key=api_key)

    print("List of available models:")
    for m in genai.list_models():
        if 'generateContent' in m.supported_generation_methods:
            print(f"- {m.name}")

except Exception as e:
    print(f"Error: {e}")
