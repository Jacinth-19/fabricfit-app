#!/usr/bin/env python3
"""Download an explicit URL list into the staging pool (the universal fallback).

Any source you are allowed to use (your own photos, partner drops, API dumps)
can be fed in as a CSV with headers:

  url,label,license,source_page,title,creator
  https://example.org/kente1.jpg,KENTE,CC BY 4.0,https://example.org/...,Kente strip,Anon

- label must exist in label_schema.json
- license must pass the CC0/PD/CC BY/CC BY-SA gate (override with
  --allow-any-license if you own the rights and accept the responsibility —
  the override is recorded in the credits row)
- file:// URLs only with --allow-local (used by the offline selftest)

Usage:
  python3 tools/dataset/scrape/manifest.py my_urls.csv [--allow-local]
"""
from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path
from urllib.parse import urlparse, unquote

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

REQUIRED = {"url", "label"}


def read_manifest(path: Path) -> list[dict]:
    with path.open(newline="", encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh))
    if not rows:
        raise SystemExit("manifest is empty")
    missing = REQUIRED - set(rows[0].keys())
    if missing:
        raise SystemExit(f"manifest missing columns: {sorted(missing)}")
    return rows


def local_path(url: str) -> Path | None:
    p = urlparse(url)
    if p.scheme == "file":
        return Path(unquote(p.path))
    return None


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("manifest", help="CSV with url,label,license,source_page,title,creator")
    ap.add_argument("--allow-local", action="store_true", help="permit file:// URLs (selftest only)")
    ap.add_argument("--allow-any-license", action="store_true",
                    help="accept rows without a free-culture license (you take responsibility)")
    ap.add_argument("--threshold", type=int, default=6)
    ap.add_argument("--min-side", type=int, default=220)
    ap.add_argument("--sleep", type=float, default=0.25)
    args = ap.parse_args()

    rows = read_manifest(Path(args.manifest))
    classes = common.load_classes()
    session = common.make_session()
    staging = common.Staging("manifest", classes, threshold=args.threshold)

    kept = skipped = errors = 0
    for i, row in enumerate(rows, 1):
        url = (row.get("url") or "").strip()
        cls = (row.get("label") or "").strip().upper()
        if not url or not cls:
            skipped += 1
            continue
        if cls not in classes:
            common.log(f"  ! row {i}: unknown label {cls!r}")
            errors += 1
            continue

        lic_raw = (row.get("license") or "").strip()
        lic = common.norm_license(lic_raw)
        if not lic:
            if args.allow_any_license:
                lic = f"UNVERIFIED: {lic_raw or 'unspecified'} (override --allow-any-license)"
                common.log(f"  ? row {i}: license override -> {lic}")
            else:
                common.log(f"  - row {i}: license rejected: {lic_raw or 'missing'}")
                skipped += 1
                continue
        if not args.allow_local and url.startswith("file:"):
            common.log(f"  - row {i}: file:// blocked (use --allow-local)")
            skipped += 1
            continue
        if common.url_blocked(url) and not url.startswith("file:"):
            common.log(f"  - row {i}: blocked host")
            skipped += 1
            continue

        try:
            if url.startswith("file:"):
                lp = local_path(url)
                data = lp.read_bytes() if lp and lp.exists() else b""
                if not data:
                    raise FileNotFoundError(url)
            else:
                data = common.fetch_bytes(session, url)
        except Exception as e:
            common.log(f"  ! row {i}: download failed ({e})")
            errors += 1
            continue

        img, why = common.validate_image(data, min_side=args.min_side)
        if img is None:
            skipped += 1
            continue
        outcome = staging.add(
            cls, img,
            title=row.get("title") or Path(urlparse(url).path).name,
            creator=row.get("creator") or "",
            license_name=lic,
            landing_url=row.get("source_page") or url,
            file_url=url,
            source_key=f"manifest:{i}:{url}",
            filename_hint=Path(urlparse(url).path).stem or f"row{i}",
            min_side=args.min_side,
        )
        if outcome == "ok":
            kept += 1
            common.log(f"  + {cls}/{Path(urlparse(url).path).name} [{lic}]")
        else:
            skipped += 1
        common.polite_sleep(args.sleep)

    st = staging.stats
    common.log(
        f"\nmanifest done: kept={st['ok']} duplicates={st['duplicate']} "
        f"conflicts={st['conflict']} invalid={st['invalid']} "
        f"filtered={skipped} errors={errors}"
    )
    common.log(f"staging: {staging.root}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
