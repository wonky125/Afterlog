import google.generativeai as genai
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

try:
    with open("android/local.properties", "r") as f:
        for line in f:
            if line.startswith("GEMINI_API_KEY"):
                api_key = line.split("=")[1].strip()
                genai.configure(api_key=api_key)
                break

    print("List of available models:")
    for m in genai.list_models():
        if 'generateContent' in m.supported_generation_methods:
            print(f"- {m.name}")

except Exception as e:
    print(f"Error: {e}")
