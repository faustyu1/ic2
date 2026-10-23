#!/usr/bin/env bash
# Builds the mod, uploads it to CurseForge and tags the release v<mod_version> (see release-tag.sh).
#
# Needs CURSEFORGE_TOKEN in the environment (never commit it) and curseforge_project_id set in
# gradle.properties. The release notes come from the mod_version section of CHANGELOG.md. Pass
# --dry-run to build and check the upload without sending it (no tag is created then).
set -euo pipefail
cd "$(dirname "$0")/.."

args=()
dry_run=
if [[ "${1:-}" == "--dry-run" ]]; then
    args+=(-PdryRun)
    dry_run=1
elif [[ -z "${CURSEFORGE_TOKEN:-}" ]]; then
    echo "publish-curseforge: CURSEFORGE_TOKEN is not set" >&2
    exit 1
else
    scripts/release-tag.sh check
fi

./gradlew build publishCurseforge ${args[@]+"${args[@]}"}

if [[ -z "$dry_run" ]]; then
    scripts/release-tag.sh create
fi
