"""Kaggle GPU worker: pretrained outfit concepts, NOT custom model training.

Embedded in a generated notebook by launch.py. No credentials or user photos needed.
The model's built-in safety checker remains enabled. Review outputs before app use.
"""
from __future__ import annotations

import hashlib
import html
import importlib.metadata
import json
import os
from pathlib import Path
import time

MODEL_ID = "stable-diffusion-v1-5/stable-diffusion-v1-5"
LICENSE_URL = f"https://huggingface.co/{MODEL_ID}/blob/main/LICENSE"
PACKAGES = [
    "diffusers==0.35.1", "transformers==4.56.2", "accelerate==1.10.1",
    "huggingface-hub==0.34.4", "safetensors==0.6.2",
]
PROMPTS = [
    ("Evening dress", "Ankara-inspired evening dress, teal and gold geometric print, "
     "puff sleeves, fitted waist, floor length flared skirt"),
    ("Occasion suit", "modern tailored two piece suit, indigo with gold patterned trim, "
     "single breasted jacket, straight leg trousers"),
    ("Wrap dress", "wax-print-inspired wrap dress, rust orange and cream floral motifs, "
     "elbow length sleeves, midi length skirt"),
    ("Formal ensemble", "kente-inspired geometric accents on a navy formal jacket and "
     "matching trousers, understated gold details"),
    ("Everyday co-ord", "relaxed shirt and wide leg trouser co-ord, green and cream "
     "botanical print, short sleeves"),
    ("Celebration outfit", "peplum blouse and ankle length skirt, violet and gold "
     "geometric fabric, square neckline, three quarter sleeves"),
    ("Linen concept", "sand coloured linen-inspired tunic with indigo patterned panels, "
     "long sleeves, matching tapered trousers"),
    ("Contemporary jumpsuit", "cobalt blue and orange abstract-print jumpsuit, "
     "wide legs, belted waist, short sleeves, tailored collar"),
]
PREFIX = "Full length fashion design illustration, one outfit on a faceless dress form, "
SUFFIX = ", plain ivory background, clearly visible garment silhouette, front view"
NEGATIVE = "text, watermark, logo, nude, nudity, underwear, cropped garment, blurry, extra limbs"
DISCLAIMER = (
    "AI-generated outfit concepts from a pretrained model; not custom-trained. "
    "Not sewing patterns, fit predictions, or verified authentic traditional designs. "
    "Review cultural details, garment construction, and model licence before use."
)


def gallery(report: dict) -> str:
    esc = html.escape
    cards = []
    for item in report["images"]:
        image = (f'<img src="{esc(item["file"], quote=True)}" alt="{esc(item["title"], quote=True)}">'
                 if item["file"] else '<p class="blocked">Image withheld by safety checker.</p>')
        cards.append(f'<article>{image}<h2>{esc(item["title"])}</h2>'
                     f'<p>{esc(item["prompt"])}</p><small>Seed {item["seed"]}</small></article>')
    return ("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
            '<meta name="viewport" content="width=device-width,initial-scale=1">'
            "<title>Ntoma Studio — outfit concepts</title><style>"
            "body{font:16px system-ui;background:#f7f3ea;color:#272c25;margin:2rem auto;max-width:1100px;padding:0 1rem}"
            "main{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:1.5rem}"
            "article{background:white;padding:1rem;border-radius:12px}img{width:100%;height:auto}"
            "h1{font-size:2rem}h2{font-size:1.2rem}p{line-height:1.5}.notice{padding:1rem;border-left:4px solid #946a2b}"
            "</style></head><body><h1>Ntoma Studio · outfit concepts</h1>"
            f'<p class="notice">{esc(DISCLAIMER)}</p><p>Model: {esc(report["model_id"])}</p>'
            f'<main>{"".join(cards)}</main></body></html>')


def main(output: Path = Path("/kaggle/working/ntoma-outfits")) -> None:
    # Keep downloaded multi-GB weights OUT of notebook output artifacts.
    os.environ["HF_HOME"] = "/tmp/ntoma-hf-cache"
    os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
    os.environ["DO_NOT_TRACK"] = "1"
    import torch
    from diffusers import StableDiffusionPipeline
    from huggingface_hub import model_info

    if not torch.cuda.is_available():
        raise RuntimeError("GPU missing. Enable a Kaggle GPU and check your account quota.")
    # Freeze this run to a resolved commit and record it, rather than fetch moving main
    # separately for each component. No remote Python/custom pipeline code is loaded.
    revision = model_info(MODEL_ID, token=False).sha
    if not revision:
        raise RuntimeError("Could not resolve the pretrained model revision.")
    pipe = StableDiffusionPipeline.from_pretrained(
        MODEL_ID, revision=revision, torch_dtype=torch.float16,
        use_safetensors=True, token=False,
    ).to("cuda")
    if pipe.safety_checker is None or pipe.feature_extractor is None:
        raise RuntimeError("Required built-in safety checker is missing; refusing generation.")
    pipe.enable_attention_slicing()
    pipe.enable_vae_slicing()
    pipe.set_progress_bar_config(disable=True)
    output.mkdir(parents=True, exist_ok=False)
    report = {
        "schema_version": 1, "task": "outfit_concept_inference", "custom_trained": False,
        "model_id": MODEL_ID, "model_revision": revision, "model_license_url": LICENSE_URL,
        "disclaimer": DISCLAIMER, "gpu": torch.cuda.get_device_name(0),
        "versions": {p: importlib.metadata.version(p) for p in
                     ("torch", "diffusers", "transformers", "accelerate", "huggingface-hub", "safetensors")},
        "width": 512, "height": 512, "inference_steps": 25, "guidance_scale": 7.0,
        "negative_prompt": NEGATIVE, "safety_checker_enabled": True, "images": [],
    }
    for i, (title, brief) in enumerate(PROMPTS):
        seed = 19000 + i
        prompt = PREFIX + brief + SUFFIX
        start = time.monotonic()
        result = pipe(
            prompt, negative_prompt=NEGATIVE, width=512, height=512,
            num_inference_steps=25, guidance_scale=7.0,
            generator=torch.Generator(device="cuda").manual_seed(seed),
        )
        flags = result.nsfw_content_detected
        if flags is None or len(flags) != 1:
            raise RuntimeError("Safety checker did not return a verdict; refusing to save.")
        blocked = bool(flags[0])
        filename = None if blocked else f"outfit-{i + 1:02d}.png"
        checksum = None
        if filename:
            result.images[0].save(output / filename)
            checksum = hashlib.sha256((output / filename).read_bytes()).hexdigest()
        report["images"].append({
            "title": title, "prompt": prompt, "seed": seed, "file": filename,
            "safety_blocked": blocked, "sha256": checksum,
            "seconds": round(time.monotonic() - start, 2),
        })
        # Persist partial progress; completion is recorded separately after the loop.
        (output / "run-report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(f"Concept {i + 1}/{len(PROMPTS)}: {'withheld' if blocked else 'saved'}", flush=True)
    report["completed"] = True
    (output / "run-report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    (output / "index.html").write_text(gallery(report), encoding="utf-8")
    print("Finished. Open ntoma-outfits/index.html in downloaded outputs. No model was trained.")


if __name__ == "__main__":
    main()
