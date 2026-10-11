# Taxonomy reconciliation proposal (DRAFT, needs owner sign-off)

Status: **proposal only.** No label file, Android enum, release target or audit
threshold has been changed. Each decision below needs an explicit owner answer
before any training export uses it.

Sources reconciled: `tools/dataset/release_targets.json` (six pattern labels),
`tools/dataset/label_schema.json` (17 legacy family classes), `docs/datasets/FAMILY_COVERAGE.md`,
and the Kaggle plan in `docs/KAGGLE_AI_PLAN.md`.

## A. Pattern model (six-way target)

Current release targets: CHECKED, DOTTED, FLORAL, GEOMETRIC, PLAID, STRIPED.
The Kaggle plan instead names stripe, checked, dot, floral, geometric, gradient, and
suggests muji → SOLID. The two lists disagree on PLAID, GRADIENT and SOLID.

| Label | Proposed handling | Reason |
|---|---|---|
| STRIPED | Keep | Clear visible class |
| DOTTED | Keep | Clear visible class |
| FLORAL | Keep | Clear visible class; watch decorative objects in museum photos |
| GEOMETRIC | Keep | Clear visible class; exclude lettering and graphics |
| CHECKED | Keep, and absorb PLAID | Checks and plaids are visually the same family in practice |
| PLAID | **Merge into CHECKED** (recommended) or keep as separate class | Separate classes would need labels the reviewers cannot reliably separate |
| GRADIENT | Not in the release set. Exclude from training | Not a release target; mapping it would invent a class |
| SOLID / plain cloth | Not a pattern class. Route to unknown/non-fabric test set | RELEASE_READINESS already lists solid cloth as an out-of-distribution example |
| Mixed or multi-pattern | Ambiguous. Excluded from the six-way training set; kept for the uncertain-output test | Forcing one label would be a weak annotation |

**Recommendation:** five pattern classes plus an ambiguous fallback: CHECKED (incl. PLAID),
DOTTED, FLORAL, GEOMETRIC, STRIPED. This changes the six-way budget to five classes
unless you keep PLAID separate. Please confirm.

## B. Family model

The 17 legacy classes in `label_schema.json` are mirrored in the Android app. They
overlap (WAX and ANKARA are aliases; several entries describe fibres, constructions or
garments, not families). `FAMILY_COVERAGE.md` already says not to force a 17-way
softmax. The family budget is `650 × K`, and K is still unset (`family_labels: []`).

Proposed split of the 17 entries:

| Entry | Proposed role |
|---|---|
| KENTE | Visual family candidate (woven) |
| ADINKRA | Visual family candidate (stamped/printed symbols) |
| WAX + ANKARA | **One visual family, WAX_ANKARA** (aliases, per FAMILY_COVERAGE) |
| BATIK | Visual family candidate (wax-resist) |
| TIEDYE | Visual family candidate |
| LACE | Visual family candidate |
| FUGU | Visual family candidate (northern strip-woven cloth) |
| BROCADE | Candidate only if reviewed examples show a distinct look; otherwise attribute |
| KENTE_PRINT | Unsupported until a provenanced example exists → uncertain fallback |
| GONJA | Unsupported until explicit Gonja/Daboya provenance exists → uncertain fallback |
| COTTON_PLAIN, LINEN, SILK, CHIFFON, VELVET, DENIM | **Fibre or construction attributes, not visual families.** Policy says fibre cannot be inferred from appearance. Move out of the family classifier |

**Proposed K:** about six visual families (KENTE, ADINKRA, WAX_ANKARA, BATIK, TIEDYE,
LACE), plus FUGU if reviewed examples exist. Final K is set by reviewed counts,
not by this list.

## Decisions needed from the owner

1. Pattern: merge PLAID into CHECKED (recommended), or keep PLAID as a separate class?
2. Pattern: confirm GRADIENT is excluded and plain cloth is routed to unknown.
3. Family: approve the visual/attribute split above, including moving the fibre and
   construction labels out of the family classifier.
4. Family: confirm WAX and ANKARA become one family.
5. Android: approve changing the 17-category enum and the app's displayed fabric
   category, or keep the app's existing demo labels until a trained model exists.

Once you answer, the approved taxonomy will be written to `label_schema.json` and
`release_targets.json` in one reviewed commit, and the audit will be re-run.
