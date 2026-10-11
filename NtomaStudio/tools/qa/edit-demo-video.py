"""Trim installation/blank launch footage from the inspected 38113656244 emulator recording."""
from pathlib import Path
import subprocess
import sys
import imageio_ffmpeg

source, output = map(Path, sys.argv[1:3])
ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
# Reviewed at 5-second intervals: app Create screen is visible from ~24s.
# Preserve all subsequent interaction (picker, crop, progress, templates and share), no speed-up.
subprocess.run([ffmpeg, '-y', '-ss', '24', '-i', str(source), '-map', '0:v:0', '-an',
                '-vf', 'fps=30', '-c:v', 'libx264', '-preset', 'fast', '-crf', '21',
                '-pix_fmt', 'yuv420p', '-movflags', '+faststart', str(output)], check=True)
