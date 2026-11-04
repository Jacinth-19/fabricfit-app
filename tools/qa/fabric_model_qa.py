#!/usr/bin/env python3
"""Audit FabricFit's bundled models and smoke-test textile models on real photos.

Requires: pip install ai-edge-litert numpy pillow

contracts: load and invoke every shipped .tflite with its declared tensor contract.
online: run the fabric classifier, weave extractor, and ImageNet OOD model on
        freely licensed photos collected by tools/dataset/scrape/. The online
        labels are search-derived and unvetted, so the resulting top-1 rate is a
        diagnostic smoke result, never a production accuracy claim.
"""
from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import math
import sys
from collections import defaultdict
from pathlib import Path

import numpy as np
from ai_edge_litert.interpreter import Interpreter
from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "FabricFit/app/src/main/assets"
KOTLIN = ROOT / "FabricFit/app/src/main/java"
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}

CONTRACTS = {
    "ghanaian_fabric_classifier.tflite": {
        "role": "17-family textile appearance classifier",
        "input": [1, 128, 128, 3], "output": [1, 17], "dtype": "float32",
    },
    "weave_motif_extractor.tflite": {
        "role": "directional weave and motif feature extractor",
        "input": [1, 128, 128, 3], "output": [1, 6], "dtype": "float32",
    },
    "mobilenet_v2_1.0_224_quant.tflite": {
        "role": "ImageNet textile/out-of-distribution hint model",
        "input": [1, 224, 224, 3], "output": [1, 1001], "dtype": "uint8",
    },
    "selfie_segmentation.tflite": {
        "role": "portrait foreground segmentation",
        "input": [1, 256, 256, 3], "output": [1, 256, 256, 1], "dtype": "float32",
    },
    "deeplabv3_257_mv.tflite": {
        "role": "DeepLabV3 PASCAL person segmentation",
        "input": [1, 257, 257, 3], "output": [1, 257, 257, 21], "dtype": "float32",
    },
    "human_parsing_5class.tflite": {
        "role": "five-zone human parsing",
        "input": [1, 128, 128, 3], "output": [1, 128, 128, 5], "dtype": "float32",
    },
    "cloth_warper_shading.tflite": {
        "role": "cloth displacement and fold shading",
        "input": [1, 64, 64, 4], "output": [1, 64, 64, 3], "dtype": "float32",
    },
    "fitmatch_ranker.tflite": {
        "role": "16-feature recommendation ranker",
        "input": [1, 16], "output": [1, 1], "dtype": "float32",
    },
}


def now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def contract_markdown(report: dict) -> str:
    lines = [
        "# FabricFit bundled model contract audit",
        "",
        f"Generated: `{report['generatedAt']}`",
        "",
        f"**{report['passedModels']}/{report['expectedModels']} models loaded, invoked, and matched their declared contracts.**",
        "",
        "This verifies packaging and inference validity, not real-world model quality.",
        "Real-fabric results are documented separately in `real-fabric-report.md`.",
        "",
        "| Model | Purpose | Bytes | Input | Output | SHA-256 |",
        "| --- | --- | ---: | --- | --- | --- |",
    ]
    for model in report["models"]:
        lines.append(
            f"| `{model['name']}` | {model['role']} | {model['bytes']:,} | "
            f"{model['inputShape']} {model['inputDtype']} | {model['outputShape']} "
            f"{model['outputDtype']} | `{model['sha256'][:16]}…` |"
        )
    lines += [
        "",
        "Only the classifier, weave extractor, and ImageNet OOD model accept fabric",
        "photos. Person segmentation, cloth warping, and recommendation ranking require",
        "different inputs and are covered here by tensor/inference contract checks.",
        "",
    ]
    return "\n".join(lines)


