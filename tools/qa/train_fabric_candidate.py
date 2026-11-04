#!/usr/bin/env python3
"""Train and honestly evaluate a candidate 17-family fabric classifier.

Input is a prepare_kaggle.py export with independent train/val/test directories.
The script refuses evaluation when any family lacks an image in any split. It
uses ImageNet transfer learning, early stopping, per-class metrics, and explicit
small-dataset/shipping gates. The TFLite output is a candidate artifact only;
this script never modifies app assets.

Requires tensorflow-cpu>=2.16 (or tensorflow on Kaggle).
"""
from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import sys
from collections import Counter
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
SCHEMA = ROOT / "FabricFit/tools/dataset/label_schema.json"
BAR_TRAIN = 300
BAR_CLAIM = 1000


def label_counts(data_root: Path, classes: list[str]) -> dict[str, Counter]:
    """Count real files on disk, then cross-check labels.csv and split leakage."""
    counts = {split: Counter() for split in ("train", "val", "test")}
    split_files: dict[str, set[str]] = {}
    for split in counts:
        seen: set[str] = set()
        for label in classes:
            directory = data_root / split / label
            if directory.is_dir():
                for path in directory.iterdir():
                    if path.is_file() and not path.name.startswith("."):
                        counts[split][label] += 1
                        seen.add(path.name)
        split_files[split] = seen
    overlaps = {
        f"{left}-{right}": sorted(split_files[left] & split_files[right])
        for left, right in (("train", "val"), ("train", "test"), ("val", "test"))
    }
    if any(overlaps.values()):
        raise SystemExit(
            "ERROR: the same image filename appears in more than one split — "
            f"fix the split before training: {json.dumps(overlaps)}"
        )
    labels_path = data_root / "labels.csv"
    if labels_path.is_file():
        declared = {split: Counter() for split in counts}
        with labels_path.open(newline="", encoding="utf-8") as handle:
            for row in csv.DictReader(handle):
                if row.get("split") in declared and row.get("label") in classes:
                    declared[row["split"]][row["label"]] += 1
        mismatched = {
            split: {
                label: {"files": counts[split][label], "labelsCsv": declared[split][label]}
                for label in classes
                if counts[split][label] != declared[split][label]
            }
            for split in counts
        }
        mismatched = {split: rows for split, rows in mismatched.items() if rows}
        if mismatched:
            raise SystemExit(
                "ERROR: labels.csv does not match the images on disk — "
                f"dataset is inconsistent: {json.dumps(mismatched)}"
            )
    return counts


def evaluate(model, dataset, classes: list[str]) -> dict:
    hits = Counter()
    totals = Counter()
    confusion = np.zeros((len(classes), len(classes)), dtype=np.int64)
    predictions = []
    offset = 0
    paths = list(dataset.file_paths)
    for batch_x, batch_y in dataset:
        scores = model.predict(batch_x, verbose=0)
        true_indices = np.argmax(batch_y.numpy(), axis=1)
        predicted_indices = np.argmax(scores, axis=1)
        for batch_index, (true_index, predicted_index) in enumerate(zip(true_indices, predicted_indices)):
            true_name = classes[int(true_index)]
            predicted_name = classes[int(predicted_index)]
            totals[true_name] += 1
            hits[true_name] += int(true_index == predicted_index)
            confusion[true_index, predicted_index] += 1
            ordered = np.argsort(-scores[batch_index])[:3]
            predictions.append({
                "path": paths[offset + batch_index] if offset + batch_index < len(paths) else "",
                "expected": true_name,
                "predicted": predicted_name,
                "confidence": float(scores[batch_index, predicted_index]),
                "correct": bool(true_index == predicted_index),
                "top3": [{"label": classes[int(i)], "score": float(scores[batch_index, i])} for i in ordered],
            })
        offset += len(batch_x)
    total = int(sum(totals.values()))
    correct = int(sum(hits.values()))
    per_class = {
        name: {
            "images": int(totals[name]),
            "correct": int(hits[name]),
            "accuracy": float(hits[name] / totals[name]) if totals[name] else None,
        }
        for name in classes
    }
    return {
        "images": total,
        "correct": correct,
        "accuracy": correct / total if total else None,
        "macroAccuracy": float(np.mean([hits[name] / totals[name] for name in classes if totals[name]])) if total else None,
        "perClass": per_class,
        "confusionMatrix": confusion.tolist(),
        "predictions": predictions,
    }


