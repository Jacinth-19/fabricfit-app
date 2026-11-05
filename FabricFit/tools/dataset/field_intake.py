#!/usr/bin/env python3
"""Validate real field photos and drop the survivors into tools/dataset/raw/.

Field days are the only way past the 300-photos-per-family bar, so the intake has
to be as strict as the online scraper: a mislabeled or blurry photo is worse than
a missing one. This tool takes a folder of photos plus one metadata row per photo
and only writes what passes every gate.

  family,file,location,contributor,consent,captured_at,notes
  KENTE,Bonwire_day1/IMG_0042.jpg,Bonwire weaving centre,AMA,photos may be used for classifier training,2026-11-02,flat cloth on loom
  LACE,Accra_shop/IMG_0119.jpg,Makola market,AMA,photos may be used for classifier training,2026-11-03,white guipure on roll

Gates applied to every row
  1. label exists in label_schema.json
  2. file exists, decodes, shortest side >= --min-side
  3. sharp enough: re-blur edge-energy ratio <= --max-blur-ratio (rejects blur)
     and visible texture: edge energy >= --min-edge-energy
  4. not a duplicate: 64-bit dHash, hamming <= --threshold, against raw/ and the
     current batch; a near-duplicate under a DIFFERENT label is a hard conflict
  5. consent text present (people-free cloth shots still need the row filled in)

Dry-run by default. Pass --apply to write raw/<CLASS>/ and append raw/CREDITS.csv,
which is what prepare_kaggle.py reads for provenance.

Usage:
  python3 FabricFit/tools/dataset/field_intake.py field/manifest.csv --root field
  python3 FabricFit/tools/dataset/field_intake.py field/manifest.csv --root field --apply
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import sys
from collections import Counter
from datetime import date
from pathlib import Path

from PIL import Image, ImageFilter, ImageOps, ImageStat

HERE = Path(__file__).resolve().parent
RAW = HERE / "raw"
SCHEMA = HERE / "label_schema.json"
CREDITS = RAW / "CREDITS.csv"
CREDIT_FIELDS = ["source", "source_key", "class", "filename", "title", "creator",
                 "license", "landing_url", "file_url", "retrieved"]
REQUIRED = ["family", "file", "contributor", "consent", "license"]
ALLOWED_LICENSES = {"CC BY 4.0", "CC BY-SA 4.0", "CC0 1.0"}
FIELD_FIELDS = ["family", "filename", "contributor", "consent", "license", "location", "captured_at", "notes"]
TRAIN_BAR = 300
CLAIM_BAR = 1000


def dhash(image: Image.Image) -> int:
    pixels = image.convert("L").resize((9, 8)).tobytes()
    bits = 0
    for y in range(8):
        row = pixels[y * 9:(y + 1) * 9]
        for x in range(8):
            bits = (bits << 1) | (1 if row[x] > row[x + 1] else 0)
    return bits


def hamming(left: int, right: int) -> int:
    return bin(left ^ right).count("1")


LAPLACIAN = ImageFilter.Kernel((3, 3), [0, 1, 0, 1, -4, 1, 0, 1, 0], scale=1)


def laplacian_variance(image: Image.Image) -> float:
    return ImageStat.Stat(image.convert("L").filter(LAPLACIAN)).var[0]


def sharpness(image: Image.Image) -> tuple[float, float]:
    """Return (edge energy, blur ratio) without OpenCV.

    Absolute Laplacian variance cannot tell a soft-but-sharp low-contrast cloth
    photo from a blurry one. Re-blurring once and comparing is scale-free: a sharp
    photo loses most of its edge energy, an already-blurred photo barely changes.
    ratio >= ~0.55 means blurred; a near-zero edge energy means a blank/failed shot.
    """
    original = laplacian_variance(image)
    reblurred = laplacian_variance(image.filter(ImageFilter.GaussianBlur(1.0)))
    return original, reblurred / original if original > 1e-6 else 1.0


def sanitize(name: str, limit: int = 70) -> str:
    cleaned = "".join(ch if ch.isalnum() or ch in "._-" else "_" for ch in name)
    while "__" in cleaned:
        cleaned = cleaned.replace("__", "_")
    return cleaned.strip("._")[:limit] or "photo"


def load_pool() -> dict[int, tuple[str, str]]:
    pool: dict[int, tuple[str, str]] = {}
    if not RAW.is_dir():
        return pool
    for path in RAW.rglob("*"):
        if path.is_file() and path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}:
            try:
                with Image.open(path) as image:
                    pool[dhash(ImageOps.exif_transpose(image))] = (path.parent.name, path.name)
            except Exception:
                continue
    return pool


def coverage(classes: list[str]) -> dict[str, int]:
    counts = Counter()
    for name in classes:
        directory = RAW / name
        if directory.is_dir():
            counts[name] = sum(
                1 for path in directory.iterdir()
                if path.is_file() and path.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}
            )
    return {name: counts[name] for name in classes}


def read_rows(path: Path) -> list[dict]:
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        missing = [column for column in REQUIRED if column not in (reader.fieldnames or [])]
        if missing:
            raise SystemExit(
                f"{path} is missing required columns {missing}; expected "
                f"{REQUIRED + ['location', 'captured_at', 'notes']}"
            )
        return list(reader)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", help="CSV with family,file,contributor,consent,...")
    parser.add_argument("--root", default=".", help="folder the manifest's file paths are relative to")
    parser.add_argument("--apply", action="store_true", help="write into raw/ (default: dry run)")
    parser.add_argument("--min-side", type=int, default=320)
    parser.add_argument("--max-blur-ratio", type=float, default=0.55,
                        help="re-blur edge-energy ratio above this is treated as blurred")
    parser.add_argument("--min-edge-energy", type=float, default=3.0,
                        help="below this the photo has no visible texture (blank/failed shot)")
    parser.add_argument("--threshold", type=int, default=6, help="dHash distance counted as duplicate")
    parser.add_argument("--report", default="", help="optional JSON report path")
    args = parser.parse_args()

    classes = json.loads(SCHEMA.read_text())["classes"]
    root = Path(args.root)
    rows = read_rows(Path(args.manifest))
    pool = load_pool()
    batch: dict[int, tuple[str, str]] = {}

    accepted, rejected = [], []
    for line_no, row in enumerate(rows, start=2):
        label = (row.get("family") or row.get("label") or "").strip().upper()
        relative = (row.get("file") or "").strip()
        consent = (row.get("consent") or "").strip()
        contributor = (row.get("contributor") or "").strip()

        def reject(reason: str) -> None:
            rejected.append({"row": line_no, "label": label, "file": relative, "reason": reason})
            print(f"  x row {line_no} {relative or '?'} [{label or '?'}]: {reason}")

        if label not in classes:
            reject(f"unknown family {label!r}")
            continue
        if not relative:
            reject("no file path")
            continue
        if not consent:
            reject("no documented, informed consent")
            continue
        if not contributor:
            reject("no contributor recorded")
            continue
        license_name = (row.get("license") or "").strip()
        if license_name not in ALLOWED_LICENSES:
            reject(f"license must be explicitly selected from {sorted(ALLOWED_LICENSES)}")
            continue
        path = (root / relative).resolve()
        try:
            path.relative_to(root.resolve())
        except ValueError:
            reject("file path escapes the supplied --root")
            continue
        if not path.is_file():
            reject("file not found")
            continue
        try:
            with Image.open(path) as source:
                image = ImageOps.exif_transpose(source)
                image.load()
        except Exception as error:
            reject(f"cannot decode ({error})")
            continue
        width, height = image.size
        if min(width, height) < args.min_side:
            reject(f"too small ({width}x{height} < {args.min_side})")
            continue
        edge_energy, blur_ratio = sharpness(image)
        if edge_energy < args.min_edge_energy:
            reject(f"no fabric texture detected (edge energy {edge_energy:.1f})")
            continue
        if blur_ratio > args.max_blur_ratio:
            reject(f"looks blurred (re-blur ratio {blur_ratio:.2f} > {args.max_blur_ratio})")
            continue
        image_hash = dhash(image)
        duplicate = ""
        for other_hash, (other_label, other_name) in {**pool, **batch}.items():
            if hamming(image_hash, other_hash) <= args.threshold:
                if other_label == label:
                    duplicate = f"duplicate of {other_label}/{other_name}"
                else:
                    duplicate = f"conflicts with {other_label}/{other_name} (same image, other label)"
                break
        if duplicate:
            reject(duplicate)
            continue

        target_dir = RAW / label
        stem = sanitize(f"{label}_{path.stem}")
        name = f"{stem}.jpg"
        suffix = 2
        existing = {item.name for item in target_dir.iterdir()} if target_dir.is_dir() else set()
        while name in existing or any(entry["savedAs"] == f"{label}/{name}" for entry in accepted):
            name = f"{stem}_{suffix}.jpg"
            suffix += 1

        batch[image_hash] = (label, name)
        accepted.append({
            "label": label,
            "source": str(path),
            "savedAs": f"{label}/{name}",
            "contributor": contributor,
            "consent": consent,
            "license": license_name,
            "location": (row.get("location") or "").strip(),
            "capturedAt": (row.get("captured_at") or "").strip(),
            "notes": (row.get("notes") or "").strip(),
            "size": f"{width}x{height}",
            "edgeEnergy": round(edge_energy, 1),
            "blurRatio": round(blur_ratio, 3),
            "_image": image,
        })
        print(f"  + {label}/{name}  ({width}x{height}, blur ratio {blur_ratio:.2f})")

    if args.apply and accepted:
        RAW.mkdir(parents=True, exist_ok=True)
        new_credits = not CREDITS.exists()
        with CREDITS.open("a", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=CREDIT_FIELDS)
            if new_credits:
                writer.writeheader()
            for entry in accepted:
                label, name = entry["savedAs"].split("/", 1)
                directory = RAW / label
                directory.mkdir(parents=True, exist_ok=True)
                buffer = io.BytesIO()
                entry["_image"].convert("RGB").save(buffer, format="JPEG", quality=92, optimize=True)
                (directory / name).write_bytes(buffer.getvalue())
                writer.writerow({
                    "source": "field",
                    "source_key": f"field:{entry['contributor']}:{name}",
                    "class": label,
                    "filename": name,
                    "title": Path(entry["source"]).name,
                    "creator": entry["contributor"],
                    "license": entry["license"],
                    "landing_url": "",
                    "file_url": "",
                    "retrieved": entry["capturedAt"] or date.today().isoformat(),
                })

        metadata_path = RAW / "FIELD_METADATA.csv"
        new_metadata = not metadata_path.exists()
        with metadata_path.open("a", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=FIELD_FIELDS)
            if new_metadata:
                writer.writeheader()
            for entry in accepted:
                label, name = entry["savedAs"].split("/", 1)
                writer.writerow({
                    "family": label, "filename": name,
                    "contributor": entry["contributor"], "consent": entry["consent"],
                    "license": entry["license"], "location": entry["location"],
                    "captured_at": entry["capturedAt"], "notes": entry["notes"],
                })

    counts = coverage(classes)
    if accepted:
        for entry in accepted:
            counts[entry["label"]] += 1
    summary = {
        "reviewed": len(rows),
        "accepted": len(accepted),
        "rejected": len(rejected),
        "applied": bool(args.apply),
        "perFamily": counts,
        "belowTrainingBar": [name for name in classes if counts[name] < TRAIN_BAR],
        "belowClaimBar": [name for name in classes if counts[name] < CLAIM_BAR],
        "rejections": rejected,
        "acceptedFiles": [{k: v for k, v in entry.items() if k != "_image"} for entry in accepted],
    }
    if args.report:
        Path(args.report).write_text(json.dumps(summary, indent=2) + "\n")

    print(f"\n{len(accepted)} accepted, {len(rejected)} rejected"
          f"{'' if args.apply else ' (dry run — rerun with --apply to write)'}")
    print(f"raw/ coverage: {sum(counts.values())} photos, "
          f"{len(classes) - len(summary['belowTrainingBar'])}/{len(classes)} families at the "
          f"{TRAIN_BAR}-photo training bar")
    short = sorted(((counts[name], name) for name in classes))[:5]
    print("thinnest families: " + ", ".join(f"{name} {count}" for count, name in short))
    if args.apply and accepted:
        print("next: review the new photos with\n"
              "  python3 tools/qa/review_fabric_dataset.py sheets --data-dir "
              f"{RAW.relative_to(HERE.parent.parent)} --output-dir qa-artifacts/field-review")
    return 0


if __name__ == "__main__":
    sys.exit(main())
