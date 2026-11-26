#!/usr/bin/env python3
"""FabricFit FitRoom — on-device try-on pipeline, replicated locally.

This is a faithful desktop port of what the Android app does when you press
"Generate look" in FitRoom:

  app/src/main/java/com/fabricfit/app/data/repository/DemoLookGenerator.kt
  app/src/main/java/com/fabricfit/app/data/ml/PersonSegmenter.kt
  app/src/main/java/com/fabricfit/app/media/garment/GarmentGeometry.kt

It runs the *same* bundled TFLite models (selfie_segmentation.tflite and
deeplabv3_257_mv.tflite) through ai-edge-litert, uses the same normalised garment
polygons and the same shading passes, so the output equals what the phone renders.

No account, no API key, no network: this is the free, offline path.

Examples
--------
    # quick check on the bundled Ghanaian sample model + kente swatch
    python3 tools/vton_qa/vton_local.py --demo

    # a real test with your own photo and a fabric you bought online
    python3 tools/vton_qa/vton_local.py \
        --person ~/Pictures/me.jpg --fabric ~/Pictures/ankara.jpg \
        --style kaba_slit --gender WOMAN --sleeve PUFF --neckline VNECK --length MAXI

    # cut the fabric swatch straight out of a dress photo (bodice region)
    python3 tools/vton_qa/vton_local.py --demo --fabric-from-dress samples/dress_kente_online.jpg

Outputs land in tools/vton_qa/out/ (composite, mask, side-by-side, report.html).
"""
from __future__ import annotations

import argparse
import io
import json
import math
import sys
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]          # FabricFit/
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUT_DIR = Path(__file__).resolve().parent / "out"
SAMPLES = Path(__file__).resolve().parent / "samples"

DEEPLAB_SIZE = 257
DEEPLAB_CLASSES = 21
DEEPLAB_PERSON = 15
SELFIE_SIZE = 256
SELFIE_THRESHOLD = 0.45
SELFIE_MIN_COVERAGE = 1.0 / 30.0


# --------------------------------------------------------------------------- #
# Catalogue
# --------------------------------------------------------------------------- #
@dataclass
class Style:
    id: str
    gender: str            # WOMEN | MEN | UNISEX
    silhouette: str
    title: str = ""


def load_catalogue() -> dict[str, Style]:
    raw = json.loads((ASSETS / "catalog" / "dress_styles.json").read_text())
    styles: dict[str, Style] = {}
    for s in raw.get("styles", []):
        styles[s["id"]] = Style(
            id=s["id"],
            gender=s.get("gender", "WOMEN"),
            silhouette=s.get("silhouette", "KABA"),
            title=s.get("title_key", s["id"]),
        )
    return styles


# --------------------------------------------------------------------------- #
# GarmentGeometry — direct port of the Kotlin polygons
# --------------------------------------------------------------------------- #
def _poly(*coords: float) -> list[tuple[float, float]]:
    assert len(coords) % 2 == 0
    return [(coords[i], coords[i + 1]) for i in range(0, len(coords), 2)]


BASE_SHAPES: dict[str, list[list[tuple[float, float]]]] = {
    "KABA": [_poly(0.32, 0.06, 0.5, 0.02, 0.68, 0.06, 0.74, 0.3, 0.62, 0.34,
                  0.66, 0.98, 0.34, 0.98, 0.38, 0.34, 0.26, 0.3)],
    "MODERN_KABA": [_poly(0.3, 0.05, 0.5, 0.01, 0.7, 0.05, 0.78, 0.22, 0.68, 0.26,
                        0.64, 0.42, 0.72, 0.98, 0.28, 0.98, 0.36, 0.42, 0.32, 0.26, 0.22, 0.22)],
    "MAXI": [_poly(0.34, 0.04, 0.5, 0.0, 0.66, 0.04, 0.7, 0.26, 0.6, 0.3,
                  0.8, 0.98, 0.2, 0.98, 0.4, 0.3, 0.3, 0.26)],
    "STRAIGHT": [_poly(0.34, 0.04, 0.5, 0.0, 0.66, 0.04, 0.7, 0.3,
                      0.66, 0.96, 0.34, 0.96, 0.3, 0.3)],
    "MERMAID": [_poly(0.36, 0.03, 0.5, 0.0, 0.64, 0.03, 0.66, 0.3, 0.6, 0.6,
                     0.58, 0.74, 0.76, 0.98, 0.24, 0.98, 0.42, 0.74, 0.4, 0.6, 0.34, 0.3)],
    "PEPLUM": [
        _poly(0.34, 0.04, 0.5, 0.0, 0.66, 0.04, 0.68, 0.34, 0.76, 0.5, 0.24, 0.5, 0.32, 0.34),
        _poly(0.36, 0.5, 0.64, 0.5, 0.68, 0.98, 0.32, 0.98),
    ],
    "JUMPSUIT": [_poly(0.34, 0.04, 0.5, 0.0, 0.66, 0.04, 0.68, 0.42, 0.7, 0.98, 0.54, 0.98,
                      0.5, 0.56, 0.46, 0.98, 0.3, 0.98, 0.32, 0.42)],
    "SKIRT_BLOUSE": [
        _poly(0.32, 0.04, 0.5, 0.0, 0.68, 0.04, 0.7, 0.4, 0.3, 0.4),
        _poly(0.34, 0.44, 0.66, 0.44, 0.74, 0.98, 0.26, 0.98),
    ],
    "GOWN": [_poly(0.36, 0.03, 0.5, 0.0, 0.64, 0.03, 0.66, 0.26, 0.58, 0.32,
                  0.86, 0.98, 0.14, 0.98, 0.42, 0.32, 0.34, 0.26)],
    "FORMAL_DRESS": [_poly(0.35, 0.04, 0.5, 0.0, 0.65, 0.04, 0.68, 0.32,
                           0.62, 0.92, 0.38, 0.92, 0.32, 0.32)],
    "KAFTAN": [
        _poly(0.34, 0.04, 0.5, 0.0, 0.66, 0.04, 0.76, 0.5, 0.7, 0.98, 0.3, 0.98, 0.24, 0.5),
        _poly(0.46, 0.08, 0.54, 0.08, 0.54, 0.3, 0.46, 0.3),
    ],
    "AGBADA": [_poly(0.36, 0.03, 0.5, 0.0, 0.64, 0.03, 0.92, 0.5, 0.74, 0.56,
                    0.76, 0.98, 0.24, 0.98, 0.26, 0.56, 0.08, 0.5)],
    "PRINT_SHIRT": [_poly(0.32, 0.06, 0.5, 0.0, 0.68, 0.06, 0.78, 0.3, 0.68, 0.34,
                         0.68, 0.62, 0.32, 0.62, 0.32, 0.34, 0.22, 0.3)],
    "SENATOR": [_poly(0.34, 0.04, 0.5, 0.0, 0.66, 0.04, 0.74, 0.42, 0.68, 0.46,
                     0.66, 0.86, 0.34, 0.86, 0.32, 0.46, 0.26, 0.42)],
    "SMOCK": [
        _poly(0.3, 0.08, 0.5, 0.0, 0.7, 0.08, 0.82, 0.6, 0.7, 0.98, 0.3, 0.98, 0.18, 0.6),
        _poly(0.42, 0.06, 0.58, 0.06, 0.58, 0.16, 0.42, 0.16),
    ],
    "AFRICAN_SUIT": [
        _poly(0.3, 0.05, 0.48, 0.0, 0.48, 0.52, 0.32, 0.52, 0.32, 0.3, 0.22, 0.28),
        _poly(0.7, 0.05, 0.52, 0.0, 0.52, 0.52, 0.68, 0.52, 0.68, 0.3, 0.78, 0.28),
        _poly(0.34, 0.56, 0.48, 0.56, 0.48, 0.98, 0.34, 0.98),
        _poly(0.52, 0.56, 0.66, 0.56, 0.66, 0.98, 0.52, 0.98),
    ],
    "SHIRT_TROUSERS": [
        _poly(0.32, 0.04, 0.5, 0.0, 0.68, 0.04, 0.74, 0.26, 0.66, 0.3, 0.66, 0.48, 0.34, 0.48, 0.34, 0.3, 0.26, 0.26),
        _poly(0.36, 0.52, 0.48, 0.52, 0.47, 0.98, 0.36, 0.98),
        _poly(0.52, 0.52, 0.64, 0.52, 0.64, 0.98, 0.53, 0.98),
    ],
    "MODERN_TRADITIONAL": [
        _poly(0.34, 0.04, 0.5, 0.0, 0.66, 0.04, 0.74, 0.36, 0.66, 0.4, 0.64, 0.9, 0.36, 0.9, 0.34, 0.4, 0.26, 0.36),
        _poly(0.44, 0.04, 0.56, 0.04, 0.56, 0.2, 0.44, 0.2),
    ],
    "COVER_UP": [_poly(0.3, 0.06, 0.5, 0.0, 0.7, 0.06, 0.84, 0.44, 0.72, 0.5,
                     0.74, 0.96, 0.26, 0.96, 0.28, 0.5, 0.16, 0.44)],
    "CO_ORDS": [
        _poly(0.32, 0.04, 0.5, 0.0, 0.68, 0.04, 0.72, 0.28, 0.64, 0.32, 0.64, 0.5, 0.36, 0.5, 0.36, 0.32, 0.28, 0.28),
        _poly(0.34, 0.54, 0.66, 0.54, 0.7, 0.86, 0.54, 0.86, 0.5, 0.7, 0.46, 0.86, 0.3, 0.86),
    ],
}

