#!/usr/bin/env bash
# Run Gradle, retrying when the failure is the dependency *download*, not the code.
#
# CI has been failing on HTTP 429 from repo.maven.apache.org ("Too Many Requests") while
# resolving org.robolectric:nativeruntime-dist-compat. That is a shared-IP rate limit on
# Maven Central, not a problem with this repository: the same commit passes on a re-run.
# A red pipeline that a re-run fixes teaches people to ignore red, which is expensive once
# it hides a real regression - so the retry is explicit, narrow, and loud.
#
# Only resolution failures are retried. A compile error or a failing test fails immediately
# and identically, because retrying those would waste ten minutes to reach the same result.
#
#   tools/qa/gradle_retry.sh :app:testDebugUnitTest :app:assembleDebug
#
# Env: GRADLE_RETRY_ATTEMPTS (default 3), GRADLE_RETRY_LOG (default gradle-retry.log)
set -uo pipefail

MAX_ATTEMPTS="${GRADLE_RETRY_ATTEMPTS:-3}"
# Default to the name the annotation steps in build.yml read. A rename here once left the
# annotations pointing at a file that no longer existed, so a real failure reported only
# "Process completed with exit code 1".
LOG="${GRADLE_RETRY_LOG:-verify-gradle.log}"

if [ "$#" -eq 0 ]; then
  echo "usage: $(basename "$0") <gradle task>... [-- extra gradle args]" >&2
  exit 2
fi

# Drop a literal `--` separator: it is convenient to write and it must not reach Gradle,
# where it would be interpreted as "end of options".
args=()
for arg in "$@"; do
  [ "$arg" = "--" ] && continue
  args+=("$arg")
done
set -- "${args[@]}"

# Patterns that mean "the network failed", never "the code is wrong".
TRANSIENT=(
  'Received status code 429'
  'Received status code 5[0-9][0-9]'
  'Could not GET .*maven'
  'Could not download .*\\.(jar|aar|pom)'
  'Read timed out'
  'Connection reset'
  'Remote host terminated the handshake'
)

is_transient() {
  local pattern
  for pattern in "${TRANSIENT[@]}"; do
    grep -qE "$pattern" "$LOG" && return 0
  done
  return 1
}

attempt=1
while [ "$attempt" -le "$MAX_ATTEMPTS" ]; do
  echo "--- gradle attempt ${attempt}/${MAX_ATTEMPTS}: $* ---"
  if ./gradlew "$@" --console=plain 2>&1 | tee "$LOG"; then
    exit 0
  fi

  if ! is_transient; then
    echo "Failed for a reason that a retry cannot fix; not retrying." >&2
    exit 1
  fi

  if [ "$attempt" -eq "$MAX_ATTEMPTS" ]; then
    echo "::error title=Dependency download failed::Maven Central kept refusing requests after ${MAX_ATTEMPTS} attempts. This is infrastructure, not code - re-run the job." >&2
    exit 1
  fi

  backoff=$((attempt * 20))
  echo "::warning title=Retrying::Dependency download failed (attempt ${attempt}/${MAX_ATTEMPTS}); retrying in ${backoff}s. See the summary annotation for the matched error."
  echo "Retrying in ${backoff}s" >&2
  sleep "$backoff"
  attempt=$((attempt + 1))
done
