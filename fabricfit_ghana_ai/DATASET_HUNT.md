# Public dataset search and reuse decision (5 Nov 2026)

The goal is not just to find image files: the images must have documented rights,
correct labels, sufficient independent cloth samples, and useful coverage of the
requested West African categories. A public URL is not a reuse licence.

| Candidate | What the listing/source says | Decision |
| --- | --- | --- |
| [African Fabric Images — Kaggle, mikuns](https://www.kaggle.com/datasets/mikuns/african-fabric) | Exact Kaggle listing search result reports `license: Unknown`; author portfolio dates it to 2018. Kaggle API discovery was also blocked in this run by missing auth. | **Do not download/repackage/train from it** until Kaggle listing, files, labels and rights are checked by an authenticated account and a suitable licence is confirmed. |
| [TextileNet — GitHub](https://github.com/hahashu/TextileNet) / [paper](https://arxiv.org/abs/2301.06160) | Large general textile dataset: paper describes 33 fibre and 27 fabric labels; project instructs users to download separate archives from Google Drive/OneDrive and run its URL downloader. It is not Ghana/West Africa-labelled. The repository README distinguishes MIT collection code from dataset/image rights; the latter must be checked against the paper and actual records. | **Best generic pretraining lead, not a target-label source.** Do not collapse its generic `lace`, `denim`, etc. into Kente, Adinkra, wax print, or Fugu. Images are not hosted in this GitHub repository. Download host is outside this sandbox's network allowlist. Require a licence audit and image-level labels before importing. |
| [Kente Cloth — UNC Digital Collections](https://cdr.lib.unc.edu/concern/multimeds/mc87q2651) | Catalog record describes 23 images of Kente production in Adanwomse, Ghana, but marks rights **In Copyright**. | **Do not redistribute** to this GitHub project or training data without permission/licence from the rights holder. A public download button is not permission. |
| [Authente-Kente paper](https://link.springer.com/article/10.1007/s00146-020-01055-2) | Research reports a Ghanaian authentic-vs-imitation Kente dataset and cites a GitHub data repository, but that repository currently returns 404. | Not accessible for this run. Contact the authors / rights holder rather than use mirrors or reconstructed copies. |
| [Wikimedia Commons](https://commons.wikimedia.org/wiki/Category:Kente) + prior FabricFit review | Existing visual review accepted 56 candidates across the repository's original 17 labels. A conservative mapping produces only 21 eight-class candidate metadata rows (not image files); `OTHER_UNKNOWN` has none. | **Use only as a small, re-fetch-and-review pool.** The 21 current records preserve attribution/licence strings, but images are not in the bundle and those strings have not been re-fetched in this task. Not enough for an 8-class training set. |

## Practical recommendation

1. Authenticate to Kaggle and check the Kaggle record's current licence, dataset
   description, file inventory and any attribution/redistribution conditions.
   If it remains `Unknown`, ask the uploader for written permission or exclude it.
2. Ask TextileNet authors for dataset image licence confirmation and use permission
   for model training/distribution; inspect its fabric label taxonomy and source
   URLs. It can support generic feature learning, not Ghanaian category labels.
3. Request the 23-photo UNC collection under a suitable licence, or obtain permission
   for private research use; otherwise keep it out of Git and training.
4. Build Ghanaian ground truth with consenting Kente/Adinkra/Fugu makers and traders,
   while recording expert-confirmed labels, physical-cloth/source group, consent,
   licence, and broad location. Keep source images in external/object storage with
   attribution metadata; put scripts/manifests and appropriately licensed small
   examples on GitHub.

## Why the raw corpus is not committed to GitHub now

The largest likely corpus is not available from GitHub and has not been lawfully
verified for redistribution; one local Ghanaian Kente collection is explicitly
“In Copyright”; and the Kaggle source is reported with an unknown licence. Uploading
those binaries to this app repository would create an irreversible copyright and
repo-size problem. The checked-in project therefore contains only metadata, review
reasons, scripts and reports—not unlicensed or unverified image files. Once rights
are confirmed, keep large licensed images in Kaggle Dataset storage or an approved
object store, and version the checksum/provenance manifest here. Do not check in
Kaggle API tokens.
