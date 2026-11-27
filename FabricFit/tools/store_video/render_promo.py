#!/usr/bin/env python3
"""
Renders the FabricFit Play Store promo videos from the app's own screenshots.

Why this exists: the previous promos were built by hand and drifted. They still carried
"HERITAGE LUXE · MADE IN ACCRA", the retired adinkra rosette and the old splash, months
after all three were replaced in the app. A generator means the promos can be rebuilt from
whatever the app currently looks like, and cannot silently disagree with it.

Everything is composed from assets in this repository:

  * store/screenshots/*.png  (1080x1920 emulator captures)
  * brand/fabricfit_icon_512.png
  * the palette below, matched to ui/theme/Color.kt

Requirements: Pillow and numpy, plus an ffmpeg binary. ffmpeg is located automatically by
`imageio_ffmpeg` if it is installed, otherwise from $FFMPEG or PATH.

    pip install pillow numpy imageio-ffmpeg
    python3 tools/store_video/render_promo.py --check     # validate inputs only
    python3 tools/store_video/render_promo.py             # render both videos

Outputs:
    store/video/fabricfit_play_store_promo_16x9.mp4   1280x720, 15s, silent
    store/video/fabricfit_shorts_reel_9x16.mp4        720x1280, 12s, silent

The videos are silent by design. The old ones shipped a synthesized marimba score; adding
audio here would mean either synthesising music again or shipping something that sounds
like a placeholder, and Play accepts silent promo video. If you add a track, mux it with
ffmpeg afterwards rather than baking it into this script.
"""

from __future__ import annotations

import argparse
import math
import os
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]          # FabricFit/
STORE = ROOT / "store"
SHOTS = STORE / "screenshots"
BRAND = ROOT / "brand" / "fabricfit_icon_512.png"
OUT = STORE / "video"

# Palette, kept in step with ui/theme/Color.kt.
FOREST = (15, 61, 46)
FOREST_DARK = (10, 43, 32)
GOLD = (212, 168, 75)
GOLD_SOFT = (200, 169, 106)
CREAM = (247, 241, 229)
CREAM_BRIGHT = (251, 247, 238)
INK = (26, 26, 26)

FPS = 24


# --------------------------------------------------------------------------------------
# Assets
# --------------------------------------------------------------------------------------

@dataclass
class Shot:
    file: Path
    caption: str


# Captions describe what the screenshot actually shows. They are deliberately plain: the
# old promos claimed capabilities the app does not have.
SCENES = [
    Shot("01_play_welcome.png", "Dress the way\nyou were meant to."),
    Shot("02_play_home.png", "Your fit, your fabric"),
    Shot("03_play_fitscan.png", "Scan any cloth"),
    Shot("04_play_scan_results.png", "Know the weave instantly"),
    Shot("05_play_discover.png", "Find the cut that suits it"),
    Shot("06_play_wardrobe.png", "Your wardrobe, organised"),
]


def _font(size: int, bold: bool = True) -> ImageFont.FreeTypeFont:
    """A serif display face if the system has one, else DejaVu, else the bitmap default."""
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/TTF/DejaVuSerif-Bold.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def _find_ffmpeg() -> str:
    env = os.environ.get("FFMPEG")
    if env and Path(env).exists():
        return env
    try:
        import imageio_ffmpeg  # type: ignore

        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        pass
    found = shutil.which("ffmpeg")
    if found:
        return found
    raise SystemExit(
        "ffmpeg not found. Install it (`pip install imageio-ffmpeg`) or set FFMPEG=/path/to/ffmpeg."
    )


