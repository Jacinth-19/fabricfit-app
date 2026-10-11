#!/usr/bin/env python3
"""Collect unlabelled-review candidates, NOT a training dataset.

Runs on an internet-connected runner. Commons per-image rights metadata and
museum public-domain flags are retained verbatim. Search queries are only label
suggestions: no candidate enters the family/pattern training directories.
"""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import io
import json
import os
from pathlib import Path
import re
import time
from urllib.parse import urlsplit

import requests
from PIL import Image, ImageOps

HERE = Path(__file__).resolve().parent
COMMONS = "https://commons.wikimedia.org/w/api.php"
CMA = "https://openaccess-api.clevelandart.org/api/artworks/"
MET = "https://collectionapi.metmuseum.org/public/collection/v1"
USER_AGENT = "NtomaStudioDatasetReview/1.0 (https://github.com/Jacinth-19/ntoma-studio)"
ALLOWED_RIGHTS = {"cc0", "pdm", "cc-by-4.0", "cc-by-3.0", "cc-by-2.0", "cc-by-2.5",
                  "cc-by-sa-4.0", "cc-by-sa-3.0", "cc-by-sa-2.0", "cc-by-sa-2.5"}
MAX_IMAGE = 20 * 1024 * 1024


def plain(value):
    import html
    return html.unescape(re.sub(r"<[^>]+>", " ", value or "")).strip()


def rights_id(url):
    # Exact Creative Commons legal URLs, not a loose 'public domain' substring.
    parsed = urlsplit(url)
    if parsed.scheme not in {"http", "https"} or parsed.hostname not in {"creativecommons.org", "www.creativecommons.org"}:
        return None
    parts = parsed.path.strip("/").split("/")
    if parts == ["publicdomain", "zero", "1.0"]:
        return "cc0"
    if parts == ["publicdomain", "mark", "1.0"]:
        return "pdm"
    if len(parts) == 3 and parts[0] == "licenses":
        result = "cc-" + "-".join(parts[1:])
        return result if result in ALLOWED_RIGHTS else None
    return None


