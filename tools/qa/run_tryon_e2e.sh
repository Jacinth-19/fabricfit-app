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
# The test writes into the app's private files dir (always writable). `adb pull` cannot
# read it, but `adb exec-out run-as` can, because the debug build is debuggable.
APP_PACKAGE="com.fabricfit.app.debug"
DEVICE_DIR="/data/data/$APP_PACKAGE/files/vton-e2e"
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

echo "== run-as listing $DEVICE_DIR ==" | tee -a "$LOG"
LISTING=$(adb shell run-as "$APP_PACKAGE" ls -la "$DEVICE_DIR" 2>&1 || true)
echo "$LISTING" >> "$LOG"
echo "$LISTING"
echo "::notice title=Device files::$(echo "$LISTING" | tr '\n' ' ' | head -c 700)"
echo "::notice title=adb identity::$(adb shell id 2>&1 | head -1) | $(adb shell getprop ro.build.version.sdk 2>&1 | head -1)"

# Copy each artifact out one by one. `run-as cat` keeps the bytes exact; busybox tar is
# not present on every emulator image, so no archive round-trip.
for name in person_input.jpg person_mask.png fabric_swatch.jpg tryon_result.jpg report.txt; do
  if adb exec-out run-as "$APP_PACKAGE" cat "$DEVICE_DIR/$name" > "$OUT_DIR/$name" 2>>"$LOG"; then
    size=$(stat -c%s "$OUT_DIR/$name" 2>/dev/null || echo 0)
    echo "pulled $name ($size bytes)"
    echo "::notice title=Pulled::$name $size bytes"
    [ "$size" -gt 0 ] || rm -f "$OUT_DIR/$name"
  fi
done
# Fall back to a plain pull for any build that still writes to shared storage.
for device_dir in /sdcard/Download/FabricFit/vton-e2e /sdcard/Android/data/$APP_PACKAGE/files/vton-e2e; do
  timeout 60 adb pull "$device_dir/." "$OUT_DIR/" >> "$LOG" 2>&1 || true
done
ls -la "$OUT_DIR" || true
# An empty artifact folder is a silent failure; say so loudly.
if ! ls "$OUT_DIR"/*.jpg >/dev/null 2>&1; then
  echo "::warning title=No device images::the test wrote nothing to the device output dir"
fi

exit "$status"
