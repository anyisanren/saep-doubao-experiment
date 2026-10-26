#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/build-env.sh"
mkdir -p .signing artifacts dist
if [[ "${SECURITY_RESTRICTED:-0}" == "1" ]]; then
  "$GRADLE_COMMAND" --no-daemon :app:assembleModifyRelease :app:assembleDeleteRelease \
    :app:lintModifyRelease :app:lintDeleteRelease -PsecurityRestricted=true "$@"
  PROFILES="modify delete"
else
  "$GRADLE_COMMAND" --no-daemon :app:assembleRelease :app:lintLabRelease \
    :app:lintControlRelease :app:lintModifyRelease :app:lintDeleteRelease "$@"
  PROFILES="lab control modify delete"
fi
for PROFILE in $PROFILES; do
  METADATA="app/build/outputs/apk/$PROFILE/release/output-metadata.json"
  APP_VERSION=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["elements"][0]["versionName"])' "$METADATA")
  if [[ "$PROFILE" == "lab" ]]; then APK_NAME="AgentLab-v${APP_VERSION}.apk"
  else APK_NAME="AgentLab-${PROFILE}-v${APP_VERSION}.apk"; fi
  cp "app/build/outputs/apk/$PROFILE/release/app-$PROFILE-release.apk" "dist/$APK_NAME"
  "$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" verify --verbose "dist/$APK_NAME"
done
(cd dist && shasum -a 256 AgentLab-*.apk > SHA256SUMS.txt)
