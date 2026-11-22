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
# The test publishes to public Downloads through MediaStore; Gradle uninstalls the app
# after the run, so anything left in its private dir would be deleted with it.
DEVICE_DIR="/sdcard/Download/FabricFit/vton-e2e"
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

echo "== listing $DEVICE_DIR ==" | tee -a "$LOG"
LISTING=$(adb shell ls -la "$DEVICE_DIR" 2>&1 || true)
echo "$LISTING" >> "$LOG"
echo "$LISTING"
echo "::notice title=Device files::$(echo "$LISTING" | tr '\n' ' ' | head -c 700)"

# Pull the folder. It survives the post-test uninstall because it is shared storage.
timeout 120 adb pull "$DEVICE_DIR/." "$OUT_DIR/" >> "$LOG" 2>&1 || true
for f in "$OUT_DIR"/*.jpg "$OUT_DIR"/*.png; do
  [ -f "$f" ] && echo "::notice title=Pulled::$(basename "$f") $(stat -c%s "$f") bytes"
done
ls -la "$OUT_DIR" || true
# An empty artifact folder is a silent failure; say so loudly.
if ! ls "$OUT_DIR"/*.jpg >/dev/null 2>&1; then
  echo "::warning title=No device images::the test wrote nothing to the device output dir"
fi

exit "$status"
