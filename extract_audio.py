from moviepy import VideoFileClip

input_file = "test_5min.mp4"
output_file = "test_5min.mp3"

print(f"Loading {input_file}...")
try:
    with VideoFileClip(input_file) as video:
        print(f"Extracting audio to {output_file}...")
        video.audio.write_audiofile(output_file)
        
    print("Success!")
except Exception as e:
    print(f"Error: {e}")
