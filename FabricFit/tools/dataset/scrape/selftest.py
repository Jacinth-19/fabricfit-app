#!/usr/bin/env python3
"""Offline self-test for the scrape/prepare toolkit — no network needed.

Covers: license gate, blocklist, Wikimedia/Openverse candidate parsers
(fixtures), the staging dedup/conflict gate, split assignment, and a full
prepare_kaggle.py run on synthetic images.

Run:  python3 tools/dataset/scrape/selftest.py
Exit code 0 = all green.
"""
from __future__ import annotations

import importlib.util
import io
import json
import random
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

FAILURES: list[str] = []


def check(name: str, cond: bool, detail: str = ""):
    status = "ok " if cond else "FAIL"
    print(f"  [{status}] {name}" + (f" — {detail}" if detail and not cond else ""))
    if not cond:
        FAILURES.append(f"{name}: {detail}")


# ---------------------------------------------------------- license gate

def test_licenses():
    print("license gate:")
    accept = {
        "CC BY-SA 4.0": "CC BY-SA 4.0",
        "cc by 4.0": "CC BY 4.0",
        "CC0 1.0": "CC0 1.0",
        "Public domain": "Public domain",
        "public domain": "Public domain",
        "CC BY-SA": "CC BY-SA",
        "cc-by-4.0": "CC BY 4.0",
        "MIT": "MIT",
        "Apache-2.0": "Apache-2.0",
        "BSD-3-Clause": "BSD-3-Clause",
        "BSD 3-Clause": "BSD-3-Clause",
        "Unlicense": "Unlicense",
    }
    for raw, want in accept.items():
        got = common.norm_license(raw)
        check(f"accept {raw!r}", got == want, f"got {got!r} want {want!r}")
    for raw in ("CC BY-NC 4.0", "CC BY-NC-SA 4.0", "CC BY-ND 4.0", "Copyrighted",
                "All rights reserved", "", "Editorial use only", "In copyright",
                "GPL-3.0", "Proprietary"):
        got = common.norm_license(raw)
        check(f"reject {raw!r}", got is None, f"got {got!r}")


def test_blocklist():
    print("blocklist:")
    for url in ("https://cdn.shutterstock.com/st/1.jpg",
                "https://www.gettyimages.com/photos/x",
                "https://i.etsystatic.com/1.jpg",
                "https://pinimg.com/x.jpg"):
        check(f"blocked {url}", common.url_blocked(url))
    for url in ("https://upload.wikimedia.org/x.jpg",
                "https://live.staticflickr.com/1.jpg",
                "https://example.org/kente.jpg"):
        check(f"allowed {url}", not common.url_blocked(url))


# ------------------------------------------------------- parser fixtures

