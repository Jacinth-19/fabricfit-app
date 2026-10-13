# FabricFit — brand positioning

The app was renamed from **Ntoma Studio** to **FabricFit**. This document is the single
source of truth for the name, tagline and feature vocabulary used across the app, the
localized strings (`tw` / `gaa` / `ee`), the store listing and the docs.

## Name

**FabricFit** — fabric scanning/discovery plus custom outfit fitting and recommendations.

## Taglines

| Use | Line |
| --- | --- |
| **Primary (shipped in `app_tagline`)** | Scan Your Fabric. Wear Your Style. |
| Tech-focused | AI-Powered Ghanaian Fabric & Outfit Matching. |
| Luxe / fashion | Where Heritage Fabric Meets Modern Fit. |

`app_tagline` is the in-app line shown on the About screen, docs headers and store
long description. It is translated in all four shipped locales.

## App Store / Play headlines

- FabricFit: Instant AI Outfit Matching for Ghanaian & African Fabrics
- FabricFit: Scan Fabrics, Discover Styles & Try On Virtually
- FabricFit: Your Personal AI Stylist for Kente, Ankara & Custom Tailoring

## Feature vocabulary

| Feature | In-app name | String key(s) |
| --- | --- | --- |
| Camera scanner | **FitScan** | `home_cta_scan`, `camera_title` |
| AI recommendation engine | **FitMatch AI** | `recs_title` |
| Virtual try-on | **FitRoom** | `tryon_title` |
| User wardrobe / saved items | **Fit Closet** / "My Fit Closet" | `tool_wardrobe`, `wardrobe_title`, `profile_row_wardrobe` |

Short feature names stay in English across locales (standard for product feature names);
the surrounding sentence is localized. The **"My Fit Closet"** phrase is also kept in
English in the `tw` / `gaa` / `ee` builds so the product term is recognisable, while the
descriptive copy around it — including `wardrobe_empty_body`, "Your wardrobe is empty" —
remains fully translated.

## Naming notes

- **"ntoma" is not a leftover string.** It is the Akan word for *cloth* and is the
  etymology given in `credits_body`. It also appears as legitimate vocabulary in the Twi
  translations (e.g. `look_ankara_men_1` = "Ankara ntoma ne trɔso"). Never blanket-replace
  lower-case `ntoma`; the rename map and the list of deliberate exceptions live in
  `BUILD.md`.
- The launcher mark (a folded-cloth page) and the splash art carry no wordmark, so they
  carry over unchanged. The Play feature graphic now carries the FabricFit wordmark.
- The Android application ID changed to `com.fabricfit.app`, which means **existing
  `com.ntoma.studio` installs cannot upgrade in place** and a Play listing under the old
  package cannot be transferred. This build is a fresh listing.
