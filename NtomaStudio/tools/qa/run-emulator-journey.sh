#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-artifacts
adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put system screen_off_timeout 600000
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell cmd statusbar expand-notifications
adb shell cmd statusbar collapse
# Compile before recording so footage contains the app, not Gradle waiting.
bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest
# Actual emulator capture; no simulated app screens.
adb shell screenrecord --size 1080x1920 --bit-rate 4000000 --time-limit 180 /sdcard/ntoma-journey.mp4 &
recording_pid=$!
set +e
bash gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ntoma.studio.FabricJourneyDeviceTest --stacktrace 2>&1 | tee qa-artifacts/device-diagnostics.txt
result=${PIPESTATUS[0]}
set -e
adb shell pkill -2 screenrecord || true
wait "$recording_pid" || true
adb pull /sdcard/ntoma-journey.mp4 qa-artifacts/demo-raw.mp4 || true
adb pull /sdcard/Android/data/com.ntoma.studio.debug/files/journey/. qa-artifacts/ || true
adb logcat -d > qa-artifacts/logcat.txt
exit "$result"
