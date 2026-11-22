#!/usr/bin/env bash
# Pull the FitRoom on-device try-on artifacts off a connected phone.
#
#   adb devices                      # confirm the phone is authorised
#   bash tools/vton_qa/pull_from_phone.sh
#
# Grabs everything TryOnDeviceE2ETest wrote to the app's external files dir and
# drops it in tools/vton_qa/out/phone/ so you can open the result on your machine.
set -euo pipefail

PKG="${PKG:-com.fabricfit.app.debug}"
REMOTE="/sdcard/Android/data/${PKG}/files/vton-e2e"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST="${HERE}/out/phone"

adb shell ls "$REMOTE" >/dev/null 2>&1 || {
  echo "No artifacts at $REMOTE" >&2
  echo "Run the test first:" >&2
  echo "  cd FabricFit && ./gradlew :app:connectedDebugAndroidTest \\" >&2
  echo "    -Pandroid.testInstrumentationRunnerArguments.class=com.fabricfit.app.data.repository.TryOnDeviceE2ETest" >&2
  exit 1
}

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
