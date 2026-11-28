# Handover — what's left after the FabricFit rename

State of `Jacinth-19/fabricfit-app` at the end of the rename session
(commit `b47bbce` on `main`, 13 Oct 2026). Everything below is **unfinished work**,
not a bug list — the rename itself is complete and green.

## Start here (new session)

The rename session ended with its pull requests merged, so it could not push. One
commit was left behind: **`775347a` — "Restore the two unpushed commits after a sandbox
git rollback"**, sitting on the branch `arena/74b9775c-ntoma-studio` on top of `b47bbce`.
It contains this handover, the README screens section, and the keystore-password doc fix.

```bash
# 1. the remote URL may point at the old repo name - the sandbox does not persist
#    .git/config, so this gets reset between sessions
git remote set-url origin https://github.com/Jacinth-19/fabricfit-app.git

# 2. push the stranded commit and open a PR
git push origin arena/74b9775c-ntoma-studio
gh pr create --base main --head arena/74b9775c-ntoma-studio \
  --title "Handover notes, README screens section, keystore docs fix"

# 3. port the two pre-rename PRs (#1 and #3) - see section 4
python3 tools/port_legacy_rename.py --self-test    # verify the rules
python3 tools/port_legacy_rename.py <worktree>     # dry run on the rebased branch
python3 tools/port_legacy_rename.py <worktree> --apply
```

**If commit `775347a` is missing too** (the sandbox re-cloned the repository and rolled
the branch pointer back during the rename session), the content still exists as plain
files in the working tree — `HANDOVER.md`, the README screens section, and the
password fix in `BUILD.md` and `docs/PLAY_DATA_SAFETY.md`. Check those files against
`main` and re-commit as needed. (Both files now describe the upload-key flow instead, since
the demo keystore has been removed.)

### Backup outside git: `/home/user/patches/`

Because the sandbox destroyed unpushed commit objects once, that work is also exported
as patch files **outside** the repository, so it does not depend on `.git` surviving:

```bash
ls /home/user/patches/            # 0*.patch + README.md + export.sh
bash /home/user/patches/export.sh # re-export after adding commits
```

Recovery, if the commits are gone:

```bash
cd /home/user/ntoma-studio
git remote set-url origin https://github.com/Jacinth-19/fabricfit-app.git
git fetch origin && git checkout -B arena/74b9775c-ntoma-studio b47bbce
git am /home/user/patches/*.patch
```

Full instructions, including what to do if `b47bbce` itself is unavailable, are in
`/home/user/patches/README.md`. The patches are verified: applied with `git am` to a
clean worktree at `b47bbce`, the result byte-matches this tree.

## Where things stand

