# Production signing

`app/demo-release.keystore` is public and uses credentials committed in the build
configuration. It is deliberately convenient for CI and disposable tester APKs;
it is **not** a production upload key. Never upload a demo-signed APK/AAB to
Google Play, and never share a new private key in Git, issues, pull requests, or
chat.

## 1. Create the upload key outside the repository

A Google Play upload-key administrator should generate the key on a trusted
machine and store it in the organisation's password manager or KMS/HSM. Example:

```bash
keytool -genkeypair -v \
  -keystore /secure/path/fabricfit-upload.keystore \
  -alias fabricfit-upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Keep independent, offline backups. Losing an upload key can require a Google Play
key-reset request. The app-signing key managed by Google Play is separate from
this upload key.

## 2. Configure protected GitHub secrets

Set these repository or organisation secrets; they must never be written to
tracked files:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 /secure/path/fabricfit-upload.keystore` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | `fabricfit-upload` |
| `KEY_PASSWORD` | Key password |

The CI release job decodes the key only inside the ephemeral runner. Gradle reads
its path and credentials from environment variables; the key is not uploaded as
an artifact.

## 3. Enforce production mode

Set the GitHub repository variable `PRODUCTION_RELEASE` to `true` when real Play
artifacts are required. The release job then fails before compilation if any of
the four signing secrets is absent, preventing an accidental demo-signed
production release.

Use a normal development/test configuration with the variable unset or `false`
for disposable builds. GitHub release notes identify artifacts as `demo signed`
or `production signed`.

## 4. Verify before Play upload

1. Confirm the CI run used `production` signing and no demo-key warning appears.
2. Download the AAB from the intended version-tagged release and compare its
   provenance with the tagged source commit.
3. Increment `versionCode` and `versionName`; never replace an already published
   Play artifact.
4. Upload to an internal testing track first and verify upgrade installation over
   the preceding production version.
5. Follow [Play Data Safety](PLAY_DATA_SAFETY.md) and the normal Play review
   checklist before promoting the release.

Local builds can alternatively supply `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD` through environment variables or private Gradle
properties outside version control.
