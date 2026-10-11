# Automated Kaggle outfit-concept prototype

**Built, not remotely run.** Generates outfit concepts with a pretrained model. This
is inference, **not custom model training**, fabric recognition, app integration or
an always-on backend. It needs no personal photos and uploads no dataset.

## Security first

If a token has been pasted into chat, revoke it in Kaggle account settings. Do not
paste a replacement into chat, source files, notebooks or command-line arguments.
The launcher supports the official Kaggle CLI's authentication; browser-based OAuth
is preferable: `kaggle auth login`. This workflow never embeds credentials in a notebook.
Do not share your `.kaggle` directory, access-token files or environment files.

This Arena session has no Kaggle connector and its outbound API network allowlist
does not include Kaggle or Hugging Face. A credential cannot lift that restriction.
Run the launcher on a computer that can access them, or use the browser path below.
No token supplied in chat was used or copied into this implementation.

## Easiest path: upload the notebook (no API token needed)

1. Sign in to Kaggle and create a Notebook. Use its import/upload option to upload
   **`outfit-concepts.ipynb`** from this directory.
2. Keep the notebook **private**, enable **GPU** and **Internet** in its settings.
   Kaggle account verification, GPU availability/quota and accepting relevant terms
   may require action in your account; this code cannot bypass them.
3. Review the [model licence](https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-v1-5/blob/main/LICENSE).
   Save a version using **Save & Run All** (the wording may vary by Kaggle UI).
   This starts a saved background run; it is different from just executing cells
   interactively in a browser session.
4. When complete, download the notebook's `ntoma-outfits` outputs. Open `index.html`
   with its PNG files alongside it. Share the output folder/ZIP for review.

The job requests eight concepts: dresses, suits, co-ords, an ensemble, a tunic outfit
and a jumpsuit. Outputs flagged by the safety checker are withheld. Review quality,
construction feasibility and cultural details; these are ideas, not sewing patterns
or authenticated Ghanaian designs. No promise of particular visual quality is made.

## One-command remote submission, waiting and download

One-time setup on your computer (Python **3.11+**, run from the repository root):

```bash
python3 -m venv .venv
# macOS/Linux:
source .venv/bin/activate
# Windows PowerShell instead: .venv\Scripts\Activate.ps1
python -m pip install -r NtomaStudio/tools/kaggle_outfits/requirements-launcher.txt
kaggle auth login
```

After reviewing the model licence, replace `YOUR_USERNAME` with your Kaggle username
(not your display name or an API token), then run:

```bash
python NtomaStudio/tools/kaggle_outfits/launch.py --username YOUR_USERNAME --submit --accept-model-license
```

This creates a uniquely named **private** notebook, submits a GPU run with a one-hour
remote runtime limit, polls until completion (up to two hours locally including queue
time), downloads only the selected artifacts, and verifies image checksums. The local
computer must remain on and connected to monitor/download; Kaggle executes the remote job.
It does not publish the notebook, alter an existing notebook, or change the Android app.
GPU usage is charged against your Kaggle account quota; no paid hosting is provisioned.

The official CLI documentation covers OAuth and the kernel submission fields/commands.
[1](https://github.com/Kaggle/kaggle-cli/blob/main/docs/README.md)
[1](https://github.com/Kaggle/kaggle-cli/blob/main/docs/kernels_metadata.md)
[1](https://github.com/Kaggle/kaggle-cli/blob/main/docs/kernels.md)

### Offline preparation only

Omit `--submit` to produce an upload bundle without importing Kaggle or reading credentials:

```bash
python NtomaStudio/tools/kaggle_outfits/launch.py --username YOUR_USERNAME
```

Output lives in the gitignored `runs/<unique-job>/`:

```text
upload/kernel-metadata.json    private notebook metadata (no credentials)
upload/outfit-concepts.ipynb   self-contained notebook
state.json                    submission/version/progress (no credentials)
outputs/ntoma-outfits/         after a successful remote run
  outfit-01.png ...           up to eight images, subject to safety filtering
  index.html                 offline gallery
  run-report.json            prompts, seeds, model revision, package versions, SHA-256
```

### Resume without starting another GPU job

Use the **exact saved state path** printed by the launcher:

```bash
python NtomaStudio/tools/kaggle_outfits/launch.py --resume PATH_TO_STATE_JSON
```

For an offline-prepared job that has never been submitted, add `--submit
--accept-model-license`. A submitted job is only monitored/downloaded. Interrupting
local waiting **does not cancel remote work**. If submission was interrupted before
Kaggle confirmed it, the state is `submitting`: the launcher refuses automatic retries.
Inspect the uniquely named notebook in Kaggle before doing anything else. Do not delete
the state and rerun blindly. Revoking credentials also should not be treated as a way
to cancel an already-running job; manage the run in Kaggle.

## Technical constraints and verification

- Uses Stable Diffusion v1.5 as a modest initial baseline, not a fashion specialist.
  The model card identifies the CreativeML OpenRAIL-M licence; use is subject to its
  restrictions. [1](https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-v1-5/blob/main/README.md)
- Model weights download inside Kaggle from Hugging Face. They are not bundled,
  exported or downloaded by this launcher. Internet/model access can fail remotely.
- Resolves and records a model commit per run; dependency versions are pinned.
  Kaggle's installed CUDA PyTorch is retained. A repeat run on different hardware or
  environment is not guaranteed bit-identical.
- Float16, 512×512, 25 steps, one image at a time; built-in safety checking stays on.
  No remote custom Python model code, fine-tuning, LoRA, dataset download or person photos.
- Model cache is outside `/kaggle/working`, avoiding multi-GB output artifacts.
- `kaggle_client.py` uses explicit SDK `version_label` requests because the pinned
  Kaggle 2.2.4 CLI accepts but ignores version suffixes in its status/output helpers.
  Results are tied to the submitted version, not whatever a later rerun produces.
- Local validation: mocked workflow tests, notebook schema validation, Python syntax,
  installed SDK request/signature checks and pip dependency-resolution dry run.
  **GPU generation and live Kaggle authentication/submission have not been tested here.**

Tests (standard library only):

```bash
python3 -m unittest discover -s NtomaStudio/tools/kaggle_outfits/tests -v
```

`outfit-concepts.ipynb` is generated from `outfit_job.py` and `launch.notebook()`.
Regenerate after changing the worker (from this directory):

```bash
python -c "from pathlib import Path; from launch import notebook, write_json; write_json(Path('outfit-concepts.ipynb'), notebook())"
```

After reviewing these baseline concepts, decide whether fine-tuning on a curated,
permitted fashion dataset is worthwhile. Neither this notebook nor having an API token
resolves the still-separate fabric-pattern dataset rights and evaluation requirements.
