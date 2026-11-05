# FabricFit Ghana model card — NOT TRAINED

**Status:** no 8-class model exists. No Keras model or TFLite file was produced.
The app's existing classifier is a separate 17-class demo model and has not been
replaced by this project.

## Intended use

Research prototype for recognizing the eight target groups: Kente; Ankara/African
wax print; Adinkra; batik; tie-dye/Adire; lace; Ghanaian Fugu/smock; Other/Unknown.
Not suitable for commercial decisions, cultural authentication, pricing, or
claims that a cloth is authentic. Cultural labels require expert confirmation.

## Data and evaluation

The prior reviewed pool has only 21 accepted candidate images that map
conservatively to these eight classes (4, 3, 4, 1, 2, 4, 3, 0). These are metadata
rows, not downloaded binaries in this project. The existing Kaggle starter
dataset's accessibility and licence were not verifiable because Kaggle
credentials are absent; it was excluded. No train/validation/test split was
created, no model was trained, and there are no test metrics.

## Limitations

The mapping from a prior 17-family search/review to this 8-class task is not a
Ghanaian textile expert certification. Kente-print, Gonja and uncertain cases are
not silently relabelled. The “Other/Unknown” class has no reviewed examples.
This pool cannot represent the material, regional, lighting, device and context
variation needed in use.

## Release gates

Before training: verify legal rights row-by-row, download and hash images, review
all pixels, confirm labels with qualified local practitioners, add independent
cloth/source sessions, and reach >=300 train images/class plus nonempty disjoint
validation/test groups. Before public accuracy claims or shipping: >=1,000 train
images/class plus a sufficiently large, representative, held-out field test.
Report per-class precision/recall/F1 and uncertainty; do not substitute validation
or training accuracy for test results.
