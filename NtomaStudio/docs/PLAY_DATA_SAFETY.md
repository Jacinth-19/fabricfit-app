# Play Store readiness notes — Ntoma Studio

## Build artifact for Play
Play Console requires an **Android App Bundle (.aab)**, not an APK:

```bash
./gradlew :app:bundleRelease   # -> app/build/outputs/bundle/release/app-release.aab
```

The production release requires a separately supplied upload keystore and verified certificate.
The public demo key has been removed and its certificate is rejected. See
[PRODUCTION_RELEASE.md](PRODUCTION_RELEASE.md) for the protected manual workflow and owner-only
secret configuration. No production-signed AAB has yet been built or published.

## Data safety form (as this build behaves)
Map the form answers to actual code behaviour:

| Question | Answer for this build | Why (code reference) |
|---|---|---|
| Photos collected/shared? | **No in default local mode; review if cloud analysis is configured** | CameraX/Photo Picker images are stored locally. Default analysis, flat templates and person-photo placement are local. An explicit user-configured cloud analysis endpoint CAN receive the selected image; shared exports are initiated by the user. Photoreal cloud try-on is not implemented. |
| Camera permission | Yes, requested at runtime | Only for fabric/person capture, with pre-explanation (`permissions/PermissionGate.kt`). |
| Analytics collected? | **No** | `DebugAnalyticsLogger` writes to logcat only; nothing is transmitted. |
| Crash logs? | On-device only | `analytics/CrashReporter.kt` writes stack traces to `filesDir/crashlogs` (max 10). Not transmitted. Swap for Crashlytics before enabling collection (see below). |
| Ads / advertising ID? | **No** | No ad SDK included; no `AD_ID` permission in the manifest. UI ad slots were removed for v1. |
| Account data? | None | No sign-in; the display name is stored locally in DataStore. |
| Data deletion | In-app | Settings → Data controls → Delete my data wipes DB, media files, and quotas (`DataControlsViewModel.deleteAll`). |

If a cloud analysis/try-on backend is enabled later, this table **must** be updated: photos would
become collected data with a declared purpose, retention, and deletion path.

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
1. Create a Firebase project, add an Android app with package `com.ntoma.studio`.
2. `google-services.json` → `app/`; apply `com.google.gms.google-services` and
   `com.google.firebase.crashlytics` plugins; add `firebase-crashlytics-ktx` + `firebase-analytics-ktx`.
3. Replace the body of `CrashReporter.install()` with
   `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)`.
4. Gate collection behind the Privacy/Notifications consent screen and update the Data safety form
   (crash logs + analytics become collected data).

## Content rating / listing checklist
- IARC questionnaire: no violence/gambling/UGC sharing beyond native share sheet → likely Everyone.
- Store listing: app icon `brand/ntoma_icon_512.png` (512×512), feature graphic 1024×500,
  phone screenshots of Home → Analysis → Try-on → Result, short + full descriptions
  (localize for tw/gaa/ewe once translations land).
- Privacy policy: host the text currently in `res/values/strings.xml` (`privacy_*`, `terms_*`) at a
  public URL and enter it in Play Console.
- Target API level: currently compile/target SDK 34 — verify against Play's current target-API
  deadline at submission time and bump if required.
