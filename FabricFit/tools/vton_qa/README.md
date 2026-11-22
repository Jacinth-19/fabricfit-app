# FitRoom virtual try-on — test it locally, for free, no API key

FabricFit's virtual try-on runs **on the phone**. There is no cloud call, no account and
no API key in the shipping path: the app segments the person with bundled TFLite models
and drapes the scanned cloth with the garment geometry, all on-device. That means you can
test it on your own phone offline — and you can test the exact same pipeline on a laptop.

This folder gives you three ways to do it, cheapest first.

| Path | What it proves | Needs |
| --- | --- | --- |
| 1. Local pipeline (`vton_local.py`) | The try-on math/models produce a garment on a real photo | Python 3 + `pillow numpy ai-edge-litert` |
| 2. Install the APK on your phone | The real app does it on your device, offline | An Android phone, the built APK |
| 3. On-device E2E test | The shipped code path works on a real handset and saves the images | Phone + USB debugging |

Nothing here costs anything. The "free API" question is answered in
[Why not a free cloud API](#why-not-a-free-cloud-api) below.

---

## 1. Run the pipeline on your laptop (no phone)

```bash
cd FabricFit
pip install --break-system-packages pillow numpy ai-edge-litert   # once

# the bundled Ghanaian sample model + real kente cloth swatch
python3 tools/vton_qa/vton_local.py --demo

# your own photo and your own cloth
python3 tools/vton_qa/vton_local.py \
    --person ~/Pictures/me.jpg \
    --fabric ~/Pictures/ankara.jpg \
    --style kaba_slit --sleeve PUFF --neckline VNECK --length MAXI

# build the cloth swatch straight out of a dress photo you found online
python3 tools/vton_qa/vton_local.py --demo --fabric-from-dress tools/vton_qa/samples/dress_kente_online.jpg
```

Outputs land in `tools/vton_qa/out/`:

| File | What it is |
| --- | --- |
| `tryon_result.jpg` | The composited look |
| `side_by_side.jpg` | Person → fabric → mask → result in one strip |
| `person_mask.png` | What the segmenter cut out |
| `report.html` | Open this in a browser: images + every measured fact |

`vton_local.py` is a port of the app's own code:

| Local | App source |
| --- | --- |
| `Segmenter` | `data/ml/PersonSegmenter.kt` — **the same** `selfie_segmentation.tflite` and `deeplabv3_257_mv.tflite` |
| `garment_shapes()` | `media/garment/GarmentGeometry.kt` — same normalised polygons |
| `measure()` | `media/garment/BodyFit.kt` — same landmark math |
| `render_tryon()` | `data/repository/DemoLookGenerator.kt` — same shading passes and mirror-tiling |
| `render_fitted()` | reference renderer for the body-fitted outline |

Useful flags:

```
--renderer fitted|app   fitted (default) = body-measured cloth; app = exact DemoLookGenerator output
--fit body|fixed        body = measured torso; fixed = the old 20–80 % box
--placement FULL|TOP_ONLY|SKIRT_ONLY|PANELS|ACCENTS
--gender WOMEN|MEN|UNISEX   override the catalogue style
--no-segment            skip TFLite (useful to isolate a model problem)
--json                  machine-readable result metadata
```

## 2. Install the app on your phone and use FitRoom

The CI release artifacts are published on GitHub Releases, so there is nothing to build:

1. On the phone, open
   <https://github.com/Jacinth-19/fabricfit-app/releases/latest> and download the `.apk`.
   (These are demo-signed test builds — fine to sideload, not for Play.)
2. Allow installs from your browser when Android asks.
3. Open FabricFit → **FitRoom** → **Use sample model** (the bundled Ghanaian model) →
   **Use kente bought online** (the cloth swatch) → pick a design → **Apply fabric**.

The app never sends the photos anywhere; airplane mode works.

Building it yourself (needs the Android SDK):

```bash
cd FabricFit
./gradlew :app:installDebug          # phone connected over USB
```

## 3. Run the on-device end-to-end test

This runs `TryOnDeviceE2ETest` — the bundled Ghanaian model + kente swatch through the
**shipping** pipeline — and writes the result images to the phone so you can look at them.

```bash
cd FabricFit
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.fabricfit.app.data.repository.TryOnDeviceE2ETest

bash tools/vton_qa/pull_from_phone.sh        # copies the images to tools/vton_qa/out/phone/
```

You get `tryon_result.jpg`, `person_input.jpg`, `fabric_swatch.jpg`, `person_mask.png`
and `report.txt` (device model, segmentation time, coverage %, file size).

CI can run the same thing: push a commit whose message contains `[tryon-e2e]`, or run the
**build** workflow manually — the `tryon-e2e` job uploads the images as
`fabricfit-tryon-e2e`.

## Proof the pipeline runs

`docs/vton/fitroom_journey.jpg` is one run of `--demo`: the bundled Ghanaian model, the
kente dress sourced online, the real cloth swatch and the FitRoom result.

`docs/vton/fit_comparison.jpg` is the same inputs through the old fixed-box geometry and
through the body-fitted outline.

Both were produced by `vton_local.py` on this repo with the bundled TFLite models —
the exact numbers are reproduced in `BodyFitParityTest`.

## The demo inputs

| Asset | What it is |
| --- | --- |
| `app/src/main/assets/catalog/samples/model_ama_fullbody.jpg` | Full-body reference model: Ghanaian woman, plain clothing, arms clear of the torso, plain background |
| `app/src/main/assets/catalog/samples/dress_kente_online.jpg` | A Ghanaian kente dress sourced online (kept as the reference garment) |
| `app/src/main/assets/catalog/samples/fabric_kente_online.jpg` | Cloth swatch cut from a real kente photograph; this is the fabric FitRoom drapes |

In the app, **Use sample model** loads the model photo and **Use kente bought online**
registers the swatch as a fabric scan (`DemoTryOnSamples.kt`).

## Verify it yourself (CI)

Every push runs the off-device geometry tests:

```bash
cd FabricFit
./gradlew :app:testDebugUnitTest --tests 'com.fabricfit.app.media.garment.*'
```

`BodyFitTest` covers the outline math (symmetry, sleeves, hem lengths, necklines, masks
that are empty or too small). `BodyFitParityTest` pins the exact numbers the Python
renderer produces on the bundled model, so the phone and the laptop cannot drift apart.

## What to expect, honestly

The bundled engine is a **demo compositor**, not a diffusion virtual try-on. It segments
the person, measures shoulder/bust/waist/hip, fits a dress outline to those landmarks and
tiles the real cloth over it with fold shading. On a clean, front-facing, full-body photo it
reads as a fitted garment. On a cropped selfie, a busy background or an angled pose it
degrades — that is expected and the UI badges the output as a demo composite, never as a
photo-real render.

`tools/vton_qa/out/comparison.jpg` shows the difference between the old fixed-box geometry
and the body-fitted geometry on the same inputs.

## Why not a free cloud API

There are free-to-try hosted try-on models (e.g. the IDM-VTON Hugging Face Space), and a
`CloudAnalysisClient` already exists in the app for operators who configure a base URL.
They are not the right default here:

- **They are not really free.** The public Spaces run on shared GPUs with a queue
  (30–120 s, sometimes more) and a daily quota; anonymous use hits "GPU quota exceeded"
  quickly, and a usable setup needs an account/token.
- **They are not private.** Every try-on uploads the wearer's photo to a third party. For a
  Ghanaian-market app handling personal photos, on-device is the honest default.
- **They are not reliable.** A demo Space can sleep, change API, or disappear; that is a
  bad dependency for a shipped feature.
- **It already works offline.** The models the app needs are bundled (about 25 MB), so the
  feature keeps working with no connection and no bill.

If you do want a generative backend later, the app already supports one: Settings → engine
→ set a cloud base URL, and the server must answer `POST /v1/tryon`
(contract in `data/remote/CloudAnalysisClient.kt`). `tools/mock_server/server.py` implements
that contract for local development.
