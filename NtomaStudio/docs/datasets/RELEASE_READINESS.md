# Recognition release gate and acquisition target

**11 October 2026 — recognition is NOT release-ready.** Ntoma Studio can retain its
clearly labelled demo/manual selection, but should not claim reliable specialist
fabric identification yet. No model has been trained or independently evaluated.

## What we actually have

- AI4Culture: **803 metadata-filtered pattern candidates**. Source, labels and design
  grouping still require review.
- Completed Commons/Cleveland collection: **951 byte-distinct candidates**, not
  951 independent designs. **Zero approved training images** in those manifests.
- Kente-print search results did not establish printed Kente; no verified Gonja
  pool. Family labels include overlapping traditions, construction, garments and fibres.
- Neither pool is an independent representative test of Ghanaian camera/gallery
  photos of existing dresses. Earlier thumbnail triage did not grant training approval.

Do not add these two pools and call the result a deduplicated training dataset.
Crops, augmentations and repeated downloads do not fill an independent-design gap.

## Provisional collection budget — not a guaranteed sufficient number

There is no universal number of datasets or photographs that guarantees release.
Use this budget to acquire and review enough data for an initial model and a
meaningful evaluation, then expand based on held-out failures and learning curves.
Machine-readable policy: `tools/dataset/release_targets.json`.

| Allocation | Independent reviewed design groups per supported class | Six-pattern allocation |
|---|---:|---:|
| Initial training milestone | 300 | 1,800 |
| Threshold selection / validation | 150 | 900 |
| Locked independent test | 200 | 1,200 |
| **Initial total** | **650** | **3,900** |

Additional locked test set: **1,000 independent unknown/non-fabric/ambiguous groups**.
Examples include solid cloth, mixed/occluded patterns, unsupported families, furniture,
plants, drawings, screen captures and non-textile objects. Never equate lack of a
six-pattern label with lack of fabric.

**Domain requirement:** all 200 class test groups must reflect intended Ghanaian
existing-dress/cloth inputs, not just museum swatches. Target at least 100 worn-outfit
camera groups and 100 gallery groups per class, with independent designs/sessions;
gallery inputs include dresses, isolated garments and cloth. At least 75 validation
groups per class must be target-domain examples too. Include several devices,
lighting conditions, distances, folds, clutter and partial views; report those slices.
The subsets are included in 150/200, not extra totals. Public wearer photos can
start coverage, but do not prove smartphone deployment-domain coverage on their own.

A group means one underlying design/source object/photo session. Related views,
crops and augmentations stay in the same split. Conservatively connect duplicate
objects/designs/sessions into a single group. Review rights, cloth region, label,
provenance, view and grouping before assigning a split. Existing app editorial photos
and their crops stay out of training and claimed independent evaluation.

For the six pattern classes the allocation is **3,900 class-group slots**, not an
assertion of 3,900 unique photos when multi-pattern annotations overlap. Resolve
checked-versus-plaid and mixed patterns before choosing a six-way softmax, multi-label
head or explicit ambiguous fallback. Do not count ambiguous weak labels as approved.

For the family model, **650 × K** is the provisional initial budget, where K is the
reviewed, coherent set of supported visual families. K is not automatically 17.
WAX/ANKARA need alias treatment; fibres cannot reliably be certified from appearance;
KENTE_PRINT and GONJA remain unsupported without documented examples. Museum transfer
data from outside Ghana does not become Ghanaian ground truth by renaming it.

Train at increasing sizes, including the 300-group milestone; expand toward
**1,000+ diverse training groups per supported class** when learning curves or hard
slices require it. Evaluate at any size honestly; do not advertise universal accuracy
merely because a class reaches 1,000 images. Final release remains conditional on tests.

## Proposed release checks (not measured results)

Freeze the model, thresholds and taxonomy using validation only. On a locked,
design-disjoint test, report macro/per-class precision, recall, confusion matrix,
calibration, uncertainty rate and selective coverage, including confidence intervals.

For a cautious first specialist release, propose:

- Every supported class: confident-output precision **95% Wilson lower bound ≥85%**
  and confident-output coverage **≥50%** on its 200-group target-domain test. Report
  recall too; abstaining on every image must not pass.
