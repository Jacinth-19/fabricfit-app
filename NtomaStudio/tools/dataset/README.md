# Fabric dataset & fine-tune pipeline

The shipped analyser is MobileNetV2 (ImageNet) + GLCM + kente-strip heuristics with
honest `ON_DEVICE_DEMO` labelling. This pipeline is the path to the **two real recognition
models** decided in `docs/ml_roadmap.md`:

1. **Visible-pattern classifier** (stripes, checks, plaid, dots, floral, geometric, solid)
   — **trains FIRST**, because the newly acquired dataset labels appearance, not provenance.
2. **Fabric-family classifier** (Kente, Kente-print, Ankara/wax, Adinkra, batik, lace,
   denim, …) — a separate model: "striped" does not prove the cloth is Kente, and floral
   does not automatically mean Ankara. Both return confidence and may answer **uncertain**
   instead of forcing a guess (thresholds in `TrainedModelRunner.kt`).

Each is a MobileNetV3-Small fine-tuned on labelled photos, exported to a ~2-3 MB
quantized TFLite model that drops into `app/src/main/assets/` — the app switches a model
on automatically the day its asset pair lands (no settings toggle).

## Layout
```
tools/dataset/
  label_schema.json           17 fabric families incl. FUGU + GONJA, folder layout, rules
  label_schema_pattern.json   7 visible-pattern classes + labelling rules (plaid vs checked)
  raw/<CLASS>/*.jpg           real fabric-FAMILY labelled photos (not committed)
  raw_pattern/<CLASS>/*.jpg   real PATTERN labelled photos (not committed)
  augment.py                  Pillow-only augmentation (--task fabric|pattern)
  train_finetune.py           Keras MobileNetV3-Small fine-tune (--task fabric|pattern)
  ingest.py                   import an app contribution zip into raw/ (validated, deduped)
  out/                        generated models + labels files (never shipped w/o review)
```

## Workflow (pattern task first, then fabric task — same commands, swap the flags)
1. **Collect**: 300+ photos/class minimum (1000+ before publishing accuracy claims).
   Market days at Makola/Kantamanto/Kejetia, weaver cooperatives in Bonwire (kente) and
   Tamale (fugu), user contributions with consent — plus the newly acquired dataset for
   the pattern task. Follow the schema guidance — lighting, distance, angle and fold
   variety matter more than volume.
2. **Augment**: `python3 tools/dataset/augment.py --task pattern --per-image 10`
   (fabric task: `--task fabric`, the default).
3. **Train**: `python3 tools/dataset/train_finetune.py --task pattern` (add `--smoke`
   first on a laptop to prove the toolchain; a smoke model is a pipeline test, NEVER a
   shipped model). Writes `out/pattern_finetuned.tflite` + `out/pattern_labels.txt`.
4. **Review**: check the printed validation accuracy per class; inspect confusions
   (CHECKED vs PLAID for patterns; KENTE vs KENTE_PRINT and FUGU vs GONJA for families).
5. **Integrate**: copy the reviewed pair into `app/src/main/assets/` as
   `pattern_classifier.tflite` + `pattern_labels.txt` (or `fabric_family_classifier.tflite`
   + `fabric_family_labels.txt`), keep the heuristic fallback, re-run
   `./gradlew :app:testDebugUnitTest`. Results switch from `ON_DEVICE_DEMO` to
   `ON_DEVICE_TRAINED` only for verdicts the shipped model actually produced.

## Web sourcing → Kaggle training (new)
Free-licensed photos can fill the pool faster than field days alone — through a
scraper toolkit with hard safety rails (license gate CC0/PD/CC BY/CC BY-SA only,
stock/marketplace blocklist, per-image `CREDITS.csv`, dHash dedup, leak-free splits):

```bash
python3 tools/dataset/scrape/selftest.py      # offline test-suite first
python3 tools/dataset/scrape/wikimedia.py --per-class 60
python3 tools/dataset/scrape/openverse.py --per-class 40   # optional OPENVERSE_API_KEY
python3 tools/dataset/scrape/github_repos.py  # allowlisted research repos
python3 tools/dataset/scrape/manifest.py my_urls.csv       # explicit URLs you vouch for

python3 tools/dataset/prepare_kaggle.py --kaggle-id YOURUSER/ntoma-fabric-17 --zip
kaggle datasets create -p tools/dataset/kaggle_export      # or upload the .zip
# Kaggle Notebook → attach dataset → python3 train_on_kaggle.py --data-dir /kaggle/input/<slug>
```

Details + honesty implications: `scrape/README.md`. Web data is **starter** data —
it never replaces the 1000+/class field + consented-contribution bar for claims.

## Honesty rules (non-negotiable)
- No accuracy claims from models trained on <1000 images/class.
- Authentic hand-woven vs machine-printed kente is a **separate task** (see
  Authente-Kente, ~88% published) — do not conflate it with category labels.
- The demo fallback and its labelling stay in the app permanently.