def _rounded(im: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, *im.size), radius=radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def _phone(shot: Image.Image, height: int) -> Image.Image:
    """A screenshot presented as a phone: rounded, with a subtle bezel and drop shadow."""
    w = int(shot.width * height / shot.height)
    body = shot.convert("RGB").resize((w, height), Image.LANCZOS)
    body = _rounded(body, radius=max(12, height // 34))

    pad = max(6, height // 90)
    framed = Image.new("RGBA", (w + pad * 2, height + pad * 2), (0, 0, 0, 0))
    bezel = Image.new("RGBA", framed.size, (18, 22, 20, 255))
    bezel = _rounded(bezel, radius=max(16, height // 30))
    framed.alpha_composite(bezel)
    framed.alpha_composite(body, (pad, pad))

    # Soft shadow so the phone lifts off the background.
    shadow = Image.new("RGBA", (framed.width + 60, framed.height + 60), (0, 0, 0, 0))
    silhouette = Image.new("RGBA", framed.size, (0, 0, 0, 120))
    silhouette.putalpha(framed.getchannel("A").point(lambda a: a * 120 // 255))
    shadow.alpha_composite(silhouette, (30, 34))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    shadow.alpha_composite(framed, (30, 30))
    return shadow


def _background(size: tuple[int, int]) -> Image.Image:
    """Deep forest ground with a warm gold bloom and a faint woven diagonal."""
    w, h = size
    base = Image.new("RGB", size, FOREST_DARK)
    draw = ImageDraw.Draw(base)

    # vertical wash
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(FOREST_DARK[i] + (FOREST[i] - FOREST_DARK[i]) * (1 - t)) for i in range(3))
        draw.line([(0, y), (w, y)], fill=c)

    # gold bloom, upper right
    bloom = Image.new("RGB", size, (0, 0, 0))
    bd = ImageDraw.Draw(bloom)
    bd.ellipse((int(w * 0.55), int(-h * 0.25), int(w * 1.25), int(h * 0.55)), fill=(70, 52, 18))
    bloom = bloom.filter(ImageFilter.GaussianBlur(120))
    base = Image.fromarray(
        np.clip(np.asarray(base, dtype=np.int16) + np.asarray(bloom, dtype=np.int16), 0, 255).astype(np.uint8)
    )

    # faint diagonal weave
    weave = Image.new("RGBA", size, (0, 0, 0, 0))
    wd = ImageDraw.Draw(weave)
    step = max(36, w // 26)
    for x in range(-h, w + h, step):
        wd.line([(x, 0), (x + h, h)], fill=(255, 255, 255, 7), width=2)
    base = Image.alpha_composite(base.convert("RGBA"), weave).convert("RGB")
    return base


def _wrap(text: str, font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    """Greedy word wrap that also honours explicit newlines in the caption."""
    draw = ImageDraw.Draw(Image.new("RGB", (8, 8)))
    lines: list[str] = []
    for paragraph in text.split("\n"):
        current = ""
        for word in paragraph.split():
            candidate = f"{current} {word}".strip()
            if draw.textlength(candidate, font=font) <= max_width or not current:
                current = candidate
            else:
                lines.append(current)
                current = word
        if current:
            lines.append(current)
    return lines


def _draw_caption(canvas: Image.Image, text: str, x: int, y: int, size: int, max_width: int) -> None:
    draw = ImageDraw.Draw(canvas)
    font = _font(size)
    line_h = int(size * 1.18)
    for i, line in enumerate(_wrap(text, font, max_width)):
        draw.text((x, y + i * line_h), line, font=font, fill=CREAM_BRIGHT)


def _draw_pill(canvas: Image.Image, text: str, x: int, y: int, size: int) -> int:
    draw = ImageDraw.Draw(canvas)
    font = _font(size, bold=False)
    box = draw.textbbox((0, 0), text, font=font)
    tw, th = box[2] - box[0], box[3] - box[1]
    px, py = int(size * 0.7), int(size * 0.45)
    w, h = tw + px * 2, th + py * 2
    draw.rounded_rectangle((x, y, x + w, y + h), radius=h // 2, outline=GOLD_SOFT, width=2)
    draw.text((x + px, y + py - box[1]), text, font=font, fill=GOLD_SOFT)
    return w


# --------------------------------------------------------------------------------------
# Scenes
# --------------------------------------------------------------------------------------

def intro_frame(size: tuple[int, int], t: float) -> Image.Image:
    """Brand open: mark scales in, wordmark and tagline follow on the shared beat."""
    canvas = _background(size).convert("RGBA")
    w, h = size
    in_scale = min(1.0, t / 0.55)
    ease = 1 - (1 - in_scale) ** 3

    # Cream plate behind the icon: the artwork is gold-and-cream line work and vanishes
    # against a dark green ground without it.
    side = int(min(w, h) * 0.24)
    plate = Image.new("RGBA", (side, side), CREAM + (255,))
    plate = _rounded(plate, radius=side // 4)
    ring = ImageDraw.Draw(plate)
    ring.rounded_rectangle((1, 1, side - 2, side - 2), radius=side // 4, outline=GOLD + (255,), width=3)
    art = Image.open(BRAND).convert("RGB").resize((int(side * 0.82),) * 2, Image.LANCZOS)
    art = _rounded(art, radius=int(side * 0.16))
    plate.alpha_composite(art, ((side - art.width) // 2, (side - art.height) // 2))

    if ease < 1.0:
        scaled = max(1, int(side * (0.80 + 0.20 * ease)))
        plate = plate.resize((scaled, scaled), Image.LANCZOS)
    canvas.alpha_composite(plate, ((w - plate.width) // 2, int(h * 0.30) - plate.height // 2))

    # Wordmark and tagline are laid out in one pass so the tagline is positioned from the
    # wordmark's measured bottom rather than a guessed fraction (the original bug: both
    # were placed from constants and overlapped).
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)

    word_font = _font(int(min(w, h) * 0.105))
    word = "FabricFit"
    wbox = draw.textbbox((0, 0), word, font=word_font)
    word_y = int(h * 0.52)
    if t > 0.30:
        a = min(1.0, (t - 0.30) / 0.40)
        draw.text(((w - (wbox[2] - wbox[0])) // 2, word_y), word, font=word_font,
                  fill=GOLD + (int(255 * a),))
    word_bottom = word_y + (wbox[3] - wbox[1])

    if t > 0.55:
        a = min(1.0, (t - 0.55) / 0.45)
        tag_font = _font(int(min(w, h) * 0.036), bold=False)
        tag = "Scan a fabric. See the outfit."
        tbox = draw.textbbox((0, 0), tag, font=tag_font)
        draw.text(((w - (tbox[2] - tbox[0])) // 2, word_bottom + int(min(w, h) * 0.045)),
                  tag, font=tag_font, fill=CREAM + (int(255 * a),))

    canvas = Image.alpha_composite(canvas, layer)

    return canvas.convert("RGB")


def scene_frame(shot_path: Path, caption: str, size: tuple[int, int], t: float,
                dur: float, index: int, total: int) -> Image.Image:
    """A product scene: phone on one side, caption and chips on the other."""
    w, h = size
    canvas = _background(size).convert("RGBA")

    # Phone slides up and settles; caption trails it slightly on the same beat.
    enter = min(1.0, t / 0.55)
    ease = 1 - (1 - enter) ** 3

    landscape = w > h
    phone_h = int(h * (0.80 if landscape else 0.60))
    shot = Image.open(SHOTS / shot_path if not Path(shot_path).is_absolute() else shot_path)
    phone = _phone(shot, phone_h)

    if landscape:
        px = int(w * 0.10)
        py = int((h - phone.height) / 2 + (1 - ease) * h * 0.16)
        canvas.alpha_composite(phone, (px, py))
        text_x = int(w * 0.50)
        text_y = int(h * 0.36)
        cap_size = int(h * 0.088)
    else:
        px = (w - phone.width) // 2
        py = int(h * 0.10 + (1 - ease) * h * 0.05)
        canvas.alpha_composite(phone, (px, py))
        # Text band sits below the phone, not on top of it.
        text_x = int(w * 0.08)
        text_y = int(h * 0.745)
        cap_size = int(h * 0.042)

    # Step marker
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    label = f"STEP {index + 1:02d}"
    f_small = _font(int(h * 0.024), bold=False)
    d.text((text_x, text_y - int(h * 0.075)), label, font=f_small,
           fill=GOLD_SOFT + (int(210 * ease),))

    _draw_caption(layer, caption, text_x, text_y, cap_size,
                  int(w * (0.44 if landscape else 0.84)))
    layer = layer.point(lambda v: v)  # keep type
    # fade the whole text block in
    alpha = layer.getchannel("A").point(lambda a: int(a * ease))
    layer.putalpha(alpha)
    canvas.alpha_composite(layer)

    # Progress rail along the bottom: shows this is one continuous run, not six clips.
    rail = Image.new("RGBA", size, (0, 0, 0, 0))
    rd = ImageDraw.Draw(rail)
    margin = int(w * 0.06)
    y = h - int(h * 0.045)
    rd.rounded_rectangle((margin, y, w - margin, y + 4), radius=2, fill=(255, 255, 255, 28))
    done = (index + min(1.0, t / dur)) / total
    rd.rounded_rectangle((margin, y, margin + int((w - margin * 2) * done), y + 4), radius=2, fill=GOLD)
    canvas.alpha_composite(rail)

    return canvas.convert("RGB")


def outro_frame(size: tuple[int, int], t: float) -> Image.Image:
    w, h = size
    canvas = _background(size).convert("RGBA")
    ease = min(1.0, t / 0.5)

    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    font = _font(int(min(w, h) * 0.075))
    text = "FabricFit"
    box = d.textbbox((0, 0), text, font=font)
    d.text(((w - (box[2] - box[0])) // 2, int(h * 0.33)), text, font=font, fill=GOLD + (255,))

    sub = _font(int(min(w, h) * 0.032), bold=False)
    line = "Ghanaian fabrics, made for you"
    box2 = d.textbbox((0, 0), line, font=sub)
    d.text(((w - (box2[2] - box2[0])) // 2, int(h * 0.43)), line, font=sub, fill=CREAM + (255,))
    layer.putalpha(layer.getchannel("A").point(lambda a: int(a * ease)))
    canvas.alpha_composite(layer)

    # Honest capability chips: only things the app actually does.
    chips = ["Works offline", "On-device analysis", "Free to use"]
    chip_font_size = int(min(w, h) * 0.026)
    widths = []
    for c in chips:
        f = _font(chip_font_size, bold=False)
        b = ImageDraw.Draw(canvas).textbbox((0, 0), c, font=f)
        widths.append(b[2] - b[0] + int(chip_font_size * 1.8))
    gap = int(min(w, h) * 0.018)
    total_w = sum(widths) + gap * (len(chips) - 1)
    x = (w - total_w) // 2
    y = int(h * 0.55)
    for c, cw in zip(chips, widths):
        _draw_pill(canvas, c, x, y, chip_font_size)
        x += cw + gap

    return canvas.convert("RGB")


# --------------------------------------------------------------------------------------
# Render + encode
# --------------------------------------------------------------------------------------

def render(size: tuple[int, int], out_path: Path, intro: float, per_scene: float,
           outro: float, scenes: list[Shot]) -> None:
    ffmpeg = _find_ffmpeg()
    total_scenes = len(scenes)
    frames: list[tuple[int, int]] = []  # (kind, index)

    def push(kind: int, idx: int, seconds: float) -> None:
        for _ in range(int(round(seconds * FPS))):
            frames.append((kind, idx))

    push(0, 0, intro)
    for i in range(total_scenes):
        push(1, i, per_scene)
    push(2, 0, outro)

    tmp = Path(tempfile.mkdtemp(prefix="promo_"))
    cmd = [
        ffmpeg, "-y", "-loglevel", "error",
        "-f", "rawvideo", "-pix_fmt", "rgb24",
        "-s", f"{size[0]}x{size[1]}", "-r", str(FPS), "-i", "-",
        "-an",
        "-c:v", "libx264", "-preset", "medium", "-crf", "20",
        "-pix_fmt", "yuv420p", "-movflags", "+faststart",
        str(out_path),
    ]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    assert proc.stdin is not None

    # Cache the phone composition per scene: rebuilding it every frame would be wasteful
    # and the slide is the only thing that changes.
    try:
        for n, (kind, idx) in enumerate(frames):
            t = n / FPS
            if kind == 0:
                frame = intro_frame(size, t)
            elif kind == 2:
                frame = outro_frame(size, t - (intro + per_scene * total_scenes))
            else:
                local = t - intro - per_scene * idx
                frame = scene_frame(scenes[idx].file, scenes[idx].caption, size,
                                    local, per_scene, idx, total_scenes)
            proc.stdin.write(frame.tobytes())
    finally:
        proc.stdin.close()
        code = proc.wait()
        shutil.rmtree(tmp, ignore_errors=True)
        if code != 0:
            raise SystemExit(f"ffmpeg failed with exit code {code}")

    print(f"  wrote {out_path.relative_to(ROOT)}  ({out_path.stat().st_size/1024/1024:.2f} MB, "
          f"{len(frames)/FPS:.1f}s)")


def check_inputs() -> int:
    problems = []
    if not BRAND.exists():
        problems.append(f"missing brand icon: {BRAND.relative_to(ROOT)}")
    for s in SCENES:
        p = SHOTS / s.file
        if not p.exists():
            problems.append(f"missing screenshot: {p.relative_to(ROOT)}")
        else:
            im = Image.open(p)
            if im.size != (1080, 1920):
                problems.append(f"{p.name} is {im.size}, expected (1080, 1920)")
    # Catch the specific drift that made the old promos wrong.
    for s in SCENES:
        p = SHOTS / s.file
        if p.exists() and p.stat().st_size < 50_000:
            problems.append(f"{p.name} is suspiciously small ({p.stat().st_size} B)")
    if problems:
        print("Input check FAILED:")
        for p in problems:
            print("  -", p)
        return 1
    print(f"Input check OK: {len(SCENES)} screenshots at 1080x1920, brand icon present.")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--check", action="store_true", help="validate inputs and exit")
    args = ap.parse_args()
    if args.check:
        return check_inputs()

    rc = check_inputs()
    if rc:
        return rc

    OUT.mkdir(parents=True, exist_ok=True)

    # 16:9 -- Play Store feature/promo video. 15s total.
    print("Rendering 16:9 (1280x720, 15s)...")
    render((1280, 720), OUT / "fabricfit_play_store_promo_16x9.mp4",
           intro=1.4, per_scene=2.0, outro=1.6, scenes=SCENES)

    # 9:16 -- Shorts / Reels. 12s: 1.5s intro + 5 x 1.8s + 1.5s outro = 12.0s.
    print("Rendering 9:16 (720x1280, 12s)...")
    render((720, 1280), OUT / "fabricfit_shorts_reel_9x16.mp4",
           intro=1.5, per_scene=1.8, outro=1.5, scenes=SCENES[:5])

    print("Done. Both videos are silent; mux a track afterwards if you want one.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