def test_wikimedia_parser():
    print("wikimedia parser:")
    from wikimedia import iter_candidates
    payload = {
        "query": {"pages": [
            {"title": "File:Kente cloth.jpg", "pageid": 1,
             "imageinfo": [{
                 "url": "https://upload.wikimedia.org/wikipedia/commons/kente.jpg",
                 "descriptionurl": "https://commons.wikimedia.org/wiki/File:Kente_cloth.jpg",
                 "width": 1600, "height": 1200, "mime": "image/jpeg",
                 "thumburl": "https://upload.wikimedia.org/thumb/kente.jpg",
                 "extmetadata": {
                     "LicenseShortName": {"value": "CC BY-SA 4.0"},
                     "Artist": {"value": "<a href='x'>A. Weaver</a>"},
                 }}]},
            {"title": "File:NoReuse.png", "pageid": 2,
             "imageinfo": [{
                 "url": "https://upload.wikimedia.org/x.png",
                 "descriptionurl": "https://commons.wikimedia.org/wiki/File:NoReuse.png",
                 "width": 900, "height": 700, "mime": "image/png",
                 "extmetadata": {"LicenseShortName": {"value": "CC BY-NC 4.0"}}}]},
            {"title": "File:Stock.jpg", "pageid": 3,
             "imageinfo": [{
                 "url": "https://upload.wikimedia.org/s.jpg",
                 "descriptionurl": "https://commons.shutterstock-like.org/File:Stock.jpg",
                 "width": 900, "height": 700, "mime": "image/jpeg",
                 "extmetadata": {"LicenseShortName": {"value": "CC0 1.0"}}}]},
            {"title": "File:Thumb.svg", "pageid": 4,
             "imageinfo": [{
                 "url": "https://upload.wikimedia.org/a.svg",
                 "descriptionurl": "https://commons.wikimedia.org/wiki/File:Thumb.svg",
                 "width": 900, "height": 700, "mime": "image/svg+xml",
                 "extmetadata": {"LicenseShortName": {"value": "CC0"}}}]},
        ]}
    }
    results = list(iter_candidates(payload))
    ok_reasons = [r for _, r in results if r is None]
    reasons = {p["title"]: r for p, r in results if r}
    check("one clean candidate", len(ok_reasons) == 1, str(results))
    check("NC rejected", "license rejected" in reasons.get("File:NoReuse.png", ""), str(reasons))
    check("blocked host rejected", "blocked host" in reasons.get("File:Stock.jpg", ""), str(reasons))
    check("svg rejected", "unsupported" in reasons.get("File:Thumb.svg", ""), str(reasons))


def test_openverse_parser():
    print("openverse parser:")
    from openverse import iter_candidates
    payload = {"results": [
        {"id": "a", "url": "https://live.staticflickr.com/1.jpg",
         "foreign_landing_url": "https://flickr.com/x", "license": "by",
         "license_version": "4.0", "title": "kente", "width": 800, "height": 600},
        {"id": "b", "url": "https://example.org/x.jpg",
         "license": "by-nc", "license_version": "4.0", "title": "nc"},
        {"id": "c", "url": "https://images.shutterstock.com/x.jpg",
         "license": "cc0", "title": "stock"},
        {"id": "d", "url": "https://example.org/pdm.jpg",
         "license": "pdm", "title": "pd marked", "width": 640, "height": 480},
    ]}
    results = {r["id"]: reason for r, reason in iter_candidates(payload)}
    check("cc by kept", results["a"] is None, str(results))
    check("pdm kept", results["d"] is None, str(results))
    check("nc rejected", "license rejected" in (results["b"] or ""), str(results))
    check("stock host rejected", results["c"] == "blocked host", str(results))


# -------------------------------------------------------------- staging

def test_staging():
    print("staging gate:")
    tmp = common.SCRAPED / "_selftest_tmp"
    if tmp.exists():
        shutil.rmtree(tmp)
    classes = common.load_classes()
    st = common.Staging("_selftest_tmp", classes, threshold=6)
    img = common.Image.new("RGB", (320, 320))
    px = img.load()
    rng = random.Random(1)
    for y in range(320):
        for x in range(320):
            px[x, y] = (rng.randrange(256), rng.randrange(256), rng.randrange(256))

    r1 = st.add("KENTE", img, title="a", source_key="t:1", license_name="CC0")
    r2 = st.add("KENTE", img, title="a copy", source_key="t:2", license_name="CC0")
    r3 = st.add("ANKARA", img, title="conflict", source_key="t:3", license_name="CC0")
    small, _ = common.validate_image(
        common.normalise_for_disk(common.Image.new("RGB", (100, 100))), min_side=220)
    r4 = st.add("KENTE", small or img, title="tiny", source_key="t:4", license_name="CC0") \
        if small is not None else "too small"
    check("first add ok", r1 == "ok", r1)
    check("dup blocked", r2.startswith("duplicate"), r2)
    check("cross-class conflict blocked", r3.startswith("cross-class conflict"), r3)
    check("undersized blocked", r4 == "too small" or r4.startswith("too small"), r4)
    check("credits written", (st.credits_path).is_file())
    check("stats", st.stats["ok"] == 1 and st.stats["duplicate"] == 1
          and st.stats["conflict"] == 1, str(st.stats))
    # pool survives reload (dedup against disk)
    st2 = common.Staging("_selftest_tmp", classes, threshold=6)
    r5 = st2.add("KENTE", img, title="again", source_key="t:5", license_name="CC0")
    check("dedup survives reload", r5.startswith("duplicate"), r5)
    shutil.rmtree(tmp)