def run_contracts(output: Path) -> int:
    expected_names = set(CONTRACTS)
    disk_names = {p.name for p in ASSETS.glob("*.tflite")}
    missing = sorted(expected_names - disk_names)
    unexpected = sorted(disk_names - expected_names)
    if missing or unexpected:
        print(f"Model asset mismatch: missing={missing}, unexpected={unexpected}", file=sys.stderr)
        return 2

    kotlin_text = "\n".join(
        path.read_text(encoding="utf-8", errors="replace") for path in KOTLIN.rglob("*.kt")
    )
    rows = []
    for name, expected in CONTRACTS.items():
        path = ASSETS / name
        interpreter = Interpreter(model_path=str(path))
        interpreter.allocate_tensors()
        inputs = interpreter.get_input_details()
        outputs = interpreter.get_output_details()
        if len(inputs) != 1 or len(outputs) != 1:
            raise AssertionError(f"{name}: expected one input/output, got {len(inputs)}/{len(outputs)}")
        tin, tout = inputs[0], outputs[0]
        if list(tin["shape"]) != expected["input"]:
            raise AssertionError(f"{name}: input {tin['shape']} != {expected['input']}")
        if list(tout["shape"]) != expected["output"]:
            raise AssertionError(f"{name}: output {tout['shape']} != {expected['output']}")
        if np.dtype(tin["dtype"]) != np.dtype(expected["dtype"]):
            raise AssertionError(f"{name}: input dtype {tin['dtype']} != {expected['dtype']}")
        if np.dtype(tout["dtype"]) != np.dtype(expected["dtype"]):
            raise AssertionError(f"{name}: output dtype {tout['dtype']} != {expected['dtype']}")
        interpreter.set_tensor(tin["index"], np.zeros(tin["shape"], dtype=tin["dtype"]))
        interpreter.invoke()
        result = interpreter.get_tensor(tout["index"])
        if np.issubdtype(result.dtype, np.floating) and not np.isfinite(result).all():
            raise AssertionError(f"{name}: non-finite inference output")
        if name not in kotlin_text:
            raise AssertionError(f"{name}: no runtime Kotlin reference found")
        row = {
            "name": name,
            "role": expected["role"],
            "bytes": path.stat().st_size,
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "inputShape": [int(v) for v in tin["shape"]],
            "outputShape": [int(v) for v in result.shape],
            "inputDtype": np.dtype(tin["dtype"]).name,
            "outputDtype": np.dtype(result.dtype).name,
            "runtimeReference": True,
            "zeroInputInference": "passed",
        }
        rows.append(row)
        print(f"PASS {name}: {row['inputShape']} -> {row['outputShape']}")

    report = {
        "generatedAt": now(),
        "expectedModels": len(CONTRACTS),
        "passedModels": len(rows),
        "allAssetsReferenced": True,
        "models": rows,
        "limitation": "Tensor contracts and zero-input inference do not establish model accuracy.",
    }
    write_json(output, report)
    output.with_suffix(".md").write_text(contract_markdown(report), encoding="utf-8")
    print(f"::notice::FabricFit model audit: {len(rows)}/{len(CONTRACTS)} bundled models passed.")
    return 0


def load_labels(name: str) -> list[str]:
    return [line.strip() for line in (ASSETS / name).read_text().splitlines() if line.strip()]


def rgb_array(path: Path, side: int, dtype=np.float32) -> np.ndarray:
    with Image.open(path) as source:
        image = ImageOps.exif_transpose(source).convert("RGB").resize((side, side), Image.BILINEAR)
        array = np.asarray(image, dtype=dtype)
    return np.ascontiguousarray(array[None])


def invoke(interpreter: Interpreter, input_array: np.ndarray) -> np.ndarray:
    tin = interpreter.get_input_details()[0]
    tout = interpreter.get_output_details()[0]
    interpreter.set_tensor(tin["index"], input_array)
    interpreter.invoke()
    return interpreter.get_tensor(tout["index"])[0]


def read_credits(data_dirs: list[Path]) -> dict[tuple[str, str], dict]:
    credits = {}
    for directory in data_dirs:
        path = directory / "CREDITS.csv"
        if not path.exists():
            continue
        with path.open(newline="", encoding="utf-8") as handle:
            for row in csv.DictReader(handle):
                credits[(row.get("class", ""), row.get("filename", ""))] = dict(row)
    return credits


def select_images(data_dirs: list[Path], classes: list[str], per_class: int) -> tuple[dict, list[str]]:
    selected = {cls: [] for cls in classes}
    missing = []
    for cls in classes:
        pools = []
        for directory in data_dirs:
            files = sorted(
                p for p in (directory / cls).glob("*") if p.suffix.lower() in IMAGE_EXTENSIONS
            ) if (directory / cls).is_dir() else []
            pools.append((directory, files))
        # Prefer one image per source before taking a second image from any source.
        for round_no in range(per_class):
            for directory, files in pools:
                if round_no < len(files) and len(selected[cls]) < per_class:
                    selected[cls].append((directory, files[round_no]))
        if not selected[cls]:
            missing.append(cls)
    return selected, missing


