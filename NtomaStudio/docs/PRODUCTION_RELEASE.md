# Production signing and launch gates

Updated 2026-10-11. Signing automation is implemented; a real production-signed AAB has **not** been created or published.

## What changed

- compileSdk/targetSdk 36; minSdk remains 24. AGP 8.10.1 / Gradle 8.11.1 / JDK 17. Kotlin and matching KSP remain 2.0.20 / 2.0.20-1.0.25: API 36 does not itself require a Kotlin upgrade.
- Bundled-runtime dependency migrated from TensorFlow Lite 2.14 to standalone LiteRT 1.4.1 (same Interpreter API). Native ELF and APK ZIP alignment checks run in CI, with actual JNI inference on an Android 16 16 KB-page emulator.
- Public demo keystore removed from the current tree. Its certificate is explicitly rejected even if someone renames the file. It remains compromised/public in Git history and must never be an upload credential.
- Release pre-build fails without credentials and an expected SHA-256 upload certificate. No demo/debug/unsigned production fallback.
- `releaseCheck` is a separate minified, debug-signed CI variant with application ID `com.ntoma.studio.check`. Its APK/AAB are **not production upload artifacts**.
- Manual `release.yml` creates a signed AAB and mapping file only; it never publishes to Play or uploads a keystore. Private key files are cleaned even on failure.

## Required owner action (do not send passwords or keys in chat)

This session's GitHub integration returned HTTP 403 on Actions secret access. It cannot inspect/create/replace signing secrets. Configure these privately in GitHub's **production environment** and enable required reviewers / restrict allowed deployment branches to `arena/5b96a1cd-ntoma-studio`.

| Name | Type | Value |
|---|---|---|
| `NTOMA_UPLOAD_KEYSTORE_BASE64` | Secret | Base64 of the actual upload keystore, without newlines |
| `NTOMA_UPLOAD_STORE_PASSWORD` | Secret | Keystore password |
| `NTOMA_UPLOAD_KEY_ALIAS` | Secret | Private-key alias |
| `NTOMA_UPLOAD_KEY_PASSWORD` | Secret | Key password |
| `NTOMA_UPLOAD_CERT_SHA256` | Variable | SHA-256 fingerprint of the approved upload certificate |

**Existing Play app:** use the registered upload key and compare its certificate to Play Console → App integrity → Upload key certificate. Do not generate/replace it casually. An existing app-signing key is not necessarily its upload key. If the public demo certificate was registered, arrange an upload-key reset in Play Console; merely changing local files is not sufficient.

**Confirmed brand-new Play app:** generate a new upload key on a trusted owner's machine or managed signing service, keep a secure recoverable backup, and enable Play App Signing. Interactive keytool keeps passwords off the command line:

```sh
umask 077
keytool -genkeypair -keystore ntoma-upload.keystore -alias ntoma-upload \
  -keyalg RSA -keysize 3072 -validity 10000
keytool -list -v -keystore ntoma-upload.keystore -alias ntoma-upload
```

Enter identity details and passwords locally, not in chat or Git. Transfer base64 directly to the secret UI/CLI on that trusted machine (do not print it into shared logs). No upload key was generated in this sandbox: without secret installation/access and a trusted backup location it would be unusable and risk losing the identity.

After secrets/review protection are configured, manually run **Signed production bundle (manual only)** from this branch with an unused version code greater than every prior Play upload, and confirm the upload-certificate check. The workflow validates range but cannot query Play's current code. Download the `production-bundle-N` artifact; upload only after launch checks below. It does not automatically dispatch or publish.

## Launch checks still required

- Actual native-speaker review for Twi, Ga, Ewe (see `localization/README.md`). English fallback is intentional, not completed localization.
- A production-signed release AAB from the manual workflow and Play's own bundle/native-library validation.
- Internal-track testing on ARM64 hardware, including 16 KB devices. CI exercises x86_64 emulator inference/alignment and minified startup, not every physical device or the full production-signed app.
- Play Console identity, app ownership, package availability, version code, policy forms, hosted privacy-policy URL, store-copy approval and any account-specific testing prerequisites.
- Data safety answers for the exact shipped configuration; optional user-configured cloud analysis can transmit selected images. Photoreal cloud try-on is not shipped.

## Sources checked 2026-10-11

- Google Play target policy: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en — API 36 for new ordinary mobile submissions/updates from 2026-08-31.
- AGP compatibility: https://developer.android.com/build/releases/about-agp and https://developer.android.com/build/releases/agp-8-10-0-release-notes — API 36 requires at least AGP 8.9.1; AGP 8.10 supports 36 and requires Gradle 8.11.1.
- Native-library validation approach: https://developer.android.com/guide/practices/page-sizes — verify ELF LOAD alignment, APK ZIP alignment and runtime on a matching device. Do not infer readiness just from a dependency version.
