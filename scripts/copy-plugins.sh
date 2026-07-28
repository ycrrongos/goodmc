#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${1:-$ROOT/plugins}"
mkdir -p "$DEST"

copy_jar() {
  local jar="$1"
  if [[ -f "$jar" ]]; then
    cp -f "$jar" "$DEST/"
    echo "Copied $(basename "$jar")"
  else
    echo "Missing: $jar" >&2
    return 1
  fi
}

# Remove old monolithic jar if present
rm -f "$DEST/GoodMC-1.0.0.jar"

copy_jar "$ROOT/adminvote/build/libs/AdminVote-1.0.0.jar"
copy_jar "$ROOT/server-vision/build/libs/Server-Vision-1.0.0.jar"
copy_jar "$ROOT/goodtpa/build/libs/GoodTPA-1.0.0.jar"
copy_jar "$ROOT/qqbridge/build/libs/QQBridge-1.0.0.jar"
copy_jar "$ROOT/mention/build/libs/Mention-1.0.0.jar"
copy_jar "$ROOT/goodmc/build/libs/GoodMC-1.0.0.jar"
copy_jar "$ROOT/server-heldlight/build/libs/Server-HeldLight-1.0.0.jar"
copy_jar "$ROOT/servermenu/build/libs/ServerMenu-1.0.0.jar"
copy_jar "$ROOT/server-fakeplayer/build/libs/Server-FakePlayer-1.0.0.jar"

echo "Done -> $DEST"