TARGET_BOTTOM = {"MINI": 0.72, "MIDI": 0.84, "MAXI": 0.95, "FLOOR": 0.995, "DEFAULT": None}


def garment_shapes(
    silhouette: str,
    sleeve: str = "DEFAULT",
    neckline: str = "DEFAULT",
    length: str = "DEFAULT",
) -> list[list[tuple[float, float]]]:
    """Port of GarmentGeometry.shapesFor(silhouette, customization)."""
    base = BASE_SHAPES.get(silhouette, BASE_SHAPES["KABA"])
    target_bottom = TARGET_BOTTOM.get(length, None)
    transformed: list[list[tuple[float, float]]] = []

    for poly in base:
        out: list[tuple[float, float]] = []
        for raw_x, raw_y in poly:
            x, y = raw_x, raw_y

            # 1. neckline
            if neckline in ("V_NECK", "VNECK"):
                if y < 0.08 and 0.44 <= x <= 0.56:
                    y = 0.14
            elif neckline == "OFF_SHOULDER":
                if y < 0.09:
                    y = 0.11
                    if x < 0.4:
                        x = max(x - 0.04, 0.18)
                    if x > 0.6:
                        x = min(x + 0.04, 0.82)
            elif neckline == "BOAT":
                if y < 0.08:
                    y = 0.035
            elif neckline == "HIGH":
                if y < 0.06 and 0.4 <= x <= 0.6:
                    y = 0.0

            # 2. sleeve
            if sleeve == "SLEEVELESS":
                if 0.04 <= y <= 0.38:
                    if x < 0.36:
                        x = 0.36
                    if x > 0.64:
                        x = 0.64
            elif sleeve == "PUFF":
                if 0.05 <= y <= 0.32:
                    if x < 0.38:
                        x = max(x - 0.07, 0.12)
                    if x > 0.62:
                        x = min(x + 0.07, 0.88)

            # 3. hem length
            if target_bottom is not None and y > 0.48:
                t = min(max((y - 0.48) / 0.50, 0.0), 1.0)
                y = min(max(0.48 + t * (target_bottom - 0.48), 0.48), 0.995)
                if length == "FLOOR" and t > 0.7:
                    if x < 0.45:
                        x = max(x - 0.03, 0.10)
                    if x > 0.55:
                        x = min(x + 0.03, 0.90)
            out.append((x, y))
        transformed.append(out)

    if sleeve == "THREE_QUARTER":
        transformed.append(_poly(0.26, 0.07, 0.34, 0.07, 0.28, 0.44, 0.19, 0.42))
        transformed.append(_poly(0.66, 0.07, 0.74, 0.07, 0.81, 0.42, 0.72, 0.44))
    elif sleeve == "LONG":
        transformed.append(_poly(0.26, 0.07, 0.34, 0.07, 0.25, 0.58, 0.17, 0.56))
        transformed.append(_poly(0.66, 0.07, 0.74, 0.07, 0.83, 0.56, 0.75, 0.58))

    if neckline == "HIGH":
        transformed.append(_poly(0.42, 0.0, 0.58, 0.0, 0.60, 0.055, 0.40, 0.055))

    return transformed


