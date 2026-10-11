#!/usr/bin/env python3
"""Prepare, submit, monitor and download a private Kaggle outfit-concept job.

Default: offline preparation only. --submit explicitly starts a GPU job.
Authenticate OUTSIDE this script using `kaggle auth login`. No token arguments,
credential files, secret prompts, repository upload or environment dumps are used.
"""
from __future__ import annotations

import argparse
from contextlib import redirect_stdout
from datetime import datetime, timezone
import hashlib
import io
import json
from pathlib import Path
import re
import time
import uuid

import outfit_job

HERE = Path(__file__).resolve().parent


class WorkflowError(RuntimeError):
    """Safe, locally-authored user guidance (never an SDK response)."""


class WaitExpired(WorkflowError):
    pass


def write_json(path: Path, value: dict) -> None:
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def notebook() -> dict:
    def cell(kind: str, source: str) -> dict:
        result = {"cell_type": kind, "metadata": {}, "source": source.splitlines(keepends=True)}
        if kind == "code":
            result.update(execution_count=None, outputs=[])
        return result

    install = (
        "import subprocess, sys\n"
        "# Keep Kaggle's CUDA-enabled PyTorch; install only the inference stack.\n"
        f"subprocess.run([sys.executable, '-m', 'pip', 'install', '--quiet', *{outfit_job.PACKAGES!r}], check=True)\n"
    )
    return {
        "nbformat": 4, "nbformat_minor": 4,
        "metadata": {"kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"},
                     "language_info": {"name": "python", "version": "3.11"}},
        "cells": [
            cell("markdown", "# Ntoma Studio — pretrained outfit concepts\n\n"
                 "**Inference only: no custom training, no photos or dataset upload.**\n\n"
                 "Requires Kaggle GPU access, Internet enabled and access to the public model host. "
                 "Uses the model's built-in safety checker. Review all results before app use.\n\n"
                 f"Model licence: {outfit_job.LICENSE_URL}\n\n"
                 "Outputs: eight requested concepts (flagged outputs are withheld), an HTML gallery "
                 "and a JSON report. Does not export model weights or change the Android app.\n"),
            cell("code", install),
            cell("code", (HERE / "outfit_job.py").read_text(encoding="utf-8")),
        ],
    }


def prepare(username: str, root: Path) -> Path:
    if not re.fullmatch(r"[a-zA-Z0-9_-]{3,50}", username):
        raise WorkflowError("Use your Kaggle username slug, not an email or profile URL.")
    suffix = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:8]
    slug = "ntoma-outfits-" + suffix
    run_dir = root / slug
    run_dir.mkdir(parents=True, exist_ok=False)
    upload = run_dir / "upload"
    upload.mkdir()
    write_json(upload / "outfit-concepts.ipynb", notebook())
    write_json(upload / "kernel-metadata.json", {
        "id": f"{username.lower()}/{slug}",
        "title": "Ntoma outfit concepts " + suffix,
        "code_file": "outfit-concepts.ipynb", "language": "python", "kernel_type": "notebook",
        "is_private": True, "enable_gpu": True, "enable_internet": True,
        "dataset_sources": [], "competition_sources": [], "kernel_sources": [], "model_sources": [],
    })
    write_json(run_dir / "state.json", {
        "schema_version": 1, "phase": "prepared", "kernel": f"{username.lower()}/{slug}",
        "task": "outfit_concept_inference", "custom_trained": False,
    })
    return run_dir / "state.json"


def read_state(path: Path) -> dict:
    state = json.loads(path.read_text(encoding="utf-8"))
    if state.get("schema_version") != 1 or state.get("task") != "outfit_concept_inference":
        raise WorkflowError("Not a supported Ntoma job state file.")
    if not re.fullmatch(r"[a-z0-9_-]{3,50}/ntoma-outfits-[a-z0-9-]+", state.get("kernel", "")):
        raise WorkflowError("Invalid job reference in state file.")
    if state.get("phase") not in {"prepared", "submitting", "submitted", "downloaded", "failed"}:
        raise WorkflowError("Invalid job phase.")
    return state


