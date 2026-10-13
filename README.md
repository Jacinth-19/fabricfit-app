# FabricFit

**Scan Your Fabric. Wear Your Style.**

FabricFit is an Android app that reads the fabric you already own — kente, ankara, wax
print, lace and more — and shows you what it could become. Photograph a cloth, get
explained outfit matches, preview a look, and carry a shareable brief to your tailor.

> Renamed from **ntoma-studio** / "Ntoma Studio" — see `FabricFit/docs/brand_positioning.md`.

## Feature names

| Feature | Name |
| --- | --- |
| Camera scanner | **FitScan** |
| AI recommendation engine | **FitMatch AI** |
| Virtual try-on | **FitRoom** |
| Wardrobe & saved items | **My Fit Closet** |

## Repository layout

```
FabricFit/          Android app (Kotlin + Jetpack Compose), package com.fabricfit.app
  app/              app module — UI, domain, data, on-device ML
  docs/             brand positioning, Play readiness, device QA, research notes
  store/            Play listing copy, icon and feature graphic, privacy policy
  tools/            i18n packs, dataset pipeline, mock backend, icon renderer
tools/              sandbox toolchain bootstrap
```

## Build

```bash
bash tools/setup_toolchain.sh
cd FabricFit
./gradlew :app:assembleDebug
```

Full instructions, verified status and release signing: `FabricFit/BUILD.md`.

## Languages

English, Twi (`tw`), Ga (`gaa`) and Ewe (`ee`). Translation packs live in
`FabricFit/tools/i18n/`; regenerate with `python3 tools/i18n/generate.py` from the
`FabricFit` directory.
