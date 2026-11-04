# Curated real online fabric smoke test

Generated: `2026-11-04T20:24:52.947381+00:00`

> **Not a production accuracy claim.** These freely licensed Commons images were
> manually selected from source descriptions and visually reviewed to remove obvious
> search contamination. They are not expert fiber/heritage certifications; Gonja is
> represented by a Northern Ghana smock proxy. One photo per family is only a
> diagnostic smoke test, not a validation set or a measure of real-world accuracy.
> The result below is raw classifier top-1, before Android's heuristic/OOD fusion.

- Model contract audit: **not supplied**
- Coverage: **16/17 fabric families**
- Images evaluated: **53**
- Raw classifier top-1 agreement: **0.075** (4/53)
- Missing families: **DENIM**
- Near-duplicates of classifier training/catalog images (dHash ≤ 6): **0**

The app remains an `ON_DEVICE_DEMO`; this smoke result must not be used as an
advertised accuracy metric. Production validation needs hundreds to thousands of
consented, human-verified field photos per family with leak-free held-out splits.

## Per-family results

| Family | Images | Correct | Raw top-1 agreement |
| --- | ---: | ---: | ---: |
| ADINKRA | 4 | 0 | 0.000 |
| ANKARA | 1 | 0 | 0.000 |
| BATIK | 1 | 1 | 1.000 |
| BROCADE | 8 | 0 | 0.000 |
| CHIFFON | 5 | 0 | 0.000 |
| COTTON_PLAIN | 2 | 0 | 0.000 |
| DENIM | 0 | 0 | 0.000 |
| FUGU | 3 | 0 | 0.000 |
| GONJA | 1 | 0 | 0.000 |
| KENTE | 3 | 1 | 0.333 |
| KENTE_PRINT | 1 | 0 | 0.000 |
| LACE | 4 | 1 | 0.250 |
| LINEN | 4 | 0 | 0.000 |
| SILK | 5 | 0 | 0.000 |
| TIEDYE | 1 | 0 | 0.000 |
| VELVET | 8 | 1 | 0.125 |
| WAX | 2 | 0 | 0.000 |

## Image-level predictions and attribution

Thumbnails in `real-fabric-contact-sheet.jpg` remain under each source image's
original license. Follow the landing-page links for attribution details.