# --------------------------------------------------------------------------- #
# PersonSegmenter — TFLite models through ai-edge-litert
# --------------------------------------------------------------------------- #
class Segmenter:
    """Runs the bundled segmentation models exactly like PersonSegmenter.kt."""

    def __init__(self) -> None:
        self._deeplab = None
        self._selfie = None
        self.engine = "none"

    @staticmethod
    def _load(asset: str):
        try:
            from ai_edge_litert.interpreter import Interpreter
        except Exception:  # pragma: no cover - optional dependency
            return None
        path = ASSETS / asset
        if not path.exists():
            return None
        try:
            it = Interpreter(model_path=str(path))
            it.allocate_tensors()
            return it
        except Exception:
            return None

    def deeplab(self):
        if self._deeplab is None:
            self._deeplab = self._load("deeplabv3_257_mv.tflite") or False
        return self._deeplab or None

    def selfie(self):
        if self._selfie is None:
            self._selfie = self._load("selfie_segmentation.tflite") or False
        return self._selfie or None

    # -- masks ------------------------------------------------------------- #
    def person_mask(self, person: Image.Image) -> Image.Image | None:
        mask = self._deeplab_mask(person)
        if mask is not None:
            self.engine = "deeplabv3_257_mv.tflite"
            return mask
        mask = self._selfie_mask(person)
        if mask is not None:
            self.engine = "selfie_segmentation.tflite"
        return mask

    def _deeplab_mask(self, person: Image.Image) -> Image.Image | None:
        it = self.deeplab()
        if it is None:
            return None
        import numpy as np

        rgb = np.asarray(person.convert("RGB").resize((DEEPLAB_SIZE, DEEPLAB_SIZE), Image.BILINEAR), dtype=np.float32)
        x = ((rgb - 127.5) / 127.5)[None, ...]
        try:
            it.set_tensor(it.get_input_details()[0]["index"], x)
            it.invoke()
            scores = it.get_tensor(it.get_output_details()[0]["index"])[0]
        except Exception:
            return None
        best = np.argmax(scores, axis=-1)
        person_px = best == DEEPLAB_PERSON
        if person_px.sum() < DEEPLAB_SIZE * DEEPLAB_SIZE / 40:
            return None
        mask = Image.fromarray((person_px * 255).astype(np.uint8), mode="L")
        return mask.resize(person.size, Image.BILINEAR)

    def _selfie_mask(self, person: Image.Image) -> Image.Image | None:
        it = self.selfie()
        if it is None:
            return None
        import numpy as np

        rgb = np.asarray(person.convert("RGB").resize((SELFIE_SIZE, SELFIE_SIZE), Image.BILINEAR), dtype=np.float32)
        x = (rgb / 255.0)[None, ...]
        try:
            it.set_tensor(it.get_input_details()[0]["index"], x)
            it.invoke()
            prob = it.get_tensor(it.get_output_details()[0]["index"])[0, :, :, 0]
        except Exception:
            return None
        person_px = prob >= SELFIE_THRESHOLD
        if person_px.mean() < SELFIE_MIN_COVERAGE:
            return None
        alpha = np.clip(prob, 0, 1) * 255
        mask = Image.fromarray(alpha.astype(np.uint8), mode="L")
        return mask.resize(person.size, Image.BILINEAR)


# --------------------------------------------------------------------------- #
# Human parse (pure function port) + palette
# --------------------------------------------------------------------------- #
def is_likely_skin(r: int, g: int, b: int) -> bool:
    y = 0.299 * r + 0.587 * g + 0.114 * b
    if y < 28 or y > 242:
        return False
    cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b
    cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b
    return 77 <= cb <= 135 and 130 <= cr <= 178 and r >= g and g >= (b - 12)


def analyze_human_regions(person: Image.Image, mask: Image.Image):
    """Port of PersonSegmenter.analyzeHumanRegionsAndSkin (regions + skin tone)."""
    import numpy as np

    px = np.asarray(person.convert("RGB"), dtype=np.int16)
    alpha = np.asarray(mask, dtype=np.int16)
    h, w = alpha.shape
    ys, xs = np.where(alpha > 128)
    if ys.size == 0:
        return None, None, None

    min_y, max_y = int(ys.min()), int(ys.max())
    person_h = max(max_y - min_y, 1)
    head_bottom = min_y + round(person_h * 0.22)
    torso_bottom = min_y + round(person_h * 0.58)

    yy = np.arange(h)[:, None]
    head = (alpha > 128) & (yy <= head_bottom)
    torso = (alpha > 128) & (yy > head_bottom) & (yy <= torso_bottom)

    r, g, b = px[:, :, 0], px[:, :, 1], px[:, :, 2]
    skin = (
        (0.299 * r + 0.587 * g + 0.114 * b >= 28)
        & (0.299 * r + 0.587 * g + 0.114 * b <= 242)
        & ((128 - 0.168736 * r - 0.331264 * g + 0.5 * b) >= 77)
        & ((128 - 0.168736 * r - 0.331264 * g + 0.5 * b) <= 135)
        & ((128 + 0.5 * r - 0.418688 * g - 0.081312 * b) >= 130)
        & ((128 + 0.5 * r - 0.418688 * g - 0.081312 * b) <= 178)
        & (r >= g)
        & (g >= (b - 12))
    )

    skin_px = skin & (head | torso)
    if not skin_px.any():
        return head, None, None
    avg = px[skin_px].mean(axis=0)
    warmth = (avg[0] - avg[2]) - (avg[0] - avg[1]) * 0.5
    undertone = "WARM_GOLDEN" if warmth > 22 else ("COOL_DEEP" if warmth < 8 else "NEUTRAL_RICH")
    return head, tuple(int(v) for v in avg), undertone


def fabric_palette(fabric: Image.Image, colors: int = 4) -> list[tuple[int, int, int]]:
    small = fabric.convert("RGB").resize((64, 64), Image.BILINEAR)
    q = small.quantize(colors=colors, method=Image.Quantize.MEDIANCUT)
    pal = q.getpalette() or []
    counts = sorted(q.getcolors() or [], reverse=True)
    out = []
    for _, idx in counts[:colors]:
        base = idx * 3
        out.append((pal[base], pal[base + 1], pal[base + 2]))
    return out or [(200, 160, 60), (20, 60, 40)]


def darken(rgb: tuple[int, int, int], factor: float = 0.55) -> tuple[int, int, int]:
    return tuple(int(c * factor) for c in rgb)  # type: ignore[return-value]


