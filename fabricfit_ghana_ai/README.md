# FabricFit Ghana AI — execution status

This is a real, conservative pipeline implementation, but **the requested trained
model has not been produced**. The data and authentication gates matter more than
having a notebook that prints an impressive but meaningless score.

## Results actually obtained in this run

- Reused prior manually reviewed records to create `dataset/metadata/images.csv`.
  The requested ~1,300-photo starter pool is only about 162/class and cannot
  satisfy this repository's 300/class training gate (or 1,000/class claim gate);
  it is a collection milestone, not training-ready. Only unambiguous candidate
  mappings from the previous 17-family review were included: KENTE 4, ANKARA_WAX_PRINT 3, ADINKRA 4, BATIK 1, TIE_DYE_ADIRE 2,
  LACE 4, GHANAIAN_FUGU_SMOCK 3, OTHER_UNKNOWN 0 (21 total).
- **Zero image binaries are present in this project bundle**, so no SHA-256,
  dimensions, image-level quality screening or split can be honestly claimed.
  Provenance fields that were absent (creator for some rows, collection date)
  remain blank rather than invented.
- The source `mikuns/african-fabric` was **not downloaded or used**. The official
  Kaggle CLI was installed in an isolated environment; `kaggle datasets list`
  returned “Authentication required”. Kaggle is also unreachable directly from
  this sandbox (TLS connection closed). Therefore source availability, contents
  and licence remain unverified, and the dataset is excluded.
- No Kaggle Notebook was pushed or executed. No training/test result, `.keras`,
  `.tflite`, or Flutter-ready model exists. No model has been integrated.

The concrete dataset report is `reports/dataset_report.html`; CSV/JSON metadata
and 231 prior image-level rejects are under `dataset/metadata/`. Their old review
round licence/provenance records are preserved. Review decisions are not an
expert's cultural certification.

## Eight-class pipeline

1. **Audit existing reviewed metadata** (already run):
   `python3 scripts/audit_existing_reviews.py`
2. **Collect** only rows with accepted labels, HTTPS URLs and exact allowed open
   licences; never query-and-auto-label:
   `python3 scripts/collect_images.py`
3. **Clean**: EXIF-normalize RGB, verify decode/dimensions, SHA-256 exact
   deduplication, optional pHash near-duplicate flags for human decisions:
   `python3 scripts/clean_dataset.py`
4. **Review** all downloaded pixels and verify rights/labels with local textile
   experts. A metadata acceptance tag is not sufficient by itself.
5. **Split** 80/10/10 by source-page group:
   `python3 scripts/prepare_dataset.py --force`
6. **Train and export**, only after every class has >=300 train images and
   independent val/test images:
   `python3 scripts/train_model.py`
7. Validate FP32/FP16/INT8 TFLite artifacts and test metrics under
   `reports/model_comparison.json`. The training script evaluates the test set
   once after best-validation checkpoint selection.
8. Integrate only if a human reviews the report and the >=1,000/class claim bar
   plus independent field test bar are met.

Install local pipeline dependencies from `requirements.txt`. Training requires
TensorFlow and may need network access for ImageNet initial weights. Kaggle's
Notebook GPU is selected when available; the notebook prints available GPUs and
fails closed if the split/data gate is not met.

## Kaggle authentication-required actions

Dataset findings and reuse decisions are documented in `DATASET_HUNT.md`. The
CLI is available at `/tmp/fabricfit-kaggle-venv/bin/kaggle` in this sandbox, but
there is no Kaggle OAuth session, API token, or `~/.kaggle/kaggle.json`. No
credentials were requested in chat or committed. Kaggle CLI 2.2.5 recommends
`kaggle auth login`; alternatively use its secure `~/.kaggle/access_token` file
or set `KAGGLE_API_TOKEN` in the shell that runs the CLI. Older Kaggle clients
accept `~/.kaggle/kaggle.json` (chmod 600). These paths are under the user home,
not inside the repository. Once securely signed in, use the official CLI helper:

```bash
scripts/kaggle_ops.sh discover
scripts/kaggle_ops.sh inspect-source
# Inspect metadata/licence before any import. Do not assume Kaggle reuse rights.
scripts/kaggle_ops.sh download-source
scripts/kaggle_ops.sh notebook-push <notebook-project-dir>
scripts/kaggle_ops.sh notebook-status <username/notebook-slug>
scripts/kaggle_ops.sh notebook-output <username/notebook-slug> <output-dir>
```

A dataset upload is intentionally not attempted: no dataset binaries are
available and mixed licences have not been resolved into a lawful redistribution
package. Do not put private keys, access tokens, contributor contact data or
consent logs in this repository.

## Package contents

- `dataset/metadata/`: source manifest, licence ledger, class distribution,
  rejected-image reasons; all missing facts stay blank/unverified.
- `notebooks/fabricfit_training.ipynb`: Kaggle GPU-aware orchestration notebook.
- `scripts/`: metadata audit, collection, cleaning/dedup, group split, two-stage
  MobileNetV3Small training, TFLite validation, Kaggle CLI actions.
- `mobile/`: labels and a stated input/output contract only. Model asset is absent.
- `reports/model_card.md`: intended use, limitations and gates; no fabricated score.

## Packaging

After legal/quality review, make a ZIP without secrets or source raw files until
redistribution rights are confirmed:

```bash
cd .. && zip -r fabricfit_ghana_ai.zip fabricfit_ghana_ai \
  -x '*/dataset/raw/*' '*/dataset/processed/*' '*/models/*' '*/.ipynb_checkpoints/*'
```
