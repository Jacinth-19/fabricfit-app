#!/usr/bin/env python3
"""Recover provenance for reviewed candidates whose staged credits row was lost.

A review round is built from sharded collection runs. If the shards' CREDITS.csv
files are merged as one file, only the last shard's rows survive and every other
image loses its source URL and licence. Rather than guess, this script re-reads
the licence and source page from Wikimedia Commons metadata and writes an
overrides CSV that `review_fabric_dataset.py export` accepts.

Only rows whose licence still passes the CC0/PD/CC BY/CC BY-SA gate are written,
so nothing with an unclear licence can slip into a training manifest.

Usage:
  python3 tools/qa/fill_missing_provenance.py \
      --index FabricFit/docs/qa/review-round-2/review-index.csv \
      --out FabricFit/tools/dataset/reviewed/round-2/provenance-overrides.csv
"""
from __future__ import annotations

import argparse
import csv
import difflib
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "FabricFit/tools/dataset/scrape"))
import common  # noqa: E402

API = "https://commons.wikimedia.org/w/api.php"


def title_candidates(filename: str, label: str) -> list[str]:
    stem = filename[:-4] if filename.lower().endswith(".jpg") else filename
    prefix = f"{label}_"
    if stem.startswith(prefix):
        stem = stem[len(prefix):]
    # names are deduplicated with _2, _3 ... on collision; try the plain name first
    variants = [stem]
    for suffix in ("_2", "_3", "_4"):
        if stem.endswith(suffix):
            variants.append(stem[: -len(suffix)])
    out = []
    for variant in variants:
        out.append(variant)
        if variant.lower().endswith((".jpg", ".jpeg", ".png", ".tif", ".tiff", ".webp")):
            out.append(variant.rsplit(".", 1)[0])
        else:
            out.append(variant + ".jpg")
    seen, unique = set(), []
    for name in out:
        if name.lower() not in seen:
            seen.add(name.lower())
            unique.append(name)
    return unique


def normalise(text: str) -> str:
    """Lowercase and drop everything but letters/digits, to compare titles."""
    return re.sub(r"[^a-z0-9]+", "", text.lower())


def search_match(session, row: dict, sleep: float):
    """Fallback: ask Commons to search the sanitised file name, then compare titles.

    Sanitising a title for disk mangled punctuation (commas, en dashes) for some
    files, so an exact title lookup cannot succeed for them. A search hit is only
    accepted when one candidate matches the sanitised stem closely and uniquely.
    """
    stem = row["filename"][:-4] if row["filename"].lower().endswith(".jpg") else row["filename"]
    stem = stem[len(f"{row['label']}_"):] if stem.startswith(f"{row['label']}_") else stem
    query = re.sub(r"[_.]+", " ", stem).strip()
    params = {
        "action": "query", "generator": "search", "gsrsearch": query,
        "gsrnamespace": 6, "gsrlimit": 10, "prop": "imageinfo",
        "iiprop": "url|extmetadata|size", "iiurlwidth": 2048,
        "format": "json", "formatversion": 2,
    }
    try:
        payload = json.loads(common.fetch_bytes(session, API, params=params))
    except Exception as error:
        print(f"  ! search failed for {row['id']}: {error}")
        return None
    common.polite_sleep(sleep)
    wanted = normalise(stem)
    scored = []
    for page in payload.get("query", {}).get("pages", []) or []:
        info = (page.get("imageinfo") or [None])[0]
        if not info:
            continue
        title = page.get("title", "").replace("File:", "")
        scored.append((difflib.SequenceMatcher(None, wanted, normalise(title)).ratio(), title, info))
    if not scored:
        return None
    scored.sort(reverse=True, key=lambda item: item[0])
    if scored[0][0] < 0.85:
        return None
    if len(scored) > 1 and scored[1][0] >= scored[0][0] - 0.02:
        print(f"  ? {row['id']}: ambiguous search match, skipped")
        return None
    _, title, info = scored[0]
    ext = info.get("extmetadata") or {}
    licence = common.norm_license(
        (ext.get("LicenseShortName") or {}).get("value", "")
        or (ext.get("License") or {}).get("value", "")
    )
    if not licence:
        return "unlicensed"
    return {
        "id": row["id"],
        "url": info.get("url", ""),
        "license": licence,
        "source_page": info.get("descriptionurl", ""),
        "title": title,
        "creator": common.strip_html((ext.get("Artist") or {}).get("value", "")),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--index", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--sleep", type=float, default=0.5)
    args = parser.parse_args()

    with open(args.index, newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    todo = [row for row in rows if not all(
        row.get(key) for key in ("file_url", "license", "landing_url"))]
    print(f"{len(todo)} of {len(rows)} candidates need provenance recovery")

    session = common.make_session()
    recovered, failed = [], []
    for row in todo:
        if row.get("source") != "wikimedia":
            failed.append((row["id"], "not a Wikimedia Commons candidate"))
            continue
        found = None
        for title in title_candidates(row["filename"], row["label"]):
            params = {
                "action": "query", "titles": f"File:{title}",
                "prop": "imageinfo", "iiprop": "url|extmetadata|size|mime",
                "iiurlwidth": 2048, "format": "json", "formatversion": 2,
            }
            try:
                payload = json.loads(common.fetch_bytes(session, API, params=params))
            except Exception as error:  # network hiccup: skip, keep the row out
                print(f"  ! {row['id']} {title}: {error}")
                break
            common.polite_sleep(args.sleep)
            pages = payload.get("query", {}).get("pages", []) or []
            for page in pages:
                info = (page.get("imageinfo") or [None])[0]
                if not info or page.get("missing"):
                    continue
                ext = info.get("extmetadata") or {}
                licence = common.norm_license(
                    (ext.get("LicenseShortName") or {}).get("value", "")
                    or (ext.get("License") or {}).get("value", "")
                )
                if not licence:
                    failed.append((row["id"], "licence at source does not pass the free-licence gate"))
                    found = "stop"
                    break
                found = {
                    "id": row["id"],
                    "url": info.get("url", ""),
                    "license": licence,
                    "source_page": info.get("descriptionurl", ""),
                    "title": page.get("title", "").replace("File:", ""),
                    "creator": common.strip_html((ext.get("Artist") or {}).get("value", "")),
                }
                break
            if found:
                break
        if found is None:
            # exact title lookups miss files whose title punctuation was
            # sanitised on disk; fall back to a fuzzy, unique search hit.
            fallback = search_match(session, row, args.sleep)
            if fallback == "unlicensed":
                failed.append((row["id"], "licence at source does not pass the free-licence gate"))
            elif fallback:
                found = fallback
        if isinstance(found, dict):
            recovered.append(found)
            print(f"  + {row['id']} <- {found['title']} [{found['license']}]")
        elif found is None:
            failed.append((row["id"], "no matching Commons file"))

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(
            handle, fieldnames=["id", "url", "license", "source_page", "title", "creator"])
        writer.writeheader()
        writer.writerows(recovered)
    print(f"recovered {len(recovered)}, unresolved {len(failed)} -> {out}")
    for row_id, reason in failed:
        print(f"::warning::{row_id}: {reason}")
    return 0 if recovered else 2


if __name__ == "__main__":
    raise SystemExit(main())
