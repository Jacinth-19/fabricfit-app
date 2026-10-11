# Ghanaian fabric-family coverage and label policy

This inventory covers **all 17 current app categories**, not a claim that Ghana has
exactly 17 textile traditions. Several categories describe a fibre, construction,
dye technique, cultural tradition or garment rather than mutually exclusive families.
A silk Kente or cotton batik can satisfy more than one description.

## Collection priorities

| App category | What a usable example needs | Collection lead / limitation |
|---|---|---|
| KENTE | Identified Kente textile; provenance supporting woven construction if used to distinguish printed imitations | Commons Kente category, original museum records; include cloth and worn dresses, group alternate views |
| KENTE_PRINT | Explicit identification of a printed Kente-style textile | A search match containing “print” is not evidence; woven museum Kente appeared in these results |
| ADINKRA | Photograph of an actual Adinkra textile with visible stamped/printed symbols | Exclude symbol alphabets, vector icons, stamps/ink pots and unrelated canvas artworks |
| WAX | Identified African wax-print cloth or garment | Commons Ghanaian wax-print files; production technique still needs provenance |
| ANKARA | Reviewed African-print garment/cloth | Strong overlap with WAX; treat as aliases or a shared visual family until definitions justify separation |
| BATIK | Provenanced wax-resist-dyed textile | Museum/Commons examples exist; Indonesian batik is transfer data, not automatically Ghanaian batik |
| TIEDYE | Identified tie-dyed cloth with visible resist pattern | Museum/Commons examples; shibori is not Ghana-specific, keep country/technique metadata |
| LACE | Visible openwork lace on cloth or garment | Museum/Commons; exclude drawings, lace-making tools and near-duplicate views |
| BROCADE | Identified brocade construction with useful textile detail | Museum/Commons; do not call every damask or ornate print brocade |
| COTTON_PLAIN | Plain cloth with documented cotton composition | Plain-looking is not proof of cotton; do not import industrial solid textures under this label blindly |
| LINEN | Cloth with documented linen composition | Museum/Commons; ordinary photos do not reliably distinguish cotton from linen |
| SILK | Cloth with documented silk composition | Museum/Commons; shiny satin can be synthetic, and silk may also be Kente/brocade |
| CHIFFON | Identified chiffon cloth/garment | Sparse initial results; translucency alone is not enough |
| VELVET | Identified velvet with visible pile | Museum/Commons; keep construction separate from fibre |
| DENIM | Identified denim textile/garment | Commons; avoid relying solely on blue colour or garment silhouette |
| FUGU | Northern Ghana smock/strip-woven cloth, textile visible | Commons Ghanaian-smocks category; garment label overlaps with cloth traditions |
| GONJA | Explicit Gonja/Daboya provenance, not arbitrary striped smocks | No usable distinct dataset verified so far; don't duplicate FUGU into GONJA |

**Do not force these into a 17-way softmax yet.** The first family training taxonomy
must resolve aliases and multi-attribute overlap. Missing categories stay unsupported,
with uncertain/demo output, instead of being trained from weak search labels.
`label_schema.json` and Android enum mappings have not been changed by this collection.

## Annotation fields before training export

- `source_id`, source page, creator, per-image licence URL and saved evidence.
- Country/cultural provenance only when documented, not guessed from appearance.
- `reviewed_family` / `reviewed_pattern`, independent from query suggestions.
- View: flat cloth, draped cloth, isolated garment, worn outfit, irrelevant.
- Cloth region or crop; keep original photo/design group linkage after cropping.
- Design/source-object/session group to bind related photos across splits.
- Review decision: accepted, uncertain, conflicting labels, unsuitable, rights hold.
- `held_out_for_app_qa`: exclude the app's existing editorial/demo/test photos and
  alternate crops of the same photo from training/evaluation claims.

Pattern additions use the same review gate and must be deduplicated against the
existing 803 AI4Culture candidates. A search for “floral textile” can return an
object described by floral decoration elsewhere; it does not constitute an annotation.
Museum images also differ from camera/gallery pictures of Ghanaian dresses.

## Readiness

The collector requests every category and reports **zeros as zeros**. Its manifest
has `reviewed_labels: []` and `REVIEW_REQUIRED_NOT_TRAINING_DATA` deliberately. No
amount of duplicating, tiling or augmentation turns a handful of original cloths
into an independent large dataset. Initial model work should use only categories
with reviewed, diverse examples and separate held-out design groups.

Free image licences do not mean all visible personal/cultural rights are settled.
Retain attribution, respect licence conditions on redistributed crops, avoid
unnecessary use of identifiable people and do not infer their identity or ethnicity.
