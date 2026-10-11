# Ntoma Studio: Kaggle dataset shortlist and AI plan

Researched: 2026-10-11. **Update:** the subsequent [fabric dataset acquisition](FABRIC_DATASETS.md) downloaded AI4Culture and isolated 803 metadata-filtered, byte-deduplicated pattern candidates. No Kaggle upload, training run, model export or app behaviour change has occurred. Outfit-concept automation is prepared and locally tested; no remote job has run.

The uploadable notebook and private-job launcher are in
[`tools/kaggle_outfits/`](../tools/kaggle_outfits/README.md). They run pretrained
outfit generation only. No chat-supplied credentials were used or stored. This session
cannot submit directly to Kaggle; use browser upload or the authenticated local launcher.

## Recommended first task: visible pattern recognition

Train a lightweight classifier for visible patterns (stripes, checks, dots, floral,
geometric, solid), independently of fabric-family labels such as Kente or Ankara.
A striped photo is not proof of Kente, and a smooth photo is not proof of silk.

### Dataset shortlist

| Dataset | Published contents | Recommendation and limitations |
|---|---|---|
| **fashion-pattern-images**, `kimduhan/fashion-pattern-images` | 10,898 images in 19 pattern classes; includes stripe, checked, dot, floral, geometric, gradient and muji. Page lists MIT and names marketplace, Pinterest and Google Image Search sources. [1](https://www.kaggle.com/datasets/kimduhan/fashion-pattern-images) | **Best technical candidate for a pattern baseline, conditional on rights review.** Includes clothing and other products, not exclusively fabric close-ups. The uploader's MIT label does not establish rights to every source image. Audit provenance, labels, duplicates and permissions before training for the app. |
| **African Fabric Images**, `mikuns/african-fabric` | Description reports approximately 1,056 images at 64×64, mainly sourced through Google Image Search; the page lists License: Unknown. [1](https://www.kaggle.com/datasets/mikuns/african-fabric) | **Not approved for training or shipping.** Topically relevant but tiny, low-resolution and rights-unclear. The published description does not establish a labelled Kente/Ankara/Adinkra classification dataset. Do not assume it solves either task. |
| **Annotated Textile Fabric Image Dataset**, `olyarnncnn/annotated-textile-fabric-image-dataset` | 12,724 images of only 44 distinct fabric samples, structured metadata and some missing pattern labels. Page lists CC BY-SA 4.0, while its description contains conditional licence guidance. [2](https://www.kaggle.com/datasets/olyarnncnn/annotated-textile-fabric-image-dataset) | **Secondary candidate for inspection, not the main training pool.** Confirm licence/provenance, review annotations, and split by fabric sample ID. Random image splitting would leak views of the same cloth. 12,724 images are not 12,724 independent fabrics. |

No production-ready Ghanaian fabric or design-generation dataset was verified in this search.
Prefer original photos with documented permission for model training and the intended
commercial use. Training permission and permission to publish a dataset on Kaggle
are separate: keep uploads private unless publication is explicitly authorised.

### Data to collect alongside Kaggle candidates

- Close-up and draped photographs of distinct fabrics, in varied lighting and backgrounds.
- Human-reviewed pattern labels, separate from fabric-family labels.
- A cloth/design ID connecting multiple shots of the same physical cloth or repeated print.
- Source, creator, permission/licence evidence and any attribution requirements.
- Held-out Ghanaian phone photos for evaluation that are never used for training or tuning.

The app's existing **Settings → Contribute fabric photos → Export for training**
flow can supply fabric-family examples. It does not currently collect pattern labels;
those would need additional annotation. Avoid counting crops or augmented images as
new independent samples. Dataset size alone cannot establish production readiness.

## Recognition training and app integration

1. Review candidate rights and sample images before ingestion. Keep research-only,
   non-commercial and unknown-licence material out of the production training pool.
2. Create a separate pattern dataset and reviewed label mapping. For an initial subset:
   `stripe → STRIPED`, `checked → CHECKED`, `dot → DOTTED`,
   `floral → FLORAL`, `geometric → GEOMETRIC`, `gradient → GRADIENT`.
   Verify that `muji` examples genuinely mean `SOLID`. Exclude ambiguous classes rather
   than forcing lettering or graphics into `SYMBOLIC`.
3. Deduplicate and group related images **before** train/validation/test splitting.
   Augment training images only. Keep class order explicit across every split.
4. Fine-tune an ImageNet-pretrained MobileNetV3-Small on Kaggle GPU, rather than train
   from scratch. Use validation data for model selection and uncertainty thresholds.
5. Evaluate held-out data with per-class precision/recall, macro-F1 and confusion
   matrices; test unfamiliar/non-fabric images and retain an uncertain/fallback path.
6. Export TFLite plus ordered labels, input/output tensor contract, preprocessing,
   model checksum, training configuration, dataset version and evaluation report.
7. Compare exported TFLite predictions against the training model, then test on real
   Android devices before enabling it in the app.

### What already exists in this checkout

- `tools/dataset/prepare_kaggle.py` and `train_on_kaggle.py`: a **fabric-family**
  export/training path built around the 17-class `label_schema.json`. It does not
  automatically ingest the shortlisted pattern datasets.
- `OnDeviceMlClassifier.kt`: a generic ImageNet MobileNetV2 with a hard-coded 224px
  byte input and 1,001 outputs, producing `MlHint` second opinions.
- `ColorPatternAnalyzer.kt`: heuristic `PatternType` results used by local analysis.
- `CloudAnalysisClient.kt`: optional `POST /v1/analyze` image-analysis contract.

**A new pattern TFLite file is not a drop-in replacement for the shipped asset.**
Implement and test a separate pattern classifier with its own labels and tensor
contract, then connect it to `FabricAnalysisRepositoryImpl`. Retain the demo fallback
and honest engine labelling. Pattern training must not overwrite the 17-family schema.

The existing source allowlist includes a non-commercial academic exception and
third-party datasets mirrored in code repositories. A repository's software licence
is not proof of permission to reuse every bundled image. Re-audit these sources;
do not combine them blindly into a commercial model. No existing allowlist entries
or permissions were changed by this plan.

## Design generation: outfit concepts selected

**Owner confirmed:** the first generation feature is **outfit designs**, and no
original fabric or outfit photos are available. Do not block a first prototype on
collecting personal photos. Start by evaluating a licensed pretrained image generator
from text prompts on Kaggle, with **no fine-tuning**. This tests whether the generator
can produce useful concepts before investing in a custom dataset.

Suggested prototype input: garment type, occasion, colours, fabric-inspired appearance,
neckline, sleeve style and length. Suggested output: a standalone fashion illustration
or mannequin-based outfit concept; no user photograph required. Treat results as design
inspiration, not a sewing pattern, fit prediction or proof of cultural authenticity.
Retain fabric-pattern recognition as a separate workstream.

### Public outfit data to inspect

**Clothing dataset (full, high resolution)**, `agrigorev/clothing-dataset-full`,
contains over 5,000 images in 20 clothing classes. The author explicitly permits
commercial use and the Kaggle release lists CC0. This is a better-documented public
starting point than unknown-licence scraped collections.
[1](https://www.kaggle.com/datasets/agrigorev/clothing-dataset-full)

However, it is a general clothing-classification dataset, **not a ready-made African
outfit-design/caption dataset**. Its documented annotations include class labels,
contributor IDs and a children's-clothing flag, not detailed design captions.
[4](https://github.com/alexeygrigorev/clothing-dataset)
Inspect image quality and garment coverage before using it. Any generative fine-tune
would need suitable curated images, descriptive captions, duplicate/source grouping
and separate evaluation. Do not train on all images merely because they are available.
Do not interpret CC0 as a guarantee about depicted brands or likeness rights.

**AFRIFASHION1600** is topically relevant but its paper describes 1,600 images at
128×128, collected from online sources, with copyright retained by original owners.
It is not cleared here for production model training.
[1](https://openaccess.thecvf.com/content/CVPR2021W/CVFAD/papers/Oyewusi_AFRIFASHION1600_A_Contemporary_African_Fashion_Dataset_for_Computer_Vision_CVPRW_2021_paper.pdf)

The practical order is: **pretrained baseline → review outfit quality → curate a
suitable permitted dataset if needed → optional LoRA fine-tuning → evaluate → host
and integrate**. Running a pretrained generator is inference, not training a new model.
An uploadable notebook and local automation launcher have now been prepared. No
pretrained weights have been downloaded and no outfit images have been generated here.

The scope distinctions remain useful for later work:

| Desired output | Suitable next step |
|---|---|
| **New fabric prints** | Collect rights-cleared, high-resolution swatches/designs with captions covering motifs, colours, layout and repeat structure. Evaluate a licensed pretrained diffusion model, then consider LoRA fine-tuning on Kaggle. Verify seamless repeats separately; text prompting alone does not guarantee them. |
| **Outfit design concepts** | Use rights-cleared garment/design images and captions describing silhouette, fabric placement and styling. Start with a licensed pretrained model to evaluate usefulness before investing in fine-tuning. |
| **A person wearing the proposed outfit** | This is virtual try-on, not just pattern generation. It needs garment/person conditioning and appropriate data permissions. Evaluate a separately licensed try-on model or hosted service before attempting bespoke training. |

`DemoLookGenerator.kt` currently performs a local compositing preview, not diffusion.
`ImageGenerationRepository` is the generation integration boundary; the analysis
endpoint is **not** a generation endpoint. The current generation input includes a
person photo, so a print-only feature would also need a distinct UI/request contract.

Recommend on-device pattern recognition and a separately hosted GPU generation
backend for the first production version. Kaggle is for training/experiments, not an
always-on app server. Cloud generation requires a hosting budget, server-side secrets,
explicit upload consent and a photo-retention/deletion policy. Do not upload user
photos silently or embed provider credentials in the Android app.

## What is needed from the owner next

1. Confirm whether a Kaggle account and GPU-enabled Notebook are available. No password
   or API key is needed in chat. This is the next setup question, not a requirement to
   provide original photos.
2. Before production hosting (not before the first notebook prototype), confirm whether
   cloud generation is acceptable and the approximate monthly budget.

Already resolved: **outfit concepts**, with **no original photos available**. Begin
with text prompts and a pretrained baseline; source and curate permitted public data
only if evaluation shows a fine-tune would be useful.

There is no Kaggle connector available in this session and direct Kaggle API access
is outside its network allowlist. Use the prepared uploadable notebook or local
launcher, then share non-secret output files for validation before app integration.