# ------------------------------------------------------- split utilities

def load_prepare_module():
    path = Path(__file__).resolve().parents[1] / "prepare_kaggle.py"
    spec = importlib.util.spec_from_file_location("prepare_kaggle_under_test", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def test_split():
    print("split assignment:")
    pk = load_prepare_module()
    uf = pk.UnionFind()
    uf.union("a", "b")
    uf.union("b", "c")
    check("union-find transitively groups", uf.find("a") == uf.find("c"))
    check("union-find keeps others out", uf.find("z") != uf.find("a"))

    groups = [[{"id": f"g{i}_{j}"} for j in range(3)] for i in range(20)]
    assign = pk.assign_splits(groups, random.Random(13))
    # every group must be in exactly one split
    per_group = {}
    for gid, split in assign.items():
        g = gid.rsplit("_", 1)[0]
        per_group.setdefault(g, set()).add(split)
    check("no group spans splits", all(len(s) == 1 for s in per_group.values()))
    sizes = {"train": 0, "val": 0, "test": 0}
    for gid, split in assign.items():
        sizes[split] += 1
    total = sum(sizes.values())
    check("roughly 70/15/15",
          abs(sizes["train"] / total - 0.70) < 0.15
          and sizes["val"] > 0 and sizes["test"] > 0, str(sizes))


# ------------------------------------------------- prepare_kaggle e2e

def make_noise(path: Path, seed: int, size: int = 320):
    from PIL import Image
    rng = random.Random(seed)
    img = Image.new("RGB", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = (rng.randrange(256), rng.randrange(256), rng.randrange(256))
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="JPEG", quality=92)


def test_prepare_e2e():
    print("prepare_kaggle e2e:")
    pk_path = Path(__file__).resolve().parents[1] / "prepare_kaggle.py"
    with tempfile.TemporaryDirectory(prefix="fabricfit_selftest_") as td:
        td = Path(td)
        raw, scraped, out = td / "raw", td / "scraped", td / "export"

        classes4 = ["KENTE", "ANKARA", "FUGU", "DENIM"]
        # 8 distinct noise images per class -> ~8 groups each
        for ci, cls in enumerate(classes4):
            for i in range(8):
                make_noise(raw / cls / f"{cls}_{i}.jpg", seed=ci * 100 + i)
        # exact duplicate in KENTE (different name) -> must be dropped
        shutil.copy(raw / "KENTE" / "KENTE_0.jpg", raw / "KENTE" / "KENTE_dup.jpg")
        # cross-class conflict: same bytes as ANKARA_0 but inside KENTE -> dropped
        shutil.copy(raw / "ANKARA" / "ANKARA_0.jpg", raw / "KENTE" / "KENTE_steal.jpg")
        # scraped pool with credits
        make_noise(scraped / "wikimedia" / "KENTE" / "wm_extra.jpg", seed=999)
        (scraped / "wikimedia" / "CREDITS.csv").write_text(
            "source,source_key,class,filename,title,creator,license,landing_url,file_url,retrieved\n"
            "wikimedia,commons:1,KENTE,wm_extra.jpg,Extra cloth,A. Weaver,CC BY 4.0,"
            "https://commons.wikimedia.org/wiki/File:Extra.jpg,https://upload.wikimedia.org/x.jpg,"
            "2026-10-10\n",
            encoding="utf-8",
        )

        proc = subprocess.run(
            [sys.executable, str(pk_path),
             "--raw-dir", str(raw), "--scraped-dir", str(scraped),
             "--out", str(out), "--kaggle-id", "selftest/fabricfit", "--force"],
            capture_output=True, text=True,
        )
        check("prepare exits 0", proc.returncode == 0, proc.stderr[-2000:] or proc.stdout[-2000:])
        if proc.returncode != 0:
            return

        # structure
        check("train/val/test exist",
              all((out / s).is_dir() for s in ("train", "val", "test")))
        check("labels.csv exists", (out / "labels.csv").is_file())
        check("CREDITS.csv exists", (out / "CREDITS.csv").is_file())
        check("dataset-metadata.json valid id",
              json.loads((out / "dataset-metadata.json").read_text())["id"] == "selftest/fabricfit")
        card = (out / "DATA_CARD.md").read_text()
        check("data card mentions classes", "KENTE" in card and "GONJA" in card)
        check("data card has honesty banner", "Honesty banner" in card or "honesty" in card.lower())

        import csv as _csv
        with (out / "labels.csv").open(newline="", encoding="utf-8") as fh:
            rows = list(_csv.DictReader(fh))
        on_disk = {str(p.relative_to(out)) for s in ("train", "val", "test")
                   for p in (out / s).rglob("*.jpg")}
        check("labels match files on disk",
              {r["path"] for r in rows} == on_disk,
              f"labels={len(rows)} disk={len(on_disk)}")

        # dup + conflict dropped: found = 8*4 + 1 dup + 1 steal + 1 scraped = 35; kept = 33
        kept = len(rows)
        check("duplicates and conflicts dropped", kept == 33, f"kept={kept}, want 33")

        # scraped credit made it through with license
        wm_rows = [r for r in rows if r["source"] == "wikimedia"]
        check("scraped image included", len(wm_rows) == 1, str(wm_rows))
        with (out / "CREDITS.csv").open(newline="", encoding="utf-8") as fh:
            creds = list(_csv.DictReader(fh))
        lic = {c["filename"]: c["license"] for c in creds}
        check("credit carries CC BY 4.0",
              any("CC BY 4.0" in v for k, v in lic.items() if k.endswith("wm_extra.jpg")
                  or "wm_" in k),
              str(list(lic.items())[:5]))

        # splits: every class with >=3 groups should hit all splits (we have ~8)
        from collections import Counter
        by_cls = Counter((r["label"], r["split"]) for r in rows)
        for cls in classes4:
            check(f"{cls} spread over splits",
                  all(by_cls[(cls, s)] > 0 for s in ("train", "val", "test")),
                  str({s: by_cls[(cls, s)] for s in ("train", "val", "test")}))

        # no image bytes shared across splits (exact-level leak check)
        import hashlib
        digests = {}
        leak = False
        for r in rows:
            d = hashlib.sha256((out / r["path"]).read_bytes()).hexdigest()
            if d in digests and digests[d][0] != r["split"]:
                leak = True
            digests[d] = (r["split"], r["path"])
        check("no identical bytes across splits", not leak)

        # rerun with --force works
        proc2 = subprocess.run(
            [sys.executable, str(pk_path),
             "--raw-dir", str(raw), "--scraped-dir", str(scraped),
             "--out", str(out), "--kaggle-id", "selftest/fabricfit", "--force"],
            capture_output=True, text=True,
        )
        check("rerun --force exits 0", proc2.returncode == 0, proc2.stderr[-1000:])


def main() -> int:
    test_licenses()
    test_blocklist()
    test_wikimedia_parser()
    test_openverse_parser()
    test_staging()
    test_split()
    test_prepare_e2e()
    print()
    if FAILURES:
        print(f"{len(FAILURES)} FAILURE(S):")
        for f in FAILURES:
            print(f"  ! {f}")
        return 1
    print("ALL SELFTESTS PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
