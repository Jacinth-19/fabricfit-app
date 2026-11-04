# Real online fabric smoke test

Generated: `2026-11-04T10:51:55.296475+00:00`

> **Not a production accuracy claim.** These are freely licensed, search-derived
> online photos with unvetted labels. The test exercises the exact bundled TFLite
> models, but a few web images per family cannot represent all real-world fabrics.
> The result below is raw classifier top-1, before Android's heuristic/OOD fusion.

- Model contract audit: **8/8 passed**
- Coverage: **17/17 fabric families**
- Images evaluated: **33**
- Raw classifier top-1 agreement: **0.091** (3/33)
- Missing families: **none**

The app remains an `ON_DEVICE_DEMO`; this smoke result must not be used as an
advertised accuracy metric. Production validation needs hundreds to thousands of
consented, human-verified field photos per family with leak-free held-out splits.

## Per-family results

| Family | Images | Correct | Raw top-1 agreement |
| --- | ---: | ---: | ---: |
| ADINKRA | 2 | 0 | 0.000 |
| ANKARA | 2 | 0 | 0.000 |
| BATIK | 2 | 1 | 0.500 |
| BROCADE | 2 | 0 | 0.000 |
| CHIFFON | 2 | 0 | 0.000 |
| COTTON_PLAIN | 2 | 0 | 0.000 |
| DENIM | 2 | 0 | 0.000 |
| FUGU | 2 | 0 | 0.000 |
| GONJA | 2 | 0 | 0.000 |
| KENTE | 2 | 0 | 0.000 |
| KENTE_PRINT | 1 | 1 | 1.000 |
| LACE | 2 | 1 | 0.500 |
| LINEN | 2 | 0 | 0.000 |
| SILK | 2 | 0 | 0.000 |
| TIEDYE | 2 | 0 | 0.000 |
| VELVET | 2 | 0 | 0.000 |
| WAX | 2 | 0 | 0.000 |

## Image-level predictions and attribution

Thumbnails in `real-fabric-contact-sheet.jpg` remain under each source image's
original license. Follow the landing-page links for attribution details.