class Collector:
    def __init__(self, root, per_query=12):
        self.root = root
        root.mkdir(parents=True, exist_ok=False)
        (root / "images").mkdir()
        (root / "evidence").mkdir()
        self.session = requests.Session()
        self.session.headers["User-Agent"] = USER_AGENT
        self.per_query = per_query
        self.rows = {}
        self.hashes = {}
        self.events = []
        self.counts = Counter()
        self.request_count = 0

    def get(self, url, **kwargs):
        self.request_count += 1
        time.sleep(1.0)
        # Limit retries and duration: unavailable sources cannot hang a whole run.
        for attempt in range(3):
            try:
                response = self.session.get(url, timeout=(10, 35), **kwargs)
                response.raise_for_status()
                return response
            except requests.RequestException as error:
                response = getattr(error, "response", None)
                status = response.status_code if response is not None else None
                if attempt == 2 or (status and 400 <= status < 500 and status != 429):
                    raise
                retry_after = response.headers.get("Retry-After", "") if response is not None else ""
                delay = min(int(retry_after), 120) if retry_after.isdigit() else (15 * (attempt + 1) if status == 429 else 2 ** attempt)
                time.sleep(delay)

    def record_error(self, source, query, error):
        # Don't persist arbitrary exception strings, signed URLs or request headers.
        status = getattr(getattr(error, "response", None), "status_code", None)
        self.events.append(dict(source=source, query=query, error_type=type(error).__name__, http_status=status))

    def add(self, task, label, query, item):
        key = item["key"]
        match = dict(task=task, suggested_label=label, query=query)
        if key in self.rows:
            if match not in self.rows[key]["matches"]:
                self.rows[key]["matches"].append(match)
            return
        lic = rights_id(item.get("license_url", ""))
        if not lic or not item.get("landing_page") or not item.get("creator"):
            self.counts["rejected_rights_or_attribution"] += 1
            return
        # Only known first-party media hosts, no arbitrary URLs from catalogue text.
        image_url = item["image_url"]
        parsed = urlsplit(image_url)
        allowed = {"upload.wikimedia.org", "thumb.wikimedia.org", "openaccess-cdn.clevelandart.org", "images.metmuseum.org"}
        if parsed.scheme != "https" or parsed.hostname not in allowed:
            self.counts["rejected_image_host"] += 1
            return
        try:
            with self.get(image_url, stream=True) as response:
                data = bytearray()
                for block in response.iter_content(65536):
                    data.extend(block)
                    if len(data) > MAX_IMAGE:
                        raise ValueError("image_too_large")
            digest = hashlib.sha256(data).hexdigest()
            with Image.open(io.BytesIO(data)) as original:
                original.load()
                if min(original.size) < 224:
                    self.counts["rejected_small_image"] += 1
                    return
                image = ImageOps.exif_transpose(original).convert("RGB")
                # Save a separate resized review copy, preserve original byte hash.
                image.thumbnail((1024, 1024))
                reduced = image.convert("L").resize((9, 8))
                pixels = list(reduced.get_flattened_data())
                bits = [pixels[y*9+x] > pixels[y*9+x+1] for y in range(8) for x in range(8)]
                dhash = f"{sum(int(b) << i for i,b in enumerate(bits)):016x}"
                uid = hashlib.sha256(key.encode()).hexdigest()[:24]
                destination = self.root / "images" / f"{uid}.jpg"
                duplicate = self.hashes.get(digest)
                if duplicate:
                    self.counts["exact_duplicates"] += 1
                    destination = self.root / self.rows[duplicate]["path"]
                else:
                    image.save(destination, quality=92)
                    self.hashes[digest] = key
            evidence = self.root / "evidence" / f"{uid}.json"
            evidence.write_text(json.dumps(item, ensure_ascii=False, indent=2) + "\n")
            self.rows[key] = dict(source_id=key, title=item["title"], source=item["source"],
                                 license=lic, license_url=item["license_url"], creator=item["creator"],
                                 landing_page=item["landing_page"], image_url=image_url,
                                 description=item.get("description", ""),
                                 path=str(destination.relative_to(self.root)),
                                 evidence=str(evidence.relative_to(self.root)), original_sha256=digest,
                                 review_sha256=hashlib.sha256(destination.read_bytes()).hexdigest(), dhash=dhash,
                                 duplicate_of=duplicate, matches=[match], reviewed_labels=[],
                                 status="REVIEW_REQUIRED_NOT_TRAINING_DATA")
            self.counts["accepted_records"] += 1
        except (requests.RequestException, OSError, ValueError) as error:
            self.record_error(item["source"], query, error)

    def commons(self, query):
        payload = self.get(COMMONS, params=dict(action="query", generator="search", gsrsearch=query,
            gsrnamespace=6, gsrlimit=self.per_query, prop="imageinfo", iiprop="url|extmetadata|size|mime",
            iiurlwidth=1024, format="json", formatversion=2)).json()
        if payload.get("error"):
            raise ValueError("commons_api_error")
        for page in payload.get("query", {}).get("pages", []):
            info = (page.get("imageinfo") or [{}])[0]
            ext = info.get("extmetadata", {})
            field = lambda name: plain(ext.get(name, {}).get("value", ""))
            if info.get("mime") not in {"image/jpeg", "image/png", "image/webp"}:
                continue
            yield dict(key=f"commons:{page['pageid']}", source="Wikimedia Commons",
                       title=page["title"], creator=field("Artist"), description=field("ImageDescription"),
                       landing_page=info.get("descriptionurl"), image_url=info.get("thumburl") or info.get("url"),
                       license_url=field("LicenseUrl"), upstream_metadata=info)

    def cma(self, query):
        payload = self.get(CMA, params={"q": query, "has_image": 1, "cc0": 1, "limit": self.per_query}).json()
        for row in payload.get("data", []):
            if row.get("share_license_status") != "CC0":
                continue
            image = row.get("images", {}).get("web", {}).get("url")
            if not image:
                continue
            creators = "; ".join(x.get("description", "") for x in row.get("creators", []))
            yield dict(key=f"cma:{row['id']}", source="Cleveland Museum of Art", title=row["title"],
                       creator=creators or "Unrecorded maker; image courtesy Cleveland Museum of Art",
                       description="; ".join(str(row.get(x, "")) for x in ("culture", "technique", "description")),
                       landing_page=row.get("url"), image_url=image,
                       license_url="https://creativecommons.org/publicdomain/zero/1.0/", upstream_metadata=row)

    def met(self, query):
        payload = self.get(MET + "/search", params={"q": query, "hasImages": "true"}).json()
        for object_id in (payload.get("objectIDs") or [])[:self.per_query]:
            row = self.get(f"{MET}/objects/{object_id}").json()
            if row.get("isPublicDomain") is not True or not row.get("primaryImageSmall"):
                continue
            yield dict(key=f"met:{object_id}", source="Metropolitan Museum of Art", title=row["title"],
                       creator=row.get("artistDisplayName") or "Unrecorded maker; image courtesy The Metropolitan Museum of Art",
                       description="; ".join(str(row.get(x, "")) for x in ("culture", "medium", "objectName", "country")),
                       landing_page=row.get("objectURL"), image_url=row["primaryImageSmall"],
                       license_url="https://creativecommons.org/publicdomain/zero/1.0/", upstream_metadata=row)

    def save(self, queries, started):
        with (self.root / "manifest.jsonl").open("w") as out:
            for row in self.rows.values():
                out.write(json.dumps(row, ensure_ascii=False) + "\n")
        coverage = {task: {label: len({r["path"] for r in self.rows.values()
                     if any(m["task"] == task and m["suggested_label"] == label for m in r["matches"])})
                    for label in labels} for task, labels in queries.items()}
        # Near-duplicates are only flagged, never blindly removed across classes.
        unique = {r["path"]: r for r in self.rows.values()}
        near = []
        rows = list(unique.values())
        for i, left in enumerate(rows):
            for right in rows[i+1:]:
                distance = (int(left["dhash"], 16) ^ int(right["dhash"], 16)).bit_count()
                if distance <= 6:
                    near.append([left["source_id"], right["source_id"], distance])
        summary = dict(started_at=started, finished_at=datetime.now(timezone.utc).isoformat(),
                       records=len(self.rows), unique_images=len(unique), coverage=coverage,
                       counts=dict(self.counts), request_count=self.request_count, errors=self.events,
                       near_duplicate_pairs=near, training_approved_images=0,
                       warning="Search hits are not ground-truth labels. Review rights, cloth region, family, pattern and design groups first.")
        (self.root / "report.json").write_text(json.dumps(summary, indent=2) + "\n")
        lines = ["# Public fabric-image collection", "", f"Unique candidate images: **{len(unique)}**. Training-approved: **0**.",
                 "", summary["warning"], "", "| Task | Suggested label | Candidate search matches |", "|---|---|---:|"]
        for task, labels in coverage.items():
            lines += [f"| {task} | {label} | {count} |" for label, count in labels.items()]
        lines += ["", "Counts overlap across queries/labels; do not add the rows together.",
                  f"Source/download errors: {len(self.events)}. Near-duplicate pairs to review: {len(near)}."]
        text = "\n".join(lines) + "\n"
        (self.root / "summary.md").write_text(text)
        if os.environ.get("GITHUB_STEP_SUMMARY"):
            with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as out:
                out.write(text)
        print(text)
        return summary


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out", type=Path, default=Path(".datasets/public-review"))
    ap.add_argument("--per-query", type=int, default=10)
    ap.add_argument("--queries-per-label", type=int, default=2)
    ap.add_argument("--sources", default="commons,cma")
    args = ap.parse_args()
    if not 1 <= args.per_query <= 50 or not 1 <= args.queries_per_label <= 3:
        ap.error("Use 1–50 hits per query and 1–3 queries per label")
    sources = args.sources.split(",")
    if set(sources) - {"commons", "cma", "met"}:
        ap.error("Unknown source")
    queries = json.loads((HERE / "collection_queries.json").read_text())
    collector = Collector(args.out, args.per_query)
    started = datetime.now(timezone.utc).isoformat()
    try:
        for task, labels in queries.items():
            for label, terms in labels.items():
                for query in terms[:args.queries_per_label]:
                    for source in sources:
                        try:
                            for item in getattr(collector, source)(query):
                                collector.add(task, label, query, item)
                        except (requests.RequestException, ValueError, KeyError) as error:
                            collector.record_error(source, query, error)
                print(f"Collected suggestions for {task}/{label}; records={len(collector.rows)}", flush=True)
                collector.save(queries, started)
    finally:
        summary = collector.save(queries, started)
    # Zero records is a failed acquisition, not a silent success.
    return 0 if summary["unique_images"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
