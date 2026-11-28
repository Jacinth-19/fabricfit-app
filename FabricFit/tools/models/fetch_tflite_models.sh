#!/usr/bin/env bash
# Downloads the real, trained TFLite models that ship in app/src/main/assets/.
#
# Why a script: the binaries are 0.5-4 MB each and are build inputs, not source. They are
# deliberately NOT committed to this repository — CI fetches them (see .github/workflows/
# build.yml) and so should you before building from a fresh clone.
#
#   bash FabricFit/tools/models/fetch_tflite_models.sh
#
# Every model here was picked and verified by hand: tensor counts, input/output shapes and
# actual predictions were checked with ai_edge_litert before it was added. See
# docs/MODELS.md for the provenance and licence of each file.
#
# Licence notes:
#   - Volcomix/virtual-background .............. Apache-2.0
#   - tensorflow/tflite-support ................ Apache-2.0
set -euo pipefail

ASSETS="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../app/src/main/assets" && pwd)"
mkdir -p "$ASSETS"

# fetch <repo> <path-in-repo> <local-filename> <min-bytes>
fetch() {
  local repo="$1" path="$2" dest="$3" min="$4"
  local out="$ASSETS/$dest"

  if [ -f "$out" ]; then
    local have
    have=$(stat -c%s "$out")
    if [ "$have" -ge "$min" ]; then
      echo "  ok       $dest ($have bytes, already present)"
      return 0
    fi
    echo "  refetch  $dest ($have bytes is smaller than expected)"
  fi

  echo "  get      $dest  <- $repo/$path"
  # github.com serves raw bytes when asked through the contents API with this Accept
  # header, which keeps the download to hosts the build environment already trusts.
  curl -fsSL --retry 3 --max-time 180 \
    -H "Accept: application/vnd.github.raw" \
    "https://api.github.com/repos/$repo/contents/$path?ref=HEAD" \
    -o "$out.part"

  local got
  got=$(stat -c%s "$out.part")
  if [ "$got" -lt "$min" ]; then
    echo "  FAIL     $dest: got $got bytes, expected at least $min" >&2
    rm -f "$out.part"
    return 1
  fi
  # A truncated or HTML error page would not carry the TFL3 FlatBuffers identifier.
  if [ "$(dd if="$out.part" bs=1 skip=4 count=4 2>/dev/null)" != "TFL3" ]; then
    echo "  FAIL     $dest: not a TFLite FlatBuffer (bad magic)" >&2
    rm -f "$out.part"
    return 1
  fi
  mv "$out.part" "$out"
  echo "  done     $dest ($got bytes)"
}

echo "==> fetching TFLite models into $ASSETS"

# Person segmentation, MediaPipe selfie segmenter 256x256 float16.
# 360 tensors, [1,256,256,3] -> [1,256,256,1]. Feed 0..255 float.
# Measured on the bundled model photo: 23.4% coverage; on a fabric-only photo: 0.0%.
fetch "Volcomix/virtual-background" \
      "public/models/selfiesegmentation_mlkit-256x256-2021_01_19-v1215.f16.tflite" \
      "selfie_segmentation.tflite" 200000

# Coarse 2-class portrait segmentation (background / person), 355 tensors.
# [1,144,256,3] -> [1,144,256,2]. Used as a lower-resolution cross-check when the
# 256x256 model is unavailable.
fetch "Volcomix/virtual-background" \
      "public/models/segm_full_v679.tflite" \
      "segm_full_v679.tflite" 300000

# ImageNet classifier used as the out-of-distribution gate: "is this even a photo of
# clothing/fabric, or is it a wall / a plate of food?". 89 tensors, 1001 classes,
# Quantized MobileNet v1. uint8 in, uint8 out.
# Sanity: person photo -> suit / bow tie / jean; kente swatch -> prayer rug / quilt.
fetch "tensorflow/tflite-support" \
      "tensorflow_lite_support/acceleration/configuration/testdata/mobilenet_v1_1.0_224_quant.tflite" \
      "mobilenet_v1_1.0_224_quant.tflite" 4000000

echo
echo "==> verifying every model opens and reports its tensors"
python3 - "$ASSETS" <<'PY' || echo "(python check skipped: install ai-edge-litert to enable)"
import sys, pathlib
try:
    from ai_edge_litert.interpreter import Interpreter
except Exception:
    raise SystemExit(1)
root = pathlib.Path(sys.argv[1])
for f in sorted(root.glob("*.tflite")):
    try:
        it = Interpreter(model_path=str(f)); it.allocate_tensors()
        i = it.get_input_details()[0]; o = it.get_output_details()[0]
        print(f"   {f.name:38s} tensors={len(it.get_tensor_details()):4d}  "
              f"{list(i['shape'])} {i['dtype'].__name__} -> {list(o['shape'])}")
    except Exception as e:
        print(f"   {f.name:38s} OPEN FAILED: {e}")
PY
echo "==> done"
