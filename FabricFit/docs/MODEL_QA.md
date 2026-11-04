# Model inventory and real-fabric validation

## Are all models in the app?

The APK bundles **eight TFLite models**, and every model has a runtime Kotlin
reference. The LiteRT audit loads each file, checks its input/output tensor
shape and dtype, invokes it, and rejects non-finite outputs:

1. `ghanaian_fabric_classifier.tflite` — 17 textile-appearance families.
2. `weave_motif_extractor.tflite` — six weave/motif features.
3. `mobilenet_v2_1.0_224_quant.tflite` — ImageNet textile and OOD hints.
4. `selfie_segmentation.tflite` — portrait foreground mask.
5. `deeplabv3_257_mv.tflite` — PASCAL person segmentation.
6. `human_parsing_5class.tflite` — five human body zones.
7. `cloth_warper_shading.tflite` — cloth displacement and fold shading.
8. `fitmatch_ranker.tflite` — 16-feature recommendation score.

See [`qa/model-contracts.md`](qa/model-contracts.md) for the generated audit.
A passing contract proves the models are present and executable; it does **not**
prove recognition quality.

## What the fabric classifier was actually trained on

The current 17-class asset is built by `tools/build_ai_models.py` from a small
set of bundled catalog images and augmented crops. Most families have only one
or two source images. FUGU and GONJA use procedural strip-weave examples. Its
near-perfect results on those same catalog images are therefore memorization,
not evidence of accuracy on unseen real fabrics.

FabricFit supports 17 **appearance families**, not every fabric or cultural
variation in Ghana or worldwide. The existing `ON_DEVICE_DEMO` status must stay
until a human-reviewed, leak-free field dataset establishes real performance.

## Real online fabric smoke test

The `[fabric-qa]` workflow collects freely licensed photographs from Wikimedia
Commons and Openverse using the existing license, host-blocklist, decode, size,
and duplicate gates. It targets every one of the 17 labels and runs the exact
bundled classifier, weave extractor, and ImageNet OOD model.

Generated outputs:

- [`qa/real-fabric-report.md`](qa/real-fabric-report.md) — per-family and
  per-image predictions with source links and licenses.
- [`qa/real-fabric-report.json`](qa/real-fabric-report.json) — raw top-3 scores,
  weave metrics, OOD hints, and machine-readable provenance.
- [`qa/real-fabric-contact-sheet.jpg`](qa/real-fabric-contact-sheet.jpg) —
  attributed visual review sheet; source images retain their original licenses.

This is an **unvetted online smoke test**, not a production accuracy benchmark.
Search labels can be wrong, photos can show garments rather than close-ups, and
two images per family cannot cover the variety of real textiles. The reported
rate is raw classifier top-1 before Android's heuristic/OOD fusion.

## Run it

```bash
git commit --allow-empty -m "test: audit real fabrics [fabric-qa]"
git push origin arena/e883b312-fabricfit-app
```

Or run locally with network access:

```bash
python3 -m pip install ai-edge-litert numpy pillow requests certifi
python3 FabricFit/tools/dataset/scrape/wikimedia.py --per-class 2
python3 FabricFit/tools/dataset/scrape/openverse.py --per-class 1
python3 tools/qa/fabric_model_qa.py contracts --output qa-artifacts/model-contracts.json
python3 tools/qa/fabric_model_qa.py online \
  --data-dir FabricFit/tools/dataset/scraped/wikimedia \
  --data-dir FabricFit/tools/dataset/scraped/openverse \
  --contracts qa-artifacts/model-contracts.json \
  --output-dir qa-artifacts --per-class 2 --require-all-classes
```

Production training still requires the dataset policy in
`FabricFit/tools/dataset/README.md`: at least 300 human-verified training photos
per class before serious training and 1,000 per class before publishing accuracy
claims or replacing the demo model.
