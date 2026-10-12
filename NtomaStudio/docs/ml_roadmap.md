# Recognition model plan (decided 2026-10-12)

Ntoma Studio uses **2 models for fabric recognition**, expanding to **4 only when advanced
dress editing and photoreal try-on are added**. This is the decision record; the code and
dataset tooling are wired to it.

## Start with these two

| # | Model | What it does | Code | Trained from |
|---|-------|--------------|------|--------------|
| 1 | **Fabric-family classifier** | Suggests Kente, Kente-print, Ankara/wax print, Adinkra, batik, lace, denim, … from a selected cloth or dress photo. Returns confidence and **"uncertain"** instead of forcing an answer. | `data/ml/FabricFamilyClassifier.kt` · assets `fabric_family_classifier.tflite` + `fabric_family_labels.txt` | `tools/dataset/` fabric task (`label_schema.json`, 17 classes) |
| 2 | **Visible-pattern classifier** | Identifies stripes, checks, plaid, dots, floral and geometric patterns (plus SOLID = "no visible pattern"). | `data/ml/PatternClassifier.kt` · assets `pattern_classifier.tflite` + `pattern_labels.txt` | `tools/dataset/` pattern task (`label_schema_pattern.json`, 7 classes) |

**Why separate them?** Appearance is not provenance. "Striped" describes how a cloth
looks; it does not prove the cloth is Kente. Likewise, floral does not automatically mean
Ankara. Keeping the classifiers separate means they can ship, fail, and say *uncertain*
independently — and the newly acquired dataset (labelled by appearance) trains model 2 first.

**Uncertainty contract (both models, enforced in `TrainedModelRunner`):**
a verdict is reported as *uncertain* — never forced onto the nearest class — when
`top < 0.40`, or when the top two classes are a near-tie (`top − second < 0.10` while
`top < 0.60`). In the app: an uncertain family collapses to **Unknown / "Not sure yet"**
with the model's honest top confidence; an uncertain pattern leaves the heuristic answer
in place. Every case is logged (`family_model_uncertain`, `pattern_model_uncertain`).

**Build order:** pattern recognition → fabric-family recognition → garment segmentation →
optional photoreal try-on. The pattern model trains first because the acquired dataset
labels appearance; the family model needs the harder field-collected provenance labels.

## Add these later

| # | Model | What it does | Status / notes |
|---|-------|--------------|----------------|
| 3 | **Garment-segmentation model** | Locates the dress/fabric region and separates it from the person, skin and background. Focuses recognition on the right area and supports replacing a dress's fabric without changing everything else. | Today: DeepLabV3 **person** segmentation (`data/ml/PersonSegmenter.kt`, QA'd in `tools/qa_deeplabv3.py`). The garment-level upgrade slots into the same try-on/compositing call sites. |
| 4 | **Optional cloud virtual-try-on model** | Combines a person's photo with a supported garment reference to produce a photorealistic preview. | Exists as the demo compositor (`VirtualTryOnRepositoryImpl`). A photoreal version **must disclose cloud processing** and **must not promise accurate fit or measurements**. |

## Things that do NOT need separate AI models

- **Colour extraction** — ordinary image processing (`ColorPatternAnalyzer` hue/palette
  pass). Keep it that way; it is deterministic, offline and testable.
- **Template fabric previews** — our existing rendering/compositing approach
  (`DemoLookGenerator`, `FabricSwatch`).
- **Recommendations** — rules based on fabric, pattern, colour and user preferences
  (`RecommendationEngine`, `OutfitSuggester`, `ColorHarmonyEngine`). No learned ranking
  until the rule version is demonstrably not enough.

## Integration contract (training → app)

- Training: `tools/dataset/train_finetune.py --task pattern|fabric` (MobileNetV3-Small,
  ImageNet init, dynamic-range-quantized TFLite, softmax outputs; inputs scaled
  `(px / 127.5) − 1` — matches `TrainedModelRunner.kt`).
- Shipping: drop the reviewed `<task>_finetuned.tflite` + `<task>_labels.txt` pair into
  `app/src/main/assets/` under the asset names above. `TrainedModelRunner` detects the
  pair and the model switches on automatically; missing assets keep the honest heuristic
  engine (`ON_DEVICE_DEMO`) in charge — there is no half-configured state.
- Honesty bars (unchanged): no shipping, no accuracy claims under 1000 images/class;
  smoke models are pipeline tests only; the heuristic fallback stays permanently.