def submit(api, state_path: Path) -> dict:
    state = read_state(state_path)
    if state["phase"] != "prepared":
        raise WorkflowError("Job submission already attempted. Resume it; do not submit it again.")
    metadata = json.loads((state_path.parent / "upload/kernel-metadata.json").read_text())
    if (metadata.get("id") != state["kernel"] or metadata.get("is_private") is not True
            or metadata.get("code_file") != "outfit-concepts.ipynb"):
        raise WorkflowError("Upload metadata does not match the private job.")
    # Persist intent BEFORE the network call. If the connection drops after Kaggle
    # accepts the job, a resume will not submit a duplicate GPU run.
    state["phase"] = "submitting"
    write_json(state_path, state)
    with redirect_stdout(io.StringIO()):
        result = api.kernels_push(str(state_path.parent / "upload"), timeout=3600, acc="NvidiaTeslaT4")
    if result is None or result.error:
        raise WorkflowError("Kaggle did not confirm submission. Check the job page; do not blindly retry.")
    if not result.version_number:
        raise WorkflowError("Kaggle returned no version. Inspect the job page before trying anything else.")
    state.update(phase="submitted", version=int(result.version_number))
    write_json(state_path, state)
    return state


def version_ref(state: dict) -> str:
    version = state.get("version")
    if type(version) is not int or version < 1:
        raise WorkflowError("No confirmed version. Inspect the notebook in Kaggle; automatic resubmission is disabled.")
    return f'{state["kernel"]}/{version}'


def wait(api, ref: str, seconds: float, interval: float = 30,
         clock=time.monotonic, sleep=time.sleep) -> str:
    deadline = clock() + seconds
    previous = None
    while clock() < deadline:
        with redirect_stdout(io.StringIO()):
            response = api.kernels_status(ref)
        status = getattr(response.status, "name", str(response.status)).split(".")[-1].upper()
        if status != previous:
            print(f"Kaggle job: {status}", flush=True)
            previous = status
        if status == "COMPLETE":
            return status
        if status in {"ERROR", "CANCEL_REQUESTED", "CANCEL_ACKNOWLEDGED", "CANCELLED", "CANCELED"}:
            return status
        if status not in {"QUEUED", "RUNNING", "NEW_SCRIPT"}:
            raise WorkflowError("Unrecognised Kaggle status; inspect the notebook page.")
        sleep(max(0, min(interval, deadline - clock())))
    raise WaitExpired("Local wait expired. The remote job may still run; use --resume, not a new submission.")


def verify_outputs(directory: Path) -> Path:
    matches = list(directory.rglob("run-report.json"))
    if len(matches) != 1:
        raise WorkflowError("Expected one run-report.json in downloaded results.")
    path = matches[0]
    report = json.loads(path.read_text(encoding="utf-8"))
    if (report.get("completed") is not True or report.get("custom_trained") is not False
            or report.get("task") != "outfit_concept_inference"
            or report.get("safety_checker_enabled") is not True
            or len(report.get("images", [])) != len(outfit_job.PROMPTS)):
        raise WorkflowError("Downloaded report is incomplete or does not match the expected job.")
    saved = 0
    seen = set()
    for item in report["images"]:
        filename = item.get("file")
        if item.get("safety_blocked") is True and filename is None:
            continue
        if (item.get("safety_blocked") is not False or not isinstance(filename, str)
                or not re.fullmatch(r"outfit-0[1-8]\.png", filename) or filename in seen):
            raise WorkflowError("Invalid image entry in report.")
        seen.add(filename)
        image = path.parent / filename
        if image.is_symlink() or not image.is_file():
            raise WorkflowError("Missing image file.")
        if hashlib.sha256(image.read_bytes()).hexdigest() != item.get("sha256"):
            raise WorkflowError("Downloaded image checksum mismatch.")
        saved += 1
    index = path.parent / "index.html"
    if not index.is_file():
        raise WorkflowError("Missing output gallery.")
    print(f"Verified {saved} outfit images; {len(outfit_job.PROMPTS) - saved} withheld by safety checker.")
    return index


