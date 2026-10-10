# Web scraping → Kaggle dataset toolkit

Turns free-licensed online photos into a **clean, leak-free, Kaggle-ready
export** of the app's 17 fabric classes (`../label_schema.json`), with
per-image provenance so nothing unlicensed or watermarked can sneak in.

```
scrape/
  common.py           license gate, blocklist, dHash, staging pool + credits
  queries.json        search terms per class (edit to tune coverage)
  wikimedia.py        Wikimedia Commons API (CC0/PD/CC BY/CC BY-SA only)
  openverse.py        Openverse API (same gate; optional OPENVERSE_API_KEY)
  github_repos.py     allowlisted research repos only (github_allowlist.json)
  manifest.py         CSV of explicit URLs you vouch for
  selftest.py         offline test-suite (run this first)
prepare_kaggle.py     raw/ + scraped/ -> kaggle_export/ (splits, credits, data card)
train_on_kaggle.py    training entrypoint for a Kaggle Notebook
```

## Safety rails (why this is not a blind scraper)

- **License gate** — only CC0 / Public domain / CC BY / CC BY-SA pass.
  NC, ND, "editorial use", "all rights reserved" are rejected (fail closed).
  The one documented exception: an allowlist entry with
  `"license_override": true` accepts a non-standard license the operator has
  read (e.g. the non-commercial academic TS-Textile-Stripe set) — the declared
  string is recorded per-image in CREDITS.csv and the restriction travels with
  the data forever. The allowlist is the audit trail; never add one casually.
- **Host blocklist** — stock agencies (Shutterstock, Getty, Alamy, …),
  marketplaces and pinboards (Etsy, Amazon, Pinterest, …) never enter the pool.
- **Provenance** — every kept image writes a row to
  `scraped/<source>/CREDITS.csv` (title, creator, license, landing page);
  `prepare_kaggle.py` merges them into the export's `CREDITS.csv`.
- **Auxiliary labels** — allowlist entries may map filenames to classes that are
  NOT in `label_schema.json` (e.g. `FAKE_KENTE` for the authenticity task).
  They are staged with full provenance but `prepare_kaggle.py` never exports
  out-of-schema classes into the 17-class dataset.
- **Dedup + label-conflict detection** — 64-bit dHash (same as `ingest.py`),
  identical images and cross-class collisions are dropped, not silently kept.
- **Leak-free splits** — a shared provenance `source_key` (same source page,
  same directory/grand-directory via the allowlist `group_by` option) plus
  near-duplicate dHash clusters are kept inside one split, so val/test never
  see a twin of a train photo and tiles/frames of one source never straddle.
- **raw/ is sacred** — scrapers only write to `scraped/`; the `raw/` pool stays
  for human-vetted, consented app contributions (`ingest.py`).

## Workflow

```bash
# 0. prove the toolkit (offline, ~30s)
python3 tools/dataset/scrape/selftest.py

# 1. scrape — each source is separate so you can stop/audit any of them
export NTOMA_SCRAPER_CONTACT="you@example.com"      # polite UA for Commons
python3 tools/dataset/scrape/wikimedia.py --per-class 60
python3 tools/dataset/scrape/openverse.py --per-class 40   # optional key: OPENVERSE_API_KEY
python3 tools/dataset/scrape/github_repos.py               # allowlist only
python3 tools/dataset/scrape/manifest.py my_urls.csv       # your own list

# 2. build the Kaggle export (deterministic, seed 13)
python3 tools/dataset/prepare_kaggle.py \
    --kaggle-id YOUR-KAGGLE-USERNAME/ntoma-fabric-17 --zip

# 3. upload (edit the id inside kaggle_export/dataset-metadata.json first)
kaggle datasets create -p tools/dataset/kaggle_export
#   ...or drag kaggle_export.zip onto https://www.kaggle.com/datasets

# 4. train: new Kaggle Notebook -> attach dataset -> run
python3 train_on_kaggle.py --data-dir /kaggle/input/<your-slug>
```

Every scraper also accepts `--dry-run` (list candidates, download nothing),
`--classes A,B` (subset), `--threshold N` (dedup distance) and `--min-side N`.

## What the export looks like

```
kaggle_export/
  train/<CLASS>/*.jpg  val/<CLASS>/*.jpg  test/<CLASS>/*.jpg   # 70/15/15 by group
  labels.csv             # path,label,split,source
  CREDITS.csv            # per-image provenance + license
  dataset-metadata.json  # for the kaggle CLI
  DATA_CARD.md           # counts, honesty banner, upload/train instructions
```

## Honesty rules (still apply with better data)

Scraped photos are **starter data** — great for baselines and experiments,
never enough for production claims:

- **300 train images/class** → honest training allowed
- **1,000 train images/class** → accuracy claims / shipping allowed
  (field days at Makola/Kantamanto/Kejetia, weavers in Bonwire/Tamale,
  consented app contributions via `ingest.py`)
- never mix authentic hand-woven kente with machine-printed samples in one label
- the `ON_DEVICE_DEMO` labelling stays until the numbers genuinely clear the bar
