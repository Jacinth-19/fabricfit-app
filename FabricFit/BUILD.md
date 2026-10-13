# Building FabricFit

## Prerequisites (sandbox)
The toolchain lives outside the workspace (not persisted):

```bash
bash /home/user/tools/setup_toolchain.sh   # JDK 17 + Gradle 8.9 + Android SDK 34 into /var/tmp, plus a 4 GB swapfile
```

The sandbox has 2 vCPU / 2 GB RAM. The 4 GB swapfile created by the script is **required** —
the Kotlin/Compose compile otherwise OOM-kills the Gradle daemon. `gradle.properties` is tuned
accordingly (1.6 GB heap, in-process Kotlin compiler, workers.max=2).

## Build & test

```bash
export JAVA_HOME=/var/tmp/tools/jdk17 ANDROID_HOME=/var/tmp/android-sdk GRADLE_USER_HOME=/var/tmp/gradle-home
cd /home/user/FabricFit
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:testDebugUnitTest      # JVM unit tests (Robolectric; first run downloads android-all)
./gradlew :app:assembleRelease        # minified APK signed with app/demo-release.keystore (demo key!)
./gradlew :app:bundleRelease          # Play Console artifact (.aab) — see docs/PLAY_DATA_SAFETY.md
```

Outputs:
- `app/build/outputs/apk/debug/app-debug.apk` (~21 MB)
- `app/build/outputs/apk/release/app-release.apk` (~2.6 MB, R8-minified)
- `app/build/outputs/bundle/release/app-release.aab` (~4.4 MB, the Play-uploadable bundle)
- Convenience copies at the repo root: `FabricFit-debug.apk`, `FabricFit-release.apk`, `FabricFit-release.aab`

## Verified status
- `assembleDebug` / `assembleRelease`: **BUILD SUCCESSFUL** (only two unchecked-cast warnings remain).
- `testDebugUnitTest`: **14/14 passing** — ColorPatternAnalyzer (solid/striped/checked classification,
  confidence caps), RecommendationEngine (ordering, score band, reasons, tradition bonus, gender filter),
  EntitlementRepository (free-tier daily/monthly quotas, rewarded bonus, premium bypass).
- No emulator in this environment (`/dev/kvm` absent), so instrumented/androidTest suites and the
  end-to-end UI journey were **not** executed on a device here.
- The above was recorded **before** the Ntoma → FabricFit rename. The rename touched the
  package, application ID, theme and several class names (see below); it was verified by
  exhaustive grep + XML/TSV validation but **not** recompiled, because the toolchain
  installer needs `dl.google.com`, which this sandbox blocks. Run
  `./gradlew :app:testDebugUnitTest :app:assembleDebug` before shipping.

## Rename map (Ntoma Studio → FabricFit)

| Was | Now |
| --- | --- |
| module folder `NtomaStudio/` | `FabricFit/` |
| package `com.ntoma.studio` | `com.fabricfit.app` (namespace + applicationId) |
| `NtomaApp` | `FabricFitApp` |
| `NtomaDatabase` | `FabricFitDatabase` (Room db file `fabricfit.db`) |
| `NtomaNotifier` | `FabricFitNotifier` (channels `fabricfit_*`) |
| `NtomaNavHost` | `FabricFitNavHost` |
| `Theme.Ntoma` / `Theme.Ntoma.Splash` | `Theme.FabricFit` / `Theme.FabricFit.Splash` |
| colours `ntoma_*` | `fabricfit_*` |
| `brand/ntoma_icon_512.png` | `brand/fabricfit_icon_512.png` |
| support address `support@ntoma.app` | `support@fabricfit.app` |

Deliberately **not** renamed, and why:
- The lower-case Akan word **ntoma** ("cloth") in Twi copy, e.g. `look_ankara_men_1`
  = "Ankara ntoma ne trɔso", and the etymology sentence in `credits_body`. Turning these
  into "FabricFit" would corrupt the translation.
- The demo keystore alias `ntoma` and store/key passwords `ntomastudio`. Those strings are
  baked into the binary `app/demo-release.keystore`; changing the Gradle config alone would
  break signing. Replace the keystore with your own upload key before a real release.
- `https://github.com/Jacinth-19/ntoma-studio` in `tools/dataset/scrape/common.py`. Rename
  the GitHub repository first, then update this URL.

## Release signing
`app/demo-release.keystore` is a throwaway demo key (storepass/keypass `ntomademo`).
Replace `signingConfigs.release` in `app/build.gradle.kts` before any real store upload.
Because the application ID changed to `com.fabricfit.app`, an existing `com.ntoma.studio`
install cannot be upgraded in place.
