#!/usr/bin/env bash
# Pull the FitRoom on-device try-on artifacts off a connected phone.
#
#   adb devices                      # confirm the phone is authorised
#   bash tools/vton_qa/pull_from_phone.sh
#
# Grabs everything TryOnDeviceE2ETest published to shared Downloads and drops it in
# tools/vton_qa/out/phone/ so you can open the result on your machine. The test writes
# through MediaStore because Gradle uninstalls the app (and its private data) afterwards.
set -euo pipefail

PKG="${PKG:-com.fabricfit.app.debug}"
# Shared storage survives the post-test uninstall; the old app-private path is a fallback
# for builds that still wrote there.
REMOTE="${VTON_DIR:-/sdcard/Download/FabricFit/vton-e2e}"
LEGACY="/sdcard/Android/data/${PKG}/files/vton-e2e"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST="${HERE}/out/phone"

if ! adb shell ls "$REMOTE" >/dev/null 2>&1; then
  # Try the legacy app-private dir, then give up with instructions.
  if adb shell ls "$LEGACY" >/dev/null 2>&1; then
    REMOTE="$LEGACY"
  else
    echo "No artifacts at $REMOTE or $LEGACY" >&2
    echo "Run the test first:" >&2
    echo "  cd FabricFit && ./gradlew :app:connectedDebugAndroidTest \\" >&2
    echo "    -Pandroid.testInstrumentationRunnerArguments.class=com.fabricfit.app.data.repository.TryOnDeviceE2ETest" >&2
    exit 1
  fi
fi

mkdir -p "$DEST"
adb pull "$REMOTE/." "$DEST"
echo
echo "Artifacts in $DEST"
ls -lh "$DEST"

if command -v python3 >/dev/null 2>&1; then
  python3 - "$DEST" <<'PY' || true
import sys
from pathlib import Path
d = Path(sys.argv[1])
report = d / "report.txt"
if report.exists():
    print("\n" + report.read_text())
PY
fi
