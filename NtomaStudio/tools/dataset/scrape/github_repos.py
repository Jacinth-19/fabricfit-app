#!/usr/bin/env python3
"""Harvest images from allowlisted GitHub repos (see github_allowlist.json).

Why GitHub-only: it is the one image source reachable from build sandboxes,
and the allowlist forces an explicit license + label mapping per repo — no
blanket crawling, no unlicensed repos, no guessing labels.

For each configured repo this script:
  1. verifies the repo's license matches expected_license
     (GitHub API, or the LICENSE file when using --local),
  2. finds matching files (API tree walk, or a local checkout with --local),
  3. downloads via api.github.com — or reads the local checkout directly,
  4. maps filename prefixes to label_schema classes; values NOT in the schema
     (e.g. FAKE_KENTE) become auxiliary classes: staged with provenance but
     never exported by prepare_kaggle.py, keeping separate tasks separate,
  5. pushes every image through the shared staging gate.

Set GITHUB_TOKEN to raise the API rate limit (60 -> 5000/h).

Usage:
  python3 tools/dataset/scrape/github_repos.py [--dry-run] [--ignore-license]
                                               [--repo owner/name] [--per-repo 50]
  python3 tools/dataset/scrape/github_repos.py --repo CSDTs/kente-cloth-authentication \
      --local /tmp/checkout/kente-cloth-authentication    # no API, bulk-safe
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

ALLOWLIST = common.HERE / "github_allowlist.json"
API = "https://api.github.com"
MAX_BLOB = 50 * 1024 * 1024  # skip anything bigger


def headers() -> dict:
    h = {"Accept": "application/vnd.github+json"}
    tok = os.environ.get("GITHUB_TOKEN")
    if tok:
        h["Authorization"] = f"Bearer {tok}"
    return h


def raw_headers() -> dict:
    h = {"Accept": "application/vnd.github.raw+json"}
    tok = os.environ.get("GITHUB_TOKEN")
    if tok:
        h["Authorization"] = f"Bearer {tok}"
    return h


def map_label(filename: str, entry: dict) -> str | None:
    """Return a schema/aux class, or None when the file must be skipped."""
    low = Path(filename).name.lower()
    for pre in entry.get("skip_prefixes", []):
        if low.startswith(pre.lower()):
            return None
    for pre, cls in (entry.get("label_map") or {}).items():
        if low.startswith(pre.lower()):
            return cls
    return None


def local_license_ok(root: Path, expected: str) -> tuple[bool, str]:
    lic_files = sorted(p for p in root.iterdir() if p.name.lower().startswith("license"))
    if not lic_files:
        return False, "no LICENSE file in checkout"
    text = lic_files[0].read_text(errors="ignore").lower()
    token = expected.split("-")[0].lower()  # BSD-3-Clause -> bsd ; MIT -> mit
    phrases = {
        "bsd": ("redistribution and use", "bsd"),
        "mit": ("permission is hereby granted", "mit license"),
        "apache": ("apache license",),
        "unlicense": ("unencumbered", "unlicense"),
    }.get(token, (token, expected.lower()))
    if any(p in text for p in phrases):
        return True, lic_files[0].name
    return False, f"{lic_files[0].name} does not look like {expected!r}"


def local_blobs(root: Path, entry: dict) -> list[dict]:
    exts = tuple(e.lower() for e in entry.get("extensions", [".jpg", ".jpeg", ".png", ".webp"]))
    prefixes = entry.get("path_prefixes", [""])
    out = []
    for p in sorted(root.rglob("*")):
        if not p.is_file() or ".git" in p.parts:
            continue
        rel = p.relative_to(root).as_posix()
        if not rel.lower().endswith(exts):
            continue
        if not any(rel.startswith(pre) for pre in prefixes):
            continue
        if p.stat().st_size > MAX_BLOB:
            continue
        out.append({"path": rel, "size": p.stat().st_size})
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--ignore-license", action="store_true",
                    help="harvest even when the live license doesn't match expected_license")
    ap.add_argument("--repo", type=str, default=None, help="harvest only this owner/name")
    ap.add_argument("--local", type=str, default=None,
                    help="path to an existing local checkout (requires --repo; skips the API)")
    ap.add_argument("--per-repo", type=int, default=1000)
    ap.add_argument("--threshold", type=int, default=6)
    ap.add_argument("--min-side", type=int, default=220)
    args = ap.parse_args()

    if args.local and not args.repo:
        print("ERROR: --local requires --repo to identify the allowlist entry.", file=sys.stderr)
        return 2

    cfg = json.loads(ALLOWLIST.read_text())
    classes = common.load_classes()
    aux = sorted({
        v
        for e in cfg.get("repos", [])
        for v in (e.get("label_map") or {}).values()
        if v not in classes
    })
    all_classes = classes + aux

    session = common.make_session(headers())
    raw_session = common.make_session(raw_headers())
    staging = None if args.dry_run else common.Staging("github", all_classes, threshold=args.threshold)
    if aux:
        common.log(f"auxiliary (out-of-schema) classes enabled: {', '.join(aux)}")

    kept = skipped = errors = 0
    for entry in cfg.get("repos", []):
        repo = entry["repo"]
        if args.repo and repo != args.repo:
            continue
        expected = entry.get("expected_license", "")

        # -- 1. license check ------------------------------------------
        if args.local:
            root = Path(args.local)
            if not root.is_dir():
                common.log(f"! {repo}: --local path {root} does not exist")
                return 2
            ok, detail = local_license_ok(root, expected)
            if not ok:
                msg = f"! {repo}: local license check failed ({detail})"
                if args.ignore_license:
                    common.log(msg + " — continuing because --ignore-license")
                else:
                    common.log(msg + " — SKIPPED")
                    errors += 1
                    continue
            common.log(f"{repo}: license OK via {detail} (expected {expected})")
        else:
            try:
                meta = json.loads(common.fetch_bytes(session, f"{API}/repos/{repo}"))
            except Exception as e:
                common.log(f"! {repo}: repo lookup failed ({e})")
                errors += 1
                continue
            live = (meta.get("license") or {}).get("spdx_id") or "NONE"
            if live != expected:
                msg = f"! {repo}: license changed ({live} != declared {expected})"
                if args.ignore_license:
                    common.log(msg + " — continuing because --ignore-license")
                else:
                    common.log(msg + " — SKIPPED (re-run with --ignore-license to override)")
                    errors += 1
                    continue

        # -- 2. file listing --------------------------------------------
        if args.local:
            blobs = local_blobs(root, entry)
        else:
            try:
                tree = json.loads(common.fetch_bytes(session, f"{API}/repos/{repo}/git/trees/HEAD?recursive=1"))
            except Exception as e:
                common.log(f"! {repo}: tree fetch failed ({e})")
                errors += 1
                continue
            exts = tuple(e.lower() for e in entry.get("extensions", [".jpg", ".jpeg", ".png", ".webp"]))
            prefixes = entry.get("path_prefixes", [""])
            blobs = [
                t for t in tree.get("tree", [])
                if t.get("type") == "blob"
                and t.get("path", "").lower().endswith(exts)
                and any(t["path"].startswith(p) for p in prefixes)
                and t.get("size", 0) <= MAX_BLOB
            ]

        skip_pres = [s.lower() for s in entry.get("skip_prefixes", [])]
        repo_kept = 0
        for blob in blobs:
            if repo_kept >= args.per_repo:
                break
            cls = map_label(blob["path"], entry)
            if cls is None:
                skipped += 1
                if Path(blob["path"]).name.lower().startswith(tuple(skip_pres)):
                    common.log(f"  - skip {blob['path']} ({entry.get('skip_note', 'skip list')})")
                continue
            if cls not in all_classes:
                common.log(f"  ! {blob['path']}: label {cls} not in schema/aux")
                skipped += 1
                continue
            if args.dry_run:
                common.log(f"  [dry] {repo}:{blob['path']} -> {cls}")
                kept += 1
                continue

            # -- 3. bytes ------------------------------------------------
            try:
                if args.local:
                    data = (root / blob["path"]).read_bytes()
                else:
                    data = common.fetch_bytes(
                        raw_session,
                        f"{API}/repos/{repo}/contents/{blob['path']}",
                        params={"ref": "HEAD"},
                    )
            except Exception as e:
                common.log(f"  ! download failed {repo}:{blob['path']} ({e})")
                errors += 1
                continue

            img, why = common.validate_image(data, min_side=args.min_side)
            if img is None:
                skipped += 1
                common.log(f"  - {blob['path']}: {why}")
                continue

            # -- 4/5. staging gate --------------------------------------
            outcome = staging.add(
                cls, img,
                title=Path(blob["path"]).name,
                creator=repo,
                license_name=expected,
                landing_url=f"https://github.com/{repo}/blob/HEAD/{blob['path']}",
                file_url=f"https://github.com/{repo}",
                source_key=f"github:{repo}@{blob['path']}",
                filename_hint=f"{Path(blob['path']).stem}",
                min_side=args.min_side,
            )
            if outcome == "ok":
                kept += 1
                repo_kept += 1
                common.log(f"  + {cls}/{blob['path']} [{expected}]")
            else:
                skipped += 1
                common.log(f"  - {blob['path']}: {outcome}")
        common.log(f"{repo}: done ({repo_kept} kept from {len(blobs)} matching files)")

    if staging:
        st = staging.stats
        common.log(
            f"\ngithub done: kept={st['ok']} duplicates={st['duplicate']} "
            f"conflicts={st['conflict']} invalid={st['invalid']} skipped={skipped} errors={errors}"
        )
        common.log(f"staging: {staging.root}")
    else:
        common.log(f"\ndry run: {kept} files would be fetched; nothing written.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
