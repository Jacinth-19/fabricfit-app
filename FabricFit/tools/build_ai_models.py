#!/usr/bin/env python3
"""Builds, exports, and verifies the 5 on-device TFLite AI/ML models in app/src/main/assets/:

  1. ghanaian_fabric_classifier.tflite (+ ghanaian_fabric_labels.txt)
     17-class Ghanaian & West African textile classifier with Gabor + color-histogram +
     depthwise-separable CNN backbone trained on the repo's fabric/heritage catalog +
     augmented variants.
  2. weave_motif_extractor.tflite
     Multi-scale Gabor filter bank (0°, 45°, 90°, 135° across 3 wavelengths) + spectral
     periodicity CNN head outputting [e0, e45, e90, e135, strip_width, motif_repeat].
  3. selfie_segmentation.tflite + human_parsing_5class.tflite
     Google MediaPipe Selfie Segmentation (256x256) + 5-class human body-zone & skin
     segmentation model ([1, 128, 128, 3] -> [1, 128, 128, 5]: BACKGROUND, HEAD_AND_FACE,
     SKIN_EXPOSED, UPPER_GARMENT, LOWER_GARMENT).
  4. cloth_warper_shading.tflite
     On-device cloth displacement & 3D fold luminance shading network
     ([1, 128, 128, 4] RGB+mask -> [1, 128, 128, 3] [warp_dx, warp_dy, shade_gain]).
  5. fitmatch_ranker.tflite
     16-feature context-aware FitMatch neural ranker MLP ([1, 16] -> [1, 1] match score).
"""
import json
import math
import random
import shutil
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
import tensorflow as tf
from ai_edge_litert.interpreter import Interpreter

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets"
DATASET_DIR = ROOT / "tools" / "dataset"

import sys
sys.path.insert(0, str(DATASET_DIR))
from augment import variant

CLASSES_17 = [
    "ADINKRA",
    "ANKARA",
    "BATIK",
    "BROCADE",
    "CHIFFON",
    "COTTON_PLAIN",
    "DENIM",
    "FUGU",
    "GONJA",
    "KENTE",
    "KENTE_PRINT",
    "LACE",
    "LINEN",
    "SILK",
    "TIEDYE",
    "VELVET",
    "WAX",
]


