#!/usr/bin/env bash
# Retry wrapper for capture_fabricfit_screens.sh.
#
# The android-emulator-runner `script` input executes ONE command per line
# (it trims, drops comments, and splits on newlines), so any multi-line shell
# logic must live in a real script file instead.
#
# The emulator occasionally fails to boot cleanly on first attempt; each
# attempt here starts from a clean app state because the capture script
# runs `pm clear` at the start.
set -uo pipefail

OUT_DIR="../qa-artifacts/screens"

rm -rf "$OUT_DIR"

attempt=1
while [ "$attempt" -le 3 ]; do
  if bash ../tools/qa/capture_fabricfit_screens.sh "$OUT_DIR" > /tmp/cap.log 2>&1; then
    break
  fi
  echo "Capture attempt $attempt failed"
  {
    echo "--- attempt $attempt ---"
    tail -5 /tmp/cap.log
  } >> /tmp/capfails.log
  rm -rf "$OUT_DIR"
  adb shell am force-stop com.fabricfit.app.debug 2>/dev/null || true
  adb shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 10
  attempt=$((attempt + 1))
done

# Total capture failure: surface the last error lines as annotations so they
# are visible on the public run page (job logs require sign-in). Only emitted
# when nothing was captured, so successful runs keep a clean annotation
# stream.
if [ ! -s "$OUT_DIR/home.png" ] && [ -f /tmp/capfails.log ]; then
  while IFS= read -r l; do
    echo "::error::capture-fail: $l"
  done < /tmp/capfails.log
fi

test -s "$OUT_DIR/home.png"