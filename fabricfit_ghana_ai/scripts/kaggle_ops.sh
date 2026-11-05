#!/usr/bin/env bash
# Official Kaggle CLI helper. Credentials must be configured securely outside Git.
# Never prints ~/.kaggle/kaggle.json or secret environment values.
set -euo pipefail
cmd=${1:-help}; shift || true
case "$cmd" in
  discover)
    kaggle datasets list -s "african fabric" --page-size 20
    ;;
  inspect-source)
    kaggle datasets metadata mikuns/african-fabric -p "${1:-/tmp/fabricfit-kaggle-source-metadata}"
    echo "Metadata downloaded. Inspect dataset-metadata.json and licence before any use."
    ;;
  download-source)
    out=${1:-/tmp/fabricfit-kaggle-source}; mkdir -p "$out"
    kaggle datasets download -d mikuns/african-fabric -p "$out" --unzip
    echo "Downloaded for local licence/content audit only; do not merge until licence and labels are verified."
    ;;
  notebook-push)
    dir=${1:?usage: kaggle_ops.sh notebook-push NOTEBOOK_DIR}
    kaggle kernels push -p "$dir"
    ;;
  notebook-status)
    slug=${1:?usage: kaggle_ops.sh notebook-status USER/NOTEBOOK}
    kaggle kernels status "$slug"
    ;;
  notebook-output)
    slug=${1:?usage: kaggle_ops.sh notebook-output USER/NOTEBOOK [OUTPUT_DIR]}
    out=${2:-fabricfit_kaggle_output}; mkdir -p "$out"
    kaggle kernels output "$slug" -p "$out"
    ;;
  help|*)
    echo "Usage: $0 {discover|inspect-source|download-source|notebook-push DIR|notebook-status USER/SLUG|notebook-output USER/SLUG [DIR]}"
    exit 2
    ;;
esac
