#!/usr/bin/env python3
"""Bounded, version-pinned Kaggle observer. Never submits/restarts GPU jobs.

Use on an internet-connected machine or GitHub Actions. Credentials are read by
Kaggle from the environment; never copied into reports. Only safe status values
and selected model/report outputs are collected. No arbitrary remote log dump.
"""
from __future__ import annotations

import argparse
from contextlib import redirect_stdout, redirect_stderr
from datetime import datetime, timezone
import io
import json
import os
from pathlib import Path
import re
import sys
import time

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "kaggle_outfits"))
from kaggle_client import VersionedKaggle

ACTIVE = {"QUEUED", "RUNNING", "NEW_SCRIPT"}
FAILED = {"ERROR", "CANCEL_REQUESTED", "CANCEL_ACKNOWLEDGED", "CANCELLED", "CANCELED"}
# Adapter caps downloads at 16 files / 32 MiB. No notebook sources or arbitrary logs.
OUTPUTS = r"(^|/)(training-report\.json|run-report\.json|metrics\.json|labels\.json|model-contract\.json|confusion-matrix\.csv|epoch-times\.csv|fabric_kaggle\.tflite|pattern_kaggle\.tflite)$"


def validate_ref(ref):
    if not re.fullmatch(r"[a-zA-Z0-9_-]{3,50}/[a-zA-Z0-9_-]{3,100}/[1-9][0-9]*", ref):
        raise ValueError("Use owner/notebook-slug/version with an explicit positive version")
    return ref


def utc():
    return datetime.now(timezone.utc).isoformat()


def save(path, state):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(state, indent=2) + '\n')
    temporary.replace(path)


def observe(api, ref, report, seconds, interval=60, clock=time.monotonic, sleep=time.sleep):
    validate_ref(ref)
    start = clock()
    state = dict(kernel_version=ref, observer_started_at=utc(), events=[],
                 status='OBSERVING', elapsed_seconds=0, remote_cancelled_by_observer=False,
                 note='Elapsed is observer wall time, not exact Kaggle GPU or queue time. No automatic resubmission.')
    consecutive_errors = 0
    save(report, state)
    while clock() - start < seconds:
        try:
            with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
                response = api.kernels_status(ref)
            status = getattr(response.status, 'name', str(response.status)).split('.')[-1].upper()
            if status not in ACTIVE | FAILED | {'COMPLETE'}:
                raise ValueError('Unrecognised remote status')
            consecutive_errors = 0
        except Exception:
            # Never echo SDK errors: they may contain tokens or signed URLs.
            consecutive_errors += 1
            status = 'API_RETRY' if consecutive_errors < 5 else 'API_UNAVAILABLE'
        elapsed = round(clock() - start, 2)
        state.update(status=status, elapsed_seconds=elapsed, last_checked_at=utc())
        state['events'].append(dict(status=status, observed_at=state['last_checked_at'], elapsed_seconds=elapsed))
        save(report, state)
        print(f'Kaggle: {status}; observer elapsed {elapsed:.0f}s', flush=True)
        if status == 'COMPLETE':
            return 0, state
        if status in FAILED or status == 'API_UNAVAILABLE':
            return 1, state
        sleep(max(0, min(interval, seconds - (clock() - start))))
    state.update(status='MONITOR_TIMEOUT', elapsed_seconds=round(clock() - start, 2))
    save(report, state)
    return 2, state


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--ref', required=True)
    ap.add_argument('--out', type=Path, default=Path('.datasets/kaggle-monitor'))
    ap.add_argument('--minutes', type=int, default=240)
    ap.add_argument('--interval', type=int, default=60)
    args = ap.parse_args(argv)
    try:
        validate_ref(args.ref)
    except ValueError as e:
        ap.error(str(e))
    if not 1 <= args.minutes <= 300 or not 30 <= args.interval <= 300:
        ap.error('Use 1–300 minutes and 30–300 second polling intervals')
    args.out.mkdir(parents=True, exist_ok=True)
    report = args.out / 'monitor-report.json'
    try:
        with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
            from kaggle.api.kaggle_api_extended import KaggleApi
            raw = KaggleApi()
            raw.authenticate()
        api = VersionedKaggle(raw)
    except Exception:
        save(report, dict(kernel_version=args.ref, status='AUTH_OR_CONNECTION_ERROR',
                          note='Check the runner secret and network. No job submitted or cancelled.'))
        print('Kaggle authentication/connection failed. Check runner setup; no credentials are logged.')
        return 1
    code, state = observe(api, args.ref, report, args.minutes*60, args.interval)
    if state['status'] == 'COMPLETE' or state['status'] in FAILED:
        try:
            with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
                files = api.kernels_output(args.ref, str(args.out/'outputs'), file_pattern=OUTPUTS,
                                           force=True, quiet=True)
            state['downloaded_files'] = [str(Path(p).relative_to(args.out)) for p in files]
            state['output_status'] = 'SELECTED_OUTPUTS_DOWNLOADED' if files else 'NO_MATCHING_OUTPUTS'
        except Exception:
            state['output_status'] = 'OUTPUT_DOWNLOAD_FAILED'
            code = 1
        save(report, state)
    summary = ('# Kaggle observer\n\n'
               f"Version: `{args.ref}`\n\nStatus: **{state['status']}**\n\n"
               f"Observed for {state['elapsed_seconds']:.0f} seconds. "
               'This is not exact GPU execution time.\n\n'
               'No job was submitted, restarted or cancelled. A monitor timeout does not stop Kaggle. '
               'Run the observer again with the same version to continue. '
               'Failed-run details may require the private Kaggle notebook page.\n')
    (args.out/'summary.md').write_text(summary)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as out:
            out.write(summary)
    print(summary)
    return code


if __name__ == '__main__':
    raise SystemExit(main())
