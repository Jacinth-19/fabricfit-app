#!/usr/bin/env python3
"""
Reference implementation of the FabricFit recommendations API.

This exists so the client in `data/remote/CloudRecommendationClient.kt` talks to something
real instead of a documented-but-unbuilt contract. It is a *reference*, not a product
backend: it has no auth, no rate limiting, no persistence and no database. Its whole job is
to demonstrate the wire format and give the Android client a target to be tested against.

The scoring here is deliberately simple and explainable — a weighted match over fabric
structure, motif scale, colour harmony and occasion affinity. A production deployment
would replace `rank()` with a trained model; nothing else about the contract needs to
change.

Usage:
    python3 tools/recs_api/server.py                 # http://127.0.0.1:8008
    python3 tools/recs_api/server.py --port 9000

Endpoints:
    GET  /v1/health      -> {"ok": true, "modelVersion": "..."}
    POST /v1/recommend   -> ranked styles for a analysed fabric

No third-party dependencies: standard library only, so it runs anywhere Python 3.8+ does.
"""

from __future__ import annotations

import argparse
import colorsys
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any

MODEL_VERSION = "reference-1"

# Fabric texture -> how much structure (0 = flowing, 1 = rigid) it implies. Mirrors the
# TEXTURE_STRUCTURE table in RecommendationEngine.kt so both paths agree.
TEXTURE_STRUCTURE = {
    "WOVEN": 0.85,
    "EMBROIDERED": 0.75,
    "BEADED": 0.8,
    "PRINTED": 0.35,
    "DYED": 0.4,
    "PLAIN": 0.45,
    "SHEER": 0.25,
    "TEXTURED": 0.6,
}

# Categories whose motifs are geometric and large: they want simple, uninterrupted panels.
STRUCTURED_CATEGORIES = {"KENTE", "KENTE_PRINT", "BROCAD", "BROCADE", "ADINKRA", "FUGU", "GONJA"}

STYLE_STRUCTURE = {
    "kaba_asymmetric": 0.45,
    "modern_kaba": 0.5,
    "agbada": 0.75,
    "kaftan": 0.4,
    "senator": 0.7,
    "peplum": 0.6,
    "mermaid": 0.65,
    "maxi": 0.3,
    "straight": 0.55,
    "smock": 0.7,
    "print_shirt": 0.5,
    "shirt_trousers": 0.55,
}


