#!/usr/bin/env python3
"""Shared helpers for the Ntoma Studio fabric scrapers (tools/dataset/scrape).

Design rules (deliberately conservative — the app is a commercial product):
  * license gate: only CC0 / Public domain / CC BY / CC BY-SA pass.
    NC, ND and "all rights reserved" are rejected outright.
  * stock-agency, marketplace and e-commerce hosts are blocklisted so
    watermarked previews never enter the pool (see the repo's honesty rules
    about watermarked stock in docs/capability_answers.md).
  * every kept image is decoded, EXIF-normalised, size-checked and
    dHash-deduplicated against the whole staging pool (cross-class collisions
    are dropped as label conflicts).
  * every kept image gets a provenance row in <source>/CREDITS.csv so the
    eventual Kaggle export can ship a data card with per-image licensing.

Scraped images land in tools/dataset/scraped/<SOURCE>/<CLASS>/ (gitignored).
Nothing here writes into raw/ — that pool is reserved for human-vetted
app contributions ingested via ingest.py.
"""
from __future__ import annotations

import csv
import datetime as _dt
import io
import json
import re
import time
from pathlib import Path
from urllib.parse import urlparse

import requests
from PIL import Image, ImageOps

HERE = Path(__file__).resolve().parent          # tools/dataset/scrape
DATASET_ROOT = HERE.parent                      # tools/dataset
SCRAPED = DATASET_ROOT / "scraped"
SCHEMA = DATASET_ROOT / "label_schema.json"

DEFAULT_UA = (
    "NtomaStudio-FabricDatasetBot/1.0 "
    "(+https://github.com/Jacinth-19/ntoma-studio; academic dataset collection)"
)

CREDIT_FIELDS = [
    "source", "source_key", "class", "filename", "title",
    "creator", "license", "landing_url", "file_url", "retrieved",
]

# ---------------------------------------------------------------- licenses

_LICENSE_REJECT = re.compile(
    r"\bnc\b|\bnd\b|non[\s-]commercial|no[\s-]derivatives|"
    r"all rights reserved|in copyright|copyrighted|"
    r"rights reserved|editorial use only",
    re.I,
)