| # | Expected | Predicted (confidence) | Match | Source and license |
| ---: | --- | --- | --- | --- |
| 1 | ADINKRA | BATIK (0.989) | no | [Adinkra Rattray.JPG](https://commons.wikimedia.org/wiki/File:Adinkra_Rattray.JPG) — Robert Sutherland Rattray; Public domain |
| 2 | ADINKRA | ANKARA (0.959) | no | [Adinkra symbols, Ghana.jpg](https://commons.wikimedia.org/wiki/File:Adinkra_symbols,_Ghana.jpg) — ZSM; CC BY-SA 3.0 |
| 3 | ANKARA | TIEDYE (0.950) | no | [Wax print.jpeg](https://commons.wikimedia.org/wiki/File:Wax_print.jpeg) — M_nunoo; CC BY-SA 3.0 |
| 4 | ANKARA | BATIK (0.802) | no | [Waxprints in a West African Shop.jpg](https://commons.wikimedia.org/wiki/File:Waxprints_in_a_West_African_Shop.jpg) — Alexander Sarlay; CC BY-SA 3.0 |
| 5 | BATIK | BATIK (0.589) | yes | [Batik Indonesia.jpg](https://commons.wikimedia.org/wiki/File:Batik_Indonesia.jpg) — MartijnL; CC BY-SA 3.0 |
| 6 | BATIK | BROCADE (0.808) | no | [COLLECTIE TROPENMUSEUM Batiksters aan het werk op een stand van batikmakerij Tanaabang Batawi TMnr 60018019.jpg](https://commons.wikimedia.org/wiki/File:COLLECTIE_TROPENMUSEUM_Batiksters_aan_het_werk_op_een_stand_van_batikmakerij_Tanaabang_Batawi_TMnr_60018019.jpg) — Unknown author Unknown author; CC BY-SA 3.0 |
| 7 | BROCADE | ANKARA (0.796) | no | [Brocade loom.jpg](https://commons.wikimedia.org/wiki/File:Brocade_loom.jpg) — Anne Oeldorf-Hirsch; CC BY 2.0 |
| 8 | BROCADE | BATIK (0.902) | no | [Nanjing Brocade Research Institute.jpg](https://commons.wikimedia.org/wiki/File:Nanjing_Brocade_Research_Institute.jpg) — Anne Oeldorf-Hirsch; CC BY 2.0 |
| 9 | CHIFFON | ADINKRA (0.477) | no | [Chiffon cake kanko.jpg](https://commons.wikimedia.org/wiki/File:Chiffon_cake_kanko.jpg) — Kanko from Nagasaki, Japan; CC BY 2.0 |
| 10 | CHIFFON | SILK (0.969) | no | [Lillian Gish 2.jpg](https://commons.wikimedia.org/wiki/File:Lillian_Gish_2.jpg) — George Grantham Bain Collection.; Public domain |
| 11 | COTTON_PLAIN | TIEDYE (0.783) | no | [Female Mallard Duck Rest 3.jpg](https://commons.wikimedia.org/wiki/File:Female_Mallard_Duck_Rest_3.jpg) — Acarpentier; CC BY 3.0 |
| 12 | COTTON_PLAIN | KENTE_PRINT (0.607) | no | [Male mallard duck 2.jpg](https://commons.wikimedia.org/wiki/File:Male_mallard_duck_2.jpg) — Acarpentier; CC BY 3.0 |
| 13 | DENIM | VELVET (0.812) | no | [Denim fabric 03 (Rico Cilliers and colormass via Poly Haven).png](https://commons.wikimedia.org/wiki/File:Denim_fabric_03_(Rico_Cilliers_and_colormass_via_Poly_Haven).png) — Rico Cilliers (Processing) colormass (Photography); CC0 1.0 |
| 14 | DENIM | VELVET (1.000) | no | [Denim fabric 03 (Rico Cilliers and colormass via Poly Haven).webp](https://commons.wikimedia.org/wiki/File:Denim_fabric_03_(Rico_Cilliers_and_colormass_via_Poly_Haven).webp) — Rico Cilliers (Processing) colormass (Photography); CC0 1.0 |
| 15 | FUGU | ANKARA (0.608) | no | [Dress 12.jpg](https://commons.wikimedia.org/wiki/File:Dress_12.jpg) — Kinvidia; CC BY-SA 4.0 |
| 16 | FUGU | ANKARA (0.317) | no | [MAN DRESSED IN LOCAL ATTIRE 03.jpg](https://commons.wikimedia.org/wiki/File:MAN_DRESSED_IN_LOCAL_ATTIRE_03.jpg) — Salifu Wumpini Hussein; CC BY-SA 4.0 |
| 17 | GONJA | KENTE_PRINT (0.604) | no | [Dryoscopus gambensis (Malaconotidae) (Northern Puffback) - (male), Mole National Park, Ghana, crop.jpg](https://commons.wikimedia.org/wiki/File:Dryoscopus_gambensis_(Malaconotidae)_(Northern_Puffback)_-_(male),_Mole_National_Park,_Ghana,_crop.jpg) — This image is created by user Bram ter Keurs at Observation.org , a global biodiversity recording project.; CC BY 3.0 |
| 18 | GONJA | ANKARA (0.462) | no | [Fruits and flower of the sausage tree (Kigelia africana), Ghana.jpg](https://commons.wikimedia.org/wiki/File:Fruits_and_flower_of_the_sausage_tree_(Kigelia_africana),_Ghana.jpg) — ZSM; CC BY-SA 4.0 |
| 19 | KENTE | WAX (0.877) | no | [Ewe kente 2, Ghana.JPG](https://commons.wikimedia.org/wiki/File:Ewe_kente_2,_Ghana.JPG) — ZSM; CC BY-SA 3.0 |
| 20 | KENTE | TIEDYE (0.988) | no | [Ewe kente stripes, Ghana.jpg](https://commons.wikimedia.org/wiki/File:Ewe_kente_stripes,_Ghana.jpg) — ZSM; CC BY-SA 3.0 |
| 21 | KENTE_PRINT | KENTE_PRINT (0.858) | yes | [Broek van Afrikaanse kente stof- Stichting Nationaal Museum van Wereldculturen - R-3633b.jpg](https://commons.wikimedia.org/wiki/File:Broek_van_Afrikaanse_kente_stof-_Stichting_Nationaal_Museum_van_Wereldculturen_-_R-3633b.jpg) — Unknown author Unknown author; CC BY-SA 3.0 |
| 22 | LACE | LACE (0.892) | yes | [Transparent lace top and skirt.png](https://commons.wikimedia.org/wiki/File:Transparent_lace_top_and_skirt.png) — Lukasz Dunikowski; CC BY 2.0 |
| 23 | LACE | SILK (0.999) | no | [Unidentified woman c1850 daguerreotype by Southworth & Hawes.jpg](https://commons.wikimedia.org/wiki/File:Unidentified_woman_c1850_daguerreotype_by_Southworth_%26_Hawes.jpg) — Southworth &amp; Hawes en; Public domain |
| 24 | LINEN | BROCADE (0.954) | no | [Egypt Coptic fabric.jpg](https://commons.wikimedia.org/wiki/File:Egypt_Coptic_fabric.jpg) — Anonymous ( Egypt ) Unknown author; Public domain |
| 25 | LINEN | BATIK (0.782) | no | [Linen Coptic fabric REM.JPG](https://commons.wikimedia.org/wiki/File:Linen_Coptic_fabric_REM.JPG) — BrokenSphere; CC BY-SA 3.0 |
| 26 | SILK | BATIK (0.980) | no | ['Sari' from Varanasi (north-central India), silk and gold-wrapped silk yarn with supplementary weft brocade.jpg](https://commons.wikimedia.org/wiki/File:%27Sari%27_from_Varanasi_(north-central_India),_silk_and_gold-wrapped_silk_yarn_with_supplementary_weft_brocade.jpg) — Unknown author Unknown author; Public domain |
| 27 | SILK | COTTON_PLAIN (0.437) | no | [Sea silk glove lo-res.JPG](https://commons.wikimedia.org/wiki/File:Sea_silk_glove_lo-res.JPG) — John Hill; CC BY-SA 3.0 |
| 28 | TIEDYE | BROCADE (0.749) | no | [Bai fabric, tie-dyed - Yunnan Provincial Museum - DSC02211.JPG](https://commons.wikimedia.org/wiki/File:Bai_fabric,_tie-dyed_-_Yunnan_Provincial_Museum_-_DSC02211.JPG) — Daderot; Public domain |
| 29 | TIEDYE | SILK (0.990) | no | [Tension Fabric Building.jpg](https://commons.wikimedia.org/wiki/File:Tension_Fabric_Building.jpg) — Montanabw; Public domain |
| 30 | VELVET | ANKARA (0.923) | no | [Iner ornamental velvet covering Scroll of the Law,.jpg](https://commons.wikimedia.org/wiki/File:Iner_ornamental_velvet_covering_Scroll_of_the_Law,.jpg) — Daniel Ventura; CC BY-SA 3.0 |
| 31 | VELVET | KENTE_PRINT (0.616) | no | [Ornamental velvet covering Scroll of the Law,.jpg](https://commons.wikimedia.org/wiki/File:Ornamental_velvet_covering_Scroll_of_the_Law,.jpg) — Daniel Ventura; CC BY-SA 3.0 |
| 32 | WAX | TIEDYE (0.802) | no | [Ankara 4372.jpg](https://commons.wikimedia.org/wiki/File:Ankara_4372.jpg) — User:Darwinek; CC BY-SA 3.0 |
| 33 | WAX | KENTE (0.702) | no | [NGC7293 (2004).jpg](https://commons.wikimedia.org/wiki/File:NGC7293_(2004).jpg) — NASA, ESA, C.R. O'Dell (Vanderbilt University), M. Meixner and P. McCullough (STScI); Public domain |

Raw per-image scores and source URLs are in `real-fabric-report.json`.
