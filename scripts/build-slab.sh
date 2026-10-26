#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/build-env.sh"
# Preserve the frozen apks/ archive and the original runs/ evidence.
LINT_TASKS=()
for VARIANT in Base A B G21 G22 G23 G24a G24b G24c G25 G26 G27 G28 G29 G2a G2f G2b G2c G2d G2e G43 Dyn; do
  LINT_TASKS+=(":slab:lint${VARIANT}Release")
done
"$GRADLE_COMMAND" --no-daemon :slab:assembleRelease "${LINT_TASKS[@]}" "$@"
mkdir -p dist/slab-rebuilt
for APK in slab/build/outputs/apk/*/release/slab-*-release.apk; do
  NAME="$(basename "$APK")"
  cp "$APK" "dist/slab-rebuilt/$NAME"
  "$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" verify "dist/slab-rebuilt/$NAME"
done
(cd dist/slab-rebuilt && shasum -a 256 ./*.apk > SHA256SUMS.txt)
printf 'Rebuilt APKs: %s/dist/slab-rebuilt/\n' "$PROJECT_ROOT"
