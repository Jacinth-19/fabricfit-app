#!/usr/bin/env python3
"""Scrape free-licensed fabric photos from the Openverse API.

Openverse aggregates CC/PD images (Flickr, Wikimedia, museums, ...). Anonymous
access is rate-limited; for anything serious register a free key at
https://openverse.org/token and export OPENVERSE_API_KEY.

Only images whose license passes the shared CC0/PD/CC BY/CC BY-SA gate are
kept, blocked hosts are skipped, and everything runs through the shared
staging gate (size check + dHash dedup + credits row).

Usage:
  python3 tools/dataset/scrape/openverse.py [--per-class 40] [--sleep 1.0]
                                            [--dry-run] [--classes KENTE,FUGU]
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from urllib.parse import urlparse

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

API = "https://api.openverse.org/v1/images/"
LICENSE_SLUGS = "cc0,by,by-sa,pdm"


def iter_candidates(payload: dict):
    """Yield (result, reason) — reason is None for usable hits."""
    for res in payload.get("results", []) or []:
        url = res.get("url") or ""
        if not url:
            yield res, "no url"
            continue
        if common.url_blocked(url):
            yield res, "blocked host"
            continue
        raw_lic = " ".join(
            str(res.get(k) or "")
            for k in ("license", "license_version")
        ).strip()
        slug = str(res.get("license") or "").lower()
        if slug == "pdm":
            lic = "Public domain"
        else:
            lic = common.norm_license(f"CC {raw_lic}" if slug in ("cc0", "by", "by-sa") else raw_lic)
        if not lic:
            yield res, f"license rejected: {raw_lic or 'unknown'}"
            continue
        w, h = res.get("width") or 0, res.get("height") or 0
        if w and h and min(w, h) < 300:
            yield res, f"too small ({w}x{h})"
            continue
        res["_license"] = lic
        yield res, None


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--per-class", type=int, default=40)
    ap.add_argument("--queries-per-class", type=int, default=3)
    ap.add_argument("--max-pages", type=int, default=3, help="API pages (50 hits each) per query")
    ap.add_argument("--sleep", type=float, default=1.0)
    ap.add_argument("--threshold", type=int, default=6)
    ap.add_argument("--min-side", type=int, default=220)
    ap.add_argument("--classes", type=str, default=None)
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    headers = {}
    key = os.environ.get("OPENVERSE_API_KEY")
    if key:
        headers["Authorization"] = f"Bearer {key}"
    session = common.make_session(headers)

    qmap = json.loads((common.HERE / "queries.json").read_text())["queries"]
    classes = common.load_classes()
    if args.classes:
        wanted = {c.strip().upper() for c in args.classes.split(",")}
        unknown = wanted - set(classes)
        if unknown:
            print(f"ERROR: unknown classes: {sorted(unknown)}", file=sys.stderr)
            return 2
    targets = [c for c in classes if c in qmap and (not args.classes or c in wanted)]

    staging = None if args.dry_run else common.Staging("openverse", classes, threshold=args.threshold)
    kept = skipped = errors = 0

    for cls in targets:
        for q in qmap[cls][: args.queries_per_class]:
            if staging and staging.stats["ok"] >= args.per_class:
                break
            page_no = 1
            while page_no <= args.max_pages:
                params = {
                    "q": q, "page": page_no, "page_size": 50,
                    "license": LICENSE_SLUGS, "mature": "false",
                    "filter_dead": "true",
                }
                try:
                    raw = common.fetch_bytes(session, API, params=params)
                except Exception as e:
                    common.log(f"  ! {cls} / {q}: API error ({e})")
                    errors += 1
                    break
                payload = json.loads(raw)
                common.polite_sleep(args.sleep)

                batch = list(iter_candidates(payload))
                if not batch:
                    break
                for res, reason in batch:
                    if reason:
                        skipped += 1
                        continue
                    if args.dry_run:
                        common.log(f"  [dry] {cls} <- {res.get('title')} ({res['_license']})")
                        kept += 1
                        continue
                    try:
                        data = common.fetch_bytes(session, res["url"])
                    except Exception as e:
                        common.log(f"  ! download failed {res.get('title')}: {e}")
                        errors += 1
                        continue
                    img, why = common.validate_image(data, min_side=args.min_side)
                    if img is None:
                        skipped += 1
                        continue
                    landing = res.get("foreign_landing_url") or ""
                    source_host = urlparse(res.get("url", "")).netloc
                    outcome = staging.add(
                        cls, img,
                        title=res.get("title") or res.get("id", ""),
                        creator=res.get("creator") or "",
                        license_name=res["_license"],
                        landing_url=landing,
                        file_url=res.get("url", ""),
                        source_key=f"openverse:{res.get('id', '')}",
                        filename_hint=f"{cls}_{res.get('title') or res.get('id', '')}_{source_host}",
                        min_side=args.min_side,
                    )
                    if outcome == "ok":
                        kept += 1
                        common.log(f"  + {cls}/{res.get('title')} [{res['_license']}]")
                    else:
                        skipped += 1
                    if staging.stats["ok"] >= args.per_class:
                        break
                if not payload.get("results") or staging and staging.stats["ok"] >= args.per_class:
                    break
                page_no += 1

    if staging:
        st = staging.stats
        common.log(
            f"\nopenverse done: kept={st['ok']} duplicates={st['duplicate']} "
            f"conflicts={st['conflict']} invalid={st['invalid']} "
            f"filtered_by_rules={skipped} api_errors={errors}"
        )
        if not key:
            common.log("tip: set OPENVERSE_API_KEY for higher rate limits (free at openverse.org/token)")
        common.log(f"staging: {staging.root}")
    else:
        common.log(f"\ndry run: {kept} candidates would be fetched; nothing written.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
