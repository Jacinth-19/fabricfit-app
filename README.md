# Heritage Luxe — by Ntoma Studio

**Your fabric · Our AI · Your style.** Scan. Discover. Try. Wear.

Heritage Luxe turns any Ghanaian fabric into stunning outfit recommendations with AI and
lets you see yourself in the perfect look — virtually. The Android app lives in
[`NtomaStudio/`](NtomaStudio/) (package `com.ntoma.studio`); build instructions are in
[`NtomaStudio/BUILD.md`](NtomaStudio/BUILD.md).

## The experience

| Screen | What it does |
|---|---|
| **Onboarding** | Forest-green brand story with the gold mark and serif wordmark. |
| **Home** | Search, a kente hero ("Ghanaian Fabrics / Timeless Style"), three actions — **Scan Fabric**, **AI Recommendations**, **Virtual Try-On** — and Featured Fabrics (Kente, Ankara, Lace, Batik). |
| **Capture Fabric** | Dark camera stage with Photo · Video · Scan modes and a white shutter. |
| **Scan Results** | Fabric card with a **% Match** chip, Colors / Pattern / Style tabs, palette dots and "View All Recommendations". |
| **Outfit Recommendations** | Occasion chips (All / Casual / Formal / Occasion) over a two-column grid of styles with match scores, hearts and try-on. |
| **Profile** | Deep-green profile with gold-ringed avatar, Scans / Favorites / Try-Ons stats and My Wardrobe · Saved Styles · Scan History · Settings · Help & Support. |

Bottom navigation follows the brand board: **Home · Discover · [raised gold-ringed camera] ·
Try On · Saved · Profile**.

## Design system

Implemented from the Heritage Luxe brand board (see
[`NtomaStudio/docs/heritage_luxe_design.md`](NtomaStudio/docs/heritage_luxe_design.md)):

- **Colour** — Forest Green `#173F35` (primary actions, headers, selected nav), Kente Gold
  `#D6A84F` (highlights, badges, active states), Burgundy `#7B2937` (accents), Warm Ivory
  `#F8F3E9` (light background) over Charcoal `#252722` / Stone Grey `#706F66` text; dark theme
  uses `#0F1F1A` / `#1B2E28` / `#253C34` surfaces. Semantic: success `#2E7D32`, warning
  `#F59E0B`, error `#D32F2F`, info `#3882F6`. Tokens live in
  `NtomaStudio/app/src/main/java/com/ntoma/studio/ui/theme/Color.kt`.
- **Type** — Playfair Display (display/headings) and Inter (body/labels), both OFL-licensed
  and bundled in `app/src/main/res/font/`; the ramp (H1 32/40 Bold … caption 12/16) is in
  `Type.kt`.
- **Components** — forest primary buttons, gold secondary, outlined tertiary; rounded cards;
  gold-ringed camera tab; fabric cards with category chips and palette dots.

## Recognition AI — 2 models now, 4 later

Per [`NtomaStudio/docs/ml_roadmap.md`](NtomaStudio/docs/ml_roadmap.md):

1. **Fabric-family classifier** — Kente, Kente-print, Ankara/wax, Adinkra, batik, lace, denim…
   with confidence and an honest **"uncertain"** instead of a forced answer.
2. **Visible-pattern classifier** — stripes, checks, plaid, dots, floral, geometric (+ solid);
   trains first from the acquired dataset. Appearance is not provenance: *striped does not
   prove Kente; floral does not automatically mean Ankara.*
3. *Later:* garment segmentation (person segmentation already ships and is QA'd).
4. *Later:* optional cloud photoreal try-on — disclosed cloud processing, no fit promises.

Colour extraction stays ordinary image processing, template previews stay
rendering/compositing, and recommendations stay rule-based
(fabric + pattern + colour + preferences). Build order: **pattern → family → segmentation →
try-on**. Training tooling: `NtomaStudio/tools/dataset/` (run
`selftest_pipeline.py` for the offline contract checks).

## Honesty first

- Every analysis shows its confidence and engine (`ON_DEVICE_DEMO`, `ON_DEVICE_TRAINED` or
  `CLOUD`); nothing is uploaded unless you opt into a cloud endpoint.
- Recommended looks are brand illustrations labelled as such, not renders of your cloth.
- No accuracy claims under 1000 labelled photos per class; smoke models never ship.

## Repository map

```
NtomaStudio/            Android app (Kotlin, Jetpack Compose, Material 3)
  app/src/main/java/…   domain / data / ui (theme = Heritage Luxe)
  docs/                 capability answers, ML roadmap, design system, QA notes
  tools/dataset/        collection → augmentation → fine-tune pipeline (+ selftests)
tools/                  sandbox toolchain setup
```

Licensed under the project's existing terms; bundled fonts (Playfair Display, Inter) are
SIL Open Font License.
