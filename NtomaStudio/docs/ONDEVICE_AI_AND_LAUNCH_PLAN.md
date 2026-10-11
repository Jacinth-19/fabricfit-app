# On-device AI, try-on and Play launch plan

Updated 2026-10-11. The owner approved local fabric previews and outfit templates now,
with optional cloud photoreal try-on later. No specialist fabric model has been trained and no
remote Kaggle job has run. The concept notebook is separate from app recognition.

## Implementation and validation status

Implemented on `arena/5b96a1cd-ntoma-studio`:
- Camera/gallery editor displays the whole oriented image with a draggable rectangular fabric selection.
- Saving uses normalised image coordinates, not the previous inverse view-space transform.
- Saved fabric swatches, design illustrations, outfit templates and person-photo overlays use the selected pixels rather than invented palette tiles.
- Templates need no person photo, allow silhouette selection and texture-repeat adjustment, and export via Android sharing.
- Privacy/disclaimer/quota wording reflects local flat previews. Updated workflow strings in Twi/Ga/Ewe fall back to English pending native-language review; localization is not complete.
- Root GitHub Actions workflow builds/tests and installs real APKs on an Android 33 x86_64 emulator.

Evidence so far:
- Run `38110916690`: build and unit tests passed; installed-app journey launched, then failed at the Create tab. The label was not clickable; the whole tab is now clickable.
- Runs `38113151166` and `38113656244`: **PASS** build, unit tests and installed-app journey (Android picker → selection → saved crop pixel assertions → local analysis → two templates → repeat adjustment → share). Final captures were visually reviewed; early splash timing was corrected before packaging.
- Deliverables: `qa-artifacts/store/` (six 1080×1920 screenshots, 1024×500 feature graphic, 512×512 icon); `qa-artifacts/ntoma-studio-demo.mp4` (actual emulator recording, trimmed only to remove installation/blank launch). Raw recording is retained.
- Marketing generation is gated on passing the journey. The test input is an original synthetic whole-dress illustration, not a real camera photograph or proof of recognition accuracy.
- APKs/reports are Actions artifacts; debug signing is not production Play signing. No APK is committed to Git.

Local Java is absent. GitHub Actions is the execution path requested by the owner. Standard Actions
artifact/log download hosts are blocked from this workspace, so compiler/device failures are also
reported through GitHub check output. Build artifacts remain downloadable from the run page.

The design discussion below predates implementation; treat it as a roadmap, not a completion report.

## The tradeoff to decide first

The original scope raised a feasibility tradeoff; no specific mobile photoreal pipeline was benchmarked in this project:

- **"Fabric recognition and try-on must run on-device for privacy."**
- **"Show the user a photorealistic rendering of a brand-new dress style on their body."**

The repository already recorded this before this session, in
`docs/research_reference_apps.md` §4: diffusion try-on on-device is
*"impossible at our size/heat budget"*, and the demo compositor is what ships, labelled as a
demo. So the honest split is:

| | On-device (privacy intact) | Cloud (privacy changes) |
|---|---|---|
| Fabric recognition from a photo | Yes | Not needed |
| See the fabric on the user's own garment | Yes — fabric preview | — |
| See the fabric on an outfit **template** (kaba & slit, shirt, A-line …) | Yes — vector templates filled with the fabric | — |
| A brand-new silhouette that is not a template | No | Yes — Genlook-class API / diffusion |
| Play Data safety | Stays "no photos collected/shared" | Photos become shared data: purpose, retention, deletion, vendor agreement, explicit opt-in |

**Recommendation: ship the on-device version first.** It is the larger perceived-quality
gain available today with no dataset, no network and no budget, and it keeps the privacy
promise and the store listing accurate. Cloud try-on stays a later, opt-in, separately
flavoured feature.

## What the app does today (verified)

- Input: `CameraScreen.kt` (CameraX) and gallery (Photo Picker) → `ImageProcessor`.
- Analysis: `FabricAnalysisRepositoryImpl` → `BitmapAnalyzer` → `ColorPatternAnalyzer`
  (pure-Kotlin heuristics: colour buckets, GLCM texture, kente-strip detector, pattern
  classification) plus `OnDeviceMlClassifier` (generic ImageNet MobileNetV2 used only as a
  coarse second opinion, keyword-mapped to a fabric family).
- Try-on: `DemoLookGenerator` **composites** the detected colours/pattern onto the person
  photo with `PersonSegmenter` (DeepLabV3). It does **not** reshape the silhouette and does
  not restyle the person. `ImageGenerationRepository` is the swap-in seam.
- Everything above is local; `docs/PLAY_DATA_SAFETY.md` is accurate for this build.

## Recommended on-device pipeline

### 1. Recognition: what is visible, never what is not

- Classify **visible appearance**: solid, striped, checked, dotted, floral, geometric,
  gradient, lace-like, denim-like, wax-print style, kente-style weave. Do **not** claim
  fibre content (cotton vs polyester) from a photo — that is not decidable visually.
- Keep the existing heuristic `ColorPatternAnalyzer` as the deterministic fallback and
  ensemble with it; add an explicit **"not sure — try a flatter photo"** outcome instead
  of a confident wrong answer.
- Drop the raw ImageNet labels from the user-facing UI; they are not fabric concepts.
- Naming: keep **kente-style / kente-print** distinct from authentic woven kente. The app's
  `KENTE` vs `KENTE_PRINT` split must survive into any new model's labels.

### 2. Input that may be a whole garment, not a swatch

The current `OnDeviceMlClassifier` centre-crops the square and scales to 224px — it assumes
a swatch. For a whole-garment or casual photo, replace that with:

