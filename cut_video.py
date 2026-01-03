from moviepy import VideoFileClip

# Parameters
input_file = "Mansions of Madness – Gameplay.mp4"
output_file = "test_5min.mp4"
start_time = "01:48:00"
end_time = "01:53:00"

print(f"Loading {input_file}...")
try:
    with VideoFileClip(input_file) as video:
        print(f"Cutting from {start_time} to {end_time}...")
        new_video = video.subclipped(start_time, end_time)
        
        print(f"Saving to {output_file}...")
        new_video.write_videofile(output_file, codec="libx264", audio_codec="aac")
        
    print("Success!")
except Exception as e:
    print(f"Error: {e}")