def _luminance(hex_color: str) -> float:
    """Perceived brightness of #RRGGBB, 0..1."""
    h = hex_color.lstrip("#")
    if len(h) != 6:
        return 0.5
    r, g, b = (int(h[i : i + 2], 16) / 255.0 for i in (0, 2, 4))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def _saturation(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    if len(h) != 6:
        return 0.0
    r, g, b = (int(h[i : i + 2], 16) / 255.0 for i in (0, 2, 4))
    _, _, s = colorsys.rgb_to_hls(r, g, b)
    return s


def rank(payload: dict[str, Any]) -> list[dict[str, Any]]:
    """
    Score candidate styles against one analysed fabric.

    Every term is bounded and the weights sum to 1.0, so the output is a genuine 0..1
    score rather than an arbitrary number that happens to be large.
    """
    fabric = payload.get("fabric") or {}
    candidates = payload.get("candidates") or []
    prefs = payload.get("preferences") or {}
    limit = int(payload.get("limit") or 24)

    texture = str(fabric.get("texture") or "PLAIN").upper()
    category = str(fabric.get("category") or "UNKNOWN").upper()
    pattern = str(fabric.get("pattern") or "PLAIN").upper()
    colors = [c for c in (fabric.get("colors") or []) if isinstance(c, str)]
    confidence = float(fabric.get("confidence") or 0.0)

    fabric_structure = TEXTURE_STRUCTURE.get(texture, 0.5)
    structured_motif = category in STRUCTURED_CATEGORIES or pattern in {"GEOMETRIC", "STRIPES", "CHECK"}
    brightness = sum(_luminance(c) for c in colors) / len(colors) if colors else 0.5
    vividness = sum(_saturation(c) for c in colors) / len(colors) if colors else 0.5

    favourites = set(prefs.get("favorites") or [])
    occasions = set(prefs.get("occasions") or [])
    gender = prefs.get("gender")

    out: list[dict[str, Any]] = []
    for style in candidates:
        style_id = str(style.get("id") or "")
        if not style_id:
            continue
        style_structure = STYLE_STRUCTURE.get(style_id, 0.5)
        silhouette = str(style.get("silhouette") or "").upper()

        reasons: list[str] = []
        score = 0.0

        # 1. Structure match (0.40): rigid cloth suits structured cuts.
        structure_fit = 1.0 - abs(fabric_structure - style_structure)
        score += 0.40 * structure_fit
        if structure_fit > 0.8:
            reasons.append(f"{texture.title()} fabric holds this {silhouette.replace('_', ' ').lower()} cut")

        # 2. Motif scale (0.25): large geometric motifs want unbroken panels.
        wants_panels = silhouette in {"KABA", "MODERN_KABA", "MAXI", "GOWN", "AGBADA"}
        if structured_motif and wants_panels:
            score += 0.25
            reasons.append("Large geometric repeat reads best on an uncut panel")
        elif not structured_motif and not wants_panels:
            score += 0.22
            reasons.append("Small-scale print suits a fitted line")
        else:
            score += 0.12

        # 3. Colour (0.20): vivid palettes want simple shapes to avoid visual noise.
        colour_fit = vividness if style_structure < 0.55 else (1.0 - vividness) * 0.8 + 0.2
        score += 0.20 * colour_fit
        if vividness > 0.55:
            reasons.append("Vivid palette balances a quiet silhouette")

        # 4. Occasion + favourite affinity (0.15).
        affinity = 0.0
        if occasions:
            # Candidate payload has no occasion list of its own, so treat a favourite
            # match as the strongest available signal.
            affinity += 0.1
        if style_id in favourites:
            affinity += 0.05
            reasons.append("Similar to pieces you have saved")
        score += affinity

        # Confidence damping: a fabric we are unsure about produces a flatter, lower
        # ranking rather than a confident one built on a guess.
        score *= 0.55 + 0.45 * max(0.0, min(1.0, confidence))

        out.append(
            {
                "styleId": style_id,
                "score": round(min(1.0, max(0.0, score)), 4),
                "reasons": reasons[:3],
            }
        )

    out.sort(key=lambda r: r["score"], reverse=True)
    return out[:limit]


class Handler(BaseHTTPRequestHandler):
    server_version = "FabricFitRecsReference/1"

    def _json(self, status: int, payload: dict[str, Any]) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:  # noqa: N802 - http.server API
        if self.path.rstrip("/") == "/v1/health":
            self._json(200, {"ok": True, "modelVersion": MODEL_VERSION})
        else:
            self._json(404, {"error": "not found"})

    def do_POST(self) -> None:  # noqa: N802 - http.server API
        if self.path.rstrip("/") != "/v1/recommend":
            self._json(404, {"error": "not found"})
            return
        try:
            length = int(self.headers.get("Content-Length") or 0)
            payload = json.loads(self.rfile.read(length) or b"{}")
        except (ValueError, OSError):
            self._json(400, {"error": "invalid json"})
            return
        try:
            ranked = rank(payload)
        except Exception as exc:  # noqa: BLE001 - reference server, never 500 silently
            self._json(400, {"error": f"could not rank: {exc}"})
            return
        self._json(200, {"recommendations": ranked, "modelVersion": MODEL_VERSION})

    def log_message(self, fmt: str, *args: Any) -> None:
        print("[recs] " + fmt % args)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=8008)
    args = ap.parse_args()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"FabricFit reference recommendations API on http://{args.host}:{args.port}")
    print(f"  POST /v1/recommend   GET /v1/health   (modelVersion={MODEL_VERSION})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nstopped")


if __name__ == "__main__":
    main()