| # | Expected | Predicted (confidence) | Match | Source and license |
| ---: | --- | --- | --- | --- |
| 1 | ADINKRA | LACE (0.982) | no | [Adinkra 1825.jpg](https://commons.wikimedia.org/wiki/File:Adinkra_1825.jpg) — Unknown author Unknown author; Public domain |
| 2 | ADINKRA | ANKARA (0.958) | no | [Adinkra mourning cloth collected in Kumasi in 1817](https://commons.wikimedia.org/wiki/File:Adinkra_cloth.JPG) — Photo of public-domain textile via British Museum; Public domain |
| 3 | ADINKRA | ANKARA (0.978) | no | [ADINKRA_NtonsoAdinkra.jpg.jpg](https://commons.wikimedia.org/wiki/File:NtonsoAdinkra.jpg) — ArtProf; CC BY-SA 3.0 |
| 4 | ADINKRA | ANKARA (0.967) | no | [ADINKRA_NtonsoAdinkraComb.jpg.jpg](https://commons.wikimedia.org/wiki/File:NtonsoAdinkraComb.jpg) — ArtProf; CC BY-SA 3.0 |
| 5 | ANKARA | SILK (0.994) | no | [Ankara Fabric With A Touch Of Smile.jpg](https://commons.wikimedia.org/wiki/File:Ankara_Fabric_With_A_Touch_Of_Smile.jpg) — Daniel Anya; CC BY-SA 4.0 |
| 6 | BATIK | BATIK (0.589) | yes | [Batik Indonesia](https://commons.wikimedia.org/wiki/File:Batik_Indonesia.jpg) — MartijnL; CC BY-SA 3.0 |
| 7 | BROCADE | BATIK (0.980) | no | [Silk sari with supplementary-weft brocade](https://commons.wikimedia.org/wiki/File:%27Sari%27_from_Varanasi_%28north-central_India%29%2C_silk_and_gold-wrapped_silk_yarn_with_supplementary_weft_brocade.jpg) — Unknown author; Public domain |
| 8 | BROCADE | ANKARA (0.855) | no | [Benaras brocade sari.jpg](https://commons.wikimedia.org/wiki/File:Benaras_brocade_sari.jpg) — Anilbhardwajnoida; CC BY-SA 3.0 |
| 9 | BROCADE | KENTE (1.000) | no | [BROCADE_First_Lady_Betty_Ford_s_pink_brocade_gown.jpg.jpg](https://commons.wikimedia.org/wiki/File:First_Lady_Betty_Ford%27s_pink_brocade_gown.jpg) — Frankie Welch; Public domain |
| 10 | BROCADE | LACE (0.483) | no | [Konia, brocade sari.jpg](https://commons.wikimedia.org/wiki/File:Konia,_brocade_sari.jpg) — Anilbhardwajnoida; CC BY-SA 3.0 |
| 11 | BROCADE | LACE (0.976) | no | [BROCADE_Textile_with_Design_of_Geometric_and_Floral_Meanders_LACMA_M.63.4.1.jpg.jpg](https://commons.wikimedia.org/wiki/File:Textile_with_Design_of_Geometric_and_Floral_Meanders_LACMA_M.63.4.1.jpg) — After James Lehman (England, circa 1688-1745); Public domain |
| 12 | BROCADE | LACE (0.586) | no | [BROCADE_Woven_Fabric_ST520_-_Woven_Fabric_-_MoMu_Antwerp.jpg.jpg](https://commons.wikimedia.org/wiki/File:Woven_Fabric_(ST520)_-_Woven_Fabric_-_MoMu_Antwerp.jpg) — creator not supplied; CC BY-SA 4.0 |
| 13 | BROCADE | ADINKRA (0.999) | no | [BROCADE_Woven_Fabric_ST521_-_Woven_Fabric_-_MoMu_Antwerp.jpg.jpg](https://commons.wikimedia.org/wiki/File:Woven_Fabric_(ST521)_-_Woven_Fabric_-_MoMu_Antwerp.jpg) — creator not supplied; CC BY-SA 4.0 |
| 14 | BROCADE | LACE (0.945) | no | [BROCADE_Woven_Fabric_ST82A_-_Woven_Fabric_-_MoMu_Antwerp.jpg.jpg](https://commons.wikimedia.org/wiki/File:Woven_Fabric_(ST82A)_-_Woven_Fabric_-_MoMu_Antwerp.jpg) — creator not supplied; CC BY-SA 4.0 |
| 15 | CHIFFON | ADINKRA (1.000) | no | [1970s Givenchy orange printed silk chiffon dress detail](https://commons.wikimedia.org/wiki/File:1970s_Givenchy_cocktail_dress%2C_orange_printed_silk_chiffon_03.jpg) — Rhode Island School of Design Museum staff photographer; CC0 1.0 |
| 16 | CHIFFON | KENTE_PRINT (0.957) | no | [Chiffon 2162 Dress.jpg](https://commons.wikimedia.org/wiki/File:Chiffon_2162_Dress.jpg) — User:ShenkarArchive; CC BY-SA 3.0 |
| 17 | CHIFFON | KENTE (0.947) | no | [First Lady Betty Ford's colorful chiffon silk gown.jpg](https://commons.wikimedia.org/wiki/File:First_Lady_Betty_Ford%27s_colorful_chiffon_silk_gown.jpg) — Albert Capraro; Public domain |
| 18 | CHIFFON | KENTE_PRINT (0.933) | no | [First Lady Betty Ford's green chiffon gown with cape.jpg](https://commons.wikimedia.org/wiki/File:First_Lady_Betty_Ford%27s_green_chiffon_gown_with_cape.jpg) — Albert Capraro; Public domain |
| 19 | CHIFFON | TIEDYE (1.000) | no | [Turquoise Chiffon Sari.jpg](https://commons.wikimedia.org/wiki/File:Turquoise_Chiffon_Sari.jpg) — Samira Khan; CC BY 2.0 |
| 20 | COTTON_PLAIN | KENTE_PRINT (0.474) | no | [COTTON_PLAIN_Fitting_muslin_dummy.jpg.jpg](https://commons.wikimedia.org/wiki/File:Fitting_muslin_dummy.jpg) — maco *nix; CC BY-SA 2.0 |
| 21 | COTTON_PLAIN | DENIM (0.531) | no | [Woman's muslin dress c. 1855.jpg](https://commons.wikimedia.org/wiki/File:Woman%27s_muslin_dress_c._1855.jpg) — Unknown author Unknown author; Public domain |
| 22 | FUGU | ANKARA (0.620) | no | [Ghanaian smock dress](https://commons.wikimedia.org/wiki/File:Dress_12.jpg) — Kinvidia; CC BY-SA 4.0 |
| 23 | FUGU | DENIM (0.389) | no | [MAN DRESSED IN LOCAL ATTIRE 01.jpg](https://commons.wikimedia.org/wiki/File:MAN_DRESSED_IN_LOCAL_ATTIRE_01.jpg) — Salifu Wumpini Hussein; CC BY-SA 4.0 |
| 24 | FUGU | SILK (0.994) | no | [Smock (fugu) making 02.jpg](https://commons.wikimedia.org/wiki/File:Smock_(fugu)_making_02.jpg) — daSupremo; CC BY-SA 4.0 |
| 25 | GONJA | TIEDYE (1.000) | no | [Smock from Northern Ghana (Gonja-style strip-cloth proxy)](https://commons.wikimedia.org/wiki/File:Smock_from_Northern_Ghana.jpg) — Ibn Shiraz; CC BY-SA 4.0 |
| 26 | KENTE | WAX (0.877) | no | [Ewe kente cloth from Ghana](https://commons.wikimedia.org/wiki/File:Ewe_kente_2%2C_Ghana.JPG) — ZSM; CC BY-SA 3.0 |
| 27 | KENTE | TIEDYE (0.988) | no | [Ewe kente stripes, Ghana.jpg](https://commons.wikimedia.org/wiki/File:Ewe_kente_stripes,_Ghana.jpg) — ZSM; CC BY-SA 3.0 |
| 28 | KENTE | KENTE (1.000) | yes | [KENTE_Kente_Cloth_MET_1972.56.1_d1.jpg.jpg](https://commons.wikimedia.org/wiki/File:Kente_Cloth_MET_1972.56.1_d1.jpg) — creator not supplied; CC0 1.0 |
| 29 | KENTE_PRINT | GONJA (0.965) | no | [Kente-style batik/printed cloth in Kumasi market](https://commons.wikimedia.org/wiki/File:Kente_(Batik)_Cloth_in_Market_-_Kumasi_-_Ghana_(4755541241).jpg) — Adam Jones; CC BY-SA 2.0 |
| 30 | LACE | BATIK (0.734) | no | [African Lace VLM 02.jpg](https://commons.wikimedia.org/wiki/File:African_Lace_VLM_02.jpg) — Wolf D.; Public domain |
| 31 | LACE | KENTE_PRINT (0.944) | no | [LACE_African_Lace_VLM_04.jpg.jpg](https://commons.wikimedia.org/wiki/File:African_Lace_VLM_04.jpg) — Wolf D.; Public domain |
| 32 | LACE | ANKARA (0.997) | no | [LACE_Bolero_lace_AM_2994-6_.jpg.jpg](https://commons.wikimedia.org/wiki/File:Bolero,_lace_(AM_2994-6).jpg) — Unknown author Unknown author; CC BY 4.0 |
| 33 | LACE | LACE (0.892) | yes | [Transparent lace top and skirt](https://commons.wikimedia.org/wiki/File:Transparent_lace_top_and_skirt.png) — Lukasz Dunikowski; CC BY 2.0 |
| 34 | LINEN | VELVET (1.000) | no | [LINEN_Lavender_evening_gown_by_Sybil_Connolly_fabric_detail.jpg.jpg](https://commons.wikimedia.org/wiki/File:Lavender_evening_gown_by_Sybil_Connolly_fabric_detail.jpg) — Hunt Museum and Eamonn O'Mahony; CC0 1.0 |
| 35 | LINEN | BATIK (0.782) | no | [Linen Coptic fabric](https://commons.wikimedia.org/wiki/File:Linen_Coptic_fabric_REM.JPG) — BrokenSphere; CC BY-SA 3.0 |
| 36 | LINEN | ANKARA (0.884) | no | [LINEN_Piece_of_woolen_and_linen_fabric_8th_century.jpg.jpg](https://commons.wikimedia.org/wiki/File:Piece_of_woolen_and_linen_fabric_8th_century.jpg) — Clemens Schmillen; CC BY-SA 4.0 |
| 37 | LINEN | LACE (0.832) | no | [LINEN_Three_Linens_3468431784_.jpg.jpg](https://commons.wikimedia.org/wiki/File:Three_Linens_(3468431784).jpg) — Budlee Fabrics; CC BY-SA 2.0 |
| 38 | SILK | ANKARA (0.748) | no | [Dress or Furnishing Fabric Italy or France, Textile. Silk brocade 30 1-2 x 21 3-4' LACMA M.81.69.6.jpg](https://commons.wikimedia.org/wiki/File:Dress_or_Furnishing_Fabric_Italy_or_France,_Textile._Silk_brocade_30_1-2_x_21_3-4%27_LACMA_M.81.69.6.jpg) — creator not supplied; Public domain |
| 39 | SILK | ADINKRA (1.000) | no | [Indian-dupioni-silk-fabrics.JPG](https://commons.wikimedia.org/wiki/File:Indian-dupioni-silk-fabrics.JPG) — Smriti tripathi; CC BY 3.0 |
| 40 | SILK | ANKARA (0.799) | no | [Neoclassical furnishing fabric, Les quatre parties du monde, Lyon France, c. 1785, silk lampas - Patricia Harris Gallery of Textiles & Costume, Royal Ontario Museum - DSC09410.jpg](https://commons.wikimedia.org/wiki/File:Neoclassical_furnishing_fabric,_Les_quatre_parties_du_monde,_Lyon_France,_c._1785,_silk_lampas_-_Patricia_Harris_Gallery_of_Textiles_%26_Costume,_Royal_Ontario_Museum_-_DSC09410.jpg) — Daderot; CC0 1.0 |
| 41 | SILK | KENTE (0.707) | no | [Persian Silk Brocade - Bergamot Armlet - Seyyed Hossein Mozhgani - 1972.jpg](https://commons.wikimedia.org/wiki/File:Persian_Silk_Brocade_-_Bergamot_Armlet_-_Seyyed_Hossein_Mozhgani_-_1972.jpg) — Sialkgraph; CC BY-SA 3.0 |
| 42 | SILK | VELVET (0.997) | no | [Rajshahi silk fabric, Sopura Silk Mills Ltd (02).jpg](https://commons.wikimedia.org/wiki/File:Rajshahi_silk_fabric,_Sopura_Silk_Mills_Ltd_(02).jpg) — Moheen Reeyad; CC BY-SA 4.0 |
| 43 | TIEDYE | BROCADE (0.767) | no | [Bai tie-dyed fabric](https://commons.wikimedia.org/wiki/File:Bai_fabric%2C_tie-dyed_-_Yunnan_Provincial_Museum_-_DSC02211.JPG) — Daderot; Public domain |
| 44 | VELVET | VELVET (1.000) | yes | [VELVET_Blue_velvet.jpg.jpg](https://commons.wikimedia.org/wiki/File:Blue_velvet.jpg) — Isokivi; CC BY-SA 3.0 |
| 45 | VELVET | DENIM (1.000) | no | [Devoré velvet (also known as burnout technique).jpg](https://commons.wikimedia.org/wiki/File:Devor%C3%A9_velvet_(also_known_as_burnout_technique).jpg) — Libby norman; CC BY-SA 3.0 |
| 46 | VELVET | ADINKRA (0.999) | no | [VELVET_Jacka_-_del_av_Nordstjärneordens_dräkt_från_cirka_1820_-_Livrustkammaren_.jpg](https://commons.wikimedia.org/wiki/File:Jacka_-_del_av_Nordstj%C3%A4rneordens_dr%C3%A4kt_fr%C3%A5n_cirka_1820_-_Livrustkammaren_-_108329.tif) — Unknown author Unknown author; Public domain |
| 47 | VELVET | KENTE_PRINT (0.616) | no | [Ornamental velvet covering](https://commons.wikimedia.org/wiki/File:Ornamental_velvet_covering_Scroll_of_the_Law%2C.jpg) — Daniel Ventura; CC BY-SA 3.0 |
| 48 | VELVET | WAX (0.982) | no | [Velvet from China, late 19th century, Honolulu Museum of Art, 3228.JPG](https://commons.wikimedia.org/wiki/File:Velvet_from_China,_late_19th_century,_Honolulu_Museum_of_Art,_3228.JPG) — Hiart; CC0 1.0 |
| 49 | VELVET | COTTON_PLAIN (0.996) | no | [VELVET_Woven_Fabric_ST196_-_MoMu_Antwerp.jpg.jpg](https://commons.wikimedia.org/wiki/File:Woven_Fabric_(ST196)_-_MoMu_Antwerp.jpg) — creator not supplied; CC BY-SA 4.0 |
| 50 | VELVET | COTTON_PLAIN (0.810) | no | [VELVET_Woven_Fabric_ST197_-_MoMu_Antwerp.jpg.jpg](https://commons.wikimedia.org/wiki/File:Woven_Fabric_(ST197)_-_MoMu_Antwerp.jpg) — creator not supplied; CC BY-SA 4.0 |
| 51 | VELVET | COTTON_PLAIN (0.923) | no | [VELVET_Woven_Fabric_ST198_-_MoMu_Antwerp.jpg.jpg](https://commons.wikimedia.org/wiki/File:Woven_Fabric_(ST198)_-_MoMu_Antwerp.jpg) — creator not supplied; CC BY-SA 4.0 |
| 52 | WAX | TIEDYE (0.950) | no | [Wax print.jpeg](https://commons.wikimedia.org/wiki/File:Wax_print.jpeg) — M_nunoo; CC BY-SA 3.0 |
| 53 | WAX | BATIK (0.802) | no | [Wax prints in a West African fabric shop](https://commons.wikimedia.org/wiki/File:Waxprints_in_a_West_African_Shop.jpg) — Alexander Sarlay; CC BY-SA 3.0 |

Raw per-image scores and source URLs are in `real-fabric-report.json`.