def make_contact_sheet(results: list[dict], output: Path) -> None:
    columns, tile_w, tile_h = 4, 240, 230
    rows = max(1, math.ceil(len(results) / columns))
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "#F7F1E5")
    draw = ImageDraw.Draw(sheet)
    for index, result in enumerate(results):
        x, y = (index % columns) * tile_w, (index // columns) * tile_h
        with Image.open(result["localPath"]) as source:
            thumb = ImageOps.fit(
                ImageOps.exif_transpose(source).convert("RGB"),
                (tile_w - 12, 160), method=Image.BILINEAR,
            )
        sheet.paste(thumb, (x + 6, y + 6))
        draw.text((x + 8, y + 171), f"#{index + 1} expected: {result['expected']}", fill="#173F32")
        draw.text(
            (x + 8, y + 188),
            f"predicted: {result['predicted']} ({result['confidence']:.2f})",
            fill="#173F32" if result["correct"] else "#8A2D2D",
        )
        draw.text((x + 8, y + 205), f"source: {result['source']}", fill="#4B4B4B")
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, quality=88, optimize=True)


def online_markdown(report: dict) -> str:
    summary = report["summary"]
    curated = report.get("evaluationType", "").startswith("manually curated")
    methodology = (
        [
            "> **Not a production accuracy claim.** These freely licensed Commons images were",
            "> manually selected from source descriptions and visually reviewed to remove obvious",
            "> search contamination. They are not expert fiber/heritage certifications; Gonja is",
            "> represented by a Northern Ghana smock proxy. One photo per family is only a",
            "> diagnostic smoke test, not a validation set or a measure of real-world accuracy.",
        ]
        if curated
        else [
            "> **Not a production accuracy claim.** These are freely licensed, search-derived",
            "> online photos with unvetted labels. Obvious irrelevant and mislabeled search results",
            "> remain in this diagnostic set. Do not interpret its score as cleaned model accuracy.",
        ]
    )
    lines = [
        "# Curated real online fabric smoke test" if curated else "# Unvetted online fabric search smoke test",
        "",
        f"Generated: `{report['generatedAt']}`",
        "",
        *methodology,
        "> The result below is raw classifier top-1, before Android's heuristic/OOD fusion.",
        "",
        f"- Model contract audit: **{report['contractSummary']}**",
        f"- Coverage: **{summary['coveredClasses']}/{summary['expectedClasses']} fabric families**",
        f"- Images evaluated: **{summary['evaluatedImages']}**",
        f"- Raw classifier top-1 agreement: **{summary['top1Agreement']:.3f}** "
        f"({summary['correctImages']}/{summary['evaluatedImages'] or 1})",
        f"- Missing families: **{', '.join(summary['missingClasses']) or 'none'}**",
        f"- Near-duplicates of classifier training/catalog images (dHash ≤ 6): "
        f"**{summary.get('catalogNearDuplicateImages', 0)}**",
        "",
        "The app remains an `ON_DEVICE_DEMO`; this smoke result must not be used as an",
        "advertised accuracy metric. Production validation needs hundreds to thousands of",
        "consented, human-verified field photos per family with leak-free held-out splits.",
        "",
        "## Per-family results",
        "",
        "| Family | Images | Correct | Raw top-1 agreement |",
        "| --- | ---: | ---: | ---: |",
    ]
    for cls, row in report["perClass"].items():
        lines.append(f"| {cls} | {row['images']} | {row['correct']} | {row['agreement']:.3f} |")
    lines += [
        "",
        "## Image-level predictions and attribution",
        "",
        "Thumbnails in `real-fabric-contact-sheet.jpg` remain under each source image's",
        "original license. Follow the landing-page links for attribution details.",
        "",
        "| # | Expected | Predicted (confidence) | Match | Source and license |",
        "| ---: | --- | --- | --- | --- |",
    ]
    for index, row in enumerate(report["results"], 1):
        landing = row.get("landingUrl") or row.get("fileUrl") or ""
        title = (row.get("title") or Path(row["localPath"]).name).replace("|", "\\|")
        creator = (row.get("creator") or "creator not supplied").replace("|", "\\|")
        source = f"[{title}]({landing}) — {creator}" if landing else f"{title} — {creator}"
        lines.append(
            f"| {index} | {row['expected']} | {row['predicted']} ({row['confidence']:.3f}) | "
            f"{'yes' if row['correct'] else 'no'} | {source}; {row.get('license') or 'see source'} |"
        )
    lines += ["", "Raw per-image scores and source URLs are in `real-fabric-report.json`.", ""]
    return "\n".join(lines)