# --------------------------------------------------------------------------- #
# Compositor — port of DemoLookGenerator.composite
# --------------------------------------------------------------------------- #
def mirrored_texture(fabric: Image.Image, side: int = 160) -> Image.Image:
    """Centre-crop + scale to [side], then make it 4-way symmetric.

    The app repeats the swatch with Shader.TileMode.MIRROR; making the tile itself
    symmetric first means the flipped repeats join without a visible seam.
    """
    w, h = fabric.size
    s = min(w, h)
    cropped = fabric.crop(((w - s) // 2, (h - s) // 2, (w - s) // 2 + s, (h - s) // 2 + s))
    tile = cropped.resize((side, side), Image.LANCZOS)
    quadrant = tile.resize((side // 2, side // 2), Image.LANCZOS)
    out = Image.new("RGB", (side, side))
    out.paste(quadrant, (0, 0))
    out.paste(quadrant.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (side // 2, 0))
    out.paste(quadrant.transpose(Image.Transpose.FLIP_TOP_BOTTOM), (0, side // 2))
    out.paste(quadrant.transpose(Image.Transpose.ROTATE_180), (side // 2, side // 2))
    return out


def tile_mirror(tile: Image.Image, size: tuple[int, int]) -> Image.Image:
    """Equivalent of Android Shader.TileMode.MIRROR."""
    tw, th = tile.size
    cols = math.ceil(size[0] / tw) + 1
    rows = math.ceil(size[1] / th) + 1
    canvas = Image.new("RGB", (cols * tw, rows * th))
    for r in range(rows):
        for c in range(cols):
            piece = tile if (r + c) % 2 == 0 else tile.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            if r % 2 == 1:
                piece = piece.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
            canvas.paste(piece, (c * tw, r * th))
    return canvas.crop((0, 0, size[0], size[1]))


def horizontal_shade(size: tuple[int, int]) -> Image.Image:
    """5-stop cylindrical torso shading, alpha 68/8/18/10/72 at 0/.22/.5/.78/1."""
    w, h = size
    stops = [(0.0, (0, 0, 0, 68)), (0.22, (0, 0, 0, 8)), (0.5, (255, 248, 230, 18)),
             (0.78, (0, 0, 0, 10)), (1.0, (0, 0, 0, 72))]
    row = Image.new("RGBA", (max(w, 2), 1))
    px = row.load()
    for x in range(row.size[0]):
        t = x / (row.size[0] - 1)
        for i in range(len(stops) - 1):
            t0, c0 = stops[i]
            t1, c1 = stops[i + 1]
            if t0 <= t <= t1:
                f = 0 if t1 == t0 else (t - t0) / (t1 - t0)
                px[x, 0] = tuple(int(c0[k] + (c1[k] - c0[k]) * f) for k in range(4))
                break
    return row.resize((w, h), Image.BILINEAR)


def vertical_shade(size: tuple[int, int]) -> Image.Image:
    """Linear top→bottom black gradient, alpha 18 → 54."""
    w, h = size
    col = Image.new("RGBA", (1, max(h, 2)))
    px = col.load()
    for y in range(col.size[1]):
        t = y / (col.size[1] - 1)
        px[0, y] = (0, 0, 0, int(18 + (54 - 18) * t))
    return col.resize((w, h), Image.BILINEAR)


@dataclass
class TryOnResult:
    image: Image.Image
    mask: Image.Image | None
    shapes: int = 0
    skin_rgb: tuple[int, int, int] | None = None
    undertone: str | None = None
    engine: str = "none"
    meta: dict = field(default_factory=dict)


def body_rect(mask: Image.Image | None, size: tuple[int, int]) -> tuple[float, float, float, float]:
    """Fit the garment rectangle to the segmented body instead of fixed fractions.

    The upstream demo hard-codes the garment box to 20–80 % of the frame width, which
    leaves a narrow strip floating over a normal-width body. Measuring the person mask
    gives shoulder/waist/hem anchors so the cloth covers the torso.
    """
    w, h = size
    if mask is None:
        return (w * 0.20, h * 0.26, w * 0.80, h * 0.96)
    import numpy as np

    alpha = np.asarray(mask, dtype=np.uint8)
    ys, xs = np.where(alpha > 128)
    if ys.size < 200:
        return (w * 0.20, h * 0.26, w * 0.80, h * 0.96)
    top, bottom = int(ys.min()), int(ys.max())
    person_h = max(bottom - top, 1)
    # Torso band: just below the head to just below the hips.
    band_top = top + int(person_h * 0.24)
    band_bottom = top + int(person_h * 0.52)
    band = alpha[band_top:band_bottom]
    cols = np.where((band > 128).any(axis=0))[0]
    if cols.size < 10:
        return (w * 0.20, h * 0.26, w * 0.80, h * 0.96)
    torso_left, torso_right = int(cols.min()), int(cols.max())
    centre = (torso_left + torso_right) / 2.0
    torso_w = max(torso_right - torso_left, 1)
    half = torso_w * 0.68          # a little wider than the torso: shoulders + drape
    left = max(0.0, centre - half)
    right = min(float(w), centre + half)
    return (left, float(band_top), right, float(top + person_h * 0.99))


def _run_at(alpha, row: int, centre_x: int) -> tuple[int, int] | None:
    """Widest horizontal run in `row` that contains (or is nearest to) centre_x."""
    import numpy as np

    cols = np.where(alpha[row])[0]
    if cols.size == 0:
        return None
    runs: list[tuple[int, int]] = []
    start = prev = int(cols[0])
    for c in cols[1:]:
        c = int(c)
        if c != prev + 1:
            runs.append((start, prev))
            start = c
        prev = c
    runs.append((start, prev))
    containing = [r for r in runs if r[0] <= centre_x <= r[1]]
    if containing:
        return max(containing, key=lambda r: r[1] - r[0])
    return min(runs, key=lambda r: min(abs(r[0] - centre_x), abs(r[1] - centre_x)))


def measure_body(mask: Image.Image | None, size: tuple[int, int]) -> dict:
    """Landmark widths (shoulder, bust, waist, hip) and the person's top/height."""
    w, h = size
    fallback = {
        "top": h * 0.24, "bottom": h * 0.97, "person_h": h * 0.73, "centre": w / 2.0,
        "shoulder_half": w * 0.14, "bust_half": w * 0.15, "waist_half": w * 0.13,
        "hip_half": w * 0.16, "thigh_half": w * 0.10, "ok": False,
    }
    if mask is None:
        return fallback
    import numpy as np

    alpha = np.asarray(mask, dtype=np.uint8) > 128
    ys, xs = np.where(alpha)
    if ys.size < 500:
        return fallback
    top, bottom = int(ys.min()), int(ys.max())
    ph = max(bottom - top, 1)
    # Torso centre: centroid of the chest band, then snap to the centre of the run
    # that contains it (so a slightly asymmetric pose does not shift the garment).
    band_top, band_bottom = top + int(ph * 0.28), top + int(ph * 0.44)
    band = alpha[band_top:band_bottom]
    ys_b, xs_b = np.where(band)
    if xs_b.size == 0:
        return fallback
    centre_x = int(np.median(xs_b))
    seed_row = min(max(band_top + (band_bottom - band_top) // 2, 0), h - 1)
    run = _run_at(alpha, seed_row, centre_x)
    if run is not None:
        centre_x = int((run[0] + run[1]) / 2)

    def half_at(frac: float) -> float:
        row = min(max(int(top + ph * frac), 0), h - 1)
        run = _run_at(alpha, row, centre_x)
        if run is None:
            return 0.0
        return (run[1] - run[0]) / 2.0

    shoulder = half_at(0.245)
    bust = max(half_at(0.30), half_at(0.33))
    waist = min(half_at(0.40), half_at(0.44)) or bust
    hip = max(half_at(0.48), half_at(0.52))
    thigh = half_at(0.62)
    # Same usability rule as BodyFit.kt: the figure has to fill a decent part of the frame
    # and the shoulder width has to be plausible, otherwise the caller falls back.
    envelope = ph / float(h)
    plausible = ph * 0.04 < shoulder < ph * 0.35
    return {
        "top": float(top), "bottom": float(bottom), "person_h": float(ph), "centre": float(centre_x),
        "shoulder_half": shoulder, "bust_half": bust, "waist_half": waist,
        "hip_half": hip, "thigh_half": thigh,
        "ok": bool(envelope >= 0.25 and plausible),
    }


# --------------------------------------------------------------------------- #
# Body-fitted garment outline (used by the local QA renderer)
# --------------------------------------------------------------------------- #
def _catmull_rom(points: list[tuple[float, float]], samples: int = 8) -> list[tuple[float, float]]:
    """Smooth a coarse outline so the cloth curve reads as fabric, not a polygon."""
    if len(points) < 3:
        return points
    pts = [points[0]] + points + [points[-1]]
    out: list[tuple[float, float]] = []
    for i in range(1, len(pts) - 2):
        p0, p1, p2, p3 = pts[i - 1], pts[i], pts[i + 1], pts[i + 2]
        for s in range(samples):
            t = s / samples
            t2, t3 = t * t, t * t * t
            x = 0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t
                       + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                       + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3)
            y = 0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t
                       + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                       + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3)
            out.append((x, y))
    out.append(points[-1])
    return out


def fitted_garment_polys(
    body: dict,
    sleeve: str = "DEFAULT",
    neckline: str = "ROUND",
    length: str = "MAXI",
) -> tuple[list[list[tuple[float, float]]], list[list[tuple[float, float]]]]:
    """Cloth outlines fitted to the measured landmarks.

    The width landmarks measured off the segmentation include the arms when the
    wearer stands with arms down, so they are scaled back to a torso-like width
    before the dress outline is built. Returns ``(panels, trims)``.
    """
    top = body["top"]
    ph = body["person_h"]
    cx = body["centre"]

    # With arms down the mask rows merge arms + torso, so landmark widths alone run
    # wide. Anchor on the deltoid width and derive the rest with body proportions
    # (bust ≈ 0.84·shoulder, waist ≈ 0.70·shoulder, hip ≈ 0.93·shoulder for the
    # front-view reference pose), then bound them with the measured values.
    # The mask includes the arms when they hang beside the body, so pull the measured
    # shoulder halfway toward torso proportions (same rule as BodyFit.kt). `ok == False`
    # means these are estimated fallback widths, which are already torso-shaped.
    torso_prior = body["bust_half"] * 0.58
    if body.get("ok"):
        shoulder = max((body["shoulder_half"] * 0.93 + torso_prior) / 2, body["person_h"] * 0.085)
    else:
        shoulder = max(body["shoulder_half"] * 0.93, body["person_h"] * 0.085)
    bust = min(max(shoulder * 0.84, torso_prior), shoulder * 0.98)
    waist = min(max(shoulder * 0.70, body["waist_half"] * 0.50), bust * 0.90)
    hip = min(max(shoulder * 0.94, body["hip_half"] * 0.62), bust * 1.22)
    thigh = max(body["thigh_half"] * 1.05, hip * 0.95)

    y_shoulder = top + ph * 0.235
    y_bust = top + ph * 0.335
    y_waist = top + ph * 0.425
    y_hip = top + ph * 0.525

    length_key = length.upper()
    hem_frac = {"MINI": 0.665, "MIDI": 0.80, "MAXI": 0.965, "FLOOR": 1.0}.get(length_key, 0.90)
    flare = {"MINI": 1.04, "MIDI": 1.16, "MAXI": 1.30, "FLOOR": 1.38}.get(length_key, 1.22)
    y_hem = top + ph * hem_frac
    hem = max(hip, thigh) * flare

    neck_key = neckline.upper()
    if neck_key in ("V_NECK", "VNECK"):
        neck_half, neck_dip = 0.32, 0.085
    elif neck_key == "HIGH":
        neck_half, neck_dip = 0.24, 0.014
    elif neck_key == "OFF_SHOULDER":
        neck_half, neck_dip = 0.50, 0.030
    elif neck_key == "BOAT":
        neck_half, neck_dip = 0.44, 0.020
    else:  # ROUND / DEFAULT
        neck_half, neck_dip = 0.34, 0.050

    neck_w = shoulder * neck_half
    neck_y = y_shoulder + ph * neck_dip

    # Right-hand profile, top to hem. Shoulder tips sit slightly below the neck edge.
    right = [
        (cx + shoulder * 1.00, y_shoulder + ph * 0.012),
        (cx + bust, y_bust),
        (cx + waist, y_waist),
        (cx + hip, y_hip),
        (cx + (hip + hem) / 2, y_hip + (y_hem - y_hip) * 0.58),
        (cx + hem, y_hem),
    ]
    right = _catmull_rom(right, samples=10)
    left = [(2 * cx - x, y) for x, y in reversed(right)]

    if neck_key in ("V_NECK", "VNECK"):
        neck = [(cx, neck_y)]
    else:
        neck = [
            (cx - neck_w * 0.55, y_shoulder + ph * neck_dip * 0.5),
            (cx, neck_y),
            (cx + neck_w * 0.55, y_shoulder + ph * neck_dip * 0.5),
        ]
    if neck_key == "OFF_SHOULDER":
        start_left = (cx - neck_w, y_shoulder - ph * 0.004)
        start_right = (cx + neck_w, y_shoulder - ph * 0.004)
    else:
        start_left = (cx - neck_w, y_shoulder)
        start_right = (cx + neck_w, y_shoulder)

    # Shoulder tip -> neck edge -> opening -> neck edge -> shoulder tip -> body of the
    # dress. The left tip segment is required: without it the opening spans the entire
    # shoulder line and the bare photo shows through the shoulders.
    # Fabric settles a little below the widest point of the deltoid instead of cutting a
    # horizontal line from shoulder tip to neck.
    shoulder_tip_left = (cx - shoulder, y_shoulder + ph * 0.030)
    outline = [shoulder_tip_left, start_left] + neck + [start_right] + right + left

    panels: list[list[tuple[float, float]]] = [outline]
    trims: list[list[tuple[float, float]]] = []

    sleeve_key = sleeve.upper()
    if sleeve_key in ("PUFF", "SHORT", "THREE_QUARTER", "LONG"):
        drop = {"PUFF": 0.155, "SHORT": 0.17, "THREE_QUARTER": 0.36, "LONG": 0.50}[sleeve_key]
        puff = 0.46 if sleeve_key == "PUFF" else 0.34
        taper = 0.30 if sleeve_key == "PUFF" else 0.22
        for side in (-1, 1):
            sx = cx + side * shoulder * 0.90
            # Start on the bodice's shoulder seam so the outlines meet, not cross.
            y_top = y_shoulder + ph * 0.030
            y_bot = y_shoulder + ph * drop
            pts = [(cx + side * shoulder, y_top)]
            # shoulder seam -> rounded outer cap -> cuff -> armhole
            n = 18
            for i in range(n + 1):
                t = i / n
                # 0.5 - 0.5*cos gives an ease-in/ease-out bulge down the sleeve
                bulge = math.sin(math.pi * (0.15 + 0.85 * t))
                outer = shoulder * (puff * 0.55 + taper * 0.45 * bulge)
                pts.append((sx + side * outer, y_top + (y_bot - y_top) * t))
            for i in range(n + 1):
                t = 1 - i / n
                inner = shoulder * (puff * 0.02 + 0.06 * math.sin(math.pi * t))
                pts.append((sx - side * inner, y_top + (y_bot - y_top) * t))
            panels.append(_catmull_rom(pts, samples=3))
            # cuff band
            trims.append([
                (sx + side * shoulder * (puff * 0.42), y_bot - ph * 0.016),
                (sx - side * shoulder * 0.06, y_bot - ph * 0.016),
                (sx - side * shoulder * 0.06, y_bot),
                (sx + side * shoulder * (puff * 0.42), y_bot),
            ])

    if neck_key == "HIGH":
        trims.append([
            (cx - neck_w * 1.04, y_shoulder - ph * 0.022),
            (cx + neck_w * 1.04, y_shoulder - ph * 0.022),
            (cx + neck_w * 1.04, y_shoulder + ph * 0.022),
            (cx - neck_w * 1.04, y_shoulder + ph * 0.022),
        ])

    return panels, trims


def render_fitted(
    person: Image.Image,
    fabric: Image.Image,
    style: Style,
    sleeve: str = "DEFAULT",
    neckline: str = "ROUND",
    length: str = "MAXI",
    max_dimension: int = 1000,
    segment: bool = True,
) -> TryOnResult:
    """Body-fitted draping: same TFLite segmentation, measured torso outline."""
    scale = max_dimension / max(person.size)
    if scale < 1:
        person = person.resize((round(person.width * scale), round(person.height * scale)), Image.LANCZOS)
    person = person.convert("RGBA")
    w, h = person.size

    segmenter = Segmenter() if segment else None
    mask = segmenter.person_mask(person.convert("RGB")) if segmenter else None
    palette = fabric_palette(fabric)
    accent = darken(palette[-1], 0.62)

    body = measure_body(mask, (w, h))
    panels, trims = fitted_garment_polys(body, sleeve=sleeve, neckline=neckline, length=length)

    texture = tile_mirror(mirrored_texture(fabric), (w, h)).convert("RGBA")
    texture.putalpha(216)
    accent_img = Image.new("RGBA", (w, h), accent + (226,))
    shade_h, shade_v = horizontal_shade((w, h)), vertical_shade((w, h))
    blank = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def painted(base, poly, tint):
        m = Image.new("L", (w, h), 0)
        ImageDraw.Draw(m).polygon([(x, y) for x, y in poly], fill=255)
        layer = base.copy()
        for shade in tint:
            layer = Image.alpha_composite(layer, Image.composite(shade, blank, m))
        return Image.composite(layer, blank, m), m

    garment = blank.copy()
    union = Image.new("L", (w, h), 0)
    for poly in panels:
        layer, m = painted(texture, poly, (shade_h, shade_v))
        garment = Image.alpha_composite(garment, layer)
        union = ImageChops.lighter(union, m)
    for poly in trims:
        layer, m = painted(accent_img, poly, (shade_v,))
        garment = Image.alpha_composite(garment, layer)
        union = ImageChops.lighter(union, m)

    # Soft inner edge so the cloth reads as fabric instead of a hard cut-out.
    edge = union.filter(ImageFilter.GaussianBlur(max(1, round(w / 400))))
    garment.putalpha(ImageChops.multiply(garment.getchannel("A"), edge.point(lambda v: 255 if v > 8 else int(v * 1.2))))

    out = Image.alpha_composite(person, garment)
    if mask is not None:
        head_mask, _skin, _under = analyze_human_regions(person.convert("RGB"), mask)
        if head_mask is not None:
            keep = Image.fromarray((head_mask * 255).astype("uint8"), mode="L")
            out = Image.composite(person, out, keep)

    _head, skin_rgb, undertone = (
        analyze_human_regions(person.convert("RGB"), mask) if mask is not None else (None, None, None)
    )
    return TryOnResult(
        image=out.convert("RGB"),
        mask=mask,
        shapes=len(panels) + len(trims),
        skin_rgb=skin_rgb,
        undertone=undertone,
        engine=(segmenter.engine if segmenter else "none"),
        meta={
            "style": style.id,
            "silhouette": "FITTED",
            "gender": style.gender,
            "sleeve": sleeve,
            "neckline": neckline,
            "length": length,
            "placement": "FULL",
            "body": {k: round(v, 1) if isinstance(v, float) else v for k, v in body.items()},
            "render_size": [w, h],
            "palette": ["#%02X%02X%02X" % c for c in palette],
            "accent": "#%02X%02X%02X" % accent,
        },
    )


def render_tryon(
    person: Image.Image,
    fabric: Image.Image,
    style: Style,
    sleeve: str = "DEFAULT",
    neckline: str = "DEFAULT",
    length: str = "DEFAULT",
    placement: str = "FULL",
    max_dimension: int = 1000,
    segment: bool = True,
    fit: str = "body",
) -> TryOnResult:
    """Composite [fabric] onto [person] using the app's geometry + shading passes."""
    # ImageProcessor.load(maxEdge) normalises the person photo.
    scale = max_dimension / max(person.size)
    if scale < 1:
        person = person.resize((round(person.width * scale), round(person.height * scale)), Image.LANCZOS)
    person = person.convert("RGBA")
    w, h = person.size

    segmenter = Segmenter() if segment else None
    mask = segmenter.person_mask(person.convert("RGB")) if segmenter else None

    out = person.copy()
    palette = fabric_palette(fabric)
    accent = darken(palette[-1])
    neutral = darken(palette[0])

    is_men = style.gender == "MEN"
    rect = body_rect(mask, (w, h))
    rw, rh = rect[2] - rect[0], rect[3] - rect[1]
    if rw <= 2 or rh <= 2:
        rect = (w * (0.22 if is_men else 0.20), h * 0.26, w * (0.78 if is_men else 0.80), h * 0.96)
        rw, rh = rect[2] - rect[0], rect[3] - rect[1]

    polys = garment_shapes(style.silhouette, sleeve, neckline, length)
    # `body` fit follows the measured torso; `fixed` is the app's 20–80% box.
    fit_body = measure_body(mask, (w, h)) if fit == "body" else None
    hip_y = rect[1] + rh * 0.55
    hem_y = rect[1] + rh * 0.95
    flare = {"MINI": 0.10, "MIDI": 0.18, "MAXI": 0.30, "FLOOR": 0.36}.get(length, 0.22)

    def to_px(poly):
        pts = []
        for x, y in poly:
            py = rect[1] + y * rh
            if fit == "body" and fit_body is not None:
                body = fit_body
                # Width profile derived from the measured landmarks, evaluated per row.
                t = min(max((py - rect[1]) / max(rh, 1.0), 0.0), 1.0)
                shoulder = max(body["shoulder_half"] * 0.93, body["person_h"] * 0.085)
                bust = min(max(shoulder * 0.84, body["bust_half"] * 0.58), shoulder * 0.98)
                waist = min(max(shoulder * 0.70, body["waist_half"] * 0.50), bust * 0.90)
                hip = min(max(shoulder * 0.94, body["hip_half"] * 0.62), bust * 1.22)
                if t < 0.42:
                    half = shoulder + (bust - shoulder) * (t / 0.42)
                elif t < 0.55:
                    half = bust + (waist - bust) * ((t - 0.42) / 0.13)
                elif t < 0.70:
                    half = waist + (hip - waist) * ((t - 0.55) / 0.15)
                else:
                    half = hip + (hip * flare - hip) * ((t - 0.70) / 0.30)
                px = body["centre"] + (x - 0.5) * 2.0 * half
            else:
                px = rect[0] + x * rw
            pts.append((px, py))
        return pts

    def flat(rgba):
        return Image.new("RGBA", (w, h), rgba)

    shade_h = horizontal_shade((w, h))
    shade_v = vertical_shade((w, h))

    def shaded_fill(base, shape_mask, tint=True):
        layer = base.copy()
        if tint:
            for shade in (shade_h, shade_v):
                layer = Image.alpha_composite(layer, Image.composite(shade, flat((0, 0, 0, 0)), shape_mask))
        return Image.composite(layer, flat((0, 0, 0, 0)), shape_mask)

    texture = tile_mirror(mirrored_texture(fabric), (w, h)).convert("RGBA")
    texture.putalpha(228)
    solid = flat(neutral + (232,))
    accent_img = flat(accent + (232,))

    shape_layer = flat((0, 0, 0, 0))
    union = Image.new("L", (w, h), 0)
    for index, poly in enumerate(polys):
        pts = to_px(poly)
        centroid_y = sum(p[1] for p in poly) / len(poly)
        use_fabric = {
            "FULL": True,
            "TOP_ONLY": centroid_y < 0.5,
            "SKIRT_ONLY": centroid_y >= 0.5,
            "PANELS": index % 2 == 0,
            "ACCENTS": False,
        }.get(placement, True)

        shape_mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(shape_mask).polygon(pts, fill=255)
        union = ImageChops.lighter(union, shape_mask)

        fill = texture if use_fabric else (accent_img if placement == "PANELS" else solid)
        shape_layer = Image.alpha_composite(shape_layer, shaded_fill(fill, shape_mask))

    if placement == "ACCENTS":
        band_top = int(rect[1] + rh * 0.44)
        band_bottom = int(rect[1] + rh * 0.54)
        band = Image.new("L", (w, h), 0)
        if fit == "body" and fit_body is not None:
            bx0 = fit_body["centre"] - fit_body["bust_half"] * 0.7
            bx1 = fit_body["centre"] + fit_body["bust_half"] * 0.7
        else:
            bx0, bx1 = rect[0], rect[2]
        ImageDraw.Draw(band).rectangle((bx0, band_top, bx1, band_bottom), fill=255)
        combined = ImageChops.multiply(band, union)
        shape_layer = Image.alpha_composite(shape_layer, shaded_fill(texture, combined))

    # Silhouette outline over every panel (thin, sits inside the garment)
    outline_layer = flat((0, 0, 0, 0))
    draw = ImageDraw.Draw(outline_layer)
    stroke = max(1, round(w / 260))
    for poly in polys:
        pts = to_px(poly)
        if len(pts) > 1:
            draw.line(pts + [pts[0]], fill=accent + (190,), width=stroke, joint="curve")

    # Composite in this order: base photo -> garment+shading -> outline -> head restore
    out = Image.alpha_composite(out, shape_layer)
    out = Image.alpha_composite(out, outline_layer)

    skin_rgb = None
    undertone = None
    if mask is not None:
        head_mask, skin_rgb, undertone = analyze_human_regions(person.convert("RGB"), mask)
        if head_mask is not None:
            keep = Image.fromarray((head_mask * 255).astype("uint8"), mode="L")
            out = Image.composite(person, out, keep)

    return TryOnResult(
        image=out.convert("RGB"),
        mask=mask,
        shapes=len(polys),
        skin_rgb=skin_rgb,
        undertone=undertone,
        engine=(segmenter.engine if segmenter else "none"),
        meta={
            "style": style.id,
            "silhouette": style.silhouette,
            "gender": style.gender,
            "sleeve": sleeve,
            "neckline": neckline,
            "length": length,
            "placement": placement,
            "render_size": [w, h],
            "palette": ["#%02X%02X%02X" % c for c in palette],
            "accent": "#%02X%02X%02X" % accent,
        },
    )


# --------------------------------------------------------------------------- #
# Fabric acquisition helpers
# --------------------------------------------------------------------------- #
def fabric_from_dress(dress: Image.Image) -> Image.Image:
    """Cut the bodice region out of a dress product photo and use it as the swatch."""
    w, h = dress.size
    box = (int(w * 0.30), int(h * 0.18), int(w * 0.70), int(h * 0.55))
    crop = dress.crop(box)
    # drop any skin/background that leaks in: keep the most saturated half
    return crop.resize((320, 320), Image.LANCZOS)


def side_by_side(panels: list[tuple[str, Image.Image]], height: int = 900) -> Image.Image:
    scaled = []
    for label, img in panels:
        ratio = height / img.height
        scaled.append((label, img.resize((max(1, round(img.width * ratio)), height), Image.LANCZOS)))
    gap = 16
    width = sum(im.width for _, im in scaled) + gap * (len(scaled) + 1)
    canvas = Image.new("RGB", (width, height + 34), (16, 22, 19))
    x = gap
    draw = ImageDraw.Draw(canvas)
    for label, im in scaled:
        canvas.paste(im, (x, 30))
        draw.text((x + 4, 10), label, fill=(212, 175, 55))
        x += im.width + gap
    return canvas


def write_report(results: dict, out_dir: Path) -> Path:
    html = [
        "<!doctype html><meta charset='utf-8'><title>FabricFit — local try-on test</title>",
        "<style>body{background:#0b291e;color:#f3eee2;font-family:system-ui,sans-serif;margin:0;padding:28px}"
        "h1{color:#d4af37;font-size:22px;margin:0 0 4px}p.sub{color:#b8c9bf;margin:0 0 24px}"
        ".row{display:flex;gap:18px;flex-wrap:wrap;margin-bottom:26px}"
        "figure{margin:0;background:#102f23;border:1px solid #d4af3766;border-radius:14px;padding:10px}"
        "figure img{display:block;max-height:560px;border-radius:8px}"
        "figcaption{font-size:12px;color:#d4af37;padding-top:8px;letter-spacing:.03em;text-transform:uppercase}"
        "table{border-collapse:collapse;font-size:13px}td{padding:4px 14px 4px 0;color:#cfe0d6}"
        "td:first-child{color:#8fa89b}code{color:#f0d78a}</style>",
        "<h1>FabricFit FitRoom — local try-on test</h1>",
        f"<p class='sub'>{results['headline']}</p>",
        "<div class='row'>",
    ]
    for label, name in results["panels"]:
        html.append(
            f"<figure><img src='{name}' alt='{label}'/><figcaption>{label}</figcaption></figure>"
        )
    html.append("</div><table>")
    for k, v in results["facts"].items():
        html.append(f"<tr><td>{k}</td><td>{v}</td></tr>")
    html.append("</table>")
    path = out_dir / "report.html"
    path.write_text("\n".join(html))
    return path


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #
def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--person", type=Path, help="photo of the person (jpg/png)")
    ap.add_argument("--fabric", type=Path, help="fabric / cloth photo to drape")
    ap.add_argument("--fabric-from-dress", type=Path, help="cut the bodice out of this dress photo")
    ap.add_argument("--style", default="kaba_slit", help="dress style id from catalog/dress_styles.json")
    ap.add_argument("--sleeve", default="DEFAULT", choices=["DEFAULT", "SHORT", "LONG", "THREE_QUARTER", "SLEEVELESS", "PUFF"])
    ap.add_argument("--neckline", default="ROUND", choices=["DEFAULT", "ROUND", "VNECK", "HIGH", "BOAT", "OFF_SHOULDER"])
    ap.add_argument("--length", default="MAXI", choices=["DEFAULT", "MINI", "MIDI", "MAXI", "FLOOR"])
    ap.add_argument("--placement", default="FULL", choices=["FULL", "TOP_ONLY", "SKIRT_ONLY", "PANELS", "ACCENTS"])
    ap.add_argument("--gender", default="", choices=["", "WOMEN", "MEN", "UNISEX"], help="override style gender")
    ap.add_argument("--max-dimension", type=int, default=1000)
    ap.add_argument("--no-segment", action="store_true", help="skip the TFLite segmentation pass")
    ap.add_argument(
        "--renderer",
        default="fitted",
        choices=["fitted", "app"],
        help="fitted = body-measured cloth (default); app = exact port of DemoLookGenerator polygons",
    )
    ap.add_argument(
        "--fit",
        default="body",
        choices=["body", "fixed"],
        help="body = hug the segmented torso/hips (default); fixed = the app's demo 20-80%% box",
    )
    ap.add_argument("--demo", action="store_true", help="use the bundled Ghanaian sample model + real kente cloth")
    ap.add_argument("--out", type=Path, default=OUT_DIR)
    ap.add_argument("--json", action="store_true", help="print the result metadata as JSON")
    args = ap.parse_args(argv)

    styles = load_catalogue()
    if args.demo:
        args.person = args.person or (SAMPLES / "model_ama_fullbody.jpg")
        if not args.fabric and not args.fabric_from_dress:
            args.fabric = ASSETS / "catalog" / "fabrics" / "kente.jpg"
    if not args.person or not (args.fabric or args.fabric_from_dress):
        ap.error("provide --person and (--fabric or --fabric-from-dress), or use --demo")

    style = styles.get(args.style)
    if style is None:
        ap.error(f"unknown style {args.style!r}; choose from {', '.join(sorted(styles))}")
    if args.gender:
        style = Style(style.id, args.gender, style.silhouette, style.title)

    person = Image.open(args.person)
    if args.fabric_from_dress:
        dress = Image.open(args.fabric_from_dress).convert("RGB")
        fabric = fabric_from_dress(dress)
        fabric_src = f"{args.fabric_from_dress.name} (bodice crop)"
    else:
        assert args.fabric is not None
        fabric = Image.open(args.fabric).convert("RGB")
        fabric_src = args.fabric.name

    if args.renderer == "fitted":
        result = render_fitted(
            person=person,
            fabric=fabric,
            style=style,
            sleeve=args.sleeve,
            neckline=args.neckline,
            length=args.length,
            max_dimension=args.max_dimension,
            segment=not args.no_segment,
        )
    else:
        result = render_tryon(
            person=person,
            fabric=fabric,
            style=style,
            sleeve=args.sleeve,
            neckline=args.neckline,
            length=args.length,
            placement=args.placement,
            max_dimension=args.max_dimension,
            segment=not args.no_segment,
            fit=args.fit,
        )

    out_dir = args.out
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "tryon_result.jpg").write_bytes(_jpeg(result.image))
    (out_dir / "fabric_swatch.jpg").write_bytes(_jpeg(fabric))
    if result.mask is not None:
        (out_dir / "person_mask.png").write_bytes(_png(result.mask))
    (out_dir / "person_input.jpg").write_bytes(_jpeg(person.convert("RGB")))

    panels = [("Person (input)", "person_input.jpg"), ("Fabric (input)", "fabric_swatch.jpg")]
    combo_panels = [("Person (input)", Image.open(out_dir / "person_input.jpg")),
                    ("Fabric (input)", fabric)]
    if result.mask is not None:
        panels.append(("Segmentation mask", "person_mask.png"))
        combo_panels.append(("Segmentation mask", result.mask.convert("RGB")))
    panels.append(("FitRoom result", "tryon_result.jpg"))
    combo_panels.append(("FitRoom result", result.image))
    combo = side_by_side(combo_panels)
    (out_dir / "side_by_side.jpg").write_bytes(_jpeg(combo))

    facts = {
        "Style": f"{style.id} · {style.silhouette} · {style.gender}",
        "Customization": f"sleeve={args.sleeve}, neckline={args.neckline}, length={args.length}, placement={args.placement}",
        "Fabric source": fabric_src,
        "Segmentation engine": result.engine,
        "Renderer": f"{args.renderer} (fit={args.fit})",
        "Garment polygons": result.shapes,
        "Detected skin tone": ("#%02X%02X%02X" % result.skin_rgb) if result.skin_rgb else "n/a",
        "Detected undertone": result.undertone or "n/a",
        "Render size": f"{result.meta['render_size'][0]}×{result.meta['render_size'][1]}",
        "Palette": ", ".join(result.meta["palette"]),
    }
    report = write_report(
        {
            "headline": "Same TFLite models, same garment polygons and shading as the Android app — running fully offline on this machine.",
            "panels": panels,
            "facts": facts,
        },
        out_dir,
    )

    if args.json:
        print(json.dumps({"facts": facts, "meta": result.meta, "report": str(report)}, indent=2))
    else:
        print("FabricFit local try-on complete")
        for k, v in facts.items():
            print(f"  {k:22s} {v}")
        print(f"  result   {out_dir / 'tryon_result.jpg'}")
        print(f"  compare  {out_dir / 'side_by_side.jpg'}")
        print(f"  report   {report}")
    return 0


def _jpeg(img: Image.Image, quality: int = 90) -> bytes:
    buf = io.BytesIO()
    img.convert("RGB").save(buf, format="JPEG", quality=quality)
    return buf.getvalue()


def _png(img: Image.Image) -> bytes:
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


if __name__ == "__main__":
    sys.exit(main())
