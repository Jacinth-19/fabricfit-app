# Unattended Kaggle observation

## What is built

`.github/workflows/monitor-kaggle.yml` runs a version-pinned observer on a GitHub
runner. It is **not** an always-on assistant and does not submit, modify, cancel,
restart or repair training jobs. It can run while your laptop is off.

- Polls an **explicit** `owner/notebook/version`, not whichever version is latest.
- Records observed queue/running/complete/error states and observer wall time.
- Retries temporary status API errors at a bounded interval; five consecutive
  errors fail the observer rather than silently hanging.
- Returns a failing Actions result for remote failure, download failure or timeout.
- Downloads only named model/report outputs (max 16 files / 32 MiB), when available.
- Always attempts to upload the observer report/artifacts, retained for seven days.
- Never re-submits a GPU job after a lost connection. Re-running the observer with
  the same version is safe and does not consume another training allocation.

**Not yet provided:** training submission for the two new classifiers, live epoch
metrics, exact GPU/queue duration, automatic diagnosis/code repair, or unbounded
monitoring. Training scripts must emit `epoch-times.csv`, `training-report.json`
and error-stage reports to enable detailed diagnosis; a failed Kaggle job may not
make outputs available, in which case its private notebook log is needed.

The observer times its own checks, not the instant Kaggle actually started. Its
configurable 1–300-minute window includes waiting. A timeout does not cancel the
remote Kaggle job; repeat observation of the same version if necessary.

## One-time secure setup (repository owner)

This Arena sandbox cannot reach Kaggle's API. The Kaggle CLI does not bypass that
network restriction. GitHub Actions is the internet-connected execution location.
Current GitHub integration access returns HTTP 403 for repository-secret management,
so the agent cannot create or inspect those secrets for you.

1. In Kaggle account settings, generate an API token. If one was previously pasted
   into chat, revoke that token. Do not paste its replacement into chat.
2. Open **GitHub → Jacinth-19/ntoma-studio → Settings → Secrets and variables →
   Actions → New repository secret**. Name it **`KAGGLE_API_TOKEN`** and paste its
   value there, not in a workflow file, variable, command argument or message.
3. Keep the token valid for the full observation window. After the final job and
   output download, revoke it in Kaggle and delete the repository secret if it was
   intended to be temporary. GitHub masking does not make a leaked token safe.
4. Provide only the **Kaggle username and saved notebook/version URL** in chat.
   Those identify the job; they are not credentials. A private notebook remains
   private on Kaggle, but selected model outputs become Actions artifacts in this
   public repository. Do not use this public workflow for sensitive datasets,
   personal photos or reports containing secrets. Use a private repository instead.
5. Enable GitHub Actions failure notifications in your account if you want an email
   when the observer fails. No separate email/SMS notification system is installed.

Kaggle's supported `KAGGLE_API_TOKEN` environment authentication is documented in
[the official CLI documentation](https://github.com/Kaggle/kaggle-cli/blob/main/docs/README.md).
Credentials are scoped to the remote-observation step, not passed into Kaggle notebook
code, logged, or uploaded with artifacts. Checkout credentials are not persisted.

## Starting observation

Once a **saved training version actually exists**, run the workflow on this session's
branch with the explicit reference:

```bash
gh workflow run monitor-kaggle.yml --ref arena/5b96a1cd-ntoma-studio \
  -f kernel_version=YOUR_USERNAME/YOUR_NOTEBOOK/1 -f minutes=240
```

GitHub may require a workflow to exist on the default branch before manual dispatch
is offered. The workflow also registers/runs offline tests on relevant working-branch
pushes. If dispatch is unavailable, the owner can set the **nonsecret repository
variable** `KAGGLE_KERNEL_VERSION` to the actual `owner/notebook/version`; a subsequent
push changing the monitor files triggers observation on this branch. It skips remote
observation when no version is configured. No training job is started by a push.

For an internet-connected computer instead of Actions:

```bash
python -m pip install -r NtomaStudio/tools/kaggle_outfits/requirements-launcher.txt
kaggle auth login
python NtomaStudio/tools/dataset/monitor_kaggle.py \
  --ref YOUR_USERNAME/YOUR_NOTEBOOK/1 --minutes 240
```

Tests (no Kaggle package, API access or credentials required):

```bash
python -m unittest discover -s NtomaStudio/tools/dataset -p test_monitor_kaggle.py -v
```

## Current status

Built and tested offline and in GitHub Actions. **No Kaggle training job has been
submitted or monitored yet.** No token supplied in chat was used. The Actions
observer test passed; its remote monitoring job correctly skipped because no saved
Kaggle version was configured. See the dataset acquisition report for training-data
readiness; public search hits are not automatically approved training labels.
