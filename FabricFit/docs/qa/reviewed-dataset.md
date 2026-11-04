# Reviewed fabric dataset and honest classifier result

This page reports what was collected, what was thrown away, what was measured,
and — plainly — why the requested **100% accuracy was not delivered and cannot be
honestly claimed**.

## 1. What was collected and how it was filtered

Two collection rounds pulled candidates from openly licensed sources: Wikimedia
Commons search, Openverse, and a curated URL manifest (`tools/dataset/scrape/`).
Every candidate was rendered onto a per-family review sheet and inspected one at
a time before any decision was made.

| Stage | Count |
| --- | ---: |
| Candidate photos collected | 287 |
| Rejected after visual review | 231 |
| Accepted into the reviewed pool | **56** |
| Families with at least one verified photo | 16 of 17 |

Why 231 were rejected (each reason is stored per image in
`tools/dataset/reviewed/round-*-decisions.csv`):

| Rejection reason | Count |
| --- | ---: |
| Duplicate, or another frame/copy of the same source photo | 72 |
| People-dominant (portrait, selfie, crowd, dancer, person at work) | 44 |
| Building, museum interior or landscape | 25 |
| Animal or food (ducks, cake, bird, elephant, footprint, tree) | 19 |
| Mislabeled or ambiguous family (Turkish "Ankara" kilim, madras plaid, kente-pattern trousers) | 15 |
| Graphic, engraving, book page or symbol chart — not a cloth photograph | 14 |
| CGI albedo texture, not a photograph | 11 |
| Other (tools, museum objects, unclear material, unrecoverable provenance) | 31 |

The single most instructive finding: searching for **ANKARA** returns Ankara, the
capital of Türkiye (buildings, manuscripts, kilims), and searching **GONJA**
returns people, elephants and grassland. Search metadata alone is not a label.

### Verified photos per family

| Family | Verified photos |
| --- | ---: |
| ADINKRA | 4 |
| ANKARA | 1 |
| BATIK | 1 |
| BROCADE | 8 |
| CHIFFON | 6 |
| COTTON_PLAIN | 2 |
| DENIM | 0 |
| FUGU | 3 |
| GONJA | 1 |
| KENTE | 4 |
| KENTE_PRINT | 1 |
| LACE | 4 |
| LINEN | 4 |
| SILK | 5 |
| TIEDYE | 2 |
| VELVET | 8 |
| WAX | 2 |
| **Total** | **56** |

Every accepted row keeps its creator, licence, source page and download URL in
`tools/dataset/reviewed/accepted-manifest.csv`. Nothing without complete
provenance was kept: four otherwise good photos were dropped because their
licence could not be recovered from the source.

## 2. Splits and training

`tools/dataset/prepare_kaggle.py` builds group-aware, leak-free train/validation/
test splits. `tools/qa/train_fabric_candidate.py` refuses to train unless **every
family has at least one independent image in train, validation and test**, and it
refuses to report anything when `labels.csv` and the files on disk disagree.

With 56 photos, that gate trips — and this is the honest result, not a bug:

```
ADINKRA train 3 / val 1 / test 0
ANKARA  train 1 / val 0 / test 0
DENIM   train 0 / val 0 / test 0
...
```

So no candidate model was produced. A model trained on 1–3 photos per family
would report whichever accuracy the split happened to give, and that number
would mean nothing. (`FabricFit/docs/qa/candidate/candidate-report.md` records
the refusal and the exact counts.)

The shipped classifier was **not** replaced. `ghanaian_fabric_classifier.tflite`
is untouched, and the app stays `ON_DEVICE_DEMO`.

## 3. What the shipped classifier actually scores

The 53 reviewed photos that downloaded cleanly were run through the shipping
classifier — real labels, no CGI, no buildings, no search noise:

| Metric | Result |
| --- | --- |
| Raw classifier top-1 agreement | **4 / 53 = 7.5%** |
| Families correct | BATIK, KENTE, LACE, VELVET (1 each) |
| Families with 0 correct | ADINKRA, ANKARA, BROCADE, CHIFFON, COTTON_PLAIN, FUGU, GONJA, KENTE_PRINT, LINEN, SILK, TIEDYE, WAX |
| Near-duplicates of classifier catalog images | 0 |

The previous curated diagnostic on one photo per family scored 2/17 (11.8%).
Both numbers are diagnostic smoke results, not production accuracy. The honest
reading is that the current model is close to useless on real photographs, which
is consistent with it having been built on procedural/catalog imagery.

Full per-image predictions, top-3 scores, confidence, licence and attribution:
`FabricFit/docs/qa/reviewed-diagnostic/real-fabric-report.md` and
`real-fabric-contact-sheet.jpg`.

## 4. About the 100% request

100% accuracy was not delivered, and it cannot be promised:

1. **The data does not exist in the reachable pool.** 56 verified openly licensed
   photos total — 1 to 8 per family. The repository's own policy needs 300+ per
   class to train honestly and 1000+ per class before any accuracy claim.
2. **A perfect score on a split this small would be memorisation, not accuracy.**
   A model can score 100% on 6 test photos and still fail on the first real user
   photo. Reporting that as "100% accuracy" would be a false claim.
3. **Several labels are genuinely ambiguous.** Wax print vs Ankara, kente cloth
   vs kente print, fugu vs Gonja smock cannot be separated reliably from filenames
   or from thumbnails; they need a knowledgeable Ghanaian textile specialist. I am
   not able to certify cultural labels, and I have not done fieldwork.
4. **Real-world accuracy depends on capture conditions** — lighting, blur,
   folds, partial views, garment-on-body vs flat cloth — which this pool barely
   represents.

## 5. What would actually move the number

1. Field photography: 300+ consented photos per family, 1000+ before claims, with
   the family recorded by the person who owns or sells the cloth.
2. An expert labelling session (Ghanaian weavers/traders or a textile department)
   to resolve wax vs Ankara, kente vs kente print, fugu vs Gonja.
3. Re-run `prepare_kaggle.py` then `tools/qa/train_fabric_candidate.py` — the
   pipeline is already leak-free, reports per-class metrics, and refuses to
   oversell thin data.
4. Only then consider replacing the app asset, keeping the same app-side fusion
   and the `ON_DEVICE_DEMO` labelling until a real held-out result justifies more.

## 6. Reproduce it

```bash
python3 tools/qa/review_fabric_dataset.py sheets \
  --data-dir FabricFit/tools/dataset/scraped/wikimedia \
  --data-dir FabricFit/tools/dataset/scraped/openverse \
  --data-dir FabricFit/tools/dataset/scraped/manifest \
  --output-dir qa-artifacts/review-round

python3 tools/qa/review_fabric_dataset.py export \
  --index FabricFit/docs/qa/review-round-1/review-index.csv \
  --decisions FabricFit/tools/dataset/reviewed/round-1-decisions.csv \
  --provenance FabricFit/docs/qa/review-round-1/provenance-overrides.csv \
  --output-dir FabricFit/tools/dataset/reviewed/round-1

python3 tools/qa/fabric_model_qa.py online \
  --data-dir qa-artifacts/staging/reviewed --per-class 20 \
  --evaluation-type curated --output-dir FabricFit/docs/qa/reviewed-diagnostic
```

In CI: `[fabric-review]` collects and renders review sheets, `[fabric-provenance]`
re-reads licences from source, `[fabric-train]` splits, evaluates and attempts
training.
