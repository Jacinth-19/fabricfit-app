# FabricFit bundled model contract audit

Generated: `2026-11-04T11:33:51.339075+00:00`

**8/8 models loaded, invoked, and matched their declared contracts.**

This verifies packaging and inference validity, not real-world model quality.
Real-fabric results are documented separately in `real-fabric-report.md`.

| Model | Purpose | Bytes | Input | Output | SHA-256 |
| --- | --- | ---: | --- | --- | --- |
| `ghanaian_fabric_classifier.tflite` | 17-family textile appearance classifier | 87,552 | [1, 128, 128, 3] float32 | [1, 17] float32 | `07299df5dea2f25f…` |
| `weave_motif_extractor.tflite` | directional weave and motif feature extractor | 16,456 | [1, 128, 128, 3] float32 | [1, 6] float32 | `eda1bca51dd38208…` |
| `mobilenet_v2_1.0_224_quant.tflite` | ImageNet textile/out-of-distribution hint model | 3,577,760 | [1, 224, 224, 3] uint8 | [1, 1001] uint8 | `f08d447cde49b4e0…` |
| `selfie_segmentation.tflite` | portrait foreground segmentation | 12,364 | [1, 256, 256, 3] float32 | [1, 256, 256, 1] float32 | `c937b38d9a81133c…` |
| `deeplabv3_257_mv.tflite` | DeepLabV3 PASCAL person segmentation | 2,779,264 | [1, 257, 257, 3] float32 | [1, 257, 257, 21] float32 | `68a539782c2c6a72…` |
| `human_parsing_5class.tflite` | five-zone human parsing | 25,268 | [1, 128, 128, 3] float32 | [1, 128, 128, 5] float32 | `ca1ec6d936d5ebc4…` |
| `cloth_warper_shading.tflite` | cloth displacement and fold shading | 25,800 | [1, 64, 64, 4] float32 | [1, 64, 64, 3] float32 | `beaf9d54c2d0e260…` |
| `fitmatch_ranker.tflite` | 16-feature recommendation ranker | 6,724 | [1, 16] float32 | [1, 1] float32 | `b363aa4d664c5bbf…` |

Only the classifier, weave extractor, and ImageNet OOD model accept fabric
photos. Person segmentation, cloth warping, and recommendation ranking require
different inputs and are covered here by tensor/inference contract checks.
