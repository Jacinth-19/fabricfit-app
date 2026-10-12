# Heritage Luxe design system — implementation notes

Companion to the brand board. Tokens are code constants in
`app/src/main/java/com/ntoma/studio/ui/theme/Color.kt`; the type ramp (Playfair Display +
Inter, bundled OFL fonts in `app/src/main/res/font/`) is in `Type.kt`.

## Colour

| Role | Light | Dark |
|---|---|---|
| Background | Warm Ivory `#F8F3E9` | `#0F1F1A` |
| Surface | White `#FFFFFF` | `#1B2E28` |
| Card | White `#FFFFFF` | `#253C34` |
| Text primary | Charcoal `#252722` | `#F2F3F7` |
| Text secondary | Stone Grey `#706F66` | `#B9C1D0` |

Primary = Forest Green `#173F35` (gold in dark), secondary = Kente Gold `#D6A84F`,
tertiary = Burgundy `#7B2937`. Semantic: success `#2E7D32`, warning `#F59E0B`,
error `#D32F2F`, info `#3882F6`.

## Screens (faithful to the board)

- **Onboarding** — forest surface, gold mark + serif wordmark, tagline, gold CTA.
- **Home** — title + search, kente hero with serif headline and gold CTA, three action
  tiles (Scan Fabric / AI Recommendations / Virtual Try-On), Featured Fabrics row.
- **Capture Fabric** — near-black stage, Photo · Video · Scan row (gold active label),
  white shutter; gallery/flash/lens controls.
- **Scan Results** — fabric card with "% Match" success chip + verified mark,
  Colors / Pattern / Style tabs with gold underline, palette dots, recommendations CTA.
- **Outfit Recommendations** — occasion chips, two-column card grid with hearts and
  match scores.
- **Profile** — forest surface, gold-ringed avatar, Scans/Favorites/Try-Ons stats,
  icon menu rows (Wardrobe, Saved Styles, Scan History, Settings, Help & Support).
- **Bottom bar** — Home · Discover · [raised forest disc, gold ring, camera] · Try On ·
  Saved · Profile.

## Honest deviations (deliberate)

- The board's **Video** camera mode is shown but labelled "coming soon" — we never ship a
  dead-or-fake control.
- Recommendation cards carry **match scores, not invented prices/ratings**: the app
  recommends styles, it does not sell dresses.
- "AI Recommended" badges only appear where a scored engine actually ranked the item.
