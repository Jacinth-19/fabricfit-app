# Debug test build — not a production or Play release

This is a **debug/test build** of Ntoma Studio, published so the app can be installed and tried.

- **Package:** `com.ntoma.studio.debug` — installs alongside a future production app, no data sharing.
- **Signed with:** the standard Android debug key. It is **not** production-signed and **cannot** be uploaded to Google Play.
- **Version:** 1.0.0-debug (versionCode 1). A Play release needs a higher code and a private upload key.
- **Language:** English only in this build.
- **Analysis:** runs on-device. No account, no analytics, no upload path in the default configuration.
- **Requirements:** Android 7.0 (API 24) or newer. You must allow installing apps from this source.
- **Build type:** debug (not R8-minified). `releaseCheck` and production variants are separate.

## What it does

1. Choose a dress/fabric photo (camera or gallery).
2. Drag to select the fabric area; the selected pixels are used.
3. Read on-device colour/pattern analysis — fabric identity is an **estimate**, not verified identification.
4. Preview flat outfit templates made from the selected fabric, adjust texture repeats and share the result.

## Honest limitations

- Outfit templates are **flat fabric layouts, not photoreal try-on** and not fit predictions.
- Cloud photoreal try-on is **not implemented**.
- Fabric recognition is a coarse general-purpose model plus colour/pattern analysis, **not** a trained African-fabric specialist.
- Continuous integration tests use an **original synthetic dress illustration**, not a real photograph.
- CI verifies Android 33 and Android 36 (16 KB pages); other physical devices are not yet covered.

Verified by the Android build and real-app-journey workflow before publishing. Screenshots and the raw emulator recording live in that workflow run's artifacts.
