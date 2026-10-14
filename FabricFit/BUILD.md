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

Outputs (sizes measured in CI, run
[#38331853856](https://github.com/Jacinth-19/fabricfit-app/actions/runs/38331853856)):
- `app/build/outputs/apk/debug/app-debug.apk` (~32.7 MB)
- `app/build/outputs/apk/release/app-release.apk` (~14.4 MB, R8-minified)
- `app/build/outputs/bundle/release/app-release.aab` (~16.5 MB, the Play-uploadable bundle)
- Convenience copies at the repo root: `FabricFit-debug.apk`, `FabricFit-release.apk`, `FabricFit-release.aab`

## Verified status

CI — GitHub Actions workflow [`.github/workflows/build.yml`](../.github/workflows/build.yml),
ubuntu-latest + JDK 17 — runs `./gradlew :app:testDebugUnitTest :app:assembleDebug` from
`FabricFit/`. As of the FabricFit rename (commit `c058e13`, run
[#38330695396](https://github.com/Jacinth-19/fabricfit-app/actions/runs/38330695396)) that
job is **green** and uploads a ~33 MB debug APK plus the test reports. The JVM suite
(68 `@Test` methods across 13 classes, Robolectric) covers ColorPatternAnalyzer,
RecommendationEngine, EntitlementRepository, SearchIntentParser, ColorHarmonyEngine,
OutfitSuggester, BackgroundRemover, TextureMetrics and the UI journey; **any failure fails
the job**, so a green run means the rename compiles and the suite passes.

The CI workflow previously lived at `FabricFit/.github/workflows/build.yml`, a path GitHub
never reads, so it had never executed; it now sits in `/.github/workflows/` with
`working-directory: FabricFit`. `gradlew` was also committed without its executable bit and
has been set to mode 100755.

**Not covered here:** the instrumented androidTest suite, which needs a device or emulator.
The device-QA job is opt-in via `workflow_dispatch` (macOS runner, `reactivecircus/android-emulator-runner`)
because it is slow and flaky by nature. Note that GitHub only exposes `workflow_dispatch`
for workflows that exist on the **default branch**, so this job can only be dispatched once
`build.yml` has landed on `main`:

```bash
gh workflow run build.yml --ref main     # verify + release + macOS device-qa
```

CI also compiles the instrumented source set (`:app:assembleDebugAndroidTest`) on every
push, so a rename inside `androidTest` fails fast without an emulator.

This sandbox cannot build locally: only `github.com`, `pypi.org` and the npm registry are
reachable, so there is no JDK, no Gradle distribution, no Android SDK and no Maven Central.
Use CI (or a dev machine) for compiles.

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

The GitHub repository was renamed too: `Jacinth-19/ntoma-studio` → **`Jacinth-19/fabricfit-app`**.
GitHub redirects the old URL, so existing clones, links and pull requests keep working.

## Release signing
`app/demo-release.keystore` is a throwaway demo key — alias `ntoma`, store and key
password both `ntomastudio`. These values are the ones in `app/build.gradle.kts`, and
they are the ones CI signs with successfully. (Earlier revisions of this file and of
`docs/PLAY_DATA_SAFETY.md` said `ntomademo`, which was simply wrong.)
Replace `signingConfigs.release` in `app/build.gradle.kts` before any real store upload.
Because the application ID changed to `com.fabricfit.app`, an existing `com.ntoma.studio`
install cannot be upgraded in place.
