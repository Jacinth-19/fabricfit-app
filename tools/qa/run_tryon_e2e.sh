#!/usr/bin/env bash
# Runs the FitRoom try-on E2E on the connected emulator and collects its artifacts.
#
# The android-emulator-runner action executes its `script` input one command per line,
# so a multi-line Gradle invocation gets split and dies with "Bad substitution" or a
# truncated command. Anything with continuations belongs in a real script like this one.
# Invoke it as a single line:
#
#   bash ../tools/qa/run_tryon_e2e.sh
#
# Expects to be run from FabricFit/ (the action's working-directory) with an emulator
# already booted. Writes images and a log to qa-artifacts/vton/.
set -uo pipefail

# Run from FabricFit/ (the action's working-directory). The upload/report steps in
# build.yml look for both qa-artifacts/vton and FabricFit/qa-artifacts/vton.
OUT_DIR="../qa-artifacts/vton"
LOG="$OUT_DIR/gradle-e2e.log"
# The test prefers shared Downloads because adb cannot read /sdcard/Android/data on
# API 30+; the app-private dir is the fallback when Downloads is not writable.
DEVICE_DIRS="/sdcard/Download/FabricFit/vton-e2e /sdcard/Android/data/com.fabricfit.app.debug/files/vton-e2e"
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

# The test prints where it wrote; pulling that exact directory is more reliable than
# guessing, and it surfaces a path problem in the annotations.
OUT_HINT=$(grep -o 'VTON_OUT_DIR=[^ ]*' "$LOG" | tail -1 | cut -d= -f2- || true)
if [ -n "${OUT_HINT:-}" ]; then
  echo "device wrote to: $OUT_HINT"
  DEVICE_DIRS="/sdcard/Download/FabricFit/vton-e2e $OUT_HINT $DEVICE_DIRS"
  echo "::notice title=Device output dir::$OUT_HINT"
fi

# Pull whatever the run produced, even when it failed partway: a picture of a wrong
# render is more useful than no picture at all.
for device_dir in $DEVICE_DIRS; do
  echo "== adb pull $device_dir ==" >> "$LOG"
  timeout 120 adb pull "$device_dir/." "$OUT_DIR/" >> "$LOG" 2>&1 && break
done
ls -la "$OUT_DIR" || true
# An empty artifact folder is a silent failure; say so loudly.
if ! ls "$OUT_DIR"/*.jpg >/dev/null 2>&1; then
  echo "::warning title=No device images::the test wrote nothing to the device output dir"
fi

exit "$status"
