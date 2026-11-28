# Play Store readiness notes — FabricFit

## Build artifact for Play
Play Console requires an **Android App Bundle (.aab)**, not an APK:

```bash
./gradlew :app:bundleRelease   # -> app/build/outputs/bundle/release/app-release.aab
```

The release bundle is signed with **your upload key**, supplied as Gradle properties or CI
secrets — no key material is committed. Create it with
`bash FabricFit/tools/release/new_upload_key.sh`, which also prints the certificate SHA-256.
**Before uploading:**
1. Run `tools/release/new_upload_key.sh` if you have not already, and enable **Play App
   Signing** in Play Console so Google holds the app signing key.
2. Store the four values as repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD`); the `release` job fails if they are missing rather than
   emitting an unsigned artifact.
3. Bump `versionCode` for every upload.

## Data safety form (as this build behaves)
Map the form answers to actual code behaviour:

| Question | Answer for this build | Why (code reference) |
|---|---|---|
| Photos collected/shared? | **No by default; yes if the user configures a cloud engine** | Images are read via CameraX/Photo Picker and stored only in app-private storage (`ImageProcessor.save` → `filesDir/media`). The default path is on-device (`FabricAnalysisRepositoryImpl`, `DemoLookGenerator`). The optional cloud engine (`data/remote/CloudAnalysisClient`, `CloudRecommendationClient`) uploads the selected image **to the endpoint the user pastes into Settings → Analysis engine**; there is no default endpoint, no endpoint of ours, and no background upload. |
| Camera permission | Yes, requested at runtime | Only for fabric/person capture, with pre-explanation (`permissions/PermissionGate.kt`). |
| Analytics collected? | **No** | `DebugAnalyticsLogger` writes to logcat only; nothing is transmitted. No Firebase Analytics, Crashlytics or other analytics SDK is included — the Firebase dependencies were removed for exactly this reason (see `app/build.gradle.kts`). |
| Crash logs? | On-device only | `analytics/CrashReporter.kt` writes stack traces to `filesDir/crashlogs` (max 10). Not transmitted. Swap for Crashlytics before enabling collection (see below). |
| Ads / advertising ID? | **No** | No ad SDK included; no `AD_ID` permission in the manifest. UI ad slots were removed for v1. |
| Account data? | None | No sign-in; the display name is stored locally in DataStore. |
| Data deletion | In-app | Settings → Data controls → Delete my data wipes DB, media files, and quotas (`DataControlsViewModel.deleteAll`). |

**Cloud engine — how to answer the form.** The optional cloud engine is code-complete, so this must
be declared rather than deferred: Play's Data Safety form asks about the *app*, not about default
settings. Tick **Photos → collected → purpose: app functionality → not shared → optional → not
processed ephemerally**, and state that data is sent only to a server address the user supplies.
Google operates no backend for FabricFit, and Play App Signing aside we receive nothing, but a
"no data collected" answer would be false for any user who turns the engine on.

**Firebase is gone.** `firebase-messaging-ktx` and `firebase-analytics-ktx` used to be declared
while no `google-services.json` or plugin existed, so neither could ever initialise. They were
removed rather than wired up: a no-analytics policy plus a bundled analytics SDK is a
contradiction waiting to become a violation, and FCM cannot receive a message without
registration credentials. Push, when it is wanted, needs the Firebase project, the plugin, the
consent flow, this table and the policy updated together.

## Permissions declared (and why)
- `CAMERA` — fabric/person capture (runtime, pre-explained).
- `POST_NOTIFICATIONS` (API 33+) — processing/recommendation/inspiration notices (runtime, opt-in).
- No contacts, location, microphone, SMS, phone, call logs, accessibility, or Bluetooth — never requested.
- Photo selection uses the **Photo Picker** (no `READ_MEDIA_IMAGES` / storage permission needed).

## Billing
- Current: `billing/DemoBillingManager` — local flag only, no Play Billing Library, UI labels it a demo.
- To monetize: add `com.android.billingclient:billing-ktx`, implement `PlayBillingManager`
  (BillingClient → `queryProductDetailsAsync` → `launchBillingFlow` → acknowledge → verify on a
  server), create in-app products in Play Console, and swap the one binding in `di/AppContainer.kt`.

## Crash reporting (Crashlytics checklist)
1. Create a Firebase project, add an Android app with package `com.fabricfit.app`.
2. `google-services.json` → `app/`; apply `com.google.gms.google-services` and
   `com.google.firebase.crashlytics` plugins; add `firebase-crashlytics-ktx` + `firebase-analytics-ktx`.
3. Replace the body of `CrashReporter.install()` with
   `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)`.
4. Gate collection behind the Privacy/Notifications consent screen and update the Data safety form
   (crash logs + analytics become collected data).

## Content rating / listing checklist
- IARC questionnaire: no violence/gambling/UGC sharing beyond native share sheet → likely Everyone.
- Store listing: copy is ready in `store/listing.md` — app icon `brand/fabricfit_icon_512.png`
  (512×512), feature graphic `store/feature_graphic.png` (1024×500),
  phone screenshots of Home → FitScan → FitMatch AI → FitRoom → My Fit Closet
  (localize for tw/gaa/ewe once translations land).
- Privacy policy: host the text currently in `res/values/strings.xml` (`privacy_*`, `terms_*`) at a
  public URL and enter it in Play Console.
- Target API level: currently compile/target SDK 34 — verify against Play's current target-API
  deadline at submission time and bump if required.
