# Contributor permission and intake protocol (DRAFT)

Status: **draft.** Not yet reviewed by a lawyer, and not yet in use. Do not collect
or accept photos under this text until the owner approves it.

Goal: collect **independent, permitted** photos of Ghanaian fabric and existing
dresses, for the recognition models, with a clear record of who agreed to what.

## Who may contribute

- Adults (18+) who took the photo or have the right to share it.
- Photographs of cloth, garments or worn outfits. Faces are not needed. Crop them out
  where possible.
- Market, weaver-cooperative and studio photos with the relevant owner's permission.

## What the contributor agrees to (consent text, draft)

1. I took this photo, or I have the right to share it.
2. I allow Ntoma Studio to store this photo and use it to **train and test** its
   fabric and pattern recognition, and to use the results in the app.
3. I allow the photo, or an edited copy, to be used in **commercial** versions of the app.
4. I understand that **withdrawing** stops future use and removes the photo from future
   training sets. It does not remove copies in models already released, unless the
   project states otherwise in writing.
5. Identifiable people: I confirm that people in the photo are not identifiable, or they
   have agreed to appear. Photos of minors are not accepted.
6. Attribution: I would like credit as (name / alias / none).
7. Location: I agree that the location is stored only at the country or city level, or
   not at all. GPS EXIF is removed before storage.

Ask for explicit tick boxes for items 2, 3 and 5. Do not use pre-ticked boxes.

## Intake record (one row per photo)

| Field | Required | Notes |
|---|---|---|
| `photo_id` | yes | Project-assigned |
| `consent_id` | yes | Links to a stored, dated consent record. No consent, no row |
| `contributor_alias` | optional | Only what the contributor chose |
| `attribution` | yes | Name/alias or `none` |
| `rights_statement` | yes | Enum: own_photo / permission_granted / licence_verified |
| `people_identifiable` | yes | `no` or `consented`. `yes` without consent is rejected |
| `country_city` | optional | Only if consented |
| `device_model`, `date_taken` | optional | Used for device-diversity slices |
| `view` | yes | flat_cloth / draped_cloth / isolated_garment / worn_outfit / other |
| `source_type` | yes | contributor / market / cooperative / public_licensed |
| `design_group_id` | yes | Groups photos of the same physical cloth or session |

Store consent records and personal data outside Git. Git holds only the pseudonymous
`photo_id`, `consent_id` and labels. Keep the mapping in access-restricted storage.

## Legal check before launch

Photos of identifiable people are personal data. Before any collection, obtain legal
advice on Ghana's Data Protection Act, 2012 (Act 843), the retention period, and the
cross-border handling of photos. This draft does not provide that advice.

## Public-collection rule

Public photos (Wikimedia Commons, museum CC0 pages) are kept separately. Each needs its
own licence check. Their attribution is retained. They supplement the independent pool
and do not replace it.
