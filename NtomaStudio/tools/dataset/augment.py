#!/usr/bin/env python3
"""Expand raw photos into an augmented training set.

Two tasks (docs/ml_roadmap.md):
  --task fabric  (default): raw/<CLASS>/*.jpg -> augmented/<CLASS>/ (label_schema.json)
  --task pattern:           raw_pattern/<CLASS>/*.jpg -> augmented_pattern/<CLASS>/
                            (label_schema_pattern.json)

Writes <n>_<variant>.jpg with photorealistic, label-preserving transforms:
random crop/scale, rotation, brightness/contrast/saturation jitter, mild
perspective, JPEG re-compression.

Pure Pillow — no deep-learning dependency, runs anywhere.

Usage: python3 tools/dataset/augment.py [--task fabric|pattern] [--per-image 10] [--size 224]
"""
import argparse
import random
from pathlib import Path

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parent

# Two training tasks share this pipeline (docs/ml_roadmap.md):
#   fabric  — fabric-family classifier: raw/<CLASS> -> augmented/<CLASS>
#   pattern — visible-pattern classifier: raw_pattern/<CLASS> -> augmented_pattern/<CLASS>
TASK_DIRS = {
    "fabric": ("raw", "augmented"),
    "pattern": ("raw_pattern", "augmented_pattern"),
}


def jitter(img: Image.Image, rng: random.Random) -> Image.Image:
    for cls, lo, hi in ((ImageEnhance.Brightness, 0.7, 1.3),
                        (ImageEnhance.Contrast, 0.75, 1.25),
                        (ImageEnhance.Color, 0.7, 1.3)):
        img = cls(img).enhance(rng.uniform(lo, hi))
    return img


def perspective(img: Image.Image, rng: random.Random) -> Image.Image:
    w, h = img.size
    d = int(0.06 * min(w, h))
    coeffs = [rng.randint(-d, d) for _ in range(8)]
    # find_coeffs-style approximation is overkill; small affine via quad warp
    try:
        return img.transform(img.size, Image.QUAD,
                             data=_quad_coeffs(w, h, coeffs), resample=Image.BILINEAR)
    except Exception:
        return img


def _quad_coeffs(w, h, c):
    # map unit-ish quad corners with small jitter; PIL QUAD wants 8 source coords
    (x0, y0, x1, y1, x2, y2, x3, y3) = c
    return (x0, y0, w + x1, y1, w + x2, h + y2, x3, h + y3)


def variant(img: Image.Image, size: int, rng: random.Random) -> Image.Image:
    w, h = img.size
    side = int(min(w, h) * rng.uniform(0.55, 1.0))
    x = rng.randint(0, max(0, w - side))
    y = rng.randint(0, max(0, h - side))
    img = img.crop((x, y, x + side, y + side))
    img = img.rotate(rng.uniform(-20, 20), expand=False, fillcolor=(128, 128, 128))
    img = perspective(img, rng)
    img = img.resize((size, size), Image.BILINEAR)
    img = jitter(img, rng)
    if rng.random() < 0.5:
        img = ImageOps.mirror(img)
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--per-image", type=int, default=10)
    ap.add_argument("--size", type=int, default=224)
    ap.add_argument("--seed", type=int, default=7)
    ap.add_argument("--task", choices=sorted(TASK_DIRS), default="fabric",
                    help="which dataset to augment (fabric family vs visible pattern)")
    args = ap.parse_args()
    rng = random.Random(args.seed)

    raw_name, out_name = TASK_DIRS[args.task]
    raw_dir = ROOT / raw_name
    out_root = ROOT / out_name
    if not raw_dir.exists():
        raise SystemExit(f"no {raw_name}/ dir at {raw_dir} — see label_schema{_pattern if args.task == 'pattern' else ''}.json for layout")
    total = 0
    out_root.mkdir(exist_ok=True)
    for cls_dir in sorted(raw_dir.iterdir()):
        if not cls_dir.is_dir():
            continue
        out_dir = out_root / cls_dir.name
        out_dir.mkdir(exist_ok=True)
        for i, src in enumerate(sorted(cls_dir.glob("*.jpg")) + sorted(cls_dir.glob("*.png"))):
            base = Image.open(src).convert("RGB")
            for v in range(args.per_image):
                out = out_dir / f"{i:03d}_{v:02d}.jpg"
                variant(base, args.size, rng).save(out, "JPEG", quality=88)
                total += 1
    print(f"wrote {total} augmented images ({args.task} task) to {out_root}")


if __name__ == "__main__":
    main()