- Camera worn-outfit slice: precision **95% Wilson lower bound ≥75%** and coverage
  ≥50%; separately report gallery, device, lighting and occlusion slices.
- Unknown/non-fabric test: confident wrong classification **95% Wilson upper bound
  ≤5%**. Unknown output is appropriate, not a recognition failure to hide.
- No split leakage, undocumented rights, weak labels promoted by search relevance,
  app-demo-photo leakage or unsupported identity/fibre/authenticity claims.
- Android camera/gallery preprocessing, quantized-model accuracy, fallback UI and
  model/device regression tests pass before the specialist model replaces demo output.

These are proposed product gates, not a universal scientific release standard and
not performance claims. At 200 independent observations, a 90% proportion has a
roughly 85–93% Wilson interval; image-level intervals are misleading if crops from
one cloth are treated as independent. Report design-group metrics and clustered
intervals for multiple related views. Passing these gates is necessary for this
proposal, not a substitute for cultural/provenance review or deployment monitoring.

## Acquisition action underway

Run [38153771996](https://github.com/Jacinth-19/ntoma-studio/actions/runs/38153771996)
was launched from commit `bad252e` on the working branch. Three internet-connected
jobs collect Commons patterns, Commons all-17-family candidates and Cleveland
textile patterns. Each has a **45-minute bounded collector**; artifacts retain full
review copies and rights evidence for 30 days.

Changes compared with the previous capped run:

- Commons continuation and Cleveland pagination, up to 150 hits/query.
- Four targeted terms/category, including real dresses and narrow dot-dress categories.
- **951 prior source IDs excluded** to avoid counting the same photos as new.
- Breadth-first query scheduling and per-label caps to reduce domination by common classes.
- Cleveland restricted to its textile department to reduce paintings/sculpture results.
- Independent caps: 900 Commons pattern, 1,100 Commons family and 900 Cleveland pattern
  candidates. These are **collection ceilings, not promised downloads or release targets**.
- Rights/creator evidence remains fail-closed; all output remains review-required.
- Status watcher attached; no Kaggle token is required for public collection.

**At this document's initial save, the jobs were still running. New verified totals
are pending.** Source IDs exclude old downloads, not alternate photos of the same
design; cross-job/cross-source bytes and design deduplication still apply. No claim
is made that this bounded batch will satisfy the budget. If a source is exhausted,
record the unresolved gap rather than duplicating it or using unlicensed imagery.

### First completed expansion result

Cleveland pattern job completed successfully: **69 additional byte-distinct
candidates**, all distinct from the 951 previous public-pool byte hashes. It skipped
130 previously collected source IDs; zero API/download errors. Suggested matches:
DOTTED 0, CHECKED 0, PLAID 0, STRIPED 1, GEOMETRIC 8, FLORAL 60. This source did
**not** fill the rare-pattern gaps, despite pagination and extra terms.

All 69 small thumbnails were triaged: eight held out of the six-pattern target
(pictorial/inscription/no-visible-pattern textiles or beaded bags), **61 need full
resolution/rights/label review**. Three supposed geometric curtains are visibly
striped; multiple curtain, griffin-roundel, velvet and floral-panel views may share
a design group. Proposed corrections and related groups are recorded separately in
`release-cma-pattern-triage.csv`, not promoted into training truth. Audit:
`release-cma-collection-audit.json`. Training approvals remain **zero**.

The full-size resized copies/evidence are in the Actions artifact; locally retrieved
Git blob `5813065d2898a02ecf2461114237d0c56afeb964` contains metadata and **320px review
thumbnails only**, not full training files. Two Commons jobs remain in progress.

### Repeatable gap audit

`python3 NtomaStudio/tools/dataset/audit_readiness.py` reports the current approved
pattern group counts as zero and the base gap as **650/class (3,900 total)**, plus
1,000 unknown/non-fabric groups. It requires explicitly reviewed annotation JSONL,
not query manifests, requires a valid image SHA-256, and flags repeated
source/hash/group identities crossing splits or assigned to conflicting groups.
Alternate camera/gallery views of one design cannot fill both independent-origin quotas.
A budget-passing report still has `release_ready: false`: measured independent model
performance and Android verification are separate required gates.
