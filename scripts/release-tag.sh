#!/usr/bin/env bash
# Checks or creates the git tag v<mod_version> for the release being published.
#
#   scripts/release-tag.sh check   - fails if the working tree has uncommitted changes, or if the tag
#                                    already exists on another commit (the version was released before)
#   scripts/release-tag.sh create  - tags HEAD as v<mod_version> unless it is already tagged
#
# The tag is created locally; push it with `git push origin v<mod_version>`.
set -euo pipefail
cd "$(dirname "$0")/.."

version=$(sed -n 's/^mod_version=//p' gradle.properties)
tag="v$version"
head=$(git rev-parse HEAD)
tagged=$(git rev-parse -q --verify "refs/tags/$tag^{commit}" || true)

case "${1:-}" in
    check)
        if [[ -n "$(git status --porcelain)" ]]; then
            echo "release-tag: commit your changes first, the release must match $tag" >&2
            exit 1
        fi
        if [[ -n "$tagged" && "$tagged" != "$head" ]]; then
            echo "release-tag: $tag already exists on another commit; bump mod_version" >&2
            exit 1
        fi
        ;;
    create)
        if [[ -z "$tagged" ]]; then
            git tag -a "$tag" -m "IC2 Community Edition $version"
            echo "release-tag: tagged $tag; push it with: git push origin $tag"
        fi
        ;;
    *)
        echo "usage: $0 check|create" >&2
        exit 2
        ;;
esac
