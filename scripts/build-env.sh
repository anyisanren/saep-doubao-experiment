#!/bin/bash
# Shared local build setup. Source from build.sh or build-slab.sh.
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"
if [[ -z "${JAVA_HOME:-}" && -d .tooling/amazon-corretto-17.jdk/Contents/Home ]]; then
  export JAVA_HOME="$PROJECT_ROOT/.tooling/amazon-corretto-17.jdk/Contents/Home"
fi
if [[ -z "${ANDROID_SDK_ROOT:-}" ]]; then
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    export ANDROID_SDK_ROOT="$ANDROID_HOME"
  elif [[ -d .tooling/android-sdk ]]; then
    export ANDROID_SDK_ROOT="$PROJECT_ROOT/.tooling/android-sdk"
  fi
fi
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK}"
mkdir -p .signing
if [[ ! -f .signing/saep-lab.jks ]]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -noprompt -keystore .signing/saep-lab.jks \
    -storepass saep-lab-local -alias saep-lab -keypass saep-lab-local \
    -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=SAEP Local Experiment'
fi
GRADLE_COMMAND=./gradlew
if [[ -x .tooling/gradle-8.9/bin/gradle ]]; then
  GRADLE_COMMAND=.tooling/gradle-8.9/bin/gradle
fi
