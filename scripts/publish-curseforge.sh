#!/usr/bin/env bash
# Builds the mod and uploads it to CurseForge.
#
# Needs CURSEFORGE_TOKEN in the environment (never commit it) and curseforge_project_id set in
# gradle.properties. Optional: CHANGELOG (release notes text). Pass --dry-run to build and check the
# upload without sending it.
set -euo pipefail
cd "$(dirname "$0")/.."

args=()
if [[ "${1:-}" == "--dry-run" ]]; then
    args+=(-PdryRun)
elif [[ -z "${CURSEFORGE_TOKEN:-}" ]]; then
    echo "publish-curseforge: CURSEFORGE_TOKEN is not set" >&2
    exit 1
fi

./gradlew build publishCurseforge ${args[@]+"${args[@]}"}
