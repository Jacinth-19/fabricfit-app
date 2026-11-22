#!/usr/bin/env bash
# Runs the FitRoom try-on E2E on the connected emulator and collects its artifacts.
#
# The android-emulator-runner action executes its `script` input one command per line
# (comments dropped, lines split), so anything with continuations, pipes or `if` blocks
# has to live in a real script like this one. Invoke it as a single line:
#
#   bash ../tools/qa/run_tryon_e2e.sh
#
# Expects to be run from FabricFit/ (the action's working-directory) with an emulator
# already booted. Writes images and a log to qa-artifacts/vton/.
set -uo pipefail

OUT_DIR="../qa-artifacts/vton"
LOG="$OUT_DIR/gradle-e2e.log"
DEVICE_DIR="/sdcard/Android/data/com.fabricfit.app.debug/files/vton-e2e"
TEST_CLASS="com.fabricfit.app.data.repository.TryOnDeviceE2ETest"

mkdir -p "$OUT_DIR"
: > "$LOG"

{
  echo "== pwd =="
  pwd
  echo "== adb devices =="
  adb devices || true
} >> "$LOG" 2>&1

# Emulator builds need x86_64 TFLite natives; the release build ships ARM only.
export SCREENSHOT_ABI=1
chmod +x ./gradlew

echo "== running $TEST_CLASS ==" | tee -a "$LOG"
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS" \
  --console=plain >> "$LOG" 2>&1
status=$?

echo "== gradle tail =="
tail -n 40 "$LOG" || true

# Pull whatever the run produced, even when it failed partway: a picture of a wrong
# render is more useful than no picture at all.
timeout 180 adb pull "$DEVICE_DIR/." "$OUT_DIR/" >> "$LOG" 2>&1 || true
ls -la "$OUT_DIR" || true

exit "$status"