def difference_hash(path: Path) -> int:
    """Match the dataset scraper's 64-bit dHash for catalog leakage screening."""
    with Image.open(path) as source:
        image = ImageOps.exif_transpose(source).convert("L").resize((9, 8))
    pixels = image.tobytes()
    value = 0
    for y in range(8):
        for x in range(8):
            value = (value << 1) | int(pixels[y * 9 + x] > pixels[y * 9 + x + 1])
    return value


def catalog_hashes() -> list[tuple[str, int]]:
    """Images used as classifier training sources in tools/build_ai_models.py."""
    paths = sorted((ASSETS / "catalog/fabrics").glob("*.jpg"))
    paths += [
        ASSETS / "catalog/heritage" / name
        for name in (
            "swatch_ankara.jpg", "swatch_batik.jpg", "swatch_kente.jpg",
            "swatch_lace.jpg", "scan_kente_closeup.jpg", "hero_kente_folds.jpg",
        )
    ]
    return [(str(path.relative_to(ASSETS)), difference_hash(path)) for path in paths if path.exists()]


def nearest_catalog_image(path: Path, catalog: list[tuple[str, int]]) -> dict:
    image_hash = difference_hash(path)
    name, distance = min(
        ((name, (image_hash ^ other).bit_count()) for name, other in catalog),
        key=lambda item: item[1],
        default=("", 64),
    )
    return {"path": name, "dHashDistance": distance, "nearDuplicate": distance <= 6}


