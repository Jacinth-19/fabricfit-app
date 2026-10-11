# Human review protocol for the approved manifest (step 3)

Purpose: turn intake or public candidates into **approved, independent** design groups.
Only approved rows count toward the budget in `release_targets.json`. Query matches,
thumbnails, duplicates and augmented crops never count.

## Gate, in order

1. **Rights:** consent record exists, or a verified licence URL is saved. Otherwise
   `rights_hold`. No training.
2. **Duplicates:** byte and perceptual duplicate check against the 951 prior candidates,
   the 803 AI4Culture candidates and the 69 new Cleveland candidates. Duplicates join the
   existing group instead of making a new one.
3. **Groups:** each design group is one physical cloth, one source object or one photo
   session. Related views, crops and augmentations stay in the same group and the
   same split.
4. **Label, independently:** two reviewers label each group without seeing each other's
   answers. Use the pattern and family labels from the approved taxonomy only. Use
   `ambiguous` or `uncertain` freely; do not force a label.
5. **Disagree → resolve:** a third reviewer decides, or the group goes to `ambiguous`.
6. **View and domain:** record flat, draped, garment or worn. Record camera or gallery
   for target-domain counts.
7. **Split:** assign train, validation or test **per group, before augmentation**.
   Augmentation applies to train only.
8. **Held out for app QA:** the app's editorial and demo photos, and their crops, are
   excluded from training and from any claimed independent evaluation.
9. **Sign-off:** record reviewer IDs (initials or pseudonyms, never tokens), the date,
   and the decision: accepted, uncertain, conflicting, unsuitable or rights_hold.

## Reviewer checklist

- Is the fabric the subject of the photo, or is the cloth incidental?
- Is the pattern visible enough to label without guessing from the search query?
- Is it a screen, a print of a photo, a drawing, a watermarked stock image or a
  near-duplicate burst frame? If so, reject.
- Is anyone identifiable? If yes and no consent exists, reject.
- Does the label rely on fibre or construction (silk, cotton, linen)? Do not label
  fibre from appearance; leave it out of the visual classifier.

## Output

An approved manifest with one row per photo. Columns: `photo_id`, `design_group_id`,
`split`, `pattern_label`, `family_label`, `view`, `domain` (camera/gallery/museum),
`source_type`, `rights_basis`, `consent_id` or licence URL, `review_decision`,
`reviewer_a`, `reviewer_b`, `reviewer_c` (if used). Rows that fail any gate are kept
with their reason, not deleted.

## Current state

No approved rows exist. The collection tools write `REVIEW_REQUIRED_NOT_TRAINING_DATA`
and `reviewed_labels: []`, and the audit reports release-ready as **NO**. This protocol
has not yet been used on any photo.
