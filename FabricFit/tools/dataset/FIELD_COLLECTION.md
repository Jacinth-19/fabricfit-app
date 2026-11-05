# Ghana fabric photo collection: field guide

This is the practical next step toward a real classifier. **No field photography has
been performed by the agent**; this is a plan and intake workflow for a consenting
local contributor/team. Do not use the current tiny reviewed web pool to train a
shipping model.

## Target and effort

- 17 families × 300 distinct, usable photographs = **5,100 minimum training pool**.
- 17 × 1,000 = **17,000 before any public accuracy claim**.
- Plan three capture conditions for each design: close-up weave/print detail,
  cloth flat or draped, and a different distance/lighting/background. Vary the
  actual cloth, camera, day, location and person capturing it—not just crops.
- Aim for at least 10 distinct cloth items per family at the 300-photo floor; keep
  all shots of one physical item together in a single split. Ten views of one bolt
  are not ten independent examples.
- Capture each class at multiple markets/co-ops; avoid getting one label only from
  one stall or one camera. Record location broadly (e.g. market/town), not a
  person's home address.

Roughly 5,100 accepted photos at 20–30 seconds each is 28–43 hours of capture,
plus travel, consent, expert labels and review. In practice, schedule multiple
market/cooperative sessions over several weeks; don't promise the target from a
single afternoon.

## Capture checklist

1. Ask the seller/weaver/cloth owner for informed permission before each session.
   Explain that the image will be stored and used to train/evaluate a fabric
   classifier and may be redistributed under the selected open licence. If they
   decline, do not photograph. Do not photograph faces, children, payment screens,
   private documents or bystanders; frame cloth only.
2. Label with the cloth owner or a knowledgeable Ghanaian textile practitioner.
   Record uncertainty explicitly; do not force a class. Especially review
   **KENTE vs KENTE_PRINT**, **FUGU vs GONJA**, and **WAX vs ANKARA**. If a label
   cannot be resolved, exclude it from supervised training pending expert review.
3. Fill one manifest row for each photo. Use a pseudonymous contributor code, not
   a full name, phone number or email. A separate consent log, if needed, should
   be access-restricted and never committed with the dataset.
4. Select a real reusable licence only if the owner understands and grants it:
   `CC BY 4.0`, `CC BY-SA 4.0`, or `CC0 1.0`. Keep the consent record separately.
   A consent sentence is not itself a copyright licence. Never choose a licence
   on another person's behalf.
5. Capture original camera files at full resolution; cloth should fill most of the
   frame while retaining enough context to show weave, drape or repeat. Take one
   wide/medium/detail view where useful. Disable beauty filters and avoid screenshots.
6. Review visually, remove burst near-duplicates and bad focus, then run intake in
   dry-run mode. Inspect every accepted/rejected row before `--apply`.

## Manifest and intake

Copy `field_manifest_template.csv`, put photos under a folder such as `field/`,
then run from the repository root:

```bash
python3 FabricFit/tools/dataset/field_intake.py field/field_manifest.csv \
  --root field --report field/intake-report.json
```

The tool checks family, consent, explicit licence, file path, decode, minimum
resolution (320 px short side), blur, blank frames, and near-duplicates/conflicting
labels against both existing raw photos and the current batch. It defaults to a
dry run. Check each printed decision and the JSON report, correct metadata or
remove the row as needed, then write only accepted rows:

```bash
python3 FabricFit/tools/dataset/field_intake.py field/field_manifest.csv \
  --root field --report field/intake-report.json --apply
```

`--apply` writes JPEGs to ignored `FabricFit/tools/dataset/raw/<FAMILY>/`, credits
to `raw/CREDITS.csv`, and field-only capture metadata to `raw/FIELD_METADATA.csv`.
Those records contain contributor codes and permission text: keep them private,
never upload raw data or the intake report to a public issue. The image processing
checks cannot recognize fabric or verify the label—human visual review remains
mandatory. It cannot obtain or substitute for consent.

After intake, do not train immediately. First render a sheet and review the photos:

```bash
python3 tools/qa/review_fabric_dataset.py sheets \
  --data-dir FabricFit/tools/dataset/raw \
  --output-dir qa-artifacts/field-review
```

Have the textile label reviewer resolve ambiguous classes. Then create groups by
physical cloth/source session and make leak-free train/validation/test splits. The
trainer's 300/class floor is necessary, not sufficient; don't replace the shipped
model until independent evaluation is credible. No augmentations or views of the
same physical cloth may cross splits. Keep `ON_DEVICE_DEMO` until a true held-out
field-photo evaluation supports changing that status.

## Safety and attribution

- Only people who can grant the rights should choose the licence. Ask separately
  for photography consent and for redistribution rights; document the wording,
  date and scope in the team's private consent record.
- Use pseudonymous IDs, broad location, no contact data. Limit access to any
  consent log. Honour withdrawal requests by removing the image, credits and any
  derived split copies, then retrain affected models.
- Let participants review how the photos/labels will be used. Do not imply that
  contributors have certified scientific ground truth or that results guarantee
  recognition.
- Intake metadata is provenance, not expert certification. Keep a separate
  reviewed label status and the source-item/session group ID before training.

## Field manifest columns

`family,file,location,contributor,consent,license,captured_at,notes`
