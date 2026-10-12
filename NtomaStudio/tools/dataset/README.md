# Fabric dataset & fine-tune pipeline

The shipped classifier is MobileNetV2 (ImageNet) + GLCM + kente-strip heuristics with
honest `ON_DEVICE_DEMO` labelling. This pipeline is the path to a **real** fabric model:
a MobileNetV3-Small fine-tuned on labelled Ghanaian fabric photos, exported to a ~2-3 MB
quantized TFLite model that drops into `app/src/main/assets/` behind the same interface.

## Public pattern data acquired (11 October 2026)

A separate importer now downloads the pinned AI4Culture textile-pattern dataset:

```bash
# Run from the repository root; Python 3.11+, GitHub CLI
python3 NtomaStudio/tools/dataset/acquire_patterns.py
```

The completed audit retained **803 unique candidate images** with upstream
CC0/public-domain declarations and source links. This is a **six-pattern** pool,
not a Kente/Ankara classifier or a production-cleared dataset. Source/design review
is still required. It stays in ignored `.datasets/ai4culture/`, separate from
`raw/<FABRIC_FAMILY>`. Do not pass it to the 17-family trainer.
See [acquisition report and next steps](../../docs/FABRIC_DATASETS.md).

## Expanded family and pattern collection

`collect_public.py` now runs via `.github/workflows/collect-datasets.yml` on an
internet-connected GitHub runner. The completed expanded acquisition downloaded
**951 byte-distinct review candidates** from Commons and Cleveland, with creator,
rights evidence, hashes and explicit unreviewed query suggestions. Photos and full
metadata remain in Actions artifacts, not Git or Android assets. See the
[acquisition report](../../docs/FABRIC_DATASETS.md) for actual counts and remaining gaps.

For unattended status/error observation of an **existing saved Kaggle version**, see
[`KAGGLE_MONITORING.md`](../../docs/KAGGLE_MONITORING.md). It requires secure runner
authentication and does not submit or automatically repair training jobs.

## Release budget and expanded collection

A new bounded run uses pagination, four queries/category and 951 prior-source
exclusions rather than redownloading the old pool. Collection ceilings are not
promised counts. See [live acquisition and release gaps](../../docs/datasets/RELEASE_READINESS.md).

Audit **reviewed annotations** separately from collector suggestions:

```bash
python3 NtomaStudio/tools/dataset/audit_readiness.py  # zero approvals, honest gaps
python3 NtomaStudio/tools/dataset/audit_readiness.py --annotations reviewed.jsonl
python3 -m unittest discover -s NtomaStudio/tools/dataset -p 'test_*.py' -v
```

Annotation rows need explicit approval, reviewer, rights evidence/verification,
cloth-region review, verified `reviewed_pattern` labels, image SHA-256, source/design group,
train/validation/test assignment and explicit app-QA exclusion status. Target-domain
camera/gallery and worn-outfit fields support the separate test slices. No reviewed
file exists yet; missing annotations mean **zero**, not query labels. The auditor
flags identity/split conflicts and never authorizes a release from counts alone.

## Layout
```
tools/dataset/
  label_schema.json   17 classes incl. FUGU + GONJA, folder layout, collection rules
  raw/<CLASS>/*.jpg   YOUR real labelled photos (not committed — see .gitignore guidance)
  augment.py          Pillow-only augmentation: crop/rotate/perspective/jitter -> augmented/
  train_finetune.py   Keras MobileNetV3-Small fine-tune -> quantized .tflite + val accuracy
  ingest.py           import an app contribution zip into raw/ (validated, deduped)
  out/                generated models (never shipped without human review)
```

## Workflow
1. **Collect and review**: use the provisional design-group budget in
   [RELEASE_READINESS.md](../../docs/datasets/RELEASE_READINESS.md), not a magic
   image-count threshold. Initial milestone: 300 training + 150 validation + 200
   independent target-domain test groups per supported class, plus unknown tests.
   Resolve overlapping family labels before using the old 17-folder trainer.
   Market/cooperative photography and consented contributions can complement public
   data; baseline acquisition is not blocked on owner-supplied original photos.
2. **Augment**: `python3 tools/dataset/augment.py --per-image 10`
3. **Train**: `python3 tools/dataset/train_finetune.py --smoke` first on a laptop (or `--dev-only` for a dev-only learning-curve run; the output is not for shipping)
   to prove the toolchain; a smoke model is a pipeline test, NEVER a shipped model).
4. **Evaluate**: tune thresholds on validation only, then use a locked,
   design-disjoint test with real camera/gallery dresses, per-class metrics,
   uncertainty coverage and confidence intervals. KENTE_PRINT/GONJA stay unsupported
   until provenanced data exists; a random image split is not independent validation.
5. **Integrate**: replace the classifier asset, keep the heuristic fallback, re-run
   `./gradlew :app:testDebugUnitTest`, and only then stop labelling results
   `ON_DEVICE_DEMO` for the classes that genuinely improved.

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
it does not replace target-domain independent tests. The old allowlist is not
per-image rights clearance; the newer rights-aware collector preserves review status.

## Honesty rules (non-negotiable)
- No release accuracy claim from dataset size alone. Report actual independent
  evaluation and uncertainty; 1000 images/class is not a guarantee.
- Authentic hand-woven vs machine-printed kente is a **separate task** (see
  Authente-Kente, ~88% published) — do not conflate it with category labels.
- The demo fallback and its labelling stay in the app permanently.
