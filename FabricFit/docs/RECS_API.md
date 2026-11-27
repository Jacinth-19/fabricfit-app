# Recommendations API

The app ranks outfits **on-device** by default. This document describes the optional
backend that can replace that ranking, and the reference implementation of it that ships
in this repository.

```bash
python3 tools/recs_api/server.py          # http://127.0.0.1:8008
```

Then in the app: **Profile → Settings → Cloud engine** → set the base URL to
`http://<your-machine>:8008`. Until a URL is set, nothing in the app calls a network
service for recommendations.

## Honesty notes

Read these before quoting this document:

- The **contract is real and implemented**, but the reference server's scoring is a
  simple weighted match — it is not a trained model. It exists so the client has
  something to be tested against.
- There is **no hosted FabricFit service**. Setting this up is an operator action; the
  app ships no endpoint, no key, and no default.
- The Android emulator reaches your host machine at `http://10.0.2.2:<port>`, not
  `localhost`.
- The server has **no authentication**. Do not expose it to the internet as-is.

## `GET /v1/health`

```json
{"ok": true, "modelVersion": "reference-1"}
```

Used by the client's connection check. Any 2xx is treated as healthy.

## `POST /v1/recommend`

Request:

```json
{
  "fabric": {
    "id": 12,
    "name": "Kente Royale",
    "category": "KENTE",
    "pattern": "GEOMETRIC",
    "texture": "WOVEN",
    "colors": ["#C8952B", "#0F3D2E", "#B83227"],
    "confidence": 0.87
  },
  "candidates": [
    {"id": "kaba_asymmetric", "titleKey": "style_kaba_title", "category": "DRESS", "silhouette": "KABA"}
  ],
  "preferences": {
    "gender": "FEMALE",
    "occasions": ["WEDDING"],
    "favorites": ["maxi"]
  },
  "limit": 24
}
```

Response:

```json
{
  "recommendations": [
    {
      "styleId": "kaba_asymmetric",
      "score": 0.6748,
      "reasons": ["Large geometric repeat reads best on an uncut panel"],
      "customization": {
        "sleeve": "SHORT",
        "neckline": "OFF_SHOULDER",
        "length": "MAXI",
        "placement": "FULL"
      }
    }
  ],
  "modelVersion": "reference-1"
}
```

### Field notes

| Field | Notes |
| --- | --- |
| `fabric.name` | May be `null`; do not assume a string. |
| `fabric.colors` | `#RRGGBB` (no alpha). Alpha is dropped by the client. |
| `fabric.confidence` | 0–1, the on-device analyser's own certainty. |
| `candidates[].titleKey` | An **i18n key**, not display copy. The backend should return ids, not translations. |
| `candidates[].silhouette` | `GarmentSilhouette` enum name. |
| `score` | 0–1. The client multiplies by 100 and clamps the displayed match to **98**, matching the on-device cap. |
| `reasons` | Plain language, shown to the user with the backend attributed. Max 3 rendered. |
| `customization` | Optional. Unknown enum names fall back to the default option. |

### How the client behaves

1. Ranking always starts on-device. The screen is never blank waiting for the network.
2. If a base URL is configured, a cloud pass runs and **overlays** scores for styles it
   recognises.
3. Styles the backend does not mention **keep their on-device score** — a partial reply
   degrades gracefully rather than dropping rows.
4. A style id that is not in the candidate list is **discarded**, so a buggy or hostile
   backend cannot invent catalogue entries.
5. On failure the UI reports `ranker = CLOUD_FAILED` and keeps showing on-device results.

## Why re-rank instead of replace

The candidate set is the app's own catalogue, already filtered for gender and occasion.
Re-ranking keeps the client in control of what can be displayed: the backend can order
and explain, but it cannot show a garment the app has no artwork for. That is also what
makes the fallback path indistinguishable from the normal one — same list, same cards.

## Running the reference server

```bash
python3 tools/recs_api/server.py --host 0.0.0.0 --port 8008
```

Standard library only, no dependencies. It implements the same structure/scale/colour
reasoning as `RecommendationEngine.kt` so that swapping between local and remote does not
produce wildly different orderings while you are testing.
