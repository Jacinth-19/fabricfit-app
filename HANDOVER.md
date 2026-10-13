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
`ntomastudio` password fix in `BUILD.md` and `docs/PLAY_DATA_SAFETY.md`. Check those
files against `main` and re-commit as needed.

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
| Gradle / AGP / Kotlin | 8.9 / 8.5.2 / 2.0.20, compileSdk + targetSdk 34, minSdk 24, versionCode 1 / versionName 1.0.0 |

CI runs on every PR and every push to `main`: `verify` (unit tests, debug APK,
androidTest compile) then `release` (R8 APK + Play bundle). The macOS emulator job
runs weekly (Mon 06:00), on `v*` tags, or manually.

## 1. Gradle 8.9 is flagged out of date (low risk, do it deliberately)

CI warns on every run:
`Gradle 8.9 is out of date: consider updating to the latest Gradle version.`

Bumping Gradle means matching AGP and Kotlin. As of writing the repo pins
**AGP 8.5.2 / Kotlin 2.0.20 / KSP 2.0.20-1.0.25**. Check the compatibility tables
before moving:

- AGP ↔ Gradle: <https://developer.android.com/build/releases/gradle-plugin>
- Kotlin ↔ AGP ↔ Compose compiler: <https://kotlinlang.org/docs/compose-compiler-migration-guide.html>
  (Kotlin 2.x ships the Compose compiler in the Kotlin plugin, so the
  `kotlin.compose` plugin in `app/build.gradle.kts` must stay in step with Kotlin.)
- KSP must match Kotlin exactly (`2.0.20-1.0.25` → `<kotlin>-<ksp>`).

Also add `gradle/actions/setup-gradle` caching still works after the bump, and update
the pinned `gradle-8.9-bin.zip` in `FabricFit/gradle/wrapper/gradle-wrapper.properties`.
`compileSdk`/`targetSdk` 34 will also need a bump against Play's current target-API
deadline (see `docs/PLAY_DATA_SAFETY.md`, which already flags this).

## 2. Play Store listing for `com.fabricfit.app`

`store/listing.md` has the short and full descriptions, categorisation and the
screenshots checklist. Still to do:

- **No phone screenshots exist yet.** `store/` holds only
  `play_icon_512.png`, `feature_graphic.png`, `listing.md` and `privacy_policy.html`.
  Capture Home → FitScan → FitMatch AI → FitRoom → My Fit Closet. An emulator can do this
  now that Actions can run the macOS job, or use a physical device.
- **Host the privacy policy** at a public URL and paste `store/privacy_policy.html`
  (contact address in it is `privacy@fabricfit.app`, currently a placeholder).
- **The application ID changed**, so this is a *new* Play listing. An existing
  `com.ntoma.studio` install cannot upgrade in place, and the old listing cannot be
  transferred — do not expect install continuity.
- Add localized listings once tw/gaa/ee store copy exists; keep the feature names in
  English, translate the surrounding sentences (same rule as the in-app strings).

## 3. Release signing (must do before any real upload)

`FabricFit/app/demo-release.keystore` is a **throwaway demo key** with alias `ntoma`
and store/key password `ntomastudio`. These are baked into the keystore binary, so
the rename deliberately left them alone — editing `build.gradle.kts` alone would break
signing. (Two docs used to claim the password was `ntomademo`; that was wrong and has
been corrected — CI signs successfully with `ntomastudio`.)

Generate a real upload key, keep it out of the repo, and switch `signingConfigs.release`
to it (or use Play App Signing and upload with a new key). Play will reject the demo key.

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
lower-case `ntoma` (the Akan word for cloth, live in the Twi strings) or the demo
keystore's `ntoma`/`ntomastudio` credentials.

## 5. Smaller follow-ups

- **Instrumented emulator test has never actually run.** The weekly schedule will
  exercise it; check the first Monday run and fix whatever it surfaces (DeepLabV3
  delegate loading on `macos-latest` is the likely friction point).
- **The Android SDK / JDK toolchain script** is `tools/setup_toolchain.sh` at the repo
  root. It still assumes the module path is `/home/user/FabricFit`, which is correct,
  but it downloads from `api.adoptium.net`, `services.gradle.org` and `dl.google.com` —
  all of which are blocked from the Arena sandbox. Build in CI, not in the sandbox.
- **Dead route constant.** `Routes.ONBOARDING` in
  `ui/navigation/Destinations.kt` is declared but never referenced — onboarding is a
  state gate in `AppShell` (`onboardingCompleted`), and Settings' "Replay onboarding"
  re-arms that flag rather than navigating. Either wire it up or delete the constant.
- **The README now documents all 29 screens** (map + tables + screenshot plan). The
  screenshot plan is still just a plan: capture the five shots listed in
  `docs/PLAY_DATA_SAFETY.md` and drop them in `store/screenshots/`.
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
