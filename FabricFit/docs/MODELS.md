# Bundled TFLite models: provenance, licence, and what each one really does

The `.tflite` files in `app/src/main/assets/` are **build inputs, not source**. They are
not committed to this repository — fetch them before building:

```bash
bash FabricFit/tools/models/fetch_tflite_models.sh
```

CI does this automatically as the first step of the `verify` and `release` jobs. The
script skips files that are already present, verifies the FlatBuffers magic before
accepting a download, and re-prints every model's tensor counts and shapes.

## What ships, and where it came from

| File | Source | Licence | Tensors | Input → Output | Role |
| --- | --- | --- | --- | --- | --- |
| `selfie_segmentation.tflite` | [`Volcomix/virtual-background`](https://github.com/Volcomix/virtual-background) — MediaPipe Selfie Segmenter, 256×256 float16 | Apache-2.0 | 360 | `[1,256,256,3]` float → `[1,256,256,1]` | Person segmentation (primary) |
| `segm_full_v679.tflite` | same repo — 2-class portrait segmentation | Apache-2.0 | 355 | `[1,144,256,3]` float → `[1,144,256,2]` | Second segmentation cross-check |
| `mobilenet_v1_1.0_224_quant.tflite` | [`tensorflow/tflite-support`](https://github.com/tensorflow/tflite-support) test data | Apache-2.0 | 89 | `[1,224,224,3]` uint8 → `[1,1001]` uint8 | ImageNet out-of-distribution gate |
| `ghanaian_fabric_classifier.tflite` | Built for this project (17-class Ghanaian textile head) | project | 43 | `[1,128,128,3]` float → `[1,17]` float | Fabric category (primary) |
| `deeplabv3_257_mv.tflite` | DeepLabV3 PASCAL VOC, MobileNet backbone | Apache-2.0 (upstream) | 184 | `[1,257,257,3]` float → `[1,257,257,21]` | 21-class semantic segmentation |
| `weave_motif_extractor.tflite` | Built for this project | project | 24 | `[1,128,128,3]` float → `[1,6]` float | Weave/motif feature head |
| `human_parsing_5class.tflite` | Built for this project | project | 11 | `[1,128,128,3]` float → `[1,128,128,5]` float | Body-zone parsing |
| `cloth_warper_shading.tflite` | Built for this project | project | 10 | `[1,64,64,4]` float → `[1,64,64,3]` float | Cloth shading map |
| `fitmatch_ranker.tflite` | Built for this project | project | 10 | `[1,16]` float → `[1,1]` float | Recommendation ranker |

"Built for this project" means the file is real and loads, but it is a **small feature
network**, not a trained deep model. The three full networks are the two segmentation
models and the MobileNet gate, plus `deeplabv3` and the fabric classifier.

## Measured behaviour

Everything below was measured with `ai_edge_litert` against the assets bundled in the
app, not estimated.

### Person segmentation — good

`selfie_segmentation.tflite`, fed `0..1` floats at 256×256 (which is what
`PersonSegmenter` does):

- bundled model photo → **23.4 % coverage**, a clean silhouette matching the subject;
- a fabric-only photo → **0.0 % coverage**.

`segm_full_v679.tflite` agrees (20.0 % / 0.3 %) but is visibly coarser: it runs at
144×256, so its mask is blocky when upscaled.

**Input scaling matters, but only past 0..1.** The selfie model is insensitive across the
usual float normalisations — `0..1` gives 23.1 % coverage and `-1..1` gives 23.4 %, with
the same bimodal distribution (>22 % of pixels above 0.9, ~76 % below 0.1). Feeding raw
`0..255` floats, however, saturates the network: coverage jumps to 92 % and the mask
degenerates to "everything is a person". `PersonSegmenter` uses `0..1`; keep it there.

### Fabric classifier — good, with one caveat

`ghanaian_fabric_classifier.tflite`:

- **15 of 15** bundled swatches correct, most at 0.99+ confidence;
- stable under rotation, mirroring and brightening;
- degrades sensibly (desaturated kente → `BATIK`, darkened lace → `SILK`);
- **confident on garbage**: solid white and black both return `LINEN` at 1.00, noise
  returns `ANKARA` at 0.997. It answers "which of these 17 fabrics is this most like",
  not "is this fabric at all".

### ImageNet OOD gate — weak, and now honestly documented

`mobilenet_v1_1.0_224_quant.tflite` is a real, full MobileNet (89 tensors, 1001 classes).
Its predictions are sensible: the model photo reads as *suit / bow tie / jean*, kente as
*prayer rug / quilt*, a lace swatch as *handkerchief*.

But ImageNet has **no textile classes**, so on flat fabric swatches the evidence is
diffuse. At the current thresholds (`entropy ≤ 0.994`, `margin ≥ 0.02`):

- **8 of 19** bundled textiles are admitted;
- **0 of 4** flat-colour / gradient controls are admitted;
- random noise is rejected.

That is a modest signal, and it is only used to nudge the category and clamp confidence —
the spectral/Gabor signature is the primary textile detector. If you tighten these
thresholds you will reject real cloth; if you loosen them, noise starts passing. Re-run
the harness in this document before changing either number.

### The dequantisation bug that was fixed

The OOD model's output is a **quantised uint8** tensor. The previous code did
`byte / 255f`, which produces a number that looks like a probability but has nothing to
do with the model's confidence. It now dequantises with the tensor's own
`(scale, zeroPoint)` read from the interpreter. On the shipped MobileNet that is
`(1/256, 0)`, so a raw byte of `0` means *zero confidence*, not 0.

## Replacing a model

1. Put the new file in `app/src/main/assets/` and add it to `fetch_tflite_models.sh`
   with its upstream repo, path and licence.
2. Check that the input/output shapes match what the Kotlin loader expects — the loader
   is shape-agnostic, but the preprocessing is not (`PersonSegmenter` feeds 0–1 floats
   to the selfie model, `OnDeviceMlClassifier` feeds 0–1 floats to the fabric classifier).
   Any float normalisation in `0..1` or `-1..1` works for the selfie net; `0..255` does not.
3. Run the harness: coverage on the bundled model photo, coverage on a fabric-only photo,
   and the 15-swatch accuracy check. Numbers well outside the ranges above mean the
   preprocessing does not match.
4. If you replace the OOD gate, re-measure the entropy/margin distribution and update
   `MAX_NORMALIZED_ENTROPY` and `MIN_TOP_MARGIN` in `OnDeviceMlClassifier.kt`.