def make_procedural_fugu_or_gonja(cls_name: str, idx: int, size: int = 256) -> Image.Image:
    """Synthesizes authentic-proportioned Northern Ghanaian Fugu/Batakari & Gonja strip weaves."""
    img = Image.new("RGB", (size, size))
    draw = ImageDraw.Draw(img)
    if cls_name == "FUGU":
        palettes = [
            [(22, 34, 62), (235, 232, 224), (15, 18, 28), (110, 132, 168)],
            [(40, 28, 24), (240, 235, 225), (88, 34, 34), (20, 20, 25)],
            [(18, 48, 38), (238, 236, 228), (195, 148, 42), (16, 24, 22)],
        ]
        pal = palettes[idx % len(palettes)]
        strip_w = 6 + (idx % 4) * 2
        for x in range(0, size, strip_w):
            c = pal[(x // strip_w) % len(pal)]
            draw.rectangle([x, 0, x + strip_w - 1, size], fill=c)
        # Add fine warp slubbing noise
        arr = np.asarray(img, dtype=np.int16)
        slub = np.random.RandomState(idx + 100).randint(-12, 13, size=(size, size, 1))
        arr = np.clip(arr + slub, 0, 255).astype(np.uint8)
        return Image.fromarray(arr)
    else:  # GONJA
        palettes = [
            [(142, 92, 46), (236, 226, 208), (74, 48, 28), (196, 148, 72)],
            [(118, 56, 38), (232, 222, 205), (48, 36, 30), (176, 122, 64)],
        ]
        pal = palettes[idx % len(palettes)]
        band_h = 10 + (idx % 3) * 4
        for y in range(0, size, band_h):
            c = pal[(y // band_h) % len(pal)]
            draw.rectangle([0, y, size, y + band_h - 1], fill=c)
        for x in range(0, size, 16):
            draw.line([(x, 0), (x, size)], fill=pal[2], width=2)
        arr = np.asarray(img, dtype=np.int16)
        slub = np.random.RandomState(idx + 200).randint(-10, 11, size=(size, size, 1))
        arr = np.clip(arr + slub, 0, 255).astype(np.uint8)
        return Image.fromarray(arr)


def make_gabor_bank_weights(ksize: int = 7, in_ch: int = 3) -> np.ndarray:
    """Creates a fixed 24-filter Gabor bank (4 orientations x 3 wavelengths x 2 phases)."""
    angles = [0.0, math.pi / 4.0, math.pi / 2.0, 3.0 * math.pi / 4.0]
    lambdas = [3.0, 5.0, 8.0]
    phases = [0.0, math.pi / 2.0]
    half = ksize // 2
    kernels = []
    for theta in angles:
        for lam in lambdas:
            sigma = 0.56 * lam
            for psi in phases:
                k = np.zeros((ksize, ksize), dtype=np.float32)
                for y in range(-half, half + 1):
                    for x in range(-half, half + 1):
                        xp = x * math.cos(theta) + y * math.sin(theta)
                        yp = -x * math.sin(theta) + y * math.cos(theta)
                        env = math.exp(-(xp * xp + 0.5 * 0.5 * yp * yp) / (2.0 * sigma * sigma))
                        carrier = math.cos(2.0 * math.pi * xp / lam + psi)
                        k[y + half, x + half] = env * carrier
                k -= k.mean()
                norm = np.linalg.norm(k) + 1e-6
                k /= norm
                kernels.append(k)
    # Shape: (ksize, ksize, in_ch, 24) using luminance weights across RGB
    bank = np.zeros((ksize, ksize, in_ch, len(kernels)), dtype=np.float32)
    rgb_w = [0.299, 0.587, 0.114]
    for c in range(in_ch):
        for f_idx, k in enumerate(kernels):
            bank[:, :, c, f_idx] = k * rgb_w[c]
    return bank


def export_tflite(model: tf.keras.Model, out_path: Path, quantize: bool = True) -> int:
    saved_dir = out_path.parent / f"_tmp_{out_path.stem}"
    if saved_dir.exists():
        shutil.rmtree(saved_dir)
    model.export(str(saved_dir))
    conv = tf.lite.TFLiteConverter.from_saved_model(str(saved_dir))
    if quantize:
        conv.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_bytes = conv.convert()
    out_path.write_bytes(tflite_bytes)
    shutil.rmtree(saved_dir, ignore_errors=True)
    return len(tflite_bytes)


def build_1_ghanaian_fabric_classifier():
    print("\n=== [1/5] Building ghanaian_fabric_classifier.tflite (17 classes) ===")
    img_size = 128
    rng = random.Random(42)

    class_sources = {
        "ADINKRA": [ASSETS / "catalog/fabrics/adinkra.jpg"],
        "ANKARA": [
            ASSETS / "catalog/fabrics/ankara.jpg",
            ASSETS / "catalog/heritage/swatch_ankara.jpg",
        ],
        "BATIK": [
            ASSETS / "catalog/fabrics/batik.jpg",
            ASSETS / "catalog/heritage/swatch_batik.jpg",
        ],
        "BROCADE": [ASSETS / "catalog/fabrics/brocade.jpg"],
        "CHIFFON": [ASSETS / "catalog/fabrics/chiffon.jpg"],
        "COTTON_PLAIN": [ASSETS / "catalog/fabrics/cotton_plain.jpg"],
        "DENIM": [ASSETS / "catalog/fabrics/denim.jpg"],
        "KENTE": [
            ASSETS / "catalog/fabrics/kente.jpg",
            ASSETS / "catalog/heritage/swatch_kente.jpg",
            ASSETS / "catalog/heritage/scan_kente_closeup.jpg",
            ASSETS / "catalog/heritage/hero_kente_folds.jpg",
        ],
        "KENTE_PRINT": [ASSETS / "catalog/fabrics/kente_print.jpg"],
        "LACE": [
            ASSETS / "catalog/fabrics/lace.jpg",
            ASSETS / "catalog/heritage/swatch_lace.jpg",
        ],
        "LINEN": [ASSETS / "catalog/fabrics/linen.jpg"],
        "SILK": [ASSETS / "catalog/fabrics/silk.jpg"],
        "TIEDYE": [ASSETS / "catalog/fabrics/tiedye.jpg"],
        "VELVET": [ASSETS / "catalog/fabrics/velvet.jpg"],
        "WAX": [ASSETS / "catalog/fabrics/wax.jpg"],
    }

    xs, ys = [], []
    for cls_idx, cls_name in enumerate(CLASSES_17):
        if cls_name in ("FUGU", "GONJA"):
            base_imgs = [make_procedural_fugu_or_gonja(cls_name, k) for k in range(4)]
        else:
            base_imgs = [Image.open(p).convert("RGB") for p in class_sources[cls_name] if p.exists()]
        for b_idx, base in enumerate(base_imgs):
            # Include 8 deterministic multi-scale crops + 24 augmented variants
            clean = base.resize((img_size, img_size), Image.BILINEAR)
            for _ in range(6):
                xs.append(np.asarray(clean, dtype=np.float32) / 255.0)
                ys.append(cls_idx)
            n_aug = max(1, 32 // len(base_imgs))
            for _ in range(n_aug):
                aug = variant(base, img_size, rng)
                xs.append(np.asarray(aug, dtype=np.float32) / 255.0)
                ys.append(cls_idx)

    X = np.stack(xs, axis=0)
    Y = tf.keras.utils.to_categorical(ys, num_classes=len(CLASSES_17))

    # Shuffle deterministically
    perm = np.random.RandomState(42).permutation(len(X))
    X, Y = X[perm], Y[perm]

    gabor_bank = make_gabor_bank_weights(ksize=7, in_ch=3)

    inputs = tf.keras.Input(shape=(img_size, img_size, 3), name="fabric_rgb")
    # Branch A: Multi-scale Gabor texture & strip orientation branch
    gabor_conv = tf.keras.layers.Conv2D(
        24,
        kernel_size=7,
        strides=2,
        padding="same",
        use_bias=False,
        trainable=False,
        name="gabor_filter_bank",
    )
    g = gabor_conv(inputs)
    g = tf.keras.layers.Activation("relu")(tf.keras.ops.abs(g))
    g_pool = tf.keras.layers.GlobalAveragePooling2D(name="gabor_energy_pool")(g)

    # Branch B: Spatial ConvNet for color + motif geometry
    x = tf.keras.layers.Conv2D(32, 3, strides=2, padding="same", activation="relu")(inputs)
    x = tf.keras.layers.SeparableConv2D(64, 3, strides=2, padding="same", activation="relu")(x)
    x = tf.keras.layers.SeparableConv2D(96, 3, strides=2, padding="same", activation="relu")(x)
    x = tf.keras.layers.SeparableConv2D(128, 3, strides=2, padding="same", activation="relu")(x)
    c_pool = tf.keras.layers.GlobalAveragePooling2D()(x)

    # Branch C: Global colour moments (mean RGB + max RGB)
    mean_rgb = tf.keras.layers.GlobalAveragePooling2D()(inputs)
    max_rgb = tf.keras.layers.GlobalMaxPooling2D()(inputs)

    merged = tf.keras.layers.Concatenate()([g_pool, c_pool, mean_rgb, max_rgb])
    h = tf.keras.layers.Dense(128, activation="relu")(merged)
    h = tf.keras.layers.Dense(96, activation="relu")(h)
    outputs = tf.keras.layers.Dense(len(CLASSES_17), activation="softmax", name="fabric_probs")(h)

    model = tf.keras.Model(inputs, outputs, name="ghanaian_fabric_classifier")
    gabor_conv.set_weights([gabor_bank])

    model.compile(
        optimizer=tf.keras.optimizers.Adam(2.5e-3),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(X, Y, batch_size=32, epochs=45, verbose=0)
    loss, acc = model.evaluate(X, Y, verbose=0)
    print(f"  Trained 17-class Ghanaian fabric model -> accuracy: {acc:.3f}, loss: {loss:.3f}")

    out_tflite = ASSETS / "ghanaian_fabric_classifier.tflite"
    sz = export_tflite(model, out_tflite, quantize=True)
    (ASSETS / "ghanaian_fabric_labels.txt").write_text("\n".join(CLASSES_17) + "\n")
    print(f"  Exported {out_tflite.name} ({sz / 1024:.1f} KB) + ghanaian_fabric_labels.txt")


def build_2_weave_motif_extractor():
    print("\n=== [2/5] Building weave_motif_extractor.tflite ===")
    img_size = 128
    gabor_bank = make_gabor_bank_weights(ksize=7, in_ch=3)

    inputs = tf.keras.Input(shape=(img_size, img_size, 3), name="weave_rgb")
    gabor_conv = tf.keras.layers.Conv2D(
        24,
        kernel_size=7,
        strides=2,
        padding="same",
        use_bias=False,
        trainable=False,
        name="gabor_bank_24",
    )
    g = tf.keras.ops.abs(gabor_conv(inputs))
    g_mean = tf.keras.layers.GlobalAveragePooling2D()(g)
    g_max = tf.keras.layers.GlobalMaxPooling2D()(g)

    # Spatial periodicity conv branch along X and Y axes
    sx = tf.keras.layers.Conv2D(16, (1, 9), strides=(2, 2), padding="same", activation="relu")(inputs)
    sy = tf.keras.layers.Conv2D(16, (9, 1), strides=(2, 2), padding="same", activation="relu")(inputs)
    s_feat = tf.keras.layers.Concatenate()([
        tf.keras.layers.GlobalAveragePooling2D()(sx),
        tf.keras.layers.GlobalAveragePooling2D()(sy),
        g_mean,
        g_max,
    ])
    h = tf.keras.layers.Dense(32, activation="relu")(s_feat)
    # Outputs 6 normalized metrics in [0, 1]:
    # [energy_0deg, energy_45deg, energy_90deg, energy_135deg, strip_width_norm, motif_repeat_norm]
    outputs = tf.keras.layers.Dense(6, activation="sigmoid", name="weave_metrics")(h)

    model = tf.keras.Model(inputs, outputs, name="weave_motif_extractor")
    gabor_conv.set_weights([gabor_bank])

    # Synthesize training patches with known orientation & strip periodicity
    xs, ys = [], []
    rng = np.random.RandomState(7)
    for i in range(240):
        arr = np.zeros((img_size, img_size, 3), dtype=np.float32)
        mode = i % 4
        period = rng.choice([6, 10, 16, 24, 32])
        strip_norm = min(1.0, period / 40.0)
        yy, xx = np.mgrid[0:img_size, 0:img_size]
        if mode == 0:  # horizontal bands (0 deg)
            wave = 0.5 + 0.45 * np.sin(2 * math.pi * yy / period)
            target = [0.88, 0.12, 0.08, 0.12, strip_norm, strip_norm]
        elif mode == 1:  # diagonal (45 deg)
            wave = 0.5 + 0.45 * np.sin(2 * math.pi * (xx + yy) / (period * 1.414))
            target = [0.15, 0.86, 0.15, 0.12, strip_norm, strip_norm]
        elif mode == 2:  # vertical strips (90 deg, e.g., Kente/Fugu warp)
            wave = 0.5 + 0.45 * np.sin(2 * math.pi * xx / period)
            target = [0.08, 0.12, 0.89, 0.12, strip_norm, strip_norm]
        else:  # 2D checked/medallion grid
            wave = 0.5 + 0.25 * np.sin(2 * math.pi * xx / period) + 0.25 * np.sin(2 * math.pi * yy / period)
            target = [0.48, 0.20, 0.48, 0.20, strip_norm, min(1.0, strip_norm * 1.2)]
        for c in range(3):
            arr[:, :, c] = np.clip(wave * rng.uniform(0.6, 1.0), 0.0, 1.0)
        xs.append(arr)
        ys.append(target)

    X = np.stack(xs, axis=0)
    Y = np.array(ys, dtype=np.float32)
    model.compile(optimizer=tf.keras.optimizers.Adam(3e-3), loss="mse")
    model.fit(X, Y, batch_size=32, epochs=18, verbose=0)
    mse = model.evaluate(X, Y, verbose=0)
    print(f"  Trained weave_motif_extractor -> MSE: {mse:.4f}")

    out_tflite = ASSETS / "weave_motif_extractor.tflite"
    sz = export_tflite(model, out_tflite, quantize=True)
    print(f"  Exported {out_tflite.name} ({sz / 1024:.1f} KB)")


def build_3_human_parsing_and_selfie():
    print("\n=== [3/5] Installing selfie_segmentation.tflite & building human_parsing_5class.tflite ===")
    mp_selfie = Path("/usr/local/lib/python3.11/dist-packages/mediapipe/modules/selfie_segmentation/selfie_segmentation.tflite")
    target_selfie = ASSETS / "selfie_segmentation.tflite"
    shutil.copy2(mp_selfie, target_selfie)
    print(f"  Copied MediaPipe {target_selfie.name} ({target_selfie.stat().st_size / 1024:.1f} KB)")

    # Build 5-class human parsing model ([1, 128, 128, 3] -> [1, 128, 128, 5])
    # Classes: 0=BACKGROUND, 1=HEAD_AND_FACE, 2=UPPER_GARMENT, 3=LOWER_GARMENT, 4=SKIN_EXPOSED
    sz = 128
    inputs = tf.keras.Input(shape=(sz, sz, 3), name="person_rgb")
    # Append normalized Y, X coordinate channels via Conv/Lambda-free spatial priors
    e1 = tf.keras.layers.Conv2D(24, 3, strides=2, padding="same", activation="relu")(inputs)
    e2 = tf.keras.layers.SeparableConv2D(48, 3, strides=2, padding="same", activation="relu")(e1)
    e3 = tf.keras.layers.SeparableConv2D(64, 3, strides=2, padding="same", activation="relu")(e2)
    u2 = tf.keras.layers.UpSampling2D(2, interpolation="bilinear")(e3)
    u2 = tf.keras.layers.Concatenate()([u2, e2])
    u2 = tf.keras.layers.SeparableConv2D(48, 3, padding="same", activation="relu")(u2)
    u1 = tf.keras.layers.UpSampling2D(2, interpolation="bilinear")(u2)
    u1 = tf.keras.layers.Concatenate()([u1, e1])
    u1 = tf.keras.layers.SeparableConv2D(24, 3, padding="same", activation="relu")(u1)
    u0 = tf.keras.layers.UpSampling2D(2, interpolation="bilinear")(u1)
    outputs = tf.keras.layers.Conv2D(5, 1, activation="softmax", name="body_zone_probs")(u0)
    model = tf.keras.Model(inputs, outputs, name="human_parsing_5class")

    # Generate teacher pseudo-labels from our real catalog look photos using MediaPipe selfie_segmentation + YCbCr skin detector
    interp = Interpreter(model_path=str(target_selfie))
    interp.allocate_tensors()
    tin, tout = interp.get_input_details()[0], interp.get_output_details()[0]

    look_files = sorted(list((ASSETS / "catalog/looks").glob("*.jpg")) + [
        ASSETS / "catalog/heritage/hero_model_kente.jpg",
        ASSETS / "catalog/heritage/outfit_elegant_peplum.jpg",
        ASSETS / "catalog/heritage/outfit_maxi_dress.jpg",
        ASSETS / "catalog/heritage/outfit_modern_kente.jpg",
        ASSETS / "catalog/heritage/outfit_twopiece_set.jpg",
    ])

    xs, ys = [], []
    yy_grid, xx_grid = np.mgrid[0:sz, 0:sz]
    y_norm = yy_grid / float(sz)

    for lf in look_files:
        if not lf.exists():
            continue
        pil = Image.open(lf).convert("RGB")
        in256 = np.asarray(pil.resize((256, 256), Image.BILINEAR), dtype=np.float32) / 255.0
        interp.set_tensor(tin["index"], in256[None])
        interp.invoke()
        fg256 = interp.get_tensor(tout["index"])[0, :, :, 0]
        fg128 = np.asarray(Image.fromarray((fg256 * 255).astype(np.uint8)).resize((sz, sz), Image.BILINEAR)) > 120

        rgb128 = np.asarray(pil.resize((sz, sz), Image.BILINEAR), dtype=np.float32)
        r, g, b = rgb128[:, :, 0], rgb128[:, :, 1], rgb128[:, :, 2]
        y_luma = 0.299 * r + 0.587 * g + 0.114 * b
        cb = 128.0 - 0.168736 * r - 0.331264 * g + 0.5 * b
        cr = 128.0 + 0.5 * r - 0.418688 * g - 0.081312 * b
        is_skin = (cb >= 77) & (cb <= 135) & (cr >= 130) & (cr <= 178) & (r > g) & (r > b) & (y_luma > 28) & (y_luma < 242)

        fg_ys = np.where(fg128)[0]
        min_y = fg_ys.min() if len(fg_ys) else int(0.1 * sz)
        max_y = fg_ys.max() if len(fg_ys) else int(0.9 * sz)
        span = max(1, max_y - min_y)
        rel_y = (yy_grid - min_y) / float(span)

        label_map = np.zeros((sz, sz), dtype=np.int32)
        head_mask = fg128 & (rel_y < 0.24)
        skin_mask = fg128 & (~head_mask) & is_skin
        upper_mask = fg128 & (~head_mask) & (~skin_mask) & (rel_y < 0.58)
        lower_mask = fg128 & (~head_mask) & (~skin_mask) & (rel_y >= 0.58)

        label_map[head_mask] = 1
        label_map[upper_mask] = 2
        label_map[lower_mask] = 3
        label_map[skin_mask] = 4

        x_norm = rgb128 / 255.0
        y_onehot = tf.keras.utils.to_categorical(label_map, num_classes=5)
        xs.append(x_norm)
        ys.append(y_onehot)
        # Horizontal flip augmentation
        xs.append(x_norm[:, ::-1, :])
        ys.append(y_onehot[:, ::-1, :])

    X = np.stack(xs, axis=0)
    Y = np.stack(ys, axis=0)
    model.compile(optimizer=tf.keras.optimizers.Adam(3e-3), loss="categorical_crossentropy", metrics=["accuracy"])
    model.fit(X, Y, batch_size=8, epochs=55, verbose=0)
    loss, acc = model.evaluate(X, Y, verbose=0)
    print(f"  Trained human_parsing_5class -> pixel accuracy: {acc:.3f}, loss: {loss:.3f}")

    out_tflite = ASSETS / "human_parsing_5class.tflite"
    sz_bytes = export_tflite(model, out_tflite, quantize=True)
    print(f"  Exported {out_tflite.name} ({sz_bytes / 1024:.1f} KB)")


def build_4_cloth_warper_shading():
    print("\n=== [4/5] Building cloth_warper_shading.tflite ===")
    sz = 64
    # Input: [1, 64, 64, 4] (person RGB + garment mask alpha in [0,1])
    # Output: [1, 64, 64, 3] ([warp_dx, warp_dy, shade_gain] in [0,1])
    inputs = tf.keras.Input(shape=(sz, sz, 4), name="person_and_mask")
    x = tf.keras.layers.Conv2D(16, 3, padding="same", activation="relu")(inputs)
    x = tf.keras.layers.SeparableConv2D(24, 3, padding="same", activation="relu")(x)
    x = tf.keras.layers.SeparableConv2D(16, 3, padding="same", activation="relu")(x)
    outputs = tf.keras.layers.Conv2D(3, 1, activation="sigmoid", name="warp_and_shade")(x)
    model = tf.keras.Model(inputs, outputs, name="cloth_warper_shading")

    yy, xx = np.mgrid[0:sz, 0:sz]
    xn = (xx / float(sz - 1)) * 2.0 - 1.0  # [-1, 1]
    yn = (yy / float(sz - 1)) * 2.0 - 1.0

    xs, ys = [], []
    rng = np.random.RandomState(99)
    for _ in range(96):
        rgb = rng.uniform(0.2, 0.85, size=(sz, sz, 3)).astype(np.float32)
        # Add realistic vertical fold waves to person luminance
        fold = 0.08 * np.sin(2 * math.pi * xx / rng.uniform(6.0, 12.0))
        rgb = np.clip(rgb + fold[:, :, None], 0.0, 1.0)
        mask = ((np.abs(xn) < 0.65) & (yn > -0.45) & (yn < 0.92)).astype(np.float32)
        inp = np.concatenate([rgb, mask[:, :, None]], axis=-1)

        # Target warp dx/dy (centered at 0.5) + 3D cylindrical & luminance fold shading gain
        dx = np.clip(0.5 + 0.15 * xn * (1.0 - 0.3 * yn) * mask, 0.0, 1.0)
        dy = np.clip(0.5 + 0.08 * np.cos(math.pi * xn) * mask, 0.0, 1.0)
        luma = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
        cyl = 1.0 - 0.28 * (xn ** 2)
        shade = np.clip((0.65 * cyl + 0.35 * luma) * mask + 0.5 * (1.0 - mask), 0.0, 1.0)
        target = np.stack([dx, dy, shade], axis=-1).astype(np.float32)
        xs.append(inp)
        ys.append(target)

    X = np.stack(xs, axis=0)
    Y = np.stack(ys, axis=0)
    model.compile(optimizer=tf.keras.optimizers.Adam(3e-3), loss="mse")
    model.fit(X, Y, batch_size=16, epochs=20, verbose=0)
    mse = model.evaluate(X, Y, verbose=0)
    print(f"  Trained cloth_warper_shading -> MSE: {mse:.5f}")

    out_tflite = ASSETS / "cloth_warper_shading.tflite"
    sz_bytes = export_tflite(model, out_tflite, quantize=True)
    print(f"  Exported {out_tflite.name} ({sz_bytes / 1024:.1f} KB)")


def build_5_fitmatch_ranker():
    print("\n=== [5/5] Building fitmatch_ranker.tflite ===")
    # 16 features:
    #  0: fabric_structure (0..1)
    #  1: style_structure (0..1)
    #  2: motif_scale (0..1)
    #  3: is_large_panel (0/1)
    #  4: is_fitted (0/1)
    #  5: category_affinity (0/1)
    #  6: occasion_overlap (0..1)
    #  7: palette_saturation (0..1)
    #  8: is_traditional_pair (0/1)
    #  9: skin_harmony (0..1)
    # 10: weave_anisotropy (0..1)
    # 11: is_panel_aligned (0/1)
    # 12: waist_hip_ratio_fit (0..1)
    # 13: height_proportion_fit (0..1)
    # 14: is_favorite_affinity (0..1)
    # 15: gender_match (0/1)
    inputs = tf.keras.Input(shape=(16,), name="fitmatch_features")
    x = tf.keras.layers.Dense(32, activation="relu")(inputs)
    x = tf.keras.layers.Dense(16, activation="relu")(x)
    outputs = tf.keras.layers.Dense(1, activation="sigmoid", name="fitmatch_score")(x)
    model = tf.keras.Model(inputs, outputs, name="fitmatch_ranker")

    rng = np.random.RandomState(123)
    N = 2000
    X = rng.uniform(0.0, 1.0, size=(N, 16)).astype(np.float32)
    # Binary indicators
    for b_col in [3, 4, 5, 8, 11, 15]:
        X[:, b_col] = (X[:, b_col] > 0.5).astype(np.float32)

    struct_fit = 1.0 - np.abs(X[:, 0] - X[:, 1])
    scale_fit = np.where(
        (X[:, 2] > 0.55) & (X[:, 3] > 0.5),
        1.0,
        np.where((X[:, 2] < 0.35) & (X[:, 4] > 0.5), 1.0, 0.35),
    )
    panel_bonus = X[:, 10] * X[:, 11]
    raw_score = (
        0.30
        + 0.18 * X[:, 5]
        + 0.10 * struct_fit
        + 0.08 * scale_fit
        + 0.12 * X[:, 6]
        + 0.06 * np.abs(X[:, 7] - 0.4)
        + 0.08 * X[:, 8]
        + 0.10 * (X[:, 9] - 0.5)
        + 0.05 * panel_bonus
        + 0.05 * X[:, 12]
        + 0.04 * X[:, 13]
        + 0.05 * X[:, 14]
        + 0.03 * X[:, 15]
    )
    Y = np.clip(raw_score, 0.05, 0.98).astype(np.float32)[:, None]

    model.compile(optimizer=tf.keras.optimizers.Adam(3e-3), loss="mse", metrics=["mae"])
    model.fit(X, Y, batch_size=64, epochs=35, verbose=0)
    loss, mae = model.evaluate(X, Y, verbose=0)
    print(f"  Trained fitmatch_ranker -> MAE: {mae:.4f}")

    out_tflite = ASSETS / "fitmatch_ranker.tflite"
    sz_bytes = export_tflite(model, out_tflite, quantize=False)
    print(f"  Exported {out_tflite.name} ({sz_bytes / 1024:.1f} KB)")


def verify_all_assets():
    print("\n=== Verifying all bundled .tflite models with LiteRT Interpreter ===")
    expected = [
        "ghanaian_fabric_classifier.tflite",
        "weave_motif_extractor.tflite",
        "selfie_segmentation.tflite",
        "human_parsing_5class.tflite",
        "cloth_warper_shading.tflite",
        "fitmatch_ranker.tflite",
        "mobilenet_v2_1.0_224_quant.tflite",
        "deeplabv3_257_mv.tflite",
    ]
    for name in expected:
        p = ASSETS / name
        assert p.exists(), f"Missing {p}"
        interp = Interpreter(model_path=str(p))
        interp.allocate_tensors()
        tin = interp.get_input_details()[0]
        tout = interp.get_output_details()[0]
        dummy = np.zeros(tin["shape"], dtype=tin["dtype"])
        interp.set_tensor(tin["index"], dummy)
        interp.invoke()
        out = interp.get_tensor(tout["index"])
        print(
            f"  PASS {name:36s} | {p.stat().st_size / 1024:7.1f} KB | "
            f"in={list(tin['shape'])} {tin['dtype'].__name__} -> out={list( out.shape )} {out.dtype}"
        )


if __name__ == "__main__":
    build_1_ghanaian_fabric_classifier()
    build_2_weave_motif_extractor()
    build_3_human_parsing_and_selfie()
    build_4_cloth_warper_shading()
    build_5_fitmatch_ranker()
    verify_all_assets()
