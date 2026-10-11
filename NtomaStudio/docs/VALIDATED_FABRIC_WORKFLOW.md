# Validated fabric workflow — 11 October 2026

## Executed evidence

[GitHub Actions run 38113656244](https://github.com/Jacinth-19/ntoma-studio/actions/runs/38113656244), commit `6ff6f1c`: build and unit tests **passed**; real installed-app journey **passed** on Android 33 x86_64. Previous passing journey: run 38113151166.

The instrumentation launched MainActivity, completed onboarding, opened Create and Android's system photo picker, selected an original whole-dress test illustration, dragged a fabric rectangle, confirmed it, asserted saved crop dimensions/pattern pixels, completed local analysis, switched between two catalogue silhouettes, changed repeats and opened Android's share sheet. An exported template file was asserted to exist.

This validates that workflow on that emulator, **not** fabric classification accuracy, real camera capture, all catalogue silhouettes, ARM-device performance, every locale, or Play readiness. The fixture is an original synthetic illustration, not a real photograph. A specialist fabric model has not been trained; optional cloud photoreal try-on remains future work.

## Deliverables

Available in the run's **build-and-unit-reports** artifact: installable debug APK, instrumentation APK and unit-test reports. Debug signing is not Play upload signing.

Available in **real-app-journey**: real screen captures, raw emulator MP4, validation note, device output/logcat and marketing drafts. Local copies are in ignored `NtomaStudio/qa-artifacts/`:

- `store/`: six 1080×1920 framed screenshots; 1024×500 feature graphic; 512×512 icon.
- `ntoma-studio-demo.mp4`: ~34-second actual emulator recording, silent; installation/blank launch trimmed, interactions otherwise retained at normal speed. Source `demo-raw.mp4` retained.
- `validation.txt`: device-test completion statement.

The assets explicitly identify the synthetic test image and flat-layout limitation. They are **marketing drafts**, not a claim that the app is ready for Play publication. Native-language review, target-SDK/Play policy review and production signing remain necessary.

## Bugs found during execution and repaired

- Bounds-only decoding incorrectly rejected every valid picker photo and aborted editor loading.
- Saved file paths lacked a URI scheme for ContentResolver; load/analysis now normalize them.
- Only the Create icon, not its label, was clickable; the whole tab now responds.
- The old inverse view transform did not match the crop shown; selection/save now share image-coordinate geometry and EXIF orientation.
- Emulator notification-shade and Compose splash-clock capture races were corrected, and recordings/screenshots reviewed before packaging.

## Scope and privacy

Templates need no person photo and render the **selected image pixels**, mirrored/repeated inside a fixed silhouette. Local placement overlays also use the selected texture; neither predicts fit or reconstructs realistic drape. Local workflow does not upload images. Existing manually configured cloud **analysis** is separate and disclosed; cloud photoreal try-on is not implemented.

New workflow strings in Twi/Ga/Ewe deliberately fall back to English until native-language review; localization is not complete. Original photos/fixtures from a user were not required or requested.


## SDK, signing and localization readiness verification — 2026-10-11

Tested app/tooling commit: `f31c107ca5fb11168facbb159d624faa5f688504`.
Actions run: [38133894522](https://github.com/Jacinth-19/ntoma-studio/actions/runs/38133894522), **success**.

- `verify`: builds debug APK, instrumented test APK, R8-minified `releaseCheck` APK and AAB; JVM unit tests pass.
- CI rejects production release pre-build when upload signing credentials are absent; no demo/unsigned production fallback.
- All bundled ARM64/x86_64 native-library ELF LOAD segments pass 16 KB alignment; minified APK passes `zipalign -c -P 16`.
- `app-journey (33, google_apis)`: **success**.
- `app-journey (36, google_apis_ps16k)`: **success**; captured `device-environment.txt` confirms **16384** bytes. Captured files check `114456227539` includes PASS validation.
- Each emulator installs and smoke-launches minified `com.ntoma.studio.check`, then runs **3 installed debug-app tests**: picker/crop/selected-pixel analysis/templates/repeat/share journey, actual LiteRT inference with non-empty predictions, and critical Twi/Ga/Ewe English fallback + two-argument quota formatting.
- CameraX upgraded to 1.4.2 after the alignment gate detected its old image-processing JNI library. Standalone LiteRT 1.4.1 replaces TensorFlow Lite 2.14. Bundled MobileNet UINT8 outputs are masked before dequantization.
- Six local tooling tests pass (2 localization, 4 upload-certificate verification); 15 existing Kaggle launcher tests pass locally. No remote Kaggle run occurred.

**Not completed:** native-speaker approval, production upload-key installation, a real signed production AAB, Play/internal-track publishing, ARM64 physical-device validation and account-specific store requirements. `releaseCheck` artifacts are test-only and must not be uploaded as production. See [PRODUCTION_RELEASE.md](PRODUCTION_RELEASE.md) and [localization review handoff](localization/README.md).
