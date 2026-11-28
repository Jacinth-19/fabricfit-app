#!/usr/bin/env bash
# Create the FabricFit upload key - the one key that must never enter this repository.
#
# Play ties an app to the first signing key it ever sees (or, with Play App Signing, to the
# upload key it pinned), and that binding cannot be undone. A key generated inside CI or
# committed to Git is a key every reader of this repository holds, so the key is created
# here, on your machine, once.
#
#   bash FabricFit/tools/release/new_upload_key.sh
#
# Then copy the four printed lines into ~/.gradle/gradle.properties (local releases) and
# into the repository secrets KEYSTORE_BASE64 / KEYSTORE_PASSWORD / KEY_ALIAS /
# KEY_PASSWORD (CI releases).
set -euo pipefail

ALIAS="${KEY_ALIAS:-fabricfit}"
OUT="${KEYSTORE_FILE:-$HOME/fabricfit-upload.jks}"
DAYS="${KEY_DAYS:-10000}"   # Play requires a validity date past 22 Oct 2033.

fail() { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }

# JDK 21 is what this project builds with, so it is the JDK most likely already installed.
if command -v keytool >/dev/null 2>&1; then
    KEYTOOL=keytool
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
    KEYTOOL="$JAVA_HOME/bin/keytool"
else
    fail "keytool not found. Install a JDK (21 is what CI uses) or set JAVA_HOME."
fi

if [ -e "$OUT" ]; then
    fail "$OUT already exists. Refusing to overwrite an upload key - move it aside first."
fi

# A 32-character base64 password: no shell metacharacters, plenty of entropy, and the
# trailing '=' stripped so it survives being pasted into YAML, Gradle properties and URLs.
gen_password() { openssl rand -base64 32 | tr -d '\n=+/' | cut -c1-32; }
command -v openssl >/dev/null 2>&1 || fail "openssl not found; it is used to generate passwords."

STORE_PASSWORD="$(gen_password)"
KEY_PASSWORD="$(gen_password)"

printf 'Generating a 4096-bit RSA upload key (alias %s, valid %s days)...\n' "$ALIAS" "$DAYS"
"$KEYTOOL" -genkeypair \
    -keystore "$OUT" \
    -storetype PKCS12 \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -validity "$DAYS" \
    -dname "CN=FabricFit, OU=Mobile, O=FabricFit, L=Accra, C=GH" \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD"

# Prove the key is usable before telling anyone to depend on it, then leave only public
# information on disk: the certificate is what Play shows in the upload dialog.
"$KEYTOOL" -list -v -keystore "$OUT" -storepass "$STORE_PASSWORD" >/dev/null \
    || fail "The generated keystore could not be read back with its own password."
CERT_SHA256="$("$KEYTOOL" -list -v -keystore "$OUT" -alias "$ALIAS" -storepass "$STORE_PASSWORD" \
    | sed -n 's/^[[:space:]]*SHA256: //p' | head -1)"

BASE64_CONTENT="$(base64 <"$OUT" | tr -d '\n')"

cat <<EOF

Upload key created: $OUT
Certificate SHA-256: ${CERT_SHA256:-<not reported>}

--- ~/.gradle/gradle.properties (local release builds) ---------------------------
KEYSTORE_FILE=$OUT
KEYSTORE_PASSWORD=$STORE_PASSWORD
KEY_ALIAS=$ALIAS
KEY_PASSWORD=$KEY_PASSWORD

--- GitHub repository secrets (CI release builds) -------------------------------
KEYSTORE_BASE64  <- the single line below
$BASE64_CONTENT
KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD <- the three values above

--- Next steps -----------------------------------------------------------------
1. Back $OUT up somewhere you control that is NOT this repository. Play App Signing
   can reset a lost *upload* key, but losing it costs a support ticket and a wait.
2. Upload the certificate to Play Console > Setup > App signing. With Play App Signing
   enabled, Google holds the *app signing* key and this file is only the upload key.
3. Delete the block above from your shell history when you are done:
   history -d \$((HISTCMD-1)) 2>/dev/null || true
EOF
