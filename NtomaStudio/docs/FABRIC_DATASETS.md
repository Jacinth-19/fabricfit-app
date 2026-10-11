# Fabric recognition datasets — acquisition report

**Audited 11 October 2026.** We now have a downloaded, reproducible starting pool
for **visible pattern recognition**, including textiles and clothing. No specialist
model has been trained, exported, uploaded to Kaggle or enabled in the Android app.
The separate **Ghanaian fabric-family** task still needs suitable labelled data.

## 1. Acquired: AI4Culture textile patterns

Sources: [publisher repository](https://github.com/datable-be/AI4_C_datasets),
[pinned metadata](https://github.com/datable-be/AI4_C_datasets/blob/767169b2b053485ee7b909fd19b96e98f6be0453/color_detection_dataset/AI4C_Textile_Pattern_Dataset_2024-06-01_0.json).
The dataset describes cloth samples, fashion objects and worn clothing. This is
relevant to selecting a fabric **or an existing dress** in Ntoma Studio. The retained
subset has not yet been audited for how many pictures show worn dresses.

Downloaded both image archives (~30.6 MB compressed) plus metadata. Checked all
three SHA-256 hashes against the pinned revision and matched all **1,245 images**.
The metadata's CC0 licence is **not** a blanket licence for its images.

| Per-image rights declaration | Images | Import decision |
|---|---:|---|
| CC0 | 767 | Candidate if a source landing page is present |
| Public Domain Mark | 44 | Candidate if a source landing page is present |
| CC BY 4.0 | 199 | Hold for complete creator/attribution review; not inherently prohibited |
| Unknown | 146 | Excluded |
| Missing | 87 | Excluded |
| CC BY-NC-ND 4.0 | 2 | Excluded |

Of the 811 CC0/public-domain records, seven lack source landing pages. This leaves
804 candidates; one byte-identical duplicate was removed, leaving **803 images**.
All 803 passed a local Pillow image-integrity check. Source declarations still need
verification; these are **metadata-filtered candidates, not a legal clearance or
production-ready dataset**. Public Domain Mark is a rights assertion, not a licence.

| Pattern label | Candidate images |
|---|---:|
| Floral | 397 |
| Striped | 140 |
| Geometric | 113 |
| Plaid | 81 |
| Checked | 39 |
| Dotted | 33 |
| **Total** | **803** |

This is imbalanced and small, especially for dots/checks. There are **no labels for
Kente, Kente-print, Ankara, Adinkra, Fugu, Gonja, fibre composition, solid or unknown**.
Geometric must not be relabelled Kente, nor floral relabelled Ankara. It cannot prove
weaving technique, authenticity or fibre content from an ordinary dress photo.

### Local files and repeatable acquisition

From the repository root (Python 3.11+ and GitHub CLI):

```bash
python3 NtomaStudio/tools/dataset/acquire_patterns.py
# Repeat the audit using checksum-verified cached files only:
python3 NtomaStudio/tools/dataset/acquire_patterns.py --offline
python3 -m unittest discover -s NtomaStudio/tools/dataset -p test_acquire_patterns.py -v
```

Generated files live in **`.datasets/ai4culture/`**, ignored by Git:

- `pt1.tar.gz`, `pt2.tar.gz`, `metadata.json`: immutable upstream archive cache.
  These archives include excluded images; never feed them directly to training.
- `candidate/<PATTERN>/<image-id>.jpg`: only the 803 metadata-filtered,
  byte-deduplicated candidates. Not yet split or augmented.
- `manifest.csv`: all 1,245 records, status, upstream rights/source links, labels,
  image hashes, candidate paths and preliminary source-object group IDs.
- `audit.json`: machine-readable counts/checksums and remaining review gates.

A small tracked copy of the completed audit is in
[`datasets/ai4culture-audit.json`](datasets/ai4culture-audit.json).
The importer is separate from the 17-family ingestion/export pipeline and cannot
silently overwrite it. Downloads use pinned GitHub API paths and SHA-256 checks;
archive paths and symlinks are not extracted. No Kaggle token is needed to acquire
this dataset. Excluded images have no candidate path in the manifest.

## 2. Ghanaian/African-specific leads

These are **leads, not approved training pools**. None was copied into `candidate/`.

| Dataset/source | What it actually offers | Decision |
|---|---|---|
| [Authente-Kente / CSDTs](https://github.com/CSDTs/kente-cloth-authentication) | Current repository tree has **27 raw JPGs: 8 `real_` and 19 `fake_`**, not thousands of independent cloths. The related paper describes a different 16-whole-cloth experiment with derived swatches. | Most relevant lead for woven-vs-printed Kente, but small and with mixed museum/source photography. BSD-style software licence does not resolve each photograph's rights. Audit individual sources before acquisition. Never adopt `fake` as a user-facing label or promise authenticity. |
| [Wax-MNIST](https://github.com/armelsoubeiga/Wax-MNIST) / [Kaggle](https://www.kaggle.com/datasets/soubeiga/wax-mnist) | Eight classes are **Batik, GTP, Hitarget, Hollandais, Realwax, Uniwax, Vlisco, Woodin**—mainly brands/trade terms, not eight fabric families. Kaggle lists 1,040 files; project text reports a different total. Images are only 28/64 px. | Do not use as a Kente/Adinkra recognizer. Scraped-source provenance and differing repository/Kaggle licensing statements need resolution. |
| [African Fabric Images](https://www.kaggle.com/datasets/mikuns/african-fabric) | Small, low-resolution collection; no verified family label scheme or suitable image-rights clearance. | Not approved. See earlier [Kaggle review](KAGGLE_AI_PLAN.md). |
| [TextileNet](https://github.com/hahashu/TextileNet) | General fabric taxonomy includes chiffon, denim, lace and velvet; it does **not** supply a Kente/Adinkra taxonomy. | Potential general-material transfer-learning lead. Verify dataset/image terms separately from code/manuscript licences before downloading/training. |

The previous fashion-pattern-images and annotated-textile shortlist remains in
[`KAGGLE_AI_PLAN.md`](KAGGLE_AI_PLAN.md). Its scraped-source and sample-leakage
warnings still apply. The old scraper allowlist is **not** a substitute for this
per-image review; it was not used for this acquisition.

## 3. Training path for Ntoma Studio

1. **Review the acquired subset:** verify source declarations, spot-check labels and
   record whether each image is a close-up, draped cloth, isolated garment or worn
   outfit. Museum/product imagery is not a replacement for Ghanaian phone photos.
2. **Deduplicate by design, not just bytes:** source IDs and the exact-duplicate
   audit are only a starting point. Group alternate museum views, perceptual near-
   duplicates and repeated cloth designs before train/validation/test splitting.
   Do not present a random image split as an independent evaluation.
3. **Prepare a separate pattern baseline for Kaggle GPU:** keep the six labels
   explicit, address imbalance with train-only sampling/weights, augment training
   only and fine-tune MobileNetV3-Small. No crops should cross splits. The existing
   `train_on_kaggle.py` targets fabric families, so it must not be pointed blindly at
   this six-pattern collection. A pattern-specific trainer/export is still needed.
4. **Expand Ghanaian-family data through verified public sources:** review the
   original museum/open-access Kente records and explicitly licensed African dress
   photos, with fabric labels checked rather than inferred from colour/pattern.
   Resolve overlapping `WAX`/`ANKARA` definitions before multi-class training.
   This does not require the owner to supply original photos to start the baseline.
5. **Evaluate and integrate honestly:** use macro-F1, per-class recall and confusion
   matrices; add non-fabric/unknown and real Ghanaian worn-dress evaluation. Retain
   uncertain results. Add a separate pattern TFLite contract and device tests rather
   than replacing the ImageNet asset or 17-family label schema blindly. Do not ship
   specialist recognition or change the demo label until evaluation supports it.

**Completed:** actual public data acquisition, per-image metadata filtering,
byte-duplicate removal, provenance manifest, checksum audit and three passing offline
import tests. **Not completed:** full source/design review, Kaggle upload, model
training, TFLite export, app integration or accuracy measurement. No APK or UI change
was made by this dataset work.
