#!/usr/bin/env python3
"""Scrape free-licensed fabric photos from Wikimedia Commons.

For every class in label_schema.json (see queries.json) this walks Commons
search results, keeps only images whose extmetadata license passes the
CC0/PD/CC BY/CC BY-SA gate, downloads a 2048px thumbnail, then runs the
shared staging gate (size check + dHash dedup + credits row).

Commons asks scrapers for a descriptive User-Agent — set
FABRICFIT_SCRAPER_CONTACT (email or URL) so operators can reach you.

Usage:
  python3 tools/dataset/scrape/wikimedia.py [--per-class 50] [--sleep 0.5]
                                            [--dry-run] [--classes KENTE,ANKARA]
                                            [--threshold 6] [--min-side 220]
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

API = "https://commons.wikimedia.org/w/api.php"
OK_MIMES = {"image/jpeg", "image/png", "image/webp", "image/tiff"}


def iter_candidates(payload: dict):
    """Yield (page, reason) — page has imageinfo when reason == None."""
    pages = payload.get("query", {}).get("pages", []) or []
    for page in pages:
        info = (page.get("imageinfo") or [None])[0]
        if not info:
            yield page, "no imageinfo"
            continue
        mime = (info.get("mime") or "").lower()
        if mime not in OK_MIMES:
            yield page, f"mime {mime or '?'} unsupported"
            continue
        if min(info.get("width") or 0, info.get("height") or 0) < 300:
            yield page, "original below 300px"
            continue
        ext = info.get("extmetadata") or {}
        raw_lic = (
            (ext.get("LicenseShortName") or {}).get("value")
            or (ext.get("License") or {}).get("value")
            or ""
        )
        lic = common.norm_license(raw_lic)
        if not lic:
            yield page, f"license rejected: {raw_lic or 'unknown'}"
            continue
        landing = info.get("descriptionurl") or ""
        if common.url_blocked(landing) or common.url_blocked(info.get("url") or ""):
            yield page, "blocked host"
            continue
        page["_license"] = lic
        page["_info"] = info
        yield page, None


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--per-class", type=int, default=50, help="max kept images per class")
    ap.add_argument("--queries-per-class", type=int, default=3, help="how many queries from queries.json to run per class")
    ap.add_argument("--max-pages", type=int, default=3, help="search result pages (50 hits each) per query")
    ap.add_argument("--sleep", type=float, default=0.5, help="politeness delay between API calls (s)")
    ap.add_argument("--threshold", type=int, default=6, help="dHash hamming distance = duplicate")
    ap.add_argument("--min-side", type=int, default=220)
    ap.add_argument("--classes", type=str, default=None, help="comma list; default all classes in queries.json")
    ap.add_argument("--dry-run", action="store_true", help="list candidates, download nothing")
    args = ap.parse_args()

    qmap = json.loads((common.HERE / "queries.json").read_text())["queries"]
    classes = common.load_classes()
    if args.classes:
        wanted = {c.strip().upper() for c in args.classes.split(",")}
        unknown = wanted - set(classes)
        if unknown:
            print(f"ERROR: unknown classes: {sorted(unknown)}", file=sys.stderr)
            return 2
    targets = [c for c in classes if c in qmap and (not args.classes or c in wanted)]

    session = common.make_session()
    staging = None if args.dry_run else common.Staging("wikimedia", classes, threshold=args.threshold)

    kept = skipped = errors = 0
    for cls in targets:
        queries = qmap[cls][: args.queries_per_class]
        for q in queries:
            if staging and staging.stats["ok"] >= args.per_class:
                break
            offset = 0
            pages_done = 0
            while pages_done < args.max_pages:
                params = {
                    "action": "query", "generator": "search",
                    "gsrsearch": q, "gsrnamespace": 6, "gsrlimit": 50,
                    "gsroffset": offset,
                    "prop": "imageinfo",
                    "iiprop": "url|extmetadata|size|mime",
                    "iiurlwidth": 2048,
                    "format": "json", "formatversion": 2,
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
                for page, reason in batch:
                    if reason:
                        skipped += 1
                        continue
                    info = page["_info"]
                    title = page.get("title", "").replace("File:", "")
                    if args.dry_run:
                        common.log(f"  [dry] {cls} <- {title} ({page['_license']})")
                        kept += 1
                        continue
                    thumb = info.get("thumburl") or info.get("url")
                    try:
                        data = common.fetch_bytes(session, thumb)
                    except Exception as e:
                        common.log(f"  ! download failed {title}: {e}")
                        errors += 1
                        continue
                    img, why = common.validate_image(data, min_side=args.min_side)
                    if img is None:
                        skipped += 1
                        continue
                    ext = info.get("extmetadata") or {}
                    outcome = staging.add(
                        cls, img,
                        title=title,
                        creator=common.strip_html((ext.get("Artist") or {}).get("value", "")),
                        license_name=page["_license"],
                        landing_url=info.get("descriptionurl", ""),
                        file_url=info.get("url", ""),
                        source_key=f"commons:{page.get('pageid', title)}",
                        filename_hint=f"{cls}_{title}",
                        min_side=args.min_side,
                    )
                    if outcome == "ok":
                        kept += 1
                        common.log(f"  + {cls}/{title} [{page['_license']}]")
                    else:
                        skipped += 1
                    if staging and staging.stats["ok"] >= args.per_class:
                        break
                if "continue" not in payload or staging and staging.stats["ok"] >= args.per_class:
                    break
                offset = payload["continue"].get("gsroffset", 0)
                pages_done += 1

    if staging:
        st = staging.stats
        common.log(
            f"\nwikimedia done: kept={st['ok']} duplicates={st['duplicate']} "
            f"conflicts={st['conflict']} invalid={st['invalid']} "
            f"filtered_by_rules={skipped} api_errors={errors}"
        )
        common.log(f"staging: {staging.root}")
    else:
        common.log(f"\ndry run: {kept} candidates would be fetched; nothing written.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
