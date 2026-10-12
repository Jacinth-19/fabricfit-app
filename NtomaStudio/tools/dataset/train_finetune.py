#!/usr/bin/env python3
"""Fine-tune MobileNetV3-Small on the augmented fabric dataset -> quantized TFLite.

Pipeline (mirrors how the shipped classifier integrates):
  1. stage augmented/ into staged_release/<RELEASE_CLASS>/ (taxonomy.py), then
     image_dataset_from_directory over staged_release/ (release_visual_families order)
  2. MobileNetV3-Small, ImageNet weights, frozen backbone -> train head -> unfreeze top blocks
  3. export SavedModel -> TFLite (int8 dynamic-range quantization, ~2-3 MB)
  4. print a validation accuracy report; copy nothing into the app until a human reviews it

Requires: pip install tensorflow (>=2.16). Not shipped in the APK; this is an offline
tooling script. DO NOT replace app assets with a model trained on <1000 images/class.

Usage: python3 tools/dataset/train_finetune.py [--epochs-head 8] [--epochs-fine 12] --smoke
  --smoke: tiny run (64px, 2+2 epochs) to prove the pipeline end-to-end on any machine.
  Full runs are NOT available here: augmented folders are not yet linked to reviewed
    rows. Use the manifest-gated export (prepare_kaggle.py --manifest ...) for learning curves.
Classes are the release visual families in label_schema.json; WAX and ANKARA merge into
WAX_ANKARA and attribute folders are never trained on.
"""
import argparse
from pathlib import Path

import numpy as np

from taxonomy import load_schema, release_classes, stage_release_folders

ROOT = Path(__file__).resolve().parent
AUG = ROOT / "augmented"
STAGED = ROOT / "staged_release"
OUT = ROOT / "out"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--epochs-head", type=int, default=8)
    ap.add_argument("--epochs-fine", type=int, default=12)
    ap.add_argument("--size", type=int, default=224)
    ap.add_argument("--smoke", action="store_true", required=True,
                    help="pipeline check only; the model is never shipped")
    args = ap.parse_args()
    args.size, args.epochs_head, args.epochs_fine = 64, 2, 2

    import tensorflow as tf

    schema = load_schema(ROOT / "label_schema.json")
    counts, missing = stage_release_folders(AUG, STAGED, schema)
    print(f"staged release folders: {counts}")
    classes = [c for c in release_classes(schema) if counts.get(c, 0) > 0]
    if missing:
        print(f"NOTE: training on {len(classes)} of {len(release_classes(schema))} release classes; "
              f"no data yet for: {missing}")
    if len(classes) < 2:
        raise SystemExit("need at least two release classes with augmented data")
    ds = tf.keras.utils.image_dataset_from_directory(
        STAGED, image_size=(args.size, args.size), batch_size=32, label_mode="categorical",
        class_names=classes, shuffle=True, seed=13, validation_split=0.15, subset="training",
    )
    val = tf.keras.utils.image_dataset_from_directory(
        STAGED, image_size=(args.size, args.size), batch_size=32, label_mode="categorical",
        class_names=classes, shuffle=False, seed=13, validation_split=0.15, subset="validation",
    )
    num_classes = len(ds.class_names)

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
    model.fit(ds, validation_data=val, epochs=args.epochs_head, verbose=2)

    # fine-tune the top blocks
    backbone.trainable = True
    for layer in backbone.layers[:-24]:
        layer.trainable = False
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-5),
                  loss="categorical_crossentropy", metrics=["accuracy"])
    model.fit(ds, validation_data=val, epochs=args.epochs_fine, verbose=2)

    loss, acc = model.evaluate(val, verbose=0)
    print(f"validation accuracy: {acc:.3f} (loss {loss:.3f}) — smoke model, not a metric")

    OUT.mkdir(exist_ok=True)
    saved = OUT / "saved_model"
    model.export(str(saved))
    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite = converter.convert()
    model_path = OUT / "fabric_smoke.tflite"
    model_path.write_bytes(tflite)
    print(f"wrote {model_path} ({len(tflite) / 1e6:.2f} MB)")



if __name__ == "__main__":
    np.random.seed(7)
    main()
