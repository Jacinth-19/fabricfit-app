#!/usr/bin/env python3
"""Turn the raw/ + scraped/ pools into a clean, Kaggle-ready export.

Pipeline (all leak-free, all reproducible):
  1. load every image from raw/<CLASS>/ (app contributions) and
     scraped/<SOURCE>/<CLASS>/ (scrape/ outputs) — underscore folders ignored
  2. validate: decode, EXIF-normalise, min side check (invalids counted)
  3. dedup: 64-bit dHash, hamming <= --threshold (duplicates AND cross-class
     conflicts dropped, never silently)
  4. group: near-duplicate clusters at the looser --group-threshold so no two
     variants of the same source photo can land in different splits
  5. split: stratified 70/15/15 by GROUP (seeded, deterministic)
  6. write: <out>/{train,val,test}/<CLASS>/*.jpg capped at --max-side,
     labels.csv, CREDITS.csv (per-image provenance + license),
     dataset-metadata.json (kaggle CLI), DATA_CARD.md (honesty + counts)

Usage:
  python3 tools/dataset/prepare_kaggle.py \
      --kaggle-id YOUR-KAGGLE-USERNAME/fabricfit-fabric-17 [--zip]

Then:
  kaggle datasets create -p tools/dataset/kaggle_export
(or upload <out>.zip via the web UI) and train with train_on_kaggle.py.
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import random
import shutil
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "scrape"))
import common  # noqa: E402
from PIL import Image, ImageOps  # noqa: E402

ROOT = Path(__file__).resolve().parent          # tools/dataset
DEFAULT_OUT = ROOT / "kaggle_export"

SOURCE_TAGS = {
    "app": "app", "wikimedia": "wm", "openverse": "ov",
    "github": "gh", "manifest": "mf",
}

BAR_TRAIN = 300      # label_schema.json: enough raw/class to train honestly
BAR_CLAIM = 1000     # label_schema.json: enough raw/class before accuracy claims


# --------------------------------------------------------------- loading

def iter_pool(raw_dir: Path, scraped_dir: Path, classes: list[str]):
    """Yield (class, path, source_name) for every candidate image."""
    for cls in classes:
        d = raw_dir / cls
        if d.is_dir():
            for f in sorted(d.iterdir()):
                if f.is_file() and f.suffix.lower() in (".jpg", ".jpeg", ".png", ".webp"):
                    yield cls, f, "app"
    if scraped_dir.is_dir():
        for src_dir in sorted(scraped_dir.iterdir()):
            if not src_dir.is_dir() or src_dir.name.startswith("_"):
                continue
            for cls in classes:
                d = src_dir / cls
                if d.is_dir():
                    for f in sorted(d.iterdir()):
                        if f.is_file() and f.suffix.lower() in (".jpg", ".jpeg", ".png", ".webp"):
                            yield cls, f, src_dir.name


def load_credit_rows(scraped_dir: Path) -> dict:
    """(source, class, filename) -> credit row."""
    out = {}
    if not scraped_dir.is_dir():
        return out
    for src in scraped_dir.iterdir():
        cpath = src / "CREDITS.csv"
        if not cpath.is_file():
            continue
        with cpath.open(newline="", encoding="utf-8") as fh:
            for row in csv.DictReader(fh):
                out[(src.name, row.get("class", ""), row.get("filename", ""))] = row
    return out


# ----------------------------------------------------------------- split

class UnionFind:
    def __init__(self):
        self.parent: dict[str, str] = {}

    def find(self, x: str) -> str:
        self.parent.setdefault(x, x)
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]
            x = self.parent[x]
        return x

    def union(self, a: str, b: str):
        ra, rb = self.find(a), self.find(b)
        if ra != rb:
            self.parent[rb] = ra


def assign_splits(groups: list[list[dict]], rng: random.Random, ratios=(0.70, 0.15, 0.15)) -> dict[str, str]:
    """Greedy stratified group assignment. Returns item_id -> split."""
    names = ["train", "val", "test"]
    targets = dict(zip(names, ratios))
    order = list(groups)
    rng.shuffle(order)
    counts = {n: 0 for n in names}
    total = sum(len(g) for g in order) or 1
    assign: dict[str, str] = {}
    for group in order:
        # pick the split furthest below its target share; ties -> train, val, test
        best = min(
            names,
            key=lambda n: (counts[n] / total - targets[n], names.index(n)),
        )
        for item in group:
            assign[item["id"]] = best
        counts[best] += len(group)
    return assign


# ---------------------------------------------------------------- export

def resize_bytes(img: Image.Image, max_side: int, quality: int = 90) -> bytes:
    w, h = img.size
    if max(w, h) > max_side:
        scale = max_side / max(w, h)
        img = img.resize((max(1, int(w * scale)), max(1, int(h * scale))), Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue()


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--raw-dir", default=str(ROOT / "raw"))
    ap.add_argument("--scraped-dir", default=str(ROOT / "scraped"))
    ap.add_argument("--out", default=str(DEFAULT_OUT))
    ap.add_argument("--kaggle-id", default="YOUR-KAGGLE-USERNAME/fabricfit-fabric-17")
    ap.add_argument("--title", default="FabricFit - 17-class Ghanaian fabric photos (starter)")
    ap.add_argument("--dataset-license", default="CC BY-SA 4.0",
                    help="collection license for dataset-metadata.json; per-image licenses live in CREDITS.csv")
    ap.add_argument("--val", type=float, default=0.15)
    ap.add_argument("--test", type=float, default=0.15)
    ap.add_argument("--min-side", type=int, default=220)
    ap.add_argument("--max-side", type=int, default=1280)
    ap.add_argument("--threshold", type=int, default=6, help="dHash distance = duplicate")
    ap.add_argument("--group-threshold", type=int, default=10,
                    help="dHash distance = same source photo (must stay in one split)")
    ap.add_argument("--seed", type=int, default=13)
    ap.add_argument("--zip", action="store_true", help="also write <out>.zip for the web upload path")
    ap.add_argument("--force", action="store_true", help="overwrite an existing out dir")
    args = ap.parse_args()

    classes = common.load_classes()
    raw_dir, scraped_dir, out = Path(args.raw_dir), Path(args.scraped_dir), Path(args.out)
    if out.exists():
        if not args.force:
            print(f"ERROR: {out} exists — pass --force to overwrite.", file=sys.stderr)
            return 2
        shutil.rmtree(out)

    credits_index = load_credit_rows(scraped_dir)
    rng = random.Random(args.seed)

    # -- 1/2/3: validate + dedup --------------------------------------
    stats: dict[str, dict] = defaultdict(lambda: defaultdict(int))
    accepted: list[dict] = []
    pool: list[tuple[int, dict]] = []           # (dhash, item) of accepted

    for cls, path, source in iter_pool(raw_dir, scraped_dir, classes):
        s = stats[cls]
        s["found"] += 1
        try:
            data = path.read_bytes()
            img = Image.open(io.BytesIO(data))
            img.load()
        except Exception:
            s["invalid"] += 1
            continue
        img = ImageOps.exif_transpose(img)
        w, h = img.size
        if min(w, h) < args.min_side:
            s["invalid"] += 1
            continue
        if img.mode != "RGB":
            img = img.convert("RGB")
        s["valid"] += 1

        dh = common.dhash(img)
        conflict = False
        for other_hash, other in pool:
            if common.hamming(dh, other_hash) <= args.threshold:
                if other["class"] == cls:
                    s["duplicate"] += 1
                else:
                    s["conflict"] += 1
                    stats[other["class"]]["conflict"] += 1
                conflict = True
                break
        if conflict:
            continue

        item = {
            "id": f"{source}:{cls}:{path}",
            "class": cls,
            "path": path,
            "source": source,
            "dhash": dh,
            "img": img,
        }
        accepted.append(item)
        pool.append((dh, item))
        s["accepted"] += 1

    # -- 4: near-dup groups (leak-free split) -------------------------
    # Two rules, applied so they cannot over-merge:
    #   a) same provenance source_key (credits row — e.g. all tiles of one
    #      AITEX strip / all frames of one fabric roll, or all variants of
    #      one Commons page) -> ONE split. This is the safety rule.
    #   b) near-duplicate images (dHash <= --group-threshold) -> ONE split,
    #      but ONLY among items WITHOUT a provenance key (raw/ pool).
    #      Chaining keyed items visually would percolate large homogeneous
    #      sets (e.g. plain weaves) into a single useless group.
    groups_by_class: dict[str, list[list[dict]]] = defaultdict(list)
    for cls in classes:
        items = [it for it in accepted if it["class"] == cls]
        if not items:
            continue
        uf = UnionFind()
        for it in items:
            uf.find(it["id"])
        # a) provenance-based grouping
        by_source: dict[str, list[dict]] = defaultdict(list)
        for it in items:
            row = credits_index.get((it["source"], it["class"], it["path"].name)) or {}
            sk = (row.get("source_key") or "").split("#")[0].strip()
            it["gkey"] = sk
            if sk:
                by_source[sk].append(it)
        for same_source in by_source.values():
            for other in same_source[1:]:
                uf.union(same_source[0]["id"], other["id"])
        # b) visual near-duplicate grouping for un-keyed (raw pool) items
        unkeyed = [it for it in items if not it.get("gkey")]
        for i, a in enumerate(unkeyed):
            for b in unkeyed[i + 1:]:
                if common.hamming(a["dhash"], b["dhash"]) <= args.group_threshold:
                    uf.union(a["id"], b["id"])
        buckets: dict[str, list[dict]] = defaultdict(list)
        for it in items:
            buckets[uf.find(it["id"])].append(it)
        groups_by_class[cls] = list(buckets.values())

    # -- 5: stratified split by group ---------------------------------
    warnings: list[str] = []
    split_of: dict[str, str] = {}
    for cls, groups in groups_by_class.items():
        if len(groups) == 1:
            warnings.append(f"{cls}: only 1 source group — everything goes to train (no val/test possible)")
        elif len(groups) == 2:
            warnings.append(f"{cls}: only 2 groups — val or test will be empty")
    n_groups = {cls: len(groups_by_class.get(cls, [])) for cls in classes}
    for cls in classes:
        kept = stats[cls].get("accepted", 0)
        g = n_groups[cls]
        if g and kept > 10 * g and g < 30:
            warnings.append(
                f"{cls}: diversity warning — {kept} images from only {g} source groups "
                f"(tiles/frames of the same sources). Image count OVERSTATES photo diversity; "
                f"keep field photos coming"
            )
    for cls, groups in groups_by_class.items():
        split_of.update(assign_splits(groups, rng, ratios=(1 - args.val - args.test, args.val, args.test)))
    for cls in classes:
        if cls not in groups_by_class:
            warnings.append(f"{cls}: NO images found in raw/ or scraped/")

    # -- 6: write ------------------------------------------------------
    out.mkdir(parents=True)
    labels_rows = []
    export_credits = []
    per_split_count: dict[str, dict] = defaultdict(lambda: defaultdict(int))

    for it in accepted:
        split = split_of[it["id"]]
        tag = SOURCE_TAGS.get(it["source"], it["source"][:3])
        cls_dir = out / split / it["class"]
        cls_dir.mkdir(parents=True, exist_ok=True)
        stem = common.sanitize_name(f"{tag}_{it['path'].stem}")
        name = f"{stem}.jpg"
        n = 2
        while (cls_dir / name).exists():
            name = f"{stem}_{n}.jpg"
            n += 1
        (cls_dir / name).write_bytes(resize_bytes(it["img"], args.max_side))
        rel = f"{split}/{it['class']}/{name}"
        labels_rows.append({
            "path": rel, "label": it["class"], "split": split, "source": it["source"],
        })
        per_split_count[it["class"]][split] += 1

        if it["source"] == "app":
            export_credits.append({
                "source": "app_contribution", "source_key": "", "class": it["class"],
                "filename": rel, "title": it["path"].name, "creator": "app user (consented)",
                "license": "user consent via in-app contribution flow",
                "landing_url": "", "file_url": "", "retrieved": "",
            })
        else:
            row = credits_index.get((it["source"], it["class"], it["path"].name)) or {}
            export_credits.append({
                "source": row.get("source", it["source"]),
                "source_key": row.get("source_key", ""),
                "class": it["class"], "filename": rel,
                "title": row.get("title", it["path"].name),
                "creator": row.get("creator", ""),
                "license": row.get("license", "UNKNOWN — check source"),
                "landing_url": row.get("landing_url", ""),
                "file_url": row.get("file_url", ""),
                "retrieved": row.get("retrieved", ""),
            })

    # non-commercial rows (operator-approved allowlist exceptions): the
    # restriction must be unmissable in warnings + DATA_CARD
    nc_rows = [r for r in export_credits
               if any(t in (r.get("license") or "").lower()
                      for t in ("non-commercial", "noncommercial"))]
    if nc_rows:
        warnings.append(
            f"{len(nc_rows)} exported image(s) carry a NON-COMMERCIAL license "
            f"(operator-approved allowlist exception) — this dataset may only be "
            f"used non-commercially; see CREDITS.csv license column"
        )

    with (out / "labels.csv").open("w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=["path", "label", "split", "source"])
        w.writeheader()
        w.writerows(sorted(labels_rows, key=lambda r: r["path"]))

    with (out / "CREDITS.csv").open("w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=common.CREDIT_FIELDS)
        w.writeheader()
        w.writerows(export_credits)

    meta = {
        "id": args.kaggle_id,
        "title": args.title,
        "licenses": [{"name": args.dataset_license}],
    }
    (out / "dataset-metadata.json").write_text(json.dumps(meta, indent=2) + "\n")

    # ship the trainer inside the export so a Kaggle notebook can run it
    # with internet OFF (dataset auto-mount only)
    trainer = Path(__file__).resolve().parent / "train_on_kaggle.py"
    if trainer.is_file():
        shutil.copy2(trainer, out / "train_on_kaggle.py")

    write_data_card(out, stats, per_split_count, warnings, args, len(accepted),
                    n_groups, len(nc_rows))

    # -- summary -------------------------------------------------------
    print(f"\n{'class':16s} {'found':>6s} {'kept':>5s} {'dup':>4s} {'conf':>4s} "
          f"{'bad':>4s} {'grp':>4s} {'train':>6s} {'val':>4s} {'test':>5s}  bar")
    for cls in classes:
        s = stats[cls]
        c = per_split_count[cls]
        kept = s.get("accepted", 0)
        if s.get("found", 0) == 0:
            bar = "-"
        elif c.get("train", 0) >= BAR_CLAIM:
            bar = "CLAIM OK"
        elif c.get("train", 0) >= BAR_TRAIN:
            bar = "train ok"
        else:
            bar = f"need {BAR_TRAIN - c.get('train', 0)} more"
        print(f"{cls:16s} {s.get('found', 0):6d} {kept:5d} {s.get('duplicate', 0):4d} "
              f"{s.get('conflict', 0):4d} {s.get('invalid', 0):4d} {n_groups.get(cls, 0):4d} "
              f"{c.get('train', 0):6d} {c.get('val', 0):4d} {c.get('test', 0):5d}  {bar}")

    if warnings:
        print("\nwarnings:")
        for wmsg in warnings:
            print(f"  ! {wmsg}")
    print(f"\nexport: {out}")
    print(f"  labels.csv rows: {len(labels_rows)}   credits rows: {len(export_credits)}")
    print("HONESTY: web-sourced photos are starter data only — the production bar "
          f"({BAR_CLAIM} real photos/class) still requires field + consented contribution photos.")

    if args.zip:
        archive = shutil.make_archive(str(out), "zip", root_dir=out)
        print(f"  archive: {archive}")
    return 0


def write_data_card(out: Path, stats, per_split_count, warnings, args,
                    kept_total: int, n_groups: dict, nc_count: int = 0) -> None:
    lines = [
        f"# {args.title}",
        "",
        "Leak-free, license-audited export built by `tools/dataset/prepare_kaggle.py`.",
        "",
        "> **Honesty banner (non-negotiable, see `tools/dataset/README.md`):**",
        "> this is **starter** data. Web-scraped images never satisfy the production bar on",
        f"> their own — no accuracy claims until **{BAR_CLAIM} real photos per class**",
        "(market/weaver field days + consented app contributions), and never mix",
        "authentic hand-woven kente with machine-printed samples in one label.",
        "",
        "## Splits",
        "",
        "Stratified 70/15/15 by **group**, seed " + str(args.seed) + ". A group is:",
        "",
        "1. all images sharing one provenance source (same Commons page, same source",
        "   directory/tiles — so frames of one fabric roll never straddle splits), and/or",
        "2. near-duplicate images (dHash distance ≤ " + str(args.group_threshold) + ").",
        "",
        "Duplicates (≤ " + str(args.threshold) + ") and cross-class conflicts are dropped before splitting.",
        "",
        "## Per-class counts",
        "",
        "`src groups` = independent sources; tiles/frames of one source count as ONE group.",
        "Treat a high image count with a low group count as quantity, not diversity.",
        "",
        "| class | found | kept | duplicates | conflicts | invalid | src groups | train | val | test |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    classes = common.load_classes()
    for cls in classes:
        s, c = stats[cls], per_split_count[cls]
        lines.append(
            f"| {cls} | {s.get('found', 0)} | {s.get('accepted', 0)} | {s.get('duplicate', 0)} "
            f"| {s.get('conflict', 0)} | {s.get('invalid', 0)} | {n_groups.get(cls, 0)} "
            f"| {c.get('train', 0)} | {c.get('val', 0)} | {c.get('test', 0)} |"
        )
    lines += [
        "",
        f"Total exported images: **{kept_total}**",
        "",
        "## Provenance & licenses",
        "",
        "Per-image provenance and license: `CREDITS.csv` (collection license for the",
        "compilation declared in `dataset-metadata.json`; individual items keep their own)",
        "",
        "- `raw/` pool: consented in-app contributions (see store privacy policy)",
        "- `scraped/wikimedia/`: Wikimedia Commons — CC0 / PD / CC BY / CC BY-SA only",
        "- `scraped/openverse/`: Openverse results — same license gate",
        "- `scraped/github/`: allowlisted research repos only (see `scrape/github_allowlist.json`)",
        "- `scraped/manifest/`: explicit CSV rows with declared licenses",
        "",
        ("Blocked outright: stock agencies, marketplaces, NC/ND licenses (see `scrape/common.py`)."
         if not nc_count else
         f"Blocked by default: stock agencies, marketplaces, ND and unlicensed repos "
         f"(see `scrape/common.py`). **Exception (operator-approved):** {nc_count} image(s) "
         f"under a *non-commercial academic* license are included — non-commercial "
         f"restrictions apply to the whole subset; see `CREDITS.csv`."),
        "",
    ]
    if warnings:
        lines += ["## Warnings", ""]
        lines += [f"- {w}" for w in warnings]
        lines += [""]
    lines += [
        "## Train on Kaggle",
        "",
        "```bash",
        "# upload (CLI) — edit the id in dataset-metadata.json first",
        "kaggle datasets create -p tools/dataset/kaggle_export",
        "",
        "# or upload kaggle_export.zip via https://www.kaggle.com/datasets",
        "```",
        "",
        "New Kaggle Notebook → attach the dataset → run `train_on_kaggle.py`",
        "(shipped inside this export):",
        "",
        "```bash",
        "python train_on_kaggle.py --data-dir /kaggle/input/<your-slug>",
        "```",
        "",
        "## Readiness bars (label_schema.json)",
        "",
        f"- **{BAR_TRAIN} train images/class** → honest training allowed",
        f"- **{BAR_CLAIM} train images/class** → accuracy claims / shipping allowed",
        "",
    ]
    (out / "DATA_CARD.md").write_text("\n".join(lines))


if __name__ == "__main__":
    sys.exit(main())
