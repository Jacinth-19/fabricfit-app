# Catalogue, recommendations, and the on-device models

This answers the four practical questions about how FabricFit sources looks, how it
recommends them, how search filters work, and what the bundled TensorFlow Lite models
can and cannot do.

## 1. Where the look images come from

Every image in the app is **bundled in the APK** under `app/src/main/assets/catalog/`.
Nothing is fetched from the network, so Discover, Home and Recommendations are fully
usable offline and with no account.

| Folder | Contents | How it was made |
| --- | --- | --- |
| `catalog/photos/` | 7 full-body editorial photographs: 4 women, 3 men, in kente, ankara, lace, kaftan, agbada and batakari smock | Generated for this app |
| `catalog/looks/` | 12 look cards, each tied to fabric categories | Cropped from `photos/` |
| `catalog/fabrics/` | 19 fabric swatch tiles | Bundled reference swatches |
| `catalog/heritage/` | Hero banners, avatars, outfit cut-outs | Gathered brand assets |
| `catalog/samples/` | The Ghanaian model photo and kente swatch used by the try-on demo | Bundled samples |

`catalog/looks.json` is the index. Each entry records:

```json
{
  "id": "look_kente_women_1",
  "gender": "WOMEN",
  "fabrics": ["KENTE", "KENTE_PRINT"],
  "asset": "catalog/looks/look_kente_women_1.jpg",
  "photo": "catalog/photos/women_kente_01.jpg",
  "scene": "Ankara & kente editorial"
}
```

The `fabrics` array is what makes recommendations work: a scanned fabric is matched to
looks by fabric category, not by comparing pixels.

### If you want hundreds of looks instead of 7

The data layer already abstracts the source. `CatalogDataSource` has one implementation,
`AssetCatalogDataSource`, which reads the bundled JSON. A `RemoteCatalogDataSource` that
fetches the same JSON shape over HTTP is a drop-in replacement — no screen changes.
Bundled images would then become URLs, which means paying for image hosting and losing
offline support for those cards. That is a product decision, not a coding one.

**Licensing.** The photos in `catalog/photos/` were generated for this app; the previous
`looks/` set was replaced for the same reason. If you add third-party photography, keep
the licence with the file and record it in `looks.json`, or you will not be able to prove
you had the right to ship it.

## 2. How the recommendation system works

`RecommendationEngine` scores every style against a scanned fabric and returns an
explainable 0–98 match. It is a small neural ranker (`fitmatch_ranker.tflite`, 16 inputs,
1 output) combined with explicit rules.

**The 16 inputs** are built in `buildFeatureVector`:

| Feature | Meaning |
| --- | --- |
| Target structure vs style structure | A stiff brocade should not become a fluid gown |
| Motif scale | Large motifs need large panels, or they get cut up at the seams |
| Large-panel / fitted flags | Silhouette family of the style |
| Fabric affinity | Does the style's `affinity` list name this fabric category |
| Occasion overlap | Fabric's suggested uses ∩ style occasions |
| Palette saturation | Bright cloth versus muted |
| Traditional match | Traditional cloth + traditional style |
| Skin-tone harmony | `ColorHarmonyEngine` against the wearer's undertone |
| Weave anisotropy | From the Gabor pass: strong directional weave suits panel-aligned cuts |
| Waist-to-hip fit | From saved measurements, when the user has entered them |
| Height fit | Silhouettes that flatter tall or petite frames |
| Favourite affinity | Styles the user has favourited before |
| Gender match | The user's preferred category |

Every recommendation carries `WhyReason` tags so the UI can say *why* it matched, instead
of showing an unexplained percentage.

**Today the signals are:** fabric analysis (colour, pattern, texture, motif scale,
stiffness), the style catalogue's own metadata, optional measurements, optional skin
tone, and favourite history. **There is no behavioural data yet** — no click-through,
no purchase history, no A/B test. Adding those is what turns this from a
content-based recommender into a collaborative one, and it needs a backend to store
events.

## 3. Search and gender filters

`DiscoverScreen` already has the filters you asked for:

- **Gender:** All / Women / Men. Men also matches `UNISEX` styles, so a unisex kaftan
  appears for everyone.
- **Category:** All / Casual / Formal / Occasion.
- **Sort:** Best match, Trending (by heritage rating), Popular (by review count).
- **Text search** across fabric, style and keyword, with autocomplete.

The catalogue behind it currently holds 20 styles: 10 women's, 8 men's, 2 unisex.

## 4. Is there a TFLite model that can do virtual try-on?

**Short answer: no. Not as a generative try-on, and nothing that runs in reasonable time
on a phone.**

The app's try-on is real and works offline, but it is **geometry plus compositing**, not
a diffusion model. `PersonSegmenter` finds the person, `BodyFit` measures shoulders,
bust, waist, hip and thigh from the mask, the garment is fitted as vector polygons, and
the fabric is drawn through them with shading. That is why a look renders in about
150 ms on an emulator.

What the bundled models actually do:

| Model | Role | Real? |
| --- | --- | --- |
| `deeplabv3_257_mv.tflite` | Person segmentation | Yes, full network |
| `mobilenet_v1_1.0_224_quant.tflite` | Out-of-distribution gate (is this even fabric?) | Yes, full network (89 tensors). Weak on flat swatches - see `docs/MODELS.md`. |
| `ghanaian_fabric_classifier.tflite` | 17-class fabric classifier | Yes — see below |
| `selfie_segmentation.tflite` | Second segmentation pass | Small fusion net |
| `human_parsing_5class.tflite` | Body-zone parsing | Small fusion net |
| `weave_motif_extractor.tflite` | Weave/motif features | Small net |
| `cloth_warper_shading.tflite` | Drape shading | Small net |
| `fitmatch_ranker.tflite` | Recommendation ranker | Small net |

The four "small" ones are tensors over a handful of layers, not trained deep networks.
They contribute features; they are not doing the heavy lifting, and they should not be
described as if they were.

### The fabric classifier is genuinely good

I tested `ghanaian_fabric_classifier.tflite` against the real swatches in the app:

- **15 of 15** bundled swatches classified correctly, most at 0.99+ confidence.
- Stable under rotation, mirroring and brightening.
- Degrades sensibly: a desaturated kente reads as BATIK, a darkened lace reads as SILK.
- Correctly reads the bundled kente photos as KENTE.

**One caveat worth knowing:** it is confident on garbage. Solid white and solid black
both come back as LINEN at 1.00, and random noise as ANKARA at 0.997. So the classifier
answers "which of these 17 fabrics is this most like", not "is this fabric at all". The
ImageNet out-of-distribution gate and the spectral checks in `OnDeviceMlClassifier` are
what reject non-fabric photos, and those paths matter more than they look.

### If you want true generative try-on later

The realistic options are server-side: IDM-VTON, OOTDiffusion, or a hosted virtual
try-on API. They need a GPU and a few seconds per image, and they upload the user's
photo to a server — which breaks the current offline and privacy story. A phone-class
diffusion try-on is not viable yet at this quality. The honest framing for the app is
"AI try-on preview" for the fitted overlay, and "photorealistic try-on" only once a
backend exists.