| Item | State |
| --- | --- |
| Product name, tagline, feature names | FabricFit / "Scan Your Fabric. Wear Your Style." / FitScan, FitMatch AI, FitRoom, My Fit Closet |
| Package + application ID | `com.fabricfit.app` |
| Command-line: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` | green in CI on `main` |
| `./gradlew :app:assembleRelease :app:bundleRelease` | green in CI on `main` |
| GitHub repository | `Jacinth-19/fabricfit-app` (renamed from `ntoma-studio`) |
| Gradle / AGP / Kotlin | 8.9 / 8.5.2 / 2.0.20, compileSdk + targetSdk 34, minSdk 24, versionCode 2 / versionName 1.0.1 |

CI runs on every PR and every push to `main`: `verify` (unit tests, debug APK,
androidTest compile) then `release` (R8 APK + Play bundle). The macOS emulator job
runs weekly (Mon 06:00), on `v*` tags, or manually.

## 1. Toolchain — done, and what it now pins

The API 36 bump moved the whole chain at once, because each layer only reveals its own
incompatibility when the previous one is satisfied:

| Component | Version | Why this one |
|---|---|---|
| `compileSdk` / `targetSdk` | 36 | Play requires API 36 for new apps and updates from 31 Aug 2026 |
| AGP | 8.9.1 | lowest release that officially supports `compileSdk 36` |
| Gradle | 8.11.1 | AGP 8.9 requires it; wrapper is pinned to `gradle-8.11.1-bin.zip` |
| Kotlin / KSP | 2.0.21 / 2.0.21-1.0.28 | KSP must match Kotlin exactly |
| Compose BOM | 2025.04.01 | compiler ships with Kotlin 2.x, so only the BOM moves |
| Robolectric | 4.16.1 | 4.13 refuses `targetSdk 36`; 4.16 covers API 23-36 |
| build-tools | 36.0.0 | pinned — AGP 8.9's default is still 35.0.0 |
| CI JDK | **21** | SDK 36 jars are compiled with Java 21; Robolectric will not build a sandbox otherwise |

`sourceCompatibility`/`targetCompatibility`/`jvmTarget` stay at **17**: JDK 21 is the
toolchain that runs the build, not the bytecode it emits. `media3` is still 1.4.1; 1.5+
needs `compileSdk 35`, which is now satisfied, so that is a safe separable follow-up.

## 1a. CI durability — traps that each cost real debugging time

Worth knowing before touching the workflows; each of these reported the wrong thing.

- **`run:` steps execute in `defaults.run.working-directory`, which is `FabricFit`.**
  `uses:` steps resolve paths against the workspace root instead. A path written for one
  context silently fails in the other - it looks like "file not found" or "no results".
  `FabricFit/tools/qa/check_store_contacts.py` therefore finds the repo root itself.
- **A YAML plain scalar is not a block scalar.** `run: bash x.sh \` followed by an
  indented continuation escapes the *space*, so the next line becomes a leading space in
  the argument: `Cannot locate tasks that match ' :app:testDebugUnitTest'`. Multi-line
  commands need `run: |` with explicit `\` continuations.
- **`github.event.head_commit` only exists on `push`.** A branch with an open PR emits two
  runs per commit, so a commit-message trigger appears to work or not depending on which
  run you look at. Both opt-in tags also match the PR title for that reason.
- **Do not make the annotators conditional.** `verify`'s annotation step had
  `env.HAS_KEY` copy-edited into it (that variable belongs to `release`), so it never ran
  and every failure reported only "exit code 1". Logs are unreadable without an Actions
  login; annotations are the only channel that survives.
- **Maven Central rate-limits shared CI IPs** with HTTP 429, which is why
  `tools/qa/gradle_retry.sh` exists. It retries only download failures and fails at once on
  compile or test errors, so it cannot hide a regression. `settings.gradle.kts` also
  prefers Google's Maven mirror ahead of Central to reduce the exposure.

## 2. Play Store listing for `com.fabricfit.app`

`store/listing.md` has the short and full descriptions, categorisation and the
screenshots checklist. Still to do:

- **Play listing screenshots — wired up, but the capture is only part reliable.** Six images
  exist in `FabricFit/store/screenshots/` (1080x1920, inside Play's accepted range) and
  `tools/qa/capture_fabricfit_screens.sh` now regenerates the Play slots from the same emulator
  run that produces `docs/screenshots/`, so the listing can no longer drift from the build by
  construction. What it cannot yet do is finish: on every attempt so far the hosted emulator
  drops off adb after the *first* screen (`device 'emulator-5554' not found`, on attempts 2 and
  3 of 3), so a run commits one refreshed screen and leaves the rest as they were. The job
  reports this honestly rather than passing.
  - The `screenshots` job is the only one it happens in, and the same runner boots fine for
    `device-qa` (which runs the full instrumented suite on API 33). The difference is that this
    job installs the API 33 **google_apis** image to get a virtual camera; try `default`
    without `-camera-back`, or add `adb kill-server && adb start-server` between attempts.
  - `04_play_scan_results.png` is not regenerated at all: that screen needs a fabric photo
    mid-analysis, which the capture script does not drive. It keeps an older image deliberately
    rather than pretending to have recaptured it.
- **Privacy policy — done, pending one manual step.** `FabricFit/store/privacy_policy.html` is
  published to https://jacinth-19.github.io/fabricfit-app/privacy.html by
  `.github/workflows/pages.yml`. The contact address is `fabricfit.app@gmail.com`, because
  `privacy@`/`support@fabricfit.app` never existed (the domain is not registered, so those
  addresses bounced) and `FabricFit/tools/qa/check_store_contacts.py` now fails the build if
  one comes back. **Manual step:** in the repository's Settings → Pages, set the source to
  "GitHub Actions" once, or the deploy job will not have a Pages site to publish to. Then paste
  the URL into Play Console → Store listing → Privacy policy.
- **The application ID changed**, so this is a *new* Play listing. An existing
  `com.ntoma.studio` install cannot upgrade in place, and the old listing cannot be
  transferred — do not expect install continuity.
- Add localized listings once tw/gaa/ee store copy exists; keep the feature names in
  English, translate the surrounding sentences (same rule as the in-app strings).

## 3. Release signing (must do before any real upload)

**Done — the demo keystore is gone.** `FabricFit/app/demo-release.keystore` (alias
`ntoma`, password `ntomastudio`) used to be committed, with `.gitignore` carrying an
explicit `!demo-release.keystore` exception so the file could be tracked. That key was
public by definition and Play would have rejected it. It has been deleted, the exception
removed, and `signingConfigs.release` no longer has a fallback: a release build without a
configured key now fails (`requireUploadKey` in `app/build.gradle.kts`) instead of quietly
producing an unsigned artifact.

Create your own upload key once with `bash FabricFit/tools/release/new_upload_key.sh`, then
put the four values in `~/.gradle/gradle.properties` locally and in the repository secrets
for CI. Enable Play App Signing so Google holds the app signing key and this file is only the
upload key. Note the history: the demo key still exists in past commits, so treat any
artifact signed before this change as publicly-signed and do not publish it.

## 4. Unrelated open pull requests

Two older PRs from previous sessions are still open and are **not** part of the rename:

- **#1** "Apply v2 Ghana fabric taxonomy to the app (18→26 classes) + taxonomy consistency checker"
- **#3** "Fabric recognition: two trained models with an uncertainty contract"

Both touch the codebase the rename just moved. Land or rebase them onto the current
`main` soon, or they will conflict against renamed paths and packages.

They were written **before** the rename, so after rebasing they will still say
`com.ntoma.studio`, reference `@style/Theme.Ntoma` and `ntoma_*`, and point at
`NtomaStudio/` — and will not compile. Port them with the tool in `tools/`:

```bash
python3 tools/port_legacy_rename.py --self-test        # guards: Akan word, keystore
python3 tools/port_legacy_rename.py .                  # dry run, lists what would change
python3 tools/port_legacy_rename.py . --apply          # rewrite in place
```

Then rename the *paths* by hand (`git mv`): `NtomaStudio/` → `FabricFit/`,
`app/src/main/java/com/ntoma/studio/` → `app/src/main/java/com/fabricfit/app/`.
The tool rewrites file contents, not paths — and it deliberately never touches bare
lower-case `ntoma` (the Akan word for cloth, live in the Twi strings). It also refused to
rewrite the demo keystore's `ntoma`/`ntomastudio` credentials; that keystore has since been
deleted, so this is history rather than a live constraint. The tool is now **retired** —
the rename it performs is already applied, and re-running it is pointless.

## 5. Smaller follow-ups

- **The instrumented suite runs, and is green.** `device-qa` used to filter itself down to
  one test with `-Pandroid.testInstrumentationRunnerArguments.class`, so every other test was
  skipped even on the runs that executed - it could only ever pass by not running. It now runs
  the whole suite on API 33, and gained `DatabaseDeviceTest` (Room on real framework SQLite)
  and `AppLaunchSmokeTest` (launches `MainActivity` through the real DI container, checks the
  start destination, tab navigation, and the first-run onboarding path). Trigger it with
  `[device-qa]` in the commit message or PR title; it also runs weekly and on `v*` tags.
  The start-up path had never been executed by any build before this.
- **The Android SDK / JDK toolchain script** is `tools/setup_toolchain.sh` at the repo
  root. It still assumes the module path is `/home/user/FabricFit`, which is correct,
  but it downloads from `api.adoptium.net`, `services.gradle.org` and `dl.google.com` —
  all of which are blocked from the Arena sandbox. Build in CI, not in the sandbox.
- **Dead route constant.** `Routes.ONBOARDING` in
  `ui/navigation/Destinations.kt` is declared but never referenced — onboarding is a
  state gate in `AppShell` (`onboardingCompleted`), and Settings' "Replay onboarding"
  re-arms that flag rather than navigating. Either wire it up or delete the constant.
  (Left alone deliberately: deleting it is a one-line change but would have turned a
  release-gate PR into an unrelated refactor.)
- **The README documents all 29 screens** (map + tables) and includes real API 33
  emulator captures of the core tabs under `FabricFit/docs/screenshots/`. Play listing
  screenshots are generated by the same run (see the listing section above); what is left
  there is the emulator dropping off adb after the first screen.
- **`credits_body` keeps the etymology** ("the name comes from 'ntoma', Akan for cloth")
  in all four locales. That is intentional and reviewed — do not "fix" it.
- Twi copy legitimately contains the word `ntoma` ("cloth"), e.g. `look_ankara_men_1`
  = "Ankara ntoma ne trɔso". Never blanket-replace lower-case `ntoma`; the full list of
  intentional survivors is in `FabricFit/BUILD.md`.

## Verification commands

```bash
# what CI runs, locally
cd FabricFit
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:assembleRelease :app:bundleRelease

# translation packs: key order, missing keys, specifier and escaping checks
python3 tools/i18n/generate.py
```

If the i18n generator reports MISSING/EXTRA/SPECIFIER MISMATCH, the English
`values/strings.xml` and the three TSV packs have drifted — fix the TSV, not the XML.
