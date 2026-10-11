# Heritage Luxe · Ntoma Studio

Implemented from the owner's supplied visual direction, 11 October 2026. This is a
visual system for the existing Android app, not a rename or a separate mock-up.

## Applied
- Exact primary, gold, burgundy, background, surface, text and border tokens in light/dark themes.
- Bundled Playfair Display (400/600) and Inter (400/500/600), including SIL OFL licences.
- Forest/ivory editorial Home, real attributed Ghanaian photography, search entry,
  working fabric category links, camera/gallery actions, recent scans and inspiration.
- Five destinations: Home, Discover, **Try On**, Saved, Profile. The centre tab is
  emphasized within the bar and has selected-state semantics. Navigation respects
  font scaling and system gesture insets.
- Discover style filters, persistent favorites, compatibility labels and template cards.
- Gold camera guide, large capture control, gallery import even when camera is unavailable.
- Prominent local-preview canvas; add-fabric action from empty Try On. Existing
  before/after comparison, saving and sharing remain in the result flow.
- Burgundy favorite accents; theme switch on Home, Discover, Try On, Saved and Profile.
- Theme-aware system bar contrast and Home content insets.

## Honesty and assets
The supplied board's sample prices, names, 98% scores and authenticity claims are
not imported. Existing demo analysis and flat-composite limitations remain. There is
no new trained recognition model or cloud photoreal try-on in this change.
Real editorial photographs retain credits; unrelated wearer photos are not passed
off as exact catalogue garments. Catalogue silhouettes are labelled outfit templates.

Fonts were retrieved from google/fonts immutable blobs:
- Inter variable source: `047c92f6e2212473dc436020afed689527076d44`
- Playfair Display variable source: `7a09eb71f10622fd083134ac9ed46f59b21bee18`
Static weight instances were made with fontTools 4.60.1. Copyright/OFL notices live
in `app/src/main/assets/licenses/`. No runtime font download is needed.

## Verification
Local localization and release-tool tests passed (one credential-dependent test
skipped). Palette equality and functional text-contrast unit tests were added.
The installed-device journey now captures light/dark Home, Discover, Try On and
Profile, checks central tab selection, then exercises the real gallery/crop/template
flow. Camera capture callbacks were corrected to navigate/update state on the main
thread, with the executor disposed on exit.

Android build/device run: https://github.com/Jacinth-19/ntoma-studio/actions/runs/38155674595
The verify job passed: compilation, unit tests (including palette/contrast tests),
debug and release-check builds, signing safeguard and 16 KB alignment. Android
33/36 device journeys are still running; no device-pass claim is made. The previous API 36 crash remains unresolved; crash-log excerpts were added
to CI diagnostics rather than hiding that failure. No old screenshots are presented
as the updated design.
