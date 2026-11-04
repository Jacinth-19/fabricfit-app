# Manually reviewed fabric photo pool

This folder is the only source of training data for the candidate classifier.
Every photo below was opened and judged one by one by the agent on the review
sheets in `FabricFit/docs/qa/review-round-*`. Search results are **not** data.

## Review rules actually applied

A candidate is **accepted** only when all of these hold:

1. The subject is cloth or a cloth garment; the fabric occupies a large share
   of the frame.
2. The family label is correct and not ambiguous with another of the 17
   families (for example a wax print is not double-counted as Ankara, and a
   brocade sari is not double-counted as silk).
3. It is not a duplicate: no dHash near-duplicate, no second frame of the same
   object/exhibit, no second copy of the same source photo.
4. The licence passes the CC0 / public-domain / CC BY / CC BY-SA gate and the
   source page and creator are recorded.
5. It is a photograph, not an engraving, book page, 3D render or 3D-scanned
   texture meant for game engines.

A candidate is **rejected** when it shows a building, monument, landscape,
animal, cake, portrait/selfie where the fabric is incidental, machinery, a
museum object that is not cloth, a printed symbol chart, a mislabeled image, or
a duplicate. Rejections are recorded with a reason, so the decision can be
audited or overturned later.

Accepted garment photos are kept only when the garment is the subject (for
example a smock displayed on a stand or worn with the cloth filling the frame).
Photos where a person dominates and the cloth is a small detail are rejected.

## What this review is not

- It is not fibre analysis or heritage certification. Distinguishing, for
  example, fugu from Gonja smock, or wax print from Ankara, needs a knowledgeable
  human and ideally a Ghanaian textile specialist. Some accepted labels are
  therefore search-metadata plus visual plausibility, not certified ground truth.
- It is not a 1000-photos-per-class dataset. One review round accepts tens of
  photos, which is far below the repository's 300/class training bar and
  1000/class claim bar.

## Files

| File | Meaning |
| --- | --- |
| `FabricFit/docs/qa/review-round-N/review-index.csv` | every candidate, with source, licence, URL, duplicate flag and decision |
| `FabricFit/docs/qa/review-round-N/*-review.jpg` | the sheet that was visually inspected for each family |
| `round-N-decisions.csv` | the reviewer's accept/reject decision and reason per candidate |
| `round-N/accepted-manifest.csv` | accepted rows only, with licence + provenance |
| `round-N/reviewed-summary.json` | counts per family and the honest-label disclaimer |
| `accepted-manifest.csv` | the union of every round, de-duplicated by source URL |

Round 1 reviewed 82 candidates (26 accepted). Round 2 reviewed 205 candidates
(30 accepted). Totals, rejection reasons and the measured classifier result are
in `FabricFit/docs/qa/reviewed-dataset.md`.

`tools/qa/review_fabric_dataset.py export` refuses to write a manifest while any
candidate is still `pending` or lacks a licence/source page.
