#!/usr/bin/env bash
# Run the instrumented suite on the attached emulator and keep the output.
#
# Called by the device-qa job. It is a script rather than an inline snippet because the
# runner executes `script:` with /bin/sh, where `set -o pipefail` is not a valid option -
# that produced an immediate exit 2 and no log at all, which looked like an emulator
# failure. Keeping the real logic here also makes it runnable by hand.
set -euo pipefail

cd "$(dirname "$0")/../.."          # -> FabricFit/
LOG="${INSTRUMENTATION_LOG:-instrumentation.log}"

echo "Running connectedDebugAndroidTest on $(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || echo 'unknown') ..."

# 30 minutes: a wedged instrumentation otherwise hangs until the runner's own limit and
# reports nothing useful. pipefail keeps gradle's exit code through the tee.
set -o pipefail
timeout 30m ./gradlew :app:connectedDebugAndroidTest --console=plain 2>&1 | tee "$LOG"
