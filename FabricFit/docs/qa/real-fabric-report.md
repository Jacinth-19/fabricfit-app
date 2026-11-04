# Curated real online fabric smoke test

Generated: `2026-11-04T11:33:52.551713+00:00`

> **Not a production accuracy claim.** These freely licensed Commons images were
> manually selected from source descriptions and visually reviewed to remove obvious
> search contamination. They are not expert fiber/heritage certifications; Gonja is
> represented by a Northern Ghana smock proxy. One photo per family is only a
> diagnostic smoke test, not a validation set or a measure of real-world accuracy.
> The result below is raw classifier top-1, before Android's heuristic/OOD fusion.

- Model contract audit: **8/8 passed**
- Coverage: **17/17 fabric families**
- Images evaluated: **17**
- Raw classifier top-1 agreement: **0.118** (2/17)
- Missing families: **none**
- Near-duplicates of classifier training/catalog images (dHash ≤ 6): **0**

The app remains an `ON_DEVICE_DEMO`; this smoke result must not be used as an
advertised accuracy metric. Production validation needs hundreds to thousands of
consented, human-verified field photos per family with leak-free held-out splits.

## Per-family results

| Family | Images | Correct | Raw top-1 agreement |
| --- | ---: | ---: | ---: |
| ADINKRA | 1 | 0 | 0.000 |
| ANKARA | 1 | 0 | 0.000 |
| BATIK | 1 | 1 | 1.000 |
| BROCADE | 1 | 0 | 0.000 |
| CHIFFON | 1 | 0 | 0.000 |
| COTTON_PLAIN | 1 | 0 | 0.000 |
| DENIM | 1 | 0 | 0.000 |
| FUGU | 1 | 0 | 0.000 |
| GONJA | 1 | 0 | 0.000 |
| KENTE | 1 | 0 | 0.000 |
| KENTE_PRINT | 1 | 0 | 0.000 |
| LACE | 1 | 1 | 1.000 |
| LINEN | 1 | 0 | 0.000 |
| SILK | 1 | 0 | 0.000 |
| TIEDYE | 1 | 0 | 0.000 |
| VELVET | 1 | 0 | 0.000 |
| WAX | 1 | 0 | 0.000 |

## Image-level predictions and attribution

Thumbnails in `real-fabric-contact-sheet.jpg` remain under each source image's
original license. Follow the landing-page links for attribution details.

| # | Expected | Predicted (confidence) | Match | Source and license |
| ---: | --- | --- | --- | --- |
| 1 | ADINKRA | ANKARA (0.958) | no | [Adinkra mourning cloth collected in Kumasi in 1817](https://commons.wikimedia.org/wiki/File:Adinkra_cloth.JPG) — Photo of public-domain textile via British Museum; Public domain |
| 2 | ANKARA | TIEDYE (0.950) | no | [Wax print](https://commons.wikimedia.org/wiki/File:Wax_print.jpeg) — M_nunoo; CC BY-SA 3.0 |
| 3 | BATIK | BATIK (0.589) | yes | [Batik Indonesia](https://commons.wikimedia.org/wiki/File:Batik_Indonesia.jpg) — MartijnL; CC BY-SA 3.0 |
| 4 | BROCADE | BATIK (0.980) | no | [Silk sari with supplementary-weft brocade](https://commons.wikimedia.org/wiki/File:%27Sari%27_from_Varanasi_%28north-central_India%29%2C_silk_and_gold-wrapped_silk_yarn_with_supplementary_weft_brocade.jpg) — Unknown author; Public domain |
| 5 | CHIFFON | ADINKRA (1.000) | no | [1970s Givenchy orange printed silk chiffon dress detail](https://commons.wikimedia.org/wiki/File:1970s_Givenchy_cocktail_dress%2C_orange_printed_silk_chiffon_03.jpg) — Rhode Island School of Design Museum staff photographer; CC0 1.0 |
| 6 | COTTON_PLAIN | LACE (0.775) | no | [Cotton fabric](https://commons.wikimedia.org/wiki/File:Cotton_fabric.jpg) — Ivan Pavlushkin; CC BY 4.0 |
| 7 | DENIM | VELVET (0.812) | no | [Denim fabric 03](https://commons.wikimedia.org/wiki/File:Denim_fabric_03_(Rico_Cilliers_and_colormass_via_Poly_Haven).png) — Rico Cilliers and colormass via Poly Haven; CC0 1.0 |
| 8 | FUGU | ANKARA (0.620) | no | [Ghanaian smock dress](https://commons.wikimedia.org/wiki/File:Dress_12.jpg) — Kinvidia; CC BY-SA 4.0 |
| 9 | GONJA | TIEDYE (1.000) | no | [Smock from Northern Ghana (Gonja-style strip-cloth proxy)](https://commons.wikimedia.org/wiki/File:Smock_from_Northern_Ghana.jpg) — Ibn Shiraz; CC BY-SA 4.0 |
| 10 | KENTE | WAX (0.877) | no | [Ewe kente cloth from Ghana](https://commons.wikimedia.org/wiki/File:Ewe_kente_2%2C_Ghana.JPG) — ZSM; CC BY-SA 3.0 |
| 11 | KENTE_PRINT | GONJA (0.965) | no | [Kente-style batik/printed cloth in Kumasi market](https://commons.wikimedia.org/wiki/File:Kente_(Batik)_Cloth_in_Market_-_Kumasi_-_Ghana_(4755541241).jpg) — Adam Jones; CC BY-SA 2.0 |
| 12 | LACE | LACE (0.892) | yes | [Transparent lace top and skirt](https://commons.wikimedia.org/wiki/File:Transparent_lace_top_and_skirt.png) — Lukasz Dunikowski; CC BY 2.0 |
| 13 | LINEN | BATIK (0.782) | no | [Linen Coptic fabric](https://commons.wikimedia.org/wiki/File:Linen_Coptic_fabric_REM.JPG) — BrokenSphere; CC BY-SA 3.0 |
| 14 | SILK | COTTON_PLAIN (0.437) | no | [Sea silk glove](https://commons.wikimedia.org/wiki/File:Sea_silk_glove_lo-res.JPG) — John Hill; CC BY-SA 3.0 |
| 15 | TIEDYE | BROCADE (0.767) | no | [Bai tie-dyed fabric](https://commons.wikimedia.org/wiki/File:Bai_fabric%2C_tie-dyed_-_Yunnan_Provincial_Museum_-_DSC02211.JPG) — Daderot; Public domain |
| 16 | VELVET | KENTE_PRINT (0.616) | no | [Ornamental velvet covering](https://commons.wikimedia.org/wiki/File:Ornamental_velvet_covering_Scroll_of_the_Law%2C.jpg) — Daniel Ventura; CC BY-SA 3.0 |
| 17 | WAX | BATIK (0.802) | no | [Wax prints in a West African fabric shop](https://commons.wikimedia.org/wiki/File:Waxprints_in_a_West_African_Shop.jpg) — Alexander Sarlay; CC BY-SA 3.0 |

Raw per-image scores and source URLs are in `real-fabric-report.json`.
