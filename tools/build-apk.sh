#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 tests/check_project.py
gradle --no-daemon :app:assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/FlipMiniOS-debug.apk
echo "APK: $(pwd)/dist/FlipMiniOS-debug.apk"
