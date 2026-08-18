#!/bin/bash
set -euo pipefail

# Cleans up old image tags on the self-hosted Docker Registry v2
# (registry.ingoschindler.de). Keeps the last N tagged versions per
# repository plus any "*latest" alias tags, deletes the rest, then
# runs garbage-collect to reclaim blob storage.
#
# Requires: curl, jq
# Auth: REGISTRY_USER / REGISTRY_PASSWORD env vars
# Requires REGISTRY_STORAGE_DELETE_ENABLED=true on the registry server,
# otherwise the DELETE calls below will fail with 405.

REGISTRY="${REGISTRY:-registry.ingoschindler.de}"
KEEP="${KEEP:-3}"
DRY_RUN="${DRY_RUN:-false}"

: "${REGISTRY_USER:?REGISTRY_USER env var required}"
: "${REGISTRY_PASSWORD:?REGISTRY_PASSWORD env var required}"

api() {
    curl -fsS -u "$REGISTRY_USER:$REGISTRY_PASSWORD" "$@"
}

echo "Registry: $REGISTRY"
echo "Keep per repo: $KEEP (+ any *latest tags)"
echo "Dry run: $DRY_RUN"
echo ""

repos=$(api "https://$REGISTRY/v2/_catalog?n=1000" | jq -r '.repositories[]')

for repo in $repos; do
    echo "== $repo =="
    tags=$(api "https://$REGISTRY/v2/$repo/tags/list" | jq -r '.tags // [] | .[]')
    if [ -z "$tags" ]; then
        echo "  no tags, skip"
        continue
    fi

    latest_tags=$(echo "$tags" | grep -E 'latest$' || true)
    versioned_tags=$(echo "$tags" | grep -vE 'latest$' || true)

    # Tag format is <prefix>-YYYY.MM.DD-<sha>, so lexical sort == chronological sort.
    sorted_versioned=$(echo "$versioned_tags" | sort)
    to_delete=$(echo "$sorted_versioned" | head -n -"$KEEP")
    to_keep=$(echo "$sorted_versioned" | tail -n "$KEEP")

    if [ -z "$to_delete" ]; then
        echo "  nothing to delete (<= $KEEP versioned tags)"
        continue
    fi

    echo "  keeping: $(echo "$latest_tags $to_keep" | tr '\n' ' ')"
    for tag in $to_delete; do
        digest=$(api -H "Accept: application/vnd.docker.distribution.manifest.v2+json" \
            -H "Accept: application/vnd.oci.image.manifest.v1+json" \
            -H "Accept: application/vnd.docker.distribution.manifest.list.v2+json" \
            --head "https://$REGISTRY/v2/$repo/manifests/$tag" \
            | grep -i '^docker-content-digest:' | tr -d '\r' | awk '{print $2}')

        if [ -z "$digest" ]; then
            echo "  [$tag] could not resolve digest, skip"
            continue
        fi

        if [ "$DRY_RUN" = "true" ]; then
            echo "  [$tag -> $digest] would delete"
        else
            echo "  [$tag -> $digest] deleting..."
            api -X DELETE "https://$REGISTRY/v2/$repo/manifests/$digest" || echo "  delete failed for $tag"
        fi
    done
done

echo ""
if [ "$DRY_RUN" = "true" ]; then
    echo "Dry run complete — no blobs reclaimed yet. Re-run without DRY_RUN=true to delete, then GC on the registry host:"
else
    echo "Manifests unlinked. Reclaim blob storage on the registry host:"
fi
echo "  registry garbage-collect /etc/docker/registry/config.yml"
