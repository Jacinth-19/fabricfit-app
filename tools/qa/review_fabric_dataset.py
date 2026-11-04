#!/usr/bin/env python3
"""Build visual review sheets and export only human-reviewed fabric source rows.

Collect candidates with the existing licensed scrapers, then run:

  python3 tools/qa/review_fabric_dataset.py sheets \
    --data-dir FabricFit/tools/dataset/scraped/wikimedia \
    --data-dir FabricFit/tools/dataset/scraped/openverse \
    --data-dir FabricFit/tools/dataset/scraped/manifest \
    --output-dir qa-artifacts/review-round-1

Every unique candidate starts as PENDING. Near-duplicates are pre-marked reject,
but still appear on the sheets for review. After inspecting every thumbnail,
record accept/reject decisions in the index (or a small override CSV), then run:

  python3 tools/qa/review_fabric_dataset.py export \
    --index qa-artifacts/review-round-1/review-index.csv \
    --output-dir qa-artifacts/reviewed

No pending row is exported. The accepted manifest contains source URLs and
provenance only; large image datasets remain out of Git.
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "FabricFit/tools/dataset/scrape"))
import common  # noqa: E402

FIELDS = [
    "id", "label", "source", "filename", "title", "creator", "license",
    "landing_url", "file_url", "near_duplicate_of", "decision", "reason",
]
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}


def credits(data_dir: Path) -> dict[str, dict]:
    path = data_dir / "CREDITS.csv"
    if not path.is_file():
        return {}
    with path.open(newline="", encoding="utf-8") as handle:
        return {row.get("filename", ""): row for row in csv.DictReader(handle)}


def candidates(data_dirs: list[Path]) -> list[dict]:
    rows = []
    sequence = 0
    hashes: list[tuple[int, str, str]] = []
    for data_dir in data_dirs:
        credit_rows = credits(data_dir)
        for label in common.load_classes():
            directory = data_dir / label
            if not directory.is_dir():
                continue
            for path in sorted(directory.iterdir()):
                if not path.is_file() or path.suffix.lower() not in IMAGE_SUFFIXES:
                    continue
                try:
                    with Image.open(path) as source:
                        image = ImageOps.exif_transpose(source)
                        image.load()
                        image_hash = common.dhash(image)
                except Exception:
                    continue
                sequence += 1
                row_id = f"R{sequence:03d}"
                duplicate_of = ""
                for other_hash, other_id, other_label in hashes:
                    if label == other_label and common.hamming(image_hash, other_hash) <= 6:
                        duplicate_of = other_id
                        break
                hashes.append((image_hash, row_id, label))
                credit = credit_rows.get(path.name, {})
                rows.append({
                    "id": row_id,
                    "label": label,
                    "source": data_dir.name,
                    "filename": path.name,
                    "title": credit.get("title", path.name),
                    "creator": credit.get("creator", ""),
                    "license": credit.get("license", ""),
                    "landing_url": credit.get("landing_url", ""),
                    "file_url": credit.get("file_url", ""),
                    "near_duplicate_of": duplicate_of,
                    "decision": "reject" if duplicate_of else "pending",
                    "reason": f"near duplicate of {duplicate_of}" if duplicate_of else "",
                    "_path": str(path),
                })
    return rows


def write_index(rows: list[dict], path: Path) -> None:
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in FIELDS})


def sheet(rows: list[dict], label: str, output: Path) -> None:
    columns = 4
    cell_w, image_h, caption_h = 300, 235, 74
    row_count = (len(rows) + columns - 1) // columns
    canvas = Image.new("RGB", (columns * cell_w, 54 + row_count * (image_h + caption_h)), "#f7f3eb")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    draw.text((10, 12), f"{label} — {len(rows)} candidates; inspect every ID before export", fill="#17130f", font=font)
    for index, row in enumerate(rows):
        x = (index % columns) * cell_w
        y = 54 + (index // columns) * (image_h + caption_h)
        try:
            with Image.open(row["_path"]) as source:
                image = ImageOps.exif_transpose(source).convert("RGB")
                image.thumbnail((cell_w - 10, image_h - 10), Image.Resampling.LANCZOS)
                canvas.paste(image, (x + (cell_w - image.width) // 2, y + (image_h - image.height) // 2))
        except Exception:
            draw.text((x + 10, y + 80), "decode failed", fill="#a00000", font=font)
        marker = f" AUTO-DUP:{row['near_duplicate_of']}" if row["near_duplicate_of"] else ""
        draw.text((x + 7, y + image_h + 5), f"{row['id']}  {row['source']}{marker}", fill="#17130f", font=font)
        title = row["title"]
        # Two short lines keep the source title visible without hiding other IDs.
        draw.text((x + 7, y + image_h + 24), title[:46], fill="#41382e", font=font)
        draw.text((x + 7, y + image_h + 43), title[46:92], fill="#41382e", font=font)
    canvas.save(output, quality=90)


def sheets(args: argparse.Namespace) -> int:
    output = Path(args.output_dir)
    output.mkdir(parents=True, exist_ok=True)
    rows = candidates([Path(path) for path in args.data_dir])
    if not rows:
        print("No reviewable images found", file=sys.stderr)
        return 2
    write_index(rows, output / "review-index.csv")
    by_class: dict[str, list[dict]] = defaultdict(list)
    for row in rows:
        by_class[row["label"]].append(row)
    for label in common.load_classes():
        class_rows = by_class.get(label, [])
        if class_rows:
            sheet(class_rows, label, output / f"{label.lower()}-review.jpg")
    summary = {
        "candidates": len(rows),
        "automaticDuplicateRejects": sum(bool(row["near_duplicate_of"]) for row in rows),
        "pendingVisualReview": sum(row["decision"] == "pending" for row in rows),
        "perClass": dict(Counter(row["label"] for row in rows)),
        "instructions": "Inspect every class sheet; change pending decisions to accept or reject. No pending row can be exported.",
    }
    (output / "review-summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
    return 0


def read_csv(path: Path) -> list[dict]:
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def export(args: argparse.Namespace) -> int:
    rows = read_csv(Path(args.index))
    overrides = {row["id"]: row for row in read_csv(Path(args.decisions))} if args.decisions else {}
    # Provenance recovered from the source's own metadata (used only when the
    # staged credits row is incomplete). Decisions never live in this file.
    provenance = {}
    for path in args.provenance or []:
        for row in read_csv(Path(path)):
            provenance[row["id"]] = row
    output = Path(args.output_dir)
    output.mkdir(parents=True, exist_ok=True)
    accepted = []
    pending = []
    counts = defaultdict(Counter)
    for row in rows:
        override = overrides.get(row["id"], {})
        decision = (override.get("decision") or row.get("decision") or "pending").strip().lower()
        reason = (override.get("reason") or row.get("reason") or "").strip()
        if decision not in {"accept", "reject"}:
            pending.append(row["id"])
            continue
        counts[row["label"]][decision] += 1
        if decision == "accept":
            for key, column in (("file_url", "url"), ("license", "license"),
                                ("landing_url", "source_page"), ("title", "title"),
                                ("creator", "creator")):
                recovered = (provenance.get(row["id"]) or {}).get(column)
                if recovered and not row.get(key):
                    row[key] = recovered
            if not row.get("file_url") or not row.get("license") or not row.get("landing_url"):
                print(f"::error::{row['id']} lacks URL/license/provenance; reject it instead")
                pending.append(row["id"])
                continue
            accepted.append({
                "url": row["file_url"],
                "label": row["label"],
                "license": row["license"],
                "source_page": row["landing_url"],
                "title": row["title"],
                "creator": row["creator"],
            })
        write_row = dict(row)
        write_row["decision"] = decision
        write_row["reason"] = reason
        row.update(write_row)
    if pending and not args.allow_pending:
        print(f"Unreviewed or invalid rows: {', '.join(pending)}", file=sys.stderr)
        return 2
    write_index(rows, output / "reviewed-index.csv")
    with (output / "accepted-manifest.csv").open("w", newline="", encoding="utf-8") as handle:
        fields = ["url", "label", "license", "source_page", "title", "creator"]
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(accepted)
    summary = {
        "accepted": len(accepted),
        "rejected": sum(count["reject"] for count in counts.values()),
        "pending": pending,
        "perClass": {label: dict(counts[label]) for label in common.load_classes()},
        "notProductionGroundTruth": True,
        "reviewType": "manual visual/source review; not expert fiber or heritage certification",
    }
    (output / "reviewed-summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    make = commands.add_parser("sheets")
    make.add_argument("--data-dir", action="append", required=True)
    make.add_argument("--output-dir", required=True)
    make.set_defaults(function=sheets)
    apply = commands.add_parser("export")
    apply.add_argument("--index", required=True)
    apply.add_argument("--decisions", help="optional override CSV with id,decision,reason")
    apply.add_argument("--provenance", action="append",
                       help="optional CSV with id,url,license,source_page,title,creator for "
                            "candidates whose staged credits row was incomplete")
    apply.add_argument("--output-dir", required=True)
    apply.add_argument("--allow-pending", action="store_true")
    apply.set_defaults(function=export)
    args = parser.parse_args()
    return args.function(args)


if __name__ == "__main__":
    raise SystemExit(main())