def run_online(args: argparse.Namespace) -> int:
    data_dirs = [Path(p).resolve() for p in args.data_dir]
    classes = load_labels("ghanaian_fabric_labels.txt")
    catalog = catalog_hashes()
    imagenet_labels = load_labels("imagenet_labels.txt")
    selected, missing = select_images(data_dirs, classes, args.per_class)
    credits = read_credits(data_dirs)

    classifier = Interpreter(model_path=str(ASSETS / "ghanaian_fabric_classifier.tflite"))
    classifier.allocate_tensors()
    weave = Interpreter(model_path=str(ASSETS / "weave_motif_extractor.tflite"))
    weave.allocate_tensors()
    ood = Interpreter(model_path=str(ASSETS / "mobilenet_v2_1.0_224_quant.tflite"))
    ood.allocate_tensors()

    results = []
    errors = []
    for cls in classes:
        for directory, path in selected[cls]:
            try:
                classifier_scores = invoke(classifier, rgb_array(path, 128) / 255.0).astype(np.float32)
                if classifier_scores.shape != (17,) or not np.isfinite(classifier_scores).all():
                    raise ValueError(f"classifier output invalid: {classifier_scores.shape}")
                order = np.argsort(-classifier_scores)
                top3 = [
                    {"label": classes[int(i)], "score": float(classifier_scores[int(i)])}
                    for i in order[:3]
                ]
                weave_scores = invoke(weave, rgb_array(path, 128) / 255.0).astype(np.float32)
                ood_scores = invoke(ood, rgb_array(path, 224, np.uint8)).astype(np.float32) / 255.0
                ood_index = 1 + int(np.argmax(ood_scores[1:]))
                credit = credits.get((cls, path.name), {})
                predicted = top3[0]["label"]
                catalog_match = nearest_catalog_image(path, catalog)
                results.append({
                    "expected": cls,
                    "predicted": predicted,
                    "confidence": top3[0]["score"],
                    "correct": predicted == cls,
                    "top3": top3,
                    "weaveMetrics": [float(v) for v in weave_scores],
                    "imagenetHint": {
                        "label": imagenet_labels[ood_index] if ood_index < len(imagenet_labels) else str(ood_index),
                        "score": float(ood_scores[ood_index]),
                    },
                    "localPath": str(path),
                    "sourceDirectory": str(directory),
                    "source": credit.get("source", directory.name),
                    "title": credit.get("title", path.name),
                    "creator": credit.get("creator", ""),
                    "license": credit.get("license", ""),
                    "landingUrl": credit.get("landing_url", ""),
                    "fileUrl": credit.get("file_url", ""),
                    "catalogNearest": {
                        "path": catalog_match["path"],
                        "dHashDistance": catalog_match["dHashDistance"],
                    },
                    "catalogNearDuplicate": catalog_match["nearDuplicate"],
                })
                print(f"{cls}: -> {predicted} ({top3[0]['score']:.3f}) [{path.name}]")
            except Exception as exc:  # keep a complete report for diagnosis
                errors.append(f"{cls}/{path.name}: {exc}")

    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    correct = sum(1 for row in results if row["correct"])
    per_class = {}
    for cls in classes:
        rows = [row for row in results if row["expected"] == cls]
        cls_correct = sum(1 for row in rows if row["correct"])
        per_class[cls] = {
            "images": len(rows),
            "correct": cls_correct,
            "agreement": cls_correct / len(rows) if rows else 0.0,
        }
    contract_data = json.loads(Path(args.contracts).read_text()) if args.contracts else None
    curated = args.evaluation_type == "curated"
    report = {
        "generatedAt": now(),
        "evaluationType": (
            "manually curated freely licensed online real-textile smoke test"
            if curated else "unvetted freely licensed online real-photo smoke test"
        ),
        "labelReview": (
            "manual source-description and visual review; not expert fiber/heritage verification"
            if curated else "none; search-derived labels include known irrelevant and mislabeled images"
        ),
        "notProductionAccuracy": True,
        "fusionIncluded": False,
        "contractSummary": (
            f"{contract_data['passedModels']}/{contract_data['expectedModels']} passed"
            if contract_data else "not supplied"
        ),
        "summary": {
            "expectedClasses": len(classes),
            "coveredClasses": len(classes) - len(missing),
            "missingClasses": missing,
            "evaluatedImages": len(results),
            "correctImages": correct,
            "top1Agreement": correct / len(results) if results else 0.0,
            "catalogNearDuplicateImages": sum(1 for row in results if row["catalogNearDuplicate"]),
            "inferenceErrors": errors,
        },
        "perClass": per_class,
        "results": results,
    }
    write_json(output_dir / "real-fabric-report.json", report)
    (output_dir / "real-fabric-report.md").write_text(online_markdown(report), encoding="utf-8")
    if results:
        # The contact sheet uses relative paths only in the committed report; local
        # absolute paths in JSON are retained for reproducibility inside the runner.
        make_contact_sheet(results, output_dir / "real-fabric-contact-sheet.jpg")

    # Remove runner-local paths from the persisted JSON; image identity remains in
    # title/source/landing URL and the contact sheet is the visual reference.
    persisted_results = []
    for row in results:
        clean = dict(row)
        clean["localPath"] = Path(row["localPath"]).name
        clean.pop("sourceDirectory", None)
        persisted_results.append(clean)
    report["results"] = persisted_results
    write_json(output_dir / "real-fabric-report.json", report)

    rate = report["summary"]["top1Agreement"]
    print(f"::notice::Real-fabric QA: {len(results)} images, {len(classes)-len(missing)}/{len(classes)} classes, raw top-1={rate:.3f}.")
    if rate < 0.60:
        print(f"::warning::Raw online-fabric top-1 agreement is only {rate:.3f}; this baseline needs better real training data.")
    if errors:
        print(f"Inference errors: {errors}", file=sys.stderr)
        return 3
    if args.require_all_classes and missing:
        print(f"Missing online coverage for: {', '.join(missing)}", file=sys.stderr)
        return 2
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    contracts = sub.add_parser("contracts", help="audit all bundled TFLite models")
    contracts.add_argument("--output", required=True)
    online = sub.add_parser("online", help="evaluate textile models on scraped real photos")
    online.add_argument("--data-dir", action="append", required=True)
    online.add_argument("--contracts", help="contract JSON from the contracts command")
    online.add_argument("--output-dir", required=True)
    online.add_argument("--per-class", type=int, default=2)
    online.add_argument(
        "--evaluation-type", choices=("search", "curated"), default="search",
        help="controls methodology labels; curated means manually selected and visually reviewed",
    )
    online.add_argument("--require-all-classes", action="store_true")
    args = parser.parse_args()
    if args.command == "contracts":
        return run_contracts(Path(args.output))
    return run_online(args)


if __name__ == "__main__":
    sys.exit(main())
