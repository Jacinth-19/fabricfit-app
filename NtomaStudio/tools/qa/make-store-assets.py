"""Frame ONLY screenshots produced by the passing installed-app journey. No invented UI."""
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFont

source, out = map(Path, sys.argv[1:3])
assert (source / 'validation.txt').read_text().startswith('PASS:')
# Early captures must show the editor, not the app splash screen.
# The test advances Compose time before screenshots; review remains required.
out.mkdir(parents=True, exist_ok=True)
font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font = lambda size: ImageFont.truetype(font_path, size)
shots = [
    ('02-whole-dress', 'Start with a dress photo', 'Drag over the fabric you want to use.'),
    ('03-selected-fabric', 'Keep the fabric. Leave the rest.', 'Only your selected image area is used.'),
    ('04-analysis', 'Explore colours and pattern', 'On-device estimates, not verified fabric identification.'),
    ('05-template', 'Your fabric. A new silhouette.', 'Flat outfit templates, made on your phone.'),
    ('06-another-outfit', 'Explore another outfit', 'Use the same fabric across the template catalogue.'),
    ('07-texture-scale', 'Adjust the texture repeats', 'A layout preview — not a fitting or photoreal try-on.'),
]
for i, (name, title, subtitle) in enumerate(shots, 1):
    screenshot = Image.open(source / f'{name}.png').convert('RGB')
    canvas = Image.new('RGB', (1080, 1920), '#f4ecdf')
    d = ImageDraw.Draw(canvas)
    d.text((60, 50), 'NTOMA STUDIO', font=font(30), fill='#1f5650')
    # wrap long headline at word boundaries
    words, lines, line = title.split(), [], ''
    for word in words:
        candidate = (line + ' ' + word).strip()
        if d.textlength(candidate, font=font(52)) > 960:
            lines.append(line); line = word
        else: line = candidate
    lines.append(line)
    for n, line in enumerate(lines): d.text((60, 104 + 62*n), line, font=font(52), fill='#201b17')
    y = 116 + len(lines)*62
    d.text((60, y), subtitle, font=font(23), fill='#534b40')
    screenshot.thumbnail((884, 1544))
    x, y = (1080 - screenshot.width)//2, 285
    canvas.paste(screenshot, (x, y))
    d.rounded_rectangle((x-3, y-3, x+screenshot.width+3, y+screenshot.height+3), 12, outline='#201b17', width=3)
    d.text((60, 1860), 'Actual app capture • Original test illustration, not a real dress photo', font=font(22), fill='#534b40')
    canvas.save(out / f'{i:02d}-{name}.png')
# Exact Play feature graphic dimensions; uses the real template capture.
banner = Image.new('RGB', (1024, 500), '#1f5650')
d = ImageDraw.Draw(banner)
d.text((44, 70), 'Ntoma Studio', font=font(55), fill='#fff4df')
d.text((44, 150), 'Your fabric. New outfit ideas.', font=font(29), fill='#fff4df')
d.text((44, 215), 'Select fabric from a dress or swatch.', font=font(23), fill='#fff4df')
d.text((44, 255), 'Explore flat templates on your phone.', font=font(23), fill='#fff4df')
d.text((44, 415), 'Layout previews, not photoreal try-on.', font=font(21), fill='#fff4df')
screenshot = Image.open(source / '05-template.png').convert('RGB')
screenshot.thumbnail((285, 450))
banner.paste(screenshot, (750, 25))
banner.save(out / 'feature-graphic-1024x500.png')
icon = Image.open(Path(__file__).resolve().parents[2] / 'store/play_icon_512.png').convert('RGB')
icon.resize((512, 512), Image.Resampling.LANCZOS).save(out / 'play-icon-512.png')
(out / 'README.md').write_text('''# Validated app marketing draft

Screenshots are real emulator captures framed with factual captions. The input dress is an original synthetic test illustration, not a real camera photograph. The app uses selected pixels in flat templates. These assets do not claim photoreal try-on or verified fabric recognition.

Sizes: screenshots 1080×1920; feature graphic 1024×500; icon 512×512. Review copy, localization, target SDK/Play requirements, production signing and privacy policy before publication. The APK is debug-signed, not a Play submission.

`../demo-raw.mp4` is actual emulator footage, without narration or fabricated screens. It may include installation/idle portions; edit only for pacing, not to conceal failures.
''')
