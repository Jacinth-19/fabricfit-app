# Native-language review handoff

**Launch decision: English only (owner-approved 2026-10-11).** Twi/Ga/Ewe resources have been moved to `unreviewed/`, outside the Android resource tree. They are not packaged, selectable or advertised as supported Android app languages. Legacy saved language choices are normalized to English.

**Status: native-speaker review deferred, not completed.** Automated text must not be labelled a native translation. The latest unreviewed workflow drafts were removed from shipping resources; all launch UI uses English. The 794 legacy strings per locale remain in archived XML, not newly certified by this work.

Review all copy in `tw-review.csv`, `gaa-review.csv` and `ee-review.csv` (834 string IDs each). They contain English source, existing unverified text when present, and blank reviewer/translation/date fields. A blank translation must never be imported into resources. The CSVs are handoff files, not runtime assets.

## Review acceptance

1. A fluent native reviewer for each locale checks meaning, terminology, spelling and natural phrasing—not just XML syntax.
2. Prioritize privacy/cloud disclosures, local-analysis limitations, flat-layout/not-fitting warnings, camera/gallery selection and quota copy. Keep meaning faithful: local preview is not photoreal try-on, fabric identity is an estimate, and selected pixels can include lighting/folds.
3. Preserve indexed placeholders exactly (e.g. `%1$d` AND `%2$d`); reordering is fine. Escape Android resource apostrophes/quotes and XML entities correctly.
4. Record reviewer/date and approved text per row. Review legacy copy too; prior presence in resources does not prove correctness.
5. Apply only reviewed translations to locale XML, run the validation script and Actions, then examine actual screens for truncation and comprehension. Do not claim full localization until signed-off copy and device review are done.

## Automated checks already provided

```sh
python tools/localization/check_strings.py
python -m unittest discover -s tools/localization -p 'test_*.py'
```

Checks reject duplicate/unknown IDs and format-argument mismatches. Installed-device tests verify the English-only behavior and two-argument quota format remain safe when a device or older preference selects Twi/Ga/Ewe. These are technical checks, **not language review**.

`--export-review` regenerates the handoff from current English resources and archived drafts and OVERWRITES blank review files. Do not use it on completed reviews without saving their approved records elsewhere.
