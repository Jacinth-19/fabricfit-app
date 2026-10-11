# Building Ntoma Studio

## Toolchain

JDK 17, Android SDK 36, Gradle 8.11.1 (wrapper), AGP 8.10.1. minSdk 24 is unchanged. CI is the tested build environment; local Java/Android SDK are not available in the Arena workspace.

```sh
cd NtomaStudio
bash gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
bash gradlew :app:assembleReleaseCheck :app:bundleReleaseCheck
```

- `app/build/outputs/apk/debug/app-debug.apk`: debug/test app.
- `app/build/outputs/apk/releaseCheck/app-releaseCheck.apk`: minified CI-only app (`com.ntoma.studio.check`), debug-signed.
- `app/build/outputs/bundle/releaseCheck/app-releaseCheck.aab`: test bundle, **not for production Play upload**.

The Android Actions workflow builds/tests these artifacts, checks native ELF/APK alignment, exercises the installed debug fabric journey and LiteRT inference on Android 33 and Android 36/16 KB, and smoke-tests minified startup.

## Production signing

```sh
bash gradlew :app:bundleRelease
```

This command **fails closed** without `NTOMA_UPLOAD_KEYSTORE`, `NTOMA_UPLOAD_STORE_PASSWORD`, `NTOMA_UPLOAD_KEY_ALIAS`, `NTOMA_UPLOAD_KEY_PASSWORD`, and matching `NTOMA_UPLOAD_CERT_SHA256`. Production signing never defaults to a demo/debug key. The public demo keystore has been removed; its certificate is forbidden.

Use the protected manual release workflow after owner configuration. See [production release instructions](docs/PRODUCTION_RELEASE.md) and [data safety notes](docs/PLAY_DATA_SAFETY.md). No production-signed bundle or Play publication is implied by a passing debug/CI build.

The earlier installed fabric journey passed on commit `6ff6f1c`, Actions run [38113656244](https://github.com/Jacinth-19/ntoma-studio/actions/runs/38113656244). SDK/signing/readiness changes require their own CI results, documented in `docs/VALIDATED_FABRIC_WORKFLOW.md`.