def strip_html(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", value or "")).strip()


def norm_license(raw: str) -> str | None:
    """Return a canonical license name if it allows commercial + derivative use.

    Anything ambiguous returns None (fail closed). Permissive software-style
    licenses (MIT/Apache/BSD/Unlicense) are accepted too — they are common on
    GitHub repos that bundle research photos, and they permit commercial use.
    """
    s = strip_html(raw or "")
    if not s:
        return None
    low = s.lower()
    if _LICENSE_REJECT.search(low):
        return None
    if re.search(r"\bcc0\b", low):
        return "CC0 1.0"
    if re.search(r"public\s*domain|\bpdm\b|^\s*pd\b", low):
        return "Public domain"
    m = re.search(r"cc[\s-]by([\s-]?sa)?([\s-]?(\d+(?:\.\d+)?))?", low)
    if m:
        base = "CC BY-SA" if m.group(1) else "CC BY"
        ver = m.group(3) or ""
        return f"{base} {ver}".strip()
    # permissive code licenses (GitHub repos bundling data/photos)
    for name, pat in (
        ("MIT", r"\bmit\b"),
        ("Apache-2.0", r"apache[\s-]?2(\.0)?"),
        ("BSD-3-Clause", r"bsd[\s-]?3"),
        ("BSD-2-Clause", r"bsd[\s-]?2"),
        ("BSD", r"\bbsd\b"),
        ("Unlicense", r"\bunlicense\b|\bpublic domain dedication\b"),
    ):
        if re.search(pat, low):
            return name
    return None


# ------------------------------------------------------------- blocklist

BLOCKED_HOST_PARTS = (
    # stock agencies (watermarked previews)
    "shutterstock", "alamy", "gettyimages", "istockphoto", "imago-images",
    "agefotostock", "stock.adobe", "adobestock", "dreamstime", "123rf",
    "depositphotos", "freepik", "vecteezy", "canstockphoto", "bigstockphoto",
    "pond5", "featurepics", "mostphotos", "fotolia",
    # marketplaces / pinboards the repo already banned for licensing safety
    "pinterest.", "pinimg", "etsy.", "etsystatic", "ebay.", "amazon.",
    "aliexpress.", "temu.", "walmart.", "wish.com",
)


def url_blocked(url: str) -> bool:
    try:
        host = urlparse(url or "").netloc.lower()
    except Exception:
        return True
    if not host:
        return True
    return any(part in host for part in BLOCKED_HOST_PARTS)


# ------------------------------------------------------------------ dHash

def dhash(img: Image.Image) -> int:
    """64-bit difference hash — identical to ingest.py so pools interoperate."""
    g = img.convert("L").resize((9, 8))
    px = g.tobytes()
    h = 0
    for y in range(8):
        for x in range(8):
            h = (h << 1) | (1 if px[y * 9 + x] > px[y * 9 + x + 1] else 0)
    return h


def hamming(a: int, b: int) -> int:
    return bin(a ^ b).count("1")


# ------------------------------------------------------------ image input

def validate_image(data: bytes, min_side: int = 220) -> tuple[Image.Image | None, str]:
    """Decode bytes, EXIF-normalise, enforce a minimum side. Returns (img|None, why)."""
    try:
        img = Image.open(io.BytesIO(data))
        img.load()
    except Exception as e:  # corrupt / not an image
        return None, f"cannot decode ({e.__class__.__name__})"
    img = ImageOps.exif_transpose(img)
    w, h = img.size
    if min(w, h) < min_side:
        return None, f"too small ({w}x{h} < {min_side})"
    if img.mode not in ("RGB", "L"):
        img = img.convert("RGB")
    elif img.mode == "L":
        img = img.convert("RGB")
    return img, "ok"


def normalise_for_disk(img: Image.Image, max_side: int = 2048) -> bytes:
    """Cap the long edge and encode as JPEG q92 for the staging pool."""
    w, h = img.size
    if max(w, h) > max_side:
        scale = max_side / max(w, h)
        img = img.resize((max(1, int(w * scale)), max(1, int(h * scale))), Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=92, optimize=True)
    return buf.getvalue()


def sanitize_name(name: str, limit: int = 80) -> str:
    name = re.sub(r"[^\w.\-]+", "_", (name or "image").strip(), flags=re.UNICODE)
    name = re.sub(r"_+", "_", name).strip("._")
    return (name[:limit] or "image")


# ------------------------------------------------------------------ HTTP

def make_session(extra_headers: dict | None = None) -> requests.Session:
    import os
    s = requests.Session()
    # CA bundle: prefer explicit env, then the OS bundle (some sandboxes ship a
    # certifi bundle missing the local root), then certifi.
    bundle = os.environ.get("REQUESTS_CA_BUNDLE")
    if not bundle:
        try:
            import certifi
            candidates = [
                "/etc/ssl/certs/ca-certificates.crt",      # Debian/Ubuntu
                "/etc/pki/tls/certs/ca-bundle.crt",        # RHEL/Fedora
                "/etc/ssl/cert.pem",                       # BSD/macOS-ish
                certifi.where(),
            ]
        except ImportError:
            candidates = [
                "/etc/ssl/certs/ca-certificates.crt",
                "/etc/pki/tls/certs/ca-bundle.crt",
                "/etc/ssl/cert.pem",
            ]
        bundle = next((c for c in candidates if Path(c).is_file()), "")
    if bundle:
        s.verify = bundle
    ua = DEFAULT_UA
    contact = os.environ.get("NTOMA_SCRAPER_CONTACT")
    if contact:
        ua = f"{ua} contact:{contact}"
    s.headers.update({"User-Agent": ua, "Accept-Encoding": "gzip"})
    if extra_headers:
        s.headers.update(extra_headers)
    return s


def fetch_bytes(
    session: requests.Session,
    url: str,
    *,
    params: dict | None = None,
    timeout: int = 45,
    retries: int = 3,
    backoff: float = 1.5,
) -> bytes:
    """GET with retry/backoff on 429/5xx. Raises the last HTTP error."""
    last: Exception | None = None
    for attempt in range(retries):
        try:
            r = session.get(url, params=params, timeout=timeout)
            if r.status_code == 429 or r.status_code >= 500:
                wait = backoff * (2 ** attempt)
                time.sleep(wait)
                last = requests.HTTPError(f"{r.status_code} for {url}")
                continue
            r.raise_for_status()
            return r.content
        except requests.RequestException as e:
            last = e
            time.sleep(backoff * (2 ** attempt))
    raise last if last else RuntimeError(f"fetch failed: {url}")


def polite_sleep(seconds: float) -> None:
    if seconds > 0:
        time.sleep(seconds)


# --------------------------------------------------------------- staging

class Staging:
    """A scraper's output pool: scraped/<source>/<CLASS>/ + CREDITS.csv.

    add() is the single gate: it dedupes (cross-class conflicts included),
    normalises, writes the JPEG and records provenance. Returns
    "ok" | "duplicate" | "conflict" | a validation reason.
    """

    def __init__(self, source: str, classes: list[str], threshold: int = 6):
        self.source = source
        self.classes = classes
        self.threshold = threshold
        self.root = SCRAPED / source
        self.root.mkdir(parents=True, exist_ok=True)
        self.credits_path = self.root / "CREDITS.csv"
        # existing hashes -> (class, filename)
        self._pool: dict[int, tuple[str, str]] = {}
        for cls in classes:
            d = self.root / cls
            if not d.is_dir():
                continue
            for f in d.iterdir():
                if f.suffix.lower() not in (".jpg", ".jpeg", ".png", ".webp"):
                    continue
                try:
                    img = Image.open(f)
                    img.load()
                    self._pool[dhash(img)] = (cls, f.name)
                except Exception:
                    pass
        self._stats = {"ok": 0, "duplicate": 0, "conflict": 0, "invalid": 0}

    # -- stats -------------------------------------------------------
    @property
    def stats(self) -> dict:
        return dict(self._stats)

    # -- the gate ----------------------------------------------------
    def add(
        self,
        class_: str,
        img: Image.Image,
        *,
        title: str = "",
        creator: str = "",
        license_name: str = "",
        landing_url: str = "",
        file_url: str = "",
        source_key: str = "",
        filename_hint: str = "",
        min_side: int = 220,
    ) -> str:
        if class_ not in self.classes:
            self._stats["invalid"] += 1
            return "unknown class"
        w, h = img.size
        if min(w, h) < min_side:
            self._stats["invalid"] += 1
            return f"too small ({w}x{h})"
        h64 = dhash(img)
        for other_hash, (other_cls, other_name) in self._pool.items():
            if hamming(h64, other_hash) <= self.threshold:
                if other_cls == class_:
                    self._stats["duplicate"] += 1
                    return f"duplicate of {other_cls}/{other_name}"
                self._stats["conflict"] += 1
                return f"cross-class conflict with {other_cls}/{other_name}"

        cls_dir = self.root / class_
        cls_dir.mkdir(parents=True, exist_ok=True)
        stem = sanitize_name(filename_hint or Path(source_key).stem or title or class_)
        name = f"{stem}.jpg"
        n = 2
        while (cls_dir / name).exists():
            name = f"{stem}_{n}.jpg"
            n += 1
        (cls_dir / name).write_bytes(normalise_for_disk(img))

        row = {
            "source": self.source,
            "source_key": source_key,
            "class": class_,
            "filename": name,
            "title": title or name,
            "creator": creator,
            "license": license_name,
            "landing_url": landing_url,
            "file_url": file_url,
            "retrieved": _dt.date.today().isoformat(),
        }
        self._append_credit(row)
        self._pool[h64] = (class_, name)
        self._stats["ok"] += 1
        return "ok"

    def _append_credit(self, row: dict) -> None:
        new_file = not self.credits_path.exists()
        with self.credits_path.open("a", newline="", encoding="utf-8") as fh:
            w = csv.DictWriter(fh, fieldnames=CREDIT_FIELDS)
            if new_file:
                w.writeheader()
            w.writerow(row)


def load_classes() -> list[str]:
    return json.loads(SCHEMA.read_text())["classes"]


def log(msg: str) -> None:
    print(msg, flush=True)