def download(api, ref: str, directory: Path) -> None:
    with redirect_stdout(io.StringIO()):
        api.kernels_output(ref, str(directory), force=True, quiet=True,
                           file_pattern=r"(^|/)(outfit-\d+\.png|run-report\.json|index\.html)$")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--username", help="Your Kaggle username (not a secret)")
    parser.add_argument("--work-dir", type=Path, default=HERE / "runs")
    parser.add_argument("--submit", action="store_true", help="Start a private GPU job and wait/download")
    parser.add_argument("--accept-model-license", action="store_true", help="Confirm you reviewed the model licence")
    parser.add_argument("--resume", type=Path, help="Resume prepared/submitted state.json without making a duplicate job")
    parser.add_argument("--wait-seconds", type=int, default=7200)
    args = parser.parse_args(argv)
    if args.wait_seconds < 1:
        parser.error("--wait-seconds must be positive")
    if args.submit and not args.accept_model_license:
        parser.error(f"Review {outfit_job.LICENSE_URL}, then pass --accept-model-license to submit.")
    if args.resume:
        if args.username:
            parser.error("Do not use --username with --resume")
        state_path = args.resume.resolve()
    else:
        if not args.username:
            parser.error("--username is required for a new job")
        state_path = prepare(args.username, args.work_dir.resolve())
    state = read_state(state_path)
    print(f"State: {state_path}")
    print(f"Notebook: https://www.kaggle.com/code/{state['kernel']}")
    if state["phase"] == "prepared" and not args.submit:
        print("Prepared offline only. No job submitted, GPU used, or credentials read.")
        print("Run again with --resume STATE_PATH --submit --accept-model-license when ready.")
        return 0
    if state["phase"] == "submitting":
        raise WorkflowError("Previous submission outcome is uncertain. Inspect Kaggle before continuing; no automatic retry.")
    if state["phase"] == "downloaded":
        print(f"Gallery: {verify_outputs(state_path.parent / 'outputs')}")
        return 0
    # Import only after explicit submit/resume. Kaggle's SDK uses its documented
    # OAuth or environment authentication, not a credential copied into a notebook.
    try:
        from kaggle import api as base_api
        from kaggle_client import VersionedKaggle
        api = VersionedKaggle(base_api)
    except (ImportError, SystemExit):
        raise WorkflowError("Install requirements-launcher.txt and run `kaggle auth login` on your own computer.") from None
    if state["phase"] == "prepared":
        state = submit(api, state_path)
    ref = version_ref(state)
    print(f"Tracking version: {ref}", flush=True)
    status = wait(api, ref, args.wait_seconds)
    output = state_path.parent / "outputs"
    if status != "COMPLETE":
        state.update(phase="failed", remote_status=status)
        write_json(state_path, state)
        try:
            download(api, ref, output)
        except Exception:
            print("Could not download partial outputs; inspect the notebook page.")
        raise WorkflowError("Kaggle job failed or was cancelled. No new run was submitted.")
    download(api, ref, output)
    gallery_path = verify_outputs(output)
    state.update(phase="downloaded", remote_status=status)
    write_json(state_path, state)
    print(f"Gallery: {gallery_path}")
    print("This is pretrained inference only. No custom model or Android app was changed.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("Stopped waiting locally. Remote work may continue; resume the saved state, not a new job.")
        raise SystemExit(130)
    except WorkflowError as exc:
        print(str(exc))
        raise SystemExit(1)
    except Exception:
        print("Kaggle request failed. Check authentication, network, GPU quota and the saved job state. No automatic resubmission.")
        raise SystemExit(1)
