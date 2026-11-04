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

## Real online fabric smoke tests

The `[fabric-qa]` workflow runs two deliberately separate diagnostics with the
exact bundled classifier, weave extractor, and ImageNet OOD model:

1. **Curated source set:** `tools/dataset/qa_real_fabrics.csv` contains one
   openly licensed textile example for each of the 17 families. Each row was
   manually selected from its Commons source page to remove obvious search
   contamination. The evaluator also screens dHash distance against the
   classifier's catalog training sources to expose possible image leakage.
   This is a diagnostic smoke test, not expert fiber/heritage verification;
   GONJA is represented by a Northern Ghana smock proxy.
2. **Unvetted search set:** on `[fabric-qa-search]` runs, the Wikimedia and
   Openverse scrapers collect up to two search-derived images per family. This
   set intentionally remains separate because irrelevant and mislabeled results
   make its raw score unfit as a cleaned accuracy estimate.

Generated curated outputs:

- [`qa/real-fabric-report.md`](qa/real-fabric-report.md) — per-family and
  per-image predictions with source links and licenses.
- [`qa/real-fabric-report.json`](qa/real-fabric-report.json) — raw top-3 scores,
  weave metrics, OOD hints, catalog-leakage screening, and provenance.
- [`qa/real-fabric-contact-sheet.jpg`](qa/real-fabric-contact-sheet.jpg) —
  attributed visual review sheet; source images retain their original licenses.

The original broad-search diagnostics are preserved under
[`qa/online-search/`](qa/online-search/real-fabric-report.md). Neither set is a
production accuracy benchmark: one or two web images per family cannot cover
real-world variety, and source descriptions are not independent expert labels.
Reported rates are raw classifier top-1 before Android's heuristic/OOD fusion.

## Run it

```bash
git commit --allow-empty -m "test: audit real fabrics [fabric-qa]"
git push origin arena/e883b312-fabricfit-app
```

Or run locally with network access:

```bash
python3 -m pip install ai-edge-litert numpy pillow requests certifi
python3 FabricFit/tools/dataset/scrape/manifest.py \
  FabricFit/tools/dataset/qa_real_fabrics.csv
python3 tools/qa/fabric_model_qa.py contracts --output qa-artifacts/model-contracts.json
python3 tools/qa/fabric_model_qa.py online \
  --data-dir FabricFit/tools/dataset/scraped/manifest \
  --contracts qa-artifacts/model-contracts.json \
  --output-dir qa-artifacts/curated --evaluation-type curated \
  --per-class 1 --require-all-classes
```

Production training still requires the dataset policy in
`FabricFit/tools/dataset/README.md`: at least 300 human-verified training photos
per class before serious training and 1,000 per class before publishing accuracy
claims or replacing the demo model.
