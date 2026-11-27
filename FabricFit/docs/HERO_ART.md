# Hero artwork

## Rules

1. **No text in the image.** Headlines are drawn in Compose over the photo so they can be
   translated (en/ee/gaa/tw), restyled or re-worded without regenerating artwork. The
   previous Home banner had "Ghanaian Fabrics Timeless Style" baked into the pixels, which
   made the copy untranslatable.
2. **Every preset hero must show its own fabric.** A kente photograph on the Ankara preset
   is worse than a plain swatch, because it teaches the wrong thing.
3. **Phone-appropriate size.** Target under ~200 KB per hero at the displayed dimensions:
   ~1328×800 for the Home banner (5:3, cropped to 216 dp tall) and ~430×768 for the three
   Create preset heroes (~1.1:1, cropped into a 320 dp frame).

## Current assets

| Asset | Size | Shows | Used by |
| --- | --- | --- | --- |
| `catalog/heritage/hero_banner_card.jpg` | 1328×800, 161 KB | Two models in hand-woven kente, right-weighted, clean negative space left | Home screen banner |
| `catalog/heritage/hero_kente_folds.jpg` | 768×515, 119 KB | Folded kente strips, real weave structure | Create preset 1 — *Royal Ashanti Kente* |
| `catalog/heritage/hero_ankara_wax.jpg` | 437×768, 67 KB | Model in blue/multi Ankara wax print, peplum + midi skirt | Create preset 2 — *Vibrant Ankara Wax* |
| `catalog/heritage/hero_guipure_lace.jpg` | 409×768, 68 KB | Model in emerald guipure lace gown with head-tie | Create preset 3 — *Ceremonial Guipure Lace* |

## Provenance

These are **AI-generated photographs**, not licensed stock and not photos of real people.
They are used as illustrative editorial imagery inside a demo app; they are not evidence
that the app has a photography library, a shoot pipeline, or models on staff. If this app
is ever published for real, replace them with licensed photography — generating new ones
does not resolve the rights question for a shipped product.

`outfit_elegant_peplum.jpg` is still used by Wardrobe, the onboarding carousel and the
`modern_kaba` art mapping, so it was left in place rather than repurposed.

## Regenerating

Prompts that produced these asked for: editorial/e-commerce photography, natural skin
tones, sharp fabric-weave detail, shallow depth of field, and — explicitly — **no text, no
logos, no watermarks**. Generators sometimes return a triptych or collage; crop a single
panel rather than shipping the grid.
