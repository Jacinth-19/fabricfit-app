#!/usr/bin/env python3
"""Train the 17-class fabric classifier on a prepare_kaggle.py export (Kaggle).

Attach the uploaded dataset to a Kaggle Notebook, upload this script (or add
it to the dataset), then:

  python3 train_on_kaggle.py --data-dir /kaggle/input/<your-slug>

Pipeline mirrors tools/dataset/train_finetune.py (MobileNetV3-Small, ImageNet
weights, frozen head then fine-tune, quantized TFLite out) but reads the
leak-free train/val/test splits from the export instead of raw/ + a random
validation_split — so the val/test numbers are trustworthy.

HONESTY GATE (printed at the end): results from datasets under
1000 train images/class are EXPERIMENT ONLY and must never be quoted as
accuracy claims or shipped as an app asset.

Requires: tensorflow (preinstalled on Kaggle).
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from collections import Counter
from pathlib import Path

BAR_TRAIN = 300
BAR_CLAIM = 1000


def find_data(root: Path) -> Path:
    """Accept the export root itself, a dir containing it, or the Kaggle input dir."""
    candidates = [root, root / "train", *sorted(root.glob("*/train")), *sorted(root.glob("*/*/train"))]
    for c in candidates:
        if c.name == "train" and c.is_dir():
            return c.parent
    raise SystemExit(f"no train/ folder found under {root} — pass --data-dir")


def per_class_accuracy(model, ds, class_names):
    hits = Counter({c: 0 for c in class_names})
    total = Counter({c: 0 for c in class_names})
    import tensorflow as tf
    for xb, yb in ds:
        pred = model.predict(xb, verbose=0)
        y_true = tf.argmax(yb, axis=1).numpy()
        y_pred = tf.argmax(pred, axis=1).numpy()
        for t, p in zip(y_true, y_pred):
            total[class_names[t]] += 1
            if t == p:
                hits[class_names[t]] += 1
    return {c: (hits[c] / total[c] if total[c] else 0.0) for c in class_names}, dict(total)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--data-dir", default=".", help="export root containing train/ val/ test/")
    ap.add_argument("--epochs-head", type=int, default=8)
    ap.add_argument("--epochs-fine", type=int, default=12)
    ap.add_argument("--size", type=int, default=224)
    ap.add_argument("--out", default="fabric_kaggle.tflite")
    ap.add_argument("--smoke", action="store_true", help="64px, 2+2 epochs — pipeline test only")
    args = ap.parse_args()
    if args.smoke:
        args.size, args.epochs_head, args.epochs_fine = 64, 2, 2

    import tensorflow as tf

    data_root = find_data(Path(args.data_dir))
    train_dir, val_dir = data_root / "train", data_root / "val"
    test_dir = data_root / "test"

    train_ds = tf.keras.utils.image_dataset_from_directory(
        train_dir, image_size=(args.size, args.size), batch_size=32,
        label_mode="categorical", shuffle=True, seed=13,
    )
    val_ds = tf.keras.utils.image_dataset_from_directory(
        val_dir, image_size=(args.size, args.size), batch_size=32,
        label_mode="categorical", shuffle=False, seed=13,
    )
    class_names = train_ds.class_names
    num_classes = len(class_names)
    print(f"classes ({num_classes}): {class_names}")

    inputs = tf.keras.Input(shape=(args.size, args.size, 3))
    x = tf.keras.applications.mobilenet_v3.preprocess_input(inputs)
    backbone = tf.keras.applications.MobileNetV3Small(
        input_shape=(args.size, args.size, 3), include_top=False, weights="imagenet")
    backbone.trainable = False
    x = backbone(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.25)(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax")(x)
    model = tf.keras.Model(inputs, outputs)

    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3),
                  loss="categorical_crossentropy", metrics=["accuracy"])
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs_head, verbose=2)

    backbone.trainable = True
    for layer in backbone.layers[:-24]:
        layer.trainable = False
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-5),
                  loss="categorical_crossentropy", metrics=["accuracy"])
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs_fine, verbose=2)

    _, val_acc = model.evaluate(val_ds, verbose=0)
    print(f"validation accuracy: {val_acc:.3f}")
    per_cls, counts = per_class_accuracy(model, val_ds, class_names)
    print("\nper-class validation accuracy:")
    for c in class_names:
        print(f"  {c:16s} {per_cls[c]:.2f}  (n={counts.get(c, 0)})")

    if test_dir.is_dir():
        test_ds = tf.keras.utils.image_dataset_from_directory(
            test_dir, image_size=(args.size, args.size), batch_size=32,
            label_mode="categorical", shuffle=False,
        )
        _, test_acc = model.evaluate(test_ds, verbose=0)
        print(f"held-out test accuracy: {test_acc:.3f}")
    else:
        print("no test/ split in export — train/val only (expected for tiny classes)")

    # train counts straight from labels.csv (honesty gate)
    train_counts = Counter()
    labels_path = data_root / "labels.csv"
    if labels_path.is_file():
        with labels_path.open(newline="", encoding="utf-8") as fh:
            for row in csv.DictReader(fh):
                if row.get("split") == "train":
                    train_counts[row.get("label")] += 1
    min_train = min((train_counts[c] for c in class_names), default=0)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite = converter.convert()
    Path(args.out).write_bytes(tflite)
    print(f"\nwrote {args.out} ({len(tflite) / 1e6:.2f} MB)")

    print("\n=== HONESTY GATE ===")
    if args.smoke:
        print("SMOKE RUN — pipeline test only. Never quote these numbers.")
    elif min_train >= BAR_CLAIM:
        print(f"min train images/class = {min_train} >= {BAR_CLAIM}: claims still need")
        print("human review + field-verified labels before shipping any asset.")
    elif min_train >= BAR_TRAIN:
        print(f"min train images/class = {min_train} >= {BAR_TRAIN}: honest EXPERIMENT allowed.")
        print(f"NOT a claim (need {BAR_CLAIM}/class), NOT shippable. Report macro + per-class only.")
    else:
        print(f"min train images/class = {min_train} < {BAR_TRAIN}: EXPLORATORY ONLY.")
        print("Collect more real photos (label_schema.json) before quoting any metric.")
    print("json:", json.dumps({"val_acc": round(float(val_acc), 4),
                               "min_train": min_train,
                               "per_class": {c: round(per_cls[c], 3) for c in class_names}}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