1. **Quality gate** — blur (Laplacian variance) and exposure; if it fails, ask for a retake
   instead of analysing.
2. **Localise the fabric region** — the bundled DeepLabV3 only finds the *person*. Add a
   segmenter that has an explicit **clothes/mask-of-clothing** class (e.g. MediaPipe
   `selfie_multiclass`, Apache-2.0 — re-verify its licence and model card before shipping).
   If clothing is found, use the clothes mask; if not, treat the photo as a swatch.
3. **Let the user tap/drag to confirm the region.** Cheapest, largest accuracy win; makes
   the demo honest because the user controls what is being read.
4. **Sample several interior patches** — erode the mask, reject patches touching skin, hair,
   or strong shadows/folds, classify each, then vote. This is what makes "a fabric caught in
   a whole-dress photo" work.
5. **Palette** — k-means in Lab space on masked pixels, weighted by area.
6. **Print repeat** — autocorrelation/FFT to find the tile unit; if there is no clear period
   (folds, non-repeating print), fall back to the best flat patch and say so.

Model: a small ImageNet-pretrained backbone (MobileNetV3-Small / EfficientNet-Lite) trained
by **transfer learning** — frozen backbone plus a light head — never from scratch. Export
int8 TFLite, ~1–3 MB, and keep the existing fallback path behind

### 3. Try-on → "Fabric preview" + templates

- **Rename and relabel** the current feature to what it is: a fabric **preview** over the
  user's own garment. Calling it "AI try-on" while shipping a compositor is the single
  fastest way to lose trust and to fail Play's accurate-representation rules.
- Improve the *preview* itself (still fully on-device): read the person mask, mask the
  existing garment, tile the extracted fabric, and re-apply the original shading
  (luminance multiply/overlay) plus a light warp so it follows folds instead of looking
  pasted on.
- Add **outfit templates**: house vector shapes (kaba & slit, agbada, A-line, shirt, two-piece)
  filled with the extracted fabric tile. This directly answers "what dress can be made from
  this fabric" honestly, offline, with no data collection. `looks.json` already holds the
  house illustration set — extend that approach rather than inventing a new asset pipeline.

## Recognition training: where Kaggle actually fits

Kaggle is **training only**; the phone runs the result. The pipeline already exists:

`tools/dataset/raw/<CLASS>/` → `prepare_kaggle.py` (licence-audited, deduped, leak-free
splits) → `train_on_kaggle.py` (MobileNetV3-Small fine-tune → int8 TFLite) → review per-class
accuracy → swap the asset behind the existing `AnalysisEngine` abstraction.

**The binding constraint is data, and it is the owner's, not Kaggle's.** Public candidates
found are mostly unusable for a commercial model:

- AITEX and TILDA textile sets are **NC/ND** → excluded.
- `fashion-pattern-images` claims MIT but is scraped from shops/Pinterest → provenance risk,
  excluded from the production pool unless cleared.
- `mikuns/african-fabric` is **licence unknown** → excluded.
- No public Ghanaian fabric dataset exists (the repo already says so).

So the honest path is **consented photos at roughly 100+ per class**, with the app's existing
**Settings → Contribute fabric photos → Export for training** flow, plus market/weaver field
days. Web-scraped starter data may be used to *prove the pipeline*, never for an accuracy
claim or a shipped asset.

### Evaluation harness (do this with the owner before any claim)

Freeze a small set of **owner-supplied** photos (swatches, worn garments, folded cloth, low
light) and score: crop/ROI correctness, palette fidelity, and classification against
owner labels. Re-run on every pipeline change. Report per-class precision/recall and macro-F1
— never a single "accuracy" number.

## Play launch and marketing (the owner's stated next step)

Order matters: **marketing last.** Advertising features before they are good violates Play's
accurate-representation policy and burns the honesty posture the repo is built on.

1. **Honesty fixes first** — relabel the try-on feature, align `PLAY_DATA_SAFETY.md`, and
   note that the Kaggle SD notebook is a *concept/marketing* tool, not app output.
2. **Screenshots**: there is no emulator in this environment (no KVM), and this is not the
   owner's phone. Two legitimate routes: (a) the owner captures real screens on-device; or
   (b) add **JVM screenshot tests** (Paparazzi/Roborazzi) that render real Compose screens
   in CI — note this needs new Gradle dependencies and cannot be built here. Screenshots must
   show what the app really does.
3. **Feature graphic (1024×500)**: `store/feature_graphic.png` is currently 1456×720 and the
   Play icon should be 512×512 (`store/play_icon_512.png` is 1024×1024). Rebuild from app UI
   plus **fabric photos the owner owns** — not third-party imagery.
4. **Video**: record the real on-device flow (capture → analysis → preview → tailor brief).
   Do not imply photoreal AI try-on.
5. **AI-generated imagery**: the SD concept images must contain **no real people**, must never
   be presented as app output or as "our AI", and are subject to the model's OpenRAIL-M
   restrictions. Any cloud path disclosed later changes the Data safety form.

## Decisions needed from the owner

1. **Confirm the split above**: on-device *fabric preview + templates* now, cloud *photoreal
   try-on* later (opt-in, separately flavoured). If photoreal on-device try-on is non-negotiable,
   it is not deliverable at this app's size/heat/quality budget, and saying so is the honest answer.
2. **Start photo collection** (~100 per class, consented) — this, not dataset hunting, is what
   unblocks real recognition.
3. **Confirm the marketing order** (relabel + accurate listing → screenshots → graphic → video),
   and who captures the on-device screenshots.