def markdown(report: dict) -> str:
    dataset = report["dataset"]
    validation = report["validation"]
    test = report["test"]
    lines = [
        "# Manually reviewed fabric-classifier candidate",
        "",
        f"Generated: `{report['generatedAt']}`",
        "",
        "> **Candidate experiment only. This model is not installed in FabricFit.**",
        "> Images were visually reviewed online, but labels are not expert fiber/heritage",
        "> certifications. A small held-out score—even 100%—is not a real-world accuracy",
        "> guarantee and must not be advertised as one.",
        "",
        f"- Training images per family (minimum): **{dataset['minTrainPerClass']}**",
        f"- Honest training bar ({BAR_TRAIN}/class): **{'met' if dataset['minTrainPerClass'] >= BAR_TRAIN else 'not met'}**",
        f"- Production/accuracy-claim bar ({BAR_CLAIM}/class): **{'met' if dataset['minTrainPerClass'] >= BAR_CLAIM else 'not met'}**",
        f"- Validation: **{validation['correct']}/{validation['images']} ({validation['accuracy']:.3%})**",
        f"- Unseen held-out test: **{test['correct']}/{test['images']} ({test['accuracy']:.3%})**",
        f"- Test macro accuracy: **{test['macroAccuracy']:.3%}**",
        f"- Candidate TFLite SHA-256: `{report['candidate']['sha256']}`",
        "",
        "## Per-family results",
        "",
        "| Family | Train | Val | Test | Val correct | Test correct | Test accuracy |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: |",
    ]
    for name in report["classes"]:
        val_row = validation["perClass"][name]
        test_row = test["perClass"][name]
        lines.append(
            f"| {name} | {dataset['trainCounts'][name]} | {dataset['valCounts'][name]} | "
            f"{dataset['testCounts'][name]} | {val_row['correct']}/{val_row['images']} | "
            f"{test_row['correct']}/{test_row['images']} | {test_row['accuracy']:.1%} |"
        )
    lines += [
        "",
        "## Unseen test predictions",
        "",
        "| Expected | Predicted | Confidence | Match | Image |",
        "| --- | --- | ---: | --- | --- |",
    ]
    for row in test["predictions"]:
        path = Path(row["path"]).name.replace("|", "\\|")
        lines.append(
            f"| {row['expected']} | {row['predicted']} | {row['confidence']:.3f} | "
            f"{'yes' if row['correct'] else 'no'} | {path} |"
        )
    lines += [
        "",
        "Raw top-3 scores, confusion matrices, dataset counts, and model provenance are in `candidate-report.json`.",
        "",
    ]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--report-dir", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--size", type=int, default=160)
    parser.add_argument("--epochs-head", type=int, default=8)
    parser.add_argument("--epochs-fine", type=int, default=10)
    parser.add_argument("--smoke", action="store_true", help="64px, 1+1 epochs; pipeline test only")
    parser.add_argument("--no-pretrained", action="store_true",
                        help="random-init backbone for offline pipeline tests only")
    args = parser.parse_args()
    if args.smoke:
        args.size, args.epochs_head, args.epochs_fine = 64, 1, 1

    classes = sorted(json.loads(SCHEMA.read_text())["classes"])
    data_root = Path(args.data_dir)
    report_dir = Path(args.report_dir)
    report_dir.mkdir(parents=True, exist_ok=True)
    counts = label_counts(data_root, classes)
    missing = {
        name: {split: counts[split][name] for split in ("train", "val", "test")}
        for name in classes
        if any(counts[split][name] < 1 for split in ("train", "val", "test"))
    }
    if missing:
        failure = {
            "generatedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
            "status": "dataset incomplete; training refused",
            "missingSplitImages": missing,
            "explanation": "Every family needs at least one independent train, validation, and test image. No accuracy was reported.",
        }
        (report_dir / "candidate-report.json").write_text(json.dumps(failure, indent=2) + "\n")
        (report_dir / "candidate-report.md").write_text(
            "# Candidate training refused\n\nEvery family needs independent train, validation, and test images.\n\n"
            f"```json\n{json.dumps(missing, indent=2)}\n```\n"
        )
        print(json.dumps(failure, indent=2), file=sys.stderr)
        return 2

    import tensorflow as tf

    common_args = {
        "image_size": (args.size, args.size), "batch_size": 16,
        "label_mode": "categorical", "class_names": classes,
    }
    train_ds = tf.keras.utils.image_dataset_from_directory(
        data_root / "train", shuffle=True, seed=13, **common_args)
    val_ds = tf.keras.utils.image_dataset_from_directory(
        data_root / "val", shuffle=False, **common_args)
    test_ds = tf.keras.utils.image_dataset_from_directory(
        data_root / "test", shuffle=False, **common_args)

    augmentation = tf.keras.Sequential([
        tf.keras.layers.RandomRotation(0.035),
        tf.keras.layers.RandomZoom(0.10),
        tf.keras.layers.RandomContrast(0.10),
    ], name="training_only_augmentation")
    inputs = tf.keras.Input(shape=(args.size, args.size, 3))
    x = augmentation(inputs)
    x = tf.keras.applications.mobilenet_v3.preprocess_input(x)
    backbone = tf.keras.applications.MobileNetV3Small(
        input_shape=(args.size, args.size, 3), include_top=False,
        weights=None if args.no_pretrained else "imagenet", include_preprocessing=False,
    )
    backbone.trainable = False
    x = backbone(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.30)(x)
    outputs = tf.keras.layers.Dense(len(classes), activation="softmax")(x)
    model = tf.keras.Model(inputs, outputs)

    train_total = sum(counts["train"].values())
    class_weight = {
        index: train_total / (len(classes) * counts["train"][name])
        for index, name in enumerate(classes)
    }
    callbacks = [tf.keras.callbacks.EarlyStopping(
        monitor="val_accuracy", mode="max", patience=3,
        restore_best_weights=True, min_delta=0.002,
    )]
    model.compile(
        optimizer=tf.keras.optimizers.Adam(8e-4),
        loss="categorical_crossentropy", metrics=["accuracy"],
    )
    head_history = model.fit(
        train_ds, validation_data=val_ds, epochs=args.epochs_head,
        callbacks=callbacks, class_weight=class_weight, verbose=2,
    )

    backbone.trainable = True
    for layer in backbone.layers[:-20]:
        layer.trainable = False
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-5),
        loss="categorical_crossentropy", metrics=["accuracy"],
    )
    fine_callbacks = [tf.keras.callbacks.EarlyStopping(
        monitor="val_accuracy", mode="max", patience=4,
        restore_best_weights=True, min_delta=0.002,
    )]
    fine_history = model.fit(
        train_ds, validation_data=val_ds, epochs=args.epochs_fine,
        callbacks=fine_callbacks, class_weight=class_weight, verbose=2,
    )

    validation = evaluate(model, val_ds, classes)
    test = evaluate(model, test_ds, classes)
    model_path = Path(args.out)
    model_path.parent.mkdir(parents=True, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    model_bytes = converter.convert()
    model_path.write_bytes(model_bytes)
    labels_path = model_path.with_name("candidate-labels.txt")
    labels_path.write_text("\n".join(classes) + "\n")

    report = {
        "generatedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
        "status": "candidate evaluated",
        "notProductionAccuracy": True,
        "eligibleForShipping": False if min(counts["train"].values()) < BAR_CLAIM else "human expert review still required",
        "pipelineSmokeOnly": bool(args.smoke),
        "classes": classes,
        "dataset": {
            "trainCounts": {name: counts["train"][name] for name in classes},
            "valCounts": {name: counts["val"][name] for name in classes},
            "testCounts": {name: counts["test"][name] for name in classes},
            "minTrainPerClass": min(counts["train"].values()),
            "honestTrainingBar": BAR_TRAIN,
            "accuracyClaimBar": BAR_CLAIM,
        },
        "training": {
            "architecture": "MobileNetV3Small ImageNet transfer learning",
            "inputSize": args.size,
            "headEpochsRun": len(head_history.history["loss"]),
            "fineTuneEpochsRun": len(fine_history.history["loss"]),
            "headHistory": head_history.history,
            "fineTuneHistory": fine_history.history,
        },
        "validation": validation,
        "test": test,
        "candidate": {
            "file": model_path.name,
            "bytes": len(model_bytes),
            "sha256": hashlib.sha256(model_bytes).hexdigest(),
            "installedInApp": False,
        },
    }
    (report_dir / "candidate-report.json").write_text(json.dumps(report, indent=2) + "\n")
    (report_dir / "candidate-report.md").write_text(markdown(report))
    print(json.dumps({
        "validationAccuracy": validation["accuracy"],
        "heldOutTestAccuracy": test["accuracy"],
        "minTrainPerClass": report["dataset"]["minTrainPerClass"],
        "installedInApp": False,
    }, indent=2))
    if test["accuracy"] == 1.0:
        print("::warning::The small held-out set scored 100%; this is NOT a 100% real-world accuracy claim.")
    print("::notice::Candidate TFLite generated for review only; app assets were not changed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
